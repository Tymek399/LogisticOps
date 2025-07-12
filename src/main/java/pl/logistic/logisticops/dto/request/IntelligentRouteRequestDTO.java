package pl.logistic.logisticops.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IntelligentRouteRequestDTO {

    @NotNull(message = "Start latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double startLatitude;

    @NotNull(message = "Start longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double startLongitude;

    @NotNull(message = "End latitude is required")
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double endLatitude;

    @NotNull(message = "End longitude is required")
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double endLongitude;

    private String startAddress;
    private String endAddress;

    @NotNull(message = "Mission ID is required")
    private Long missionId;

    @NotEmpty(message = "At least one vehicle is required")
    private List<Long> vehicleIds;

    private LocalDateTime plannedDeparture;

    private Boolean avoidRestrictions = true;
    private Boolean includeAlternatives = true;
}