package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.Model.VehicleTracking;
import pl.logistic.logisticops.repository.VehicleTrackingRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VehicleTrackingService {

    private final VehicleTrackingRepository trackingRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AlertService alertService;

    public VehicleTracking updateVehiclePosition(Long transportId, Long vehicleId,
                                                 Double latitude, Double longitude,
                                                 Double speed, Integer heading,
                                                 Double fuelLevel, String sensorData) {

        VehicleTracking tracking = VehicleTracking.builder()
                .latitude(latitude)
                .longitude(longitude)
                .speedKmh(speed)
                .headingDegrees(heading)
                .fuelLevelPercentage(fuelLevel)
                .sensorData(sensorData)
                .recordedAt(LocalDateTime.now())
                .build();

        tracking = trackingRepository.save(tracking);

        // Send real-time update
        messagingTemplate.convertAndSend("/topic/vehicle/" + vehicleId + "/tracking", tracking);
        messagingTemplate.convertAndSend("/topic/transport/" + transportId + "/tracking", tracking);

        // Check for alerts
        checkTrackingAlerts(tracking, transportId);

        return tracking;
    }

    private void checkTrackingAlerts(VehicleTracking tracking, Long transportId) {
        // Check fuel level
        if (tracking.getFuelLevelPercentage() != null && tracking.getFuelLevelPercentage() < 20) {
            alertService.createAlert(
                    "Niski poziom paliwa: " + tracking.getFuelLevelPercentage() + "%",
                    AlertLevel.Medium,
                    transportId,
                    "FUEL"
            );
        }

        // Check speed
        if (tracking.getSpeedKmh() != null && tracking.getSpeedKmh() > 90) {
            alertService.createAlert(
                    "Przekroczenie prędkości: " + tracking.getSpeedKmh() + " km/h",
                    AlertLevel.Medium,
                    transportId,
                    "SPEED"
            );
        }
    }

    public List<VehicleTracking> getVehicleHistory(Long vehicleId, LocalDateTime from, LocalDateTime to) {
        return trackingRepository.findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                vehicleId, from, to);
    }

    public Map<String, Object> getCurrentPositions(Long transportId) {
        List<VehicleTracking> currentPositions = trackingRepository.findLatestByTransportId(transportId);

        return Map.of(
                "transportId", transportId,
                "vehicles", currentPositions,
                "timestamp", LocalDateTime.now()
        );
    }
}
