package pl.logistic.logisticops.dto.request;

import lombok.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTransportLocationRequestDTO {

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @Min(value = 0, message = "Speed cannot be negative")
    @Max(value = 200, message = "Speed cannot exceed 200 km/h")
    private Double speedKmh;

    @Min(value = 0, message = "Heading must be between 0 and 359 degrees")
    @Max(value = 359, message = "Heading must be between 0 and 359 degrees")
    private Integer headingDegrees;

    @DecimalMin(value = "0.0", message = "Fuel level cannot be negative")
    @DecimalMax(value = "100.0", message = "Fuel level cannot exceed 100%")
    private Double fuelLevelPercentage;

    @DecimalMin(value = "0.0", message = "Progress cannot be negative")
    @DecimalMax(value = "100.0", message = "Progress cannot exceed 100%")
    private Double progressPercentage;

    private String sensorData;
}
