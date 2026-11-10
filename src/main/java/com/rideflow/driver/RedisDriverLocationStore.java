package com.rideflow.driver;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Redis-backed geospatial implementation of {@link DriverLocationStore}.
 * Stores coordinates in a Redis Geospatial index (GEOADD / GEOSEARCH) and stores detailed
 * driver location state with a TTL for automatic expiration.
 *
 * Automatically falls back to an in-memory store if Redis is disabled or unavailable.
 */
@Component
@Primary
public class RedisDriverLocationStore implements DriverLocationStore {

    private static final Logger log = LoggerFactory.getLogger(RedisDriverLocationStore.class);

    private final InMemoryDriverLocationStore inMemoryFallback;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rideflow.redis.enabled:true}")
    private boolean redisEnabled = true;

    @Value("${rideflow.redis.driver-geo-key:rideflow:drivers:geo}")
    private String geoKey = "rideflow:drivers:geo";

    @Value("${rideflow.redis.driver-loc-key-prefix:rideflow:drivers:loc:}")
    private String locKeyPrefix = "rideflow:drivers:loc:";

    @Value("${rideflow.redis.location-ttl-seconds:600}")
    private long locationTtlSeconds = 600;

    public RedisDriverLocationStore(InMemoryDriverLocationStore inMemoryFallback,
                                    @Autowired(required = false) StringRedisTemplate redisTemplate,
                                    ObjectMapper objectMapper) {
        this.inMemoryFallback = inMemoryFallback;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void setRedisEnabled(boolean redisEnabled) {
        this.redisEnabled = redisEnabled;
    }

    public boolean isRedisEnabled() {
        return redisEnabled;
    }

    public void setLocationTtlSeconds(long locationTtlSeconds) {
        this.locationTtlSeconds = locationTtlSeconds;
    }

    public long getLocationTtlSeconds() {
        return locationTtlSeconds;
    }

    public void setGeoKey(String geoKey) {
        this.geoKey = geoKey;
    }

    public String getGeoKey() {
        return geoKey;
    }

    public void setLocKeyPrefix(String locKeyPrefix) {
        this.locKeyPrefix = locKeyPrefix;
    }

    public String getLocKeyPrefix() {
        return locKeyPrefix;
    }

    @Override
    public void saveLocation(DriverLocation location) {
        if (location == null || location.getDriverId() == null) {
            throw new IllegalArgumentException("Location and driverId cannot be null");
        }

        // Always keep in-memory store consistent
        inMemoryFallback.saveLocation(location);

        if (redisEnabled && redisTemplate != null) {
            try {
                // Redis expects Point(longitude, latitude) -> X is longitude, Y is latitude
                Point point = new Point(location.getLongitude(), location.getLatitude());
                redisTemplate.opsForGeo().add(geoKey, point, location.getDriverId().toString());

                String json = objectMapper.writeValueAsString(location);
                redisTemplate.opsForValue().set(
                        locKeyPrefix + location.getDriverId(),
                        json,
                        Duration.ofSeconds(locationTtlSeconds)
                );
            } catch (Exception e) {
                log.warn("Redis operation failed in saveLocation for driver {}; relying on in-memory store. Error: {}",
                        location.getDriverId(), e.getMessage());
            }
        }
    }

    @Override
    public Optional<DriverLocation> getLocation(UUID driverId) {
        if (driverId == null) {
            return Optional.empty();
        }

        if (redisEnabled && redisTemplate != null) {
            try {
                String json = redisTemplate.opsForValue().get(locKeyPrefix + driverId);
                if (json != null) {
                    return Optional.of(objectMapper.readValue(json, DriverLocation.class));
                }
                // If key is missing from Redis (e.g. expired or not present), check fallback
                return inMemoryFallback.getLocation(driverId);
            } catch (Exception e) {
                log.warn("Redis operation failed in getLocation for driver {}; falling back to in-memory store. Error: {}",
                        driverId, e.getMessage());
                return inMemoryFallback.getLocation(driverId);
            }
        }

        return inMemoryFallback.getLocation(driverId);
    }

    @Override
    public void removeLocation(UUID driverId) {
        if (driverId == null) {
            return;
        }

        inMemoryFallback.removeLocation(driverId);

        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.opsForGeo().remove(geoKey, driverId.toString());
                redisTemplate.delete(locKeyPrefix + driverId);
            } catch (Exception e) {
                log.warn("Redis operation failed in removeLocation for driver {}. Error: {}", driverId, e.getMessage());
            }
        }
    }

    @Override
    public void clear() {
        inMemoryFallback.clear();

        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.delete(geoKey);
                Set<String> keys = redisTemplate.keys(locKeyPrefix + "*");
                if (keys != null && !keys.isEmpty()) {
                    redisTemplate.delete(keys);
                }
            } catch (Exception e) {
                log.warn("Redis operation failed in clear. Error: {}", e.getMessage());
            }
        }
    }

    @Override
    public Collection<DriverLocation> getAllLocations() {
        if (redisEnabled && redisTemplate != null) {
            try {
                Set<String> keys = redisTemplate.keys(locKeyPrefix + "*");
                if (keys != null && !keys.isEmpty()) {
                    List<DriverLocation> locations = new ArrayList<>();
                    for (String key : keys) {
                        String json = redisTemplate.opsForValue().get(key);
                        if (json != null) {
                            locations.add(objectMapper.readValue(json, DriverLocation.class));
                        }
                    }
                    if (!locations.isEmpty()) {
                        return locations;
                    }
                }
            } catch (Exception e) {
                log.warn("Redis operation failed in getAllLocations; falling back to in-memory store. Error: {}", e.getMessage());
            }
        }

        return inMemoryFallback.getAllLocations();
    }

    @Override
    public Collection<DriverLocation> findLocationsNear(double latitude, double longitude, double radiusKm) {
        if (redisEnabled && redisTemplate != null) {
            try {
                Point searchPoint = new Point(longitude, latitude);
                Distance searchRadius = new Distance(radiusKm, Metrics.KILOMETERS);

                GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().search(
                        geoKey,
                        GeoReference.fromCoordinate(searchPoint),
                        searchRadius,
                        RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs().includeDistance().sortAscending()
                );

                if (results != null) {
                    List<DriverLocation> nearbyLocations = new ArrayList<>();
                    for (GeoResult<RedisGeoCommands.GeoLocation<String>> geoResult : results) {
                        String driverIdStr = geoResult.getContent().getName();
                        String locJson = redisTemplate.opsForValue().get(locKeyPrefix + driverIdStr);

                        if (locJson != null) {
                            try {
                                DriverLocation loc = objectMapper.readValue(locJson, DriverLocation.class);
                                nearbyLocations.add(loc);
                            } catch (JsonProcessingException parseEx) {
                                log.error("Failed to parse DriverLocation JSON for driver {}", driverIdStr, parseEx);
                            }
                        } else {
                            // Location key expired in Redis; clean up stale geo member
                            redisTemplate.opsForGeo().remove(geoKey, driverIdStr);
                        }
                    }
                    return nearbyLocations;
                }
            } catch (Exception e) {
                log.warn("Redis geospatial search failed for point ({}, {}); falling back to in-memory store. Error: {}",
                        latitude, longitude, e.getMessage());
            }
        }

        return inMemoryFallback.findLocationsNear(latitude, longitude, radiusKm);
    }
}
