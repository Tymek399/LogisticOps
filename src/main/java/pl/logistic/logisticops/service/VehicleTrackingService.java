package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.dto.VehicleTrackingDTO;
import pl.logistic.logisticops.enums.AlertLevel;
import pl.logistic.logisticops.mapper.VehicleTrackingMapper;
import pl.logistic.logisticops.model.*;
import pl.logistic.logisticops.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VehicleTrackingService {

    private final VehicleTrackingRepository trackingRepository;
    private final TransportRepository transportRepository;
    private final VehicleSpecificationRepository vehicleRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AlertService alertService;
    private final VehicleTrackingMapper trackingMapper;

    public VehicleTrackingDTO updateVehiclePosition(Long transportId, Long vehicleId,
                                                    Double latitude, Double longitude,
                                                    Double speed, Integer heading,
                                                    Double fuelLevel, String sensorData) {

        Transport transport = transportRepository.findById(transportId)
                .orElseThrow(() -> new IllegalArgumentException("Transport not found"));

        VehicleSpecification vehicle = vehicleRepository.findById(vehicleId)
                .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

        VehicleTracking tracking = VehicleTracking.builder()
                .transport(transport)
                .vehicle(vehicle)
                .latitude(latitude)
                .longitude(longitude)
                .speedKmh(speed)
                .headingDegrees(heading)
                .fuelLevelPercentage(fuelLevel)
                .sensorData(sensorData)
                .recordedAt(LocalDateTime.now())
                .build();

        tracking = trackingRepository.save(tracking);

        VehicleTrackingDTO dto = trackingMapper.toDTO(tracking);

        // Send real-time update
        messagingTemplate.convertAndSend("/topic/vehicle/" + vehicleId + "/tracking", dto);
        messagingTemplate.convertAndSend("/topic/transport/" + transportId + "/tracking", dto);

        // Check for alerts
        checkTrackingAlerts(tracking, transportId);

        return dto;
    }

    private void checkTrackingAlerts(VehicleTracking tracking, Long transportId) {
        // Check fuel level
        if (tracking.getFuelLevelPercentage() != null && tracking.getFuelLevelPercentage() < 20) {
            alertService.createAlert(
                    "Niski poziom paliwa: " + tracking.getFuelLevelPercentage() + "%",
                    AlertLevel.MEDIUM,
                    transportId,
                    null,
                    "FUEL"
            );
        }

        // Check speed
        if (tracking.getSpeedKmh() != null && tracking.getSpeedKmh() > 90) {
            alertService.createAlert(
                    "Przekroczenie prędkości: " + tracking.getSpeedKmh() + " km/h",
                    AlertLevel.MEDIUM,
                    transportId,
                    null,
                    "SPEED"
            );
        }
    }

    public List<VehicleTrackingDTO> getVehicleHistory(Long vehicleId, LocalDateTime from, LocalDateTime to) {
        return trackingRepository.findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtDesc(
                        vehicleId, from, to)
                .stream()
                .map(trackingMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<VehicleTrackingDTO> getTransportTracking(Long transportId) {
        return trackingRepository.findLatestByTransportId(transportId)
                .stream()
                .map(trackingMapper::toDTO)
                .collect(Collectors.toList());
    }
}
