package com.rideflow.driver;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Thread-safe in-memory implementation of {@link DriverLocationStore} backed by a {@link ConcurrentHashMap}.
 */
@Component
public class InMemoryDriverLocationStore implements DriverLocationStore {

    private final ConcurrentMap<UUID, DriverLocation> store = new ConcurrentHashMap<>();

    @Override
    public void saveLocation(DriverLocation location) {
        if (location == null || location.getDriverId() == null) {
            throw new IllegalArgumentException("Location and driverId cannot be null");
        }
        store.put(location.getDriverId(), location);
    }

    @Override
    public Optional<DriverLocation> getLocation(UUID driverId) {
        if (driverId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(store.get(driverId));
    }

    @Override
    public void removeLocation(UUID driverId) {
        if (driverId != null) {
            store.remove(driverId);
        }
    }

    @Override
    public void clear() {
        store.clear();
    }

    @Override
    public java.util.Collection<DriverLocation> getAllLocations() {
        return new java.util.ArrayList<>(store.values());
    }
}
