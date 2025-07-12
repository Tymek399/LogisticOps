package pl.logistic.logisticops.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateInfrastructureRequestDTO {

    private String externalId;

    @NotNull(message = "Infrastructure name is required")
    @Size(min = 2, max = 200, message = "Name must be between 2 and 200 characters")
    private String name;

    @NotNull(message = "Infrastructure type is required")
    private String type; // BRIDGE, TUNNEL, WEIGHT_STATION, etc.

    @NotNull(message = "Latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @NotNull(message = "Longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    private String roadNumber;

    @Min(value = 100, message = "Max height must be at least 100 cm")
    @Max(value = 1000, message = "Max height cannot exceed 1000 cm")
    private Integer maxHeightCm;

    @Min(value = 1000, message = "Max weight must be at least 1000 kg")
    @Max(value = 100000, message = "Max weight cannot exceed 100000 kg")
    private Integer maxWeightKg;

    @Min(value = 1000, message = "Max axle weight must be at least 1000 kg")
    @Max(value = 20000, message = "Max axle weight cannot exceed 20000 kg")
    private Integer maxAxleWeightKg;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
}
