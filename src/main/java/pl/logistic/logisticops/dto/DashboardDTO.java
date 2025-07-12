package pl.logistic.logisticops.dto;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDTO {
    private TransportStatisticsDTO transportStats;
    private List<AlertDTO> recentAlerts;
    private List<TransportDTO> activeTransports;
    private List<MissionDTO> activeMissions;
    private List<InfrastructureDTO> problematicInfrastructure;
    private Long totalVehicles;
    private Long availableVehicles;
}