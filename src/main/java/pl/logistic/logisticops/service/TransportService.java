package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.logistic.logisticops.Model.*;
import pl.logistic.logisticops.enums.TransportStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class TransportService {

    private final TransportRepository transportRepository;
    private final TransportVehicleRepository transportVehicleRepository;
    private final VehicleTrackingRepository vehicleTrackingRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public Transport createTransport(String name, Long missionId, Long routeId,
                                     List<Long> vehicleIds, Long userId) {
        Transport transport = Transport.builder()
                .name(name)
                .transportNumber(generateTransportNumber())
                .status(TransportStatus.PLANNED)
                .createdByUserId(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .progressPercentage(0.0)
                .build();

        transport = transportRepository.save(transport);

        // Add vehicles to transport
        for (int i = 0; i < vehicleIds.size(); i++) {
            TransportVehicle tv = TransportVehicle.builder()
                    .transport(transport)
                    .sequenceOrder(i)
                    .build();
            transportVehicleRepository.save(tv);
        }

        return transport;
    }

    public List<Transport> getActiveTransports() {
        return transportRepository.findByStatusIn(List.of(
                TransportStatus.APPROVED,
                TransportStatus.READY_TO_DEPART,
                TransportStatus.IN_TRANSIT,
                TransportStatus.DELAYED,
                TransportStatus.STOPPED
        ));
    }

    public Optional<Transport> getTransportById(Long id) {
        return transportRepository.findById(id);
    }

    public Transport updateTransportStatus(Long transportId, TransportStatus status) {
        Transport transport = transportRepository.findById(transportId)
                .orElseThrow(() -> new IllegalArgumentException("Transport not found"));

        transport.setStatus(status);
        transport.setUpdatedAt(LocalDateTime.now());

        if (status == TransportStatus.IN_TRANSIT && transport.getActualDeparture() == null) {
            transport.setActualDeparture(LocalDateTime.now());
        }

        if (status == TransportStatus.ARRIVED && transport.getActualArrival() == null) {
            transport.setActualArrival(LocalDateTime.now());
            transport.setProgressPercentage(100.0);
        }

        transport = transportRepository.save(transport);

        // Send WebSocket update
        messagingTemplate.convertAndSend("/topic/transport/" + transportId, transport);
        messagingTemplate.convertAndSend("/topic/transports/all", transport);

        return transport;
    }

    public Transport updateTransportLocation(Long transportId, Double latitude,
                                             Double longitude, Double progress) {
        Transport transport = transportRepository.findById(transportId)
                .orElseThrow(() -> new IllegalArgumentException("Transport not found"));

        transport.setCurrentLatitude(latitude);
        transport.setCurrentLongitude(longitude);
        transport.setProgressPercentage(progress);
        transport.setUpdatedAt(LocalDateTime.now());

        // Calculate remaining distance based on progress
        if (transport.getApprovedRoute() != null) {
            Double totalDistance = transport.getApprovedRoute().getTotalDistanceKm();
            if (totalDistance != null) {
                transport.setDistanceCoveredKm(totalDistance * progress / 100);
                transport.setDistanceRemainingKm(totalDistance * (100 - progress) / 100);
            }
        }

        transport = transportRepository.save(transport);

        // Send real-time update
        messagingTemplate.convertAndSend("/topic/transport/" + transportId + "/location",
                Map.of("latitude", latitude, "longitude", longitude, "progress", progress));

        return transport;
    }

    private String generateTransportNumber() {
        return "TR-" + System.currentTimeMillis();
    }
}