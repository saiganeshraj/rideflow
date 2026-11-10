package com.rideflow.driver;

import java.util.Optional;
import java.util.UUID;

/**
 * Interface defining contract for storing and querying driver real-time locations.
 */
public interface DriverLocationStore {

    /**
     * Stores or updates the location for a driver.
     *
     * @param location the driver location details
     */
    void saveLocation(DriverLocation location);

    /**
     * Retrieves the latest reported location for the specified driver ID.
     *
     * @param driverId the UUID of the driver
     * @return Optional containing the DriverLocation if present
     */
    Optional<DriverLocation> getLocation(UUID driverId);

    /**
     * Removes the location entry for the specified driver ID.
     *
     * @param driverId the UUID of the driver
     */
    void removeLocation(UUID driverId);

    /**
     * Clears all stored driver locations.
     */
    void clear();

    /**
     * Retrieves all currently stored driver locations.
     *
     * @return collection of all current driver locations
     */
    java.util.Collection<DriverLocation> getAllLocations();

    /**
     * Finds driver locations within a given radius (in kilometers) from the target coordinates.
     * Implementations may use geospatial indexing (e.g. Redis GEO) or in-memory distance calculations.
     *
     * @param latitude  center latitude (-90.0 to 90.0)
     * @param longitude center longitude (-180.0 to 180.0)
     * @param radiusKm  search radius in kilometers
     * @return collection of driver locations located within the search radius
     */
    default java.util.Collection<DriverLocation> findLocationsNear(double latitude, double longitude, double radiusKm) {
        return getAllLocations();
    }
}
