package com.rideflow.driver;

import java.time.Instant;
import java.util.UUID;

/**
 * Public response representation of vehicle details.
 */
public class VehicleResponse {

    private UUID id;
    private UUID driverId;
    private VehicleType vehicleType;
    private String make;
    private String model;
    private String registrationNumber;
    private String color;
    private Instant createdAt;
    private Instant updatedAt;

    public VehicleResponse() {
    }

    public VehicleResponse(UUID id, UUID driverId, VehicleType vehicleType, String make, String model,
                           String registrationNumber, String color, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.driverId = driverId;
        this.vehicleType = vehicleType;
        this.make = make;
        this.model = model;
        this.registrationNumber = registrationNumber;
        this.color = color;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static VehicleResponse from(Vehicle vehicle) {
        if (vehicle == null) {
            return null;
        }
        return new VehicleResponse(
                vehicle.getId(),
                vehicle.getDriver() != null ? vehicle.getDriver().getId() : null,
                vehicle.getVehicleType(),
                vehicle.getMake(),
                vehicle.getModel(),
                vehicle.getRegistrationNumber(),
                vehicle.getColor(),
                vehicle.getCreatedAt(),
                vehicle.getUpdatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDriverId() {
        return driverId;
    }

    public void setDriverId(UUID driverId) {
        this.driverId = driverId;
    }

    public VehicleType getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(VehicleType vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getMake() {
        return make;
    }

    public void setMake(String make) {
        this.make = make;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
