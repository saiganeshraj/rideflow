package com.rideflow.driver;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for vehicle registration and updates.
 */
@Schema(description = "Request payload for vehicle details")
public class VehicleRegistrationRequest {

    @NotNull(message = "Vehicle type is required")
    @Schema(description = "Type of vehicle", example = "SEDAN", requiredMode = Schema.RequiredMode.REQUIRED)
    private VehicleType vehicleType;

    @NotBlank(message = "Make is required")
    @Size(min = 1, max = 50, message = "Make must be between 1 and 50 characters")
    @Schema(description = "Vehicle manufacturer", example = "Toyota", requiredMode = Schema.RequiredMode.REQUIRED)
    private String make;

    @NotBlank(message = "Model is required")
    @Size(min = 1, max = 50, message = "Model must be between 1 and 50 characters")
    @Schema(description = "Vehicle model", example = "Camry", requiredMode = Schema.RequiredMode.REQUIRED)
    private String model;

    @NotBlank(message = "Registration number is required")
    @Size(min = 3, max = 20, message = "Registration number must be between 3 and 20 characters")
    @Schema(description = "Unique license plate / vehicle registration number", example = "KA-01-AB-1234", requiredMode = Schema.RequiredMode.REQUIRED)
    private String registrationNumber;

    @NotBlank(message = "Color is required")
    @Size(min = 1, max = 30, message = "Color must be between 1 and 30 characters")
    @Schema(description = "Vehicle color", example = "Silver", requiredMode = Schema.RequiredMode.REQUIRED)
    private String color;

    public VehicleRegistrationRequest() {
    }

    public VehicleRegistrationRequest(VehicleType vehicleType, String make, String model, String registrationNumber, String color) {
        this.vehicleType = vehicleType;
        this.make = make != null ? make.trim() : null;
        this.model = model != null ? model.trim() : null;
        this.registrationNumber = registrationNumber != null ? registrationNumber.trim() : null;
        this.color = color != null ? color.trim() : null;
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
        this.make = make != null ? make.trim() : null;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model != null ? model.trim() : null;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber != null ? registrationNumber.trim() : null;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color != null ? color.trim() : null;
    }
}
