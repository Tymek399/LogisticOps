package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.logistic.logisticops.dto.*;
import pl.logistic.logisticops.dto.request.*;
import pl.logistic.logisticops.enums.TransportStatus;
import pl.logistic.logisticops.mapper.TransportMapper;
import pl.logistic.logisticops.model.*;
import pl.logistic.logisticops.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class TransportService {

    private final TransportRepository transportRepository;
    private final TransportVehicleRepository transportVehicleRepository;
    private final VehicleSpecificationRepository vehicleRepository;
    private final RouteProposalRepository routeRepository;
    private final MissionRepository missionRepository;
    private final VehicleTrackingService trackingService;
    private final SimpMessagingTemplate messagingTemplate;
    private final TransportMapper transportMapper;

    public Page<TransportDTO> getAllTransports(Pageable pageable) {
        return transportRepository.findAll(pageable)
                .map(transportMapper::toDTO);
    }

    public List<TransportDTO> getActiveTransports() {
        List<TransportStatus> activeStatuses = List.of(
                TransportStatus.APPROVED,
                TransportStatus.READY_TO_DEPART,
                TransportStatus.IN_TRANSIT,
                TransportStatus.DELAYED,
                TransportStatus.STOPPED
        );
        return transportRepository.findByStatusIn(activeStatuses)
                .stream()
                .map(transportMapper::toDTO)
                .collect(Collectors.toList());
    }

    public Optional<TransportDTO> getTransportById(Long id) {
        return transportRepository.findById(id)
                .map(transportMapper::toDTO);
    }

    public List<TransportDTO> getTransportsByMission(Long missionId) {
        return transportRepository.findByMissionId(missionId)
                .stream()
                .map(transportMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<TransportDTO> getTransportsByStatus(TransportStatus status) {
        return transportRepository.findByStatus(status)
                .stream()
                .map(transportMapper::toDTO)
                .collect(Collectors.toList());
    }

    public TransportDTO createTransport(CreateTransportRequestDTO request, Long userId) {
        Mission mission = missionRepository.findById(request.getMissionId())
                .orElseThrow(() -> new IllegalArgumentException("Mission not found"));

        RouteProposal route = null;
        if (request.getRouteProposalId() != null) {
            route = routeRepository.findById(request.getRouteProposalId())
                    .orElseThrow(() -> new IllegalArgumentException("Route proposal not found"));
        }

        Transport transport = Transport.builder()
                .transportNumber(generateTransportNumber())
                .name(request.getName())
                .status(TransportStatus.PLANNED)
                .mission(mission)
                .approvedRoute(route)
                .plannedDeparture(request.getPlannedDeparture())
                .estimatedArrival(calculateEstimatedArrival(route, request.getPlannedDeparture()))
                .progressPercentage(0.0)
                .distanceCoveredKm(0.0)
                .distanceRemainingKm(route != null ? route.getTotalDistanceKm() : null)
                .createdByUserId(userId)
                .build();

        transport = transportRepository.save(transport);

        // Assign vehicles
        for (CreateTransportRequestDTO.VehicleAssignmentDTO vehicleAssignment : request.getVehicles()) {
            VehicleSpecification vehicle = vehicleRepository.findById(vehicleAssignment.getVehicleId())
                    .orElseThrow(() -> new IllegalArgumentException("Vehicle not found"));

            TransportVehicle transportVehicle = TransportVehicle.builder()
                    .transport(transport)
                    .vehicle(vehicle)
                    .role(vehicleAssignment.getRole())
                    .sequenceOrder(vehicleAssignment.getSequenceOrder())
                    .build();

            transportVehicleRepository.save(transportVehicle);
        }

        TransportDTO dto = transportMapper.toDTO(transport);

        // Send WebSocket notification
        messagingTemplate.convertAndSend("/topic/transports/new", dto);

        return dto;
    }

    public TransportDTO updateTransportStatus(Long id, TransportStatus status) {
        Transport transport = transportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transport not found"));

        TransportStatus oldStatus = transport.getStatus();
        transport.setStatus(status);

        // Update timestamps based on status
        switch (status) {
            case IN_TRANSIT:
                if (transport.getActualDeparture() == null) {
                    transport.setActualDeparture(LocalDateTime.now());
                }
                break;
            case ARRIVED:
                if (transport.getActualArrival() == null) {
                    transport.setActualArrival(LocalDateTime.now());
                    transport.setProgressPercentage(100.0);
                }
                break;
            case COMPLETED:
                if (transport.getActualArrival() == null) {
                    transport.setActualArrival(LocalDateTime.now());
                }
                transport.setProgressPercentage(100.0);
                break;
        }

        transport = transportRepository.save(transport);
        TransportDTO dto = transportMapper.toDTO(transport);

        // Send WebSocket notifications
        messagingTemplate.convertAndSend("/topic/transport/" + id + "/status",
                Map.of("oldStatus", oldStatus, "newStatus", status, "transport", dto));
        messagingTemplate.convertAndSend("/topic/transports/all", dto);

        return dto;
    }

    public TransportDTO updateTransportLocation(Long id, UpdateTransportLocationRequestDTO request) {
        Transport transport = transportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transport not found"));

        transport.setCurrentLatitude(request.getLatitude());
        transport.setCurrentLongitude(request.getLongitude());

        if (request.getProgressPercentage() != null) {
            transport.setProgressPercentage(request.getProgressPercentage());

            // Update distance calculations
            if (transport.getApprovedRoute() != null && transport.getApprovedRoute().getTotalDistanceKm() != null) {
                double totalDistance = transport.getApprovedRoute().getTotalDistanceKm();
                double progress = request.getProgressPercentage() / 100.0;
                transport.setDistanceCoveredKm(totalDistance * progress);
                transport.setDistanceRemainingKm(totalDistance * (1 - progress));
            }
        }

        transport = transportRepository.save(transport);

        // Update vehicle tracking for all vehicles in transport
        if (transport.getVehicles() != null) {
            for (TransportVehicle tv : transport.getVehicles()) {
                trackingService.updateVehiclePosition(
                        id,
                        tv.getVehicle().getId(),
                        request.getLatitude(),
                        request.getLongitude(),
                        request.getSpeedKmh(),
                        request.getHeadingDegrees(),
                        request.getFuelLevelPercentage(),
                        request.getSensorData()
                );
            }
        }

        TransportDTO dto = transportMapper.toDTO(transport);

        // Send real-time location update
        messagingTemplate.convertAndSend("/topic/transport/" + id + "/location",
                Map.of(
                        "latitude", request.getLatitude(),
                        "longitude", request.getLongitude(),
                        "progress", request.getProgressPercentage(),
                        "timestamp", System.currentTimeMillis()
                ));

        return dto;
    }

    public TransportDTO approveRoute(Long transportId, Long routeId) {
        Transport transport = transportRepository.findById(transportId)
                .orElseThrow(() -> new IllegalArgumentException("Transport not found"));

        RouteProposal route = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route proposal not found"));

        // Mark route as approved
        route.setApproved(true);
        routeRepository.save(route);

        // Update transport
        transport.setApprovedRoute(route);
        transport.setStatus(TransportStatus.APPROVED);
        transport.setDistanceRemainingKm(route.getTotalDistanceKm());

        if (transport.getPlannedDeparture() != null) {
            transport.setEstimatedArrival(calculateEstimatedArrival(route, transport.getPlannedDeparture()));
        }

        transport = transportRepository.save(transport);

        TransportDTO dto = transportMapper.toDTO(transport);

        // Send notification
        messagingTemplate.convertAndSend("/topic/transport/" + transportId + "/route-approved",
                Map.of("routeId", routeId, "transport", dto));

        return dto;
    }

    public List<VehicleTrackingDTO> getTransportTracking(Long transportId) {
        return trackingService.getTransportTracking(transportId);
    }

    public void deleteTransport(Long id) {
        Transport transport = transportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transport not found"));

        // Can only delete if not started
        if (transport.getStatus() == TransportStatus.IN_TRANSIT ||
                transport.getStatus() == TransportStatus.COMPLETED) {
            throw new IllegalStateException("Cannot delete transport that is in transit or completed");
        }

        transportRepository.delete(transport);

        // Send notification
        messagingTemplate.convertAndSend("/topic/transports/deleted",
                Map.of("transportId", id));
    }

    private String generateTransportNumber() {
        return "TR-" + System.currentTimeMillis();
    }

    private LocalDateTime calculateEstimatedArrival(RouteProposal route, LocalDateTime departure) {
        if (route != null && route.getEstimatedTimeMinutes() != null && departure != null) {
            return departure.plusMinutes(route.getEstimatedTimeMinutes().longValue());
        }
        return null;
    }
}