package com.rideflow.driver;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.domain.geo.GeoReference;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisDriverLocationStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private GeoOperations<String, String> geoOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private InMemoryDriverLocationStore inMemoryFallback;
    private ObjectMapper objectMapper;
    private RedisDriverLocationStore store;

    private final UUID driverId = UUID.randomUUID();
    private final double lat = 12.9772;
    private final double lon = 77.5708;

    @BeforeEach
    void setUp() {
        inMemoryFallback = new InMemoryDriverLocationStore();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        lenient().when(redisTemplate.opsForGeo()).thenReturn(geoOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        store = new RedisDriverLocationStore(inMemoryFallback, redisTemplate, objectMapper);
        store.setRedisEnabled(true);
        store.setLocationTtlSeconds(600);
        store.setGeoKey("rideflow:drivers:geo");
        store.setLocKeyPrefix("rideflow:drivers:loc:");
    }

    // ==================== 1. Location Updates & Retrieval ====================

    @Test
    void testSaveLocation_storesInRedisGeoAndValueWithTtl() throws Exception {
        DriverLocation loc = new DriverLocation(driverId, lat, lon, 90.0, 30.0, Instant.now());

        store.saveLocation(loc);

        // Verify GEOADD Point(lon, lat)
        verify(geoOperations).add(eq("rideflow:drivers:geo"), eq(new Point(lon, lat)), eq(driverId.toString()));
        // Verify Value SET with 600s TTL
        verify(valueOperations).set(eq("rideflow:drivers:loc:" + driverId), anyString(), eq(Duration.ofSeconds(600)));

        // Verify in-memory consistency
        Optional<DriverLocation> inMem = inMemoryFallback.getLocation(driverId);
        assertTrue(inMem.isPresent());
        assertEquals(lat, inMem.get().getLatitude());
    }

    @Test
    void testGetLocation_readsFromRedisWhenPresent() throws Exception {
        DriverLocation loc = new DriverLocation(driverId, lat, lon, 90.0, 30.0, Instant.now());
        String json = objectMapper.writeValueAsString(loc);

        when(valueOperations.get("rideflow:drivers:loc:" + driverId)).thenReturn(json);

        Optional<DriverLocation> result = store.getLocation(driverId);

        assertTrue(result.isPresent());
        assertEquals(driverId, result.get().getDriverId());
        assertEquals(lat, result.get().getLatitude());
        assertEquals(lon, result.get().getLongitude());
    }

    // ==================== 2. Geospatial Radius Filtering ====================

    @Test
    void testFindLocationsNear_performsRedisGeoSearchAndReturnsDrivers() throws Exception {
        DriverLocation loc = new DriverLocation(driverId, lat, lon, 90.0, 30.0, Instant.now());
        String json = objectMapper.writeValueAsString(loc);

        RedisGeoCommands.GeoLocation<String> member = new RedisGeoCommands.GeoLocation<>(driverId.toString(), new Point(lon, lat));
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult = new GeoResult<>(member, new Distance(0.5, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(List.of(geoResult));

        when(geoOperations.search(eq("rideflow:drivers:geo"), any(GeoReference.class), any(Distance.class), any()))
                .thenReturn(geoResults);
        when(valueOperations.get("rideflow:drivers:loc:" + driverId)).thenReturn(json);

        Collection<DriverLocation> nearby = store.findLocationsNear(lat, lon, 5.0);

        assertEquals(1, nearby.size());
        assertEquals(driverId, nearby.iterator().next().getDriverId());
    }

    // ==================== 3. Expiration Handling ====================

    @Test
    void testExpiration_whenLocationTtlExpires_excludedAndCleanedFromGeoIndex() {
        // Geo index still has the member, but value key expired (returns null)
        RedisGeoCommands.GeoLocation<String> member = new RedisGeoCommands.GeoLocation<>(driverId.toString(), new Point(lon, lat));
        GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult = new GeoResult<>(member, new Distance(0.5, Metrics.KILOMETERS));
        GeoResults<RedisGeoCommands.GeoLocation<String>> geoResults = new GeoResults<>(List.of(geoResult));

        when(geoOperations.search(eq("rideflow:drivers:geo"), any(GeoReference.class), any(Distance.class), any()))
                .thenReturn(geoResults);
        when(valueOperations.get("rideflow:drivers:loc:" + driverId)).thenReturn(null);

        Collection<DriverLocation> nearby = store.findLocationsNear(lat, lon, 5.0);

        // Expired driver should be excluded
        assertTrue(nearby.isEmpty());
        // Clean up from geo index
        verify(geoOperations).remove("rideflow:drivers:geo", driverId.toString());
    }

    // ==================== 4. Redis-Unavailable Graceful Fallback ====================

    @Test
    void testRedisUnavailable_onSaveLocation_fallsBackToInMemoryGracefully() {
        DriverLocation loc = new DriverLocation(driverId, lat, lon, 90.0, 30.0, Instant.now());

        when(geoOperations.add(anyString(), any(Point.class), anyString()))
                .thenThrow(new RedisConnectionFailureException("Connection refused: localhost/127.0.0.1:6379"));

        // Must not throw an exception to caller
        store.saveLocation(loc);

        // In-memory fallback still contains the location
        Optional<DriverLocation> fallbackLoc = inMemoryFallback.getLocation(driverId);
        assertTrue(fallbackLoc.isPresent());
        assertEquals(lat, fallbackLoc.get().getLatitude());
    }

    @Test
    void testRedisUnavailable_onGetLocation_fallsBackToInMemoryGracefully() {
        DriverLocation loc = new DriverLocation(driverId, lat, lon, 90.0, 30.0, Instant.now());
        inMemoryFallback.saveLocation(loc);

        when(valueOperations.get(anyString()))
                .thenThrow(new QueryTimeoutException("Redis read timed out"));

        Optional<DriverLocation> result = store.getLocation(driverId);

        assertTrue(result.isPresent());
        assertEquals(driverId, result.get().getDriverId());
    }

    @Test
    void testRedisUnavailable_onFindLocationsNear_fallsBackToInMemoryGracefully() {
        DriverLocation loc = new DriverLocation(driverId, lat, lon, 90.0, 30.0, Instant.now());
        inMemoryFallback.saveLocation(loc);

        when(geoOperations.search(anyString(), any(GeoReference.class), any(Distance.class), any()))
                .thenThrow(new RedisConnectionFailureException("Cluster down"));

        Collection<DriverLocation> results = store.findLocationsNear(lat, lon, 5.0);

        assertNotNull(results);
        assertEquals(1, results.size());
        assertEquals(driverId, results.iterator().next().getDriverId());
    }

    // ==================== 5. Removal & Clear ====================

    @Test
    void testRemoveLocation_cleansFromBothStores() {
        DriverLocation loc = new DriverLocation(driverId, lat, lon, 90.0, 30.0, Instant.now());
        inMemoryFallback.saveLocation(loc);

        store.removeLocation(driverId);

        verify(geoOperations).remove("rideflow:drivers:geo", driverId.toString());
        verify(redisTemplate).delete("rideflow:drivers:loc:" + driverId);
        assertFalse(inMemoryFallback.getLocation(driverId).isPresent());
    }
}
