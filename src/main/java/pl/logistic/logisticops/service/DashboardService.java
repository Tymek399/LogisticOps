package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.dto.*;
import pl.logistic.logisticops.enums.MissionStatus;
import pl.logistic.logisticops.enums.TransportStatus;
import pl.logistic.logisticops.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransportRepository transportRepository;
    private final MissionRepository missionRepository;
    private final VehicleSpecificationRepository vehicleRepository;
    private final AlertService alertService;
    private final TransportService transportService;
    private final MissionService missionService;
    private final InfrastructureService infrastructureService;

    public DashboardDTO getDashboardData() {
        return DashboardDTO.builder()
                .transportStats(getTransportStatistics())
                .recentAlerts(alertService.getRecentAlerts(24))
                .activeTransports(transportService.getActiveTransports())
                .activeMissions(getActiveMissions())
                .problematicInfrastructure(getProblematicInfrastructure())
                .totalVehicles(vehicleRepository.count())
                .availableVehicles(vehicleRepository.countByActiveTrue())
                .build();
    }

    public TransportStatisticsDTO getTransportStatistics() {
        Long totalTransports = transportRepository.count();
        Long activeTransports = transportRepository.countByStatus(TransportStatus.IN_TRANSIT);
        Long completedTransports = transportRepository.countByStatus(TransportStatus.COMPLETED);
        Long delayedTransports = transportRepository.countByStatus(TransportStatus.DELAYED);

        return TransportStatisticsDTO.builder()
                .totalTransports(totalTransports)
                .activeTransports(activeTransports)
                .completedTransports(completedTransports)
                .delayedTransports(delayedTransports)
                .averageCompletionTime(calculateAverageCompletionTime())
                .totalDistanceCovered(calculateTotalDistanceCovered())
                .averageFuelConsumption(calculateAverageFuelConsumption())
                .lastUpdated(LocalDateTime.now())
                .build();
    }

    public DashboardDTO refreshDashboardData() {
        // Force refresh of all data
        return getDashboardData();
    }

    private List<MissionDTO> getActiveMissions() {
        return missionRepository.findByStatusInOrderByPriorityDescCreatedAtDesc(
                        List.of(MissionStatus.PLANNED, MissionStatus.ACTIVE))
                .stream()
                .limit(5)
                .map(mission -> MissionDTO.builder()
                        .id(mission.getId())
                        .name(mission.getName())
                        .status(mission.getStatus())
                        .priority(mission.getPriority())
                        .startDate(mission.getStartDate())
                        .createdAt(mission.getCreatedAt())
                        .build())
                .toList();
    }

    private List<InfrastructureDTO> getProblematicInfrastructure() {
        return infrastructureService.getActiveInfrastructure()
                .stream()
                .filter(infra -> infra.getMaxHeightCm() != null && infra.getMaxHeightCm() < 400)
                .limit(5)
                .toList();
    }

    private Double calculateAverageCompletionTime() {
        // Simplified calculation - in real implementation, this would calculate
        // average time between start and completion
        return 8.5; // hours
    }

    private Double calculateTotalDistanceCovered() {
        return transportRepository.findAll()
                .stream()
                .mapToDouble(transport -> transport.getDistanceCoveredKm() != null ? transport.getDistanceCoveredKm() : 0.0)
                .sum();
    }

    private Double calculateAverageFuelConsumption() {
        // Simplified calculation
        return 0.35; // liters per km
    }
}
