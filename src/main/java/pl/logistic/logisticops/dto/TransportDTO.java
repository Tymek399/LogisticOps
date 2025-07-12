package pl.logistic.logisticops.dto;

import lombok.*;
import pl.logistic.logisticops.enums.TransportStatus;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportDTO {
    private Long id;
    private String transportNumber;
    private String name;
    private TransportStatus status;
    private Long missionId;
    private String missionName;
    private Long approvedRouteId;
    private Double currentLatitude;
    private Double currentLongitude;
    private Double progressPercentage;
    private Double distanceCoveredKm;
    private Double distanceRemainingKm;
    private LocalDateTime plannedDeparture;
    private LocalDateTime actualDeparture;
    private LocalDateTime estimatedArrival;
    private LocalDateTime actualArrival;
    private Long createdByUserId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<TransportVehicleDTO> vehicles;
}