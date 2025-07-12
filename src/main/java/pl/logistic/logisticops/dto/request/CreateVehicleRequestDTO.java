package pl.logistic.logisticops.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;
import pl.logistic.logisticops.enums.VehicleType;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateVehicleRequestDTO {

    @NotNull(message = "Model is required")
    @Size(min = 2, max = 100, message = "Model must be between 2 and 100 characters")
    private String model;

    @NotNull(message = "Vehicle type is required")
    private VehicleType type;

    @Min(value = 1, message = "Total weight must be positive")
    @Max(value = 100000, message = "Total weight cannot exceed 100,000 kg")
    private Integer totalWeightKg;

    @Min(value = 1, message = "Axle count must be positive")
    @Max(value = 20, message = "Axle count cannot exceed 20")
    private Integer axleCount;

    @Min(value = 1, message = "Max axle load must be positive")
    @Max(value = 50000, message = "Max axle load cannot exceed 50,000 kg")
    private Integer maxAxleLoadKg;

    @Min(value = 50, message = "Height must be at least 50 cm")
    @Max(value = 1000, message = "Height cannot exceed 1000 cm")
    private Integer heightCm;

    @Min(value = 100, message = "Length must be at least 100 cm")
    @Max(value = 2000, message = "Length cannot exceed 2000 cm")
    private Integer lengthCm;

    @Min(value = 100, message = "Width must be at least 100 cm")
    @Max(value = 500, message = "Width cannot exceed 500 cm")
    private Integer widthCm;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
}