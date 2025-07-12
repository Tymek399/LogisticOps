package pl.logistic.logisticops.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteProposalDTO {
    private Long id;
    private Long missionId;
    private String missionName;
    private String routeType;
    private Double totalDistanceKm;
    private Double estimatedTimeMinutes;
    private Double fuelConsumptionLiters;
    private Boolean approved;
    private LocalDateTime generatedAt;
    private List<RouteSegmentDTO> segments;
    private List<RouteObstacleDTO> obstacles;
    private Integer obstacleCount;
    private String summary;
}