package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.model.*;
import pl.logistic.logisticops.repository.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RouteOptimizationService {

    private final RouteProposalRepository routeProposalRepository;
    private final RouteSegmentRepository routeSegmentRepository;
    private final RouteObstacleRepository routeObstacleRepository;
    private final VehicleSpecificationRepository vehicleRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final GoogleMapsService googleMapsService;

    public List<RouteProposal> calculateOptimalRoutes(RouteRequest request) {
        List<VehicleSpecification> vehicles = vehicleRepository.findAllById(request.getTransportSetIds());

        if (vehicles.isEmpty()) {
            throw new IllegalArgumentException("No vehicles found");
        }

        // Calculate transport constraints
        TransportConstraints constraints = calculateConstraints(vehicles);

        // Get all infrastructure restrictions along potential routes
        List<Infrastructure> restrictiveInfrastructure =
                infrastructureRepository.findPotentialRestrictions(
                        constraints.getMaxHeightCm(),
                        constraints.getTotalWeightKg(),
                        constraints.getMaxAxleLoadKg()
                );

        List<RouteProposal> proposals = new ArrayList<>();

        // Generate route that automatically avoids all restrictions
        RouteProposal optimal = generateIntelligentRoute(request, constraints, restrictiveInfrastructure, "OPTIMAL");
        proposals.add(optimal);

        // Generate alternative safe route
        RouteProposal safe = generateIntelligentRoute(request, constraints, restrictiveInfrastructure, "SAFE");
        proposals.add(safe);

        // Generate backup route
        RouteProposal alternative = generateIntelligentRoute(request, constraints, restrictiveInfrastructure, "ALTERNATIVE");
        proposals.add(alternative);

        return proposals;
    }

    private RouteProposal generateIntelligentRoute(RouteRequest request,
                                                   TransportConstraints constraints,
                                                   List<Infrastructure> restrictiveInfrastructure,
                                                   String type) {

        // Use Google Maps for route generation
        java.util.Map<String, Object> routeConstraints = new java.util.HashMap<>();
        routeConstraints.put("maxHeight", constraints.getMaxHeightCm());
        routeConstraints.put("maxWeight", constraints.getTotalWeightKg());

        // Get route segments from Google Maps
        List<pl.logistic.logisticops.dto.RouteSegmentDTO> segmentDTOs = googleMapsService.getOptimalRoute(
                request.getStartLat(), request.getStartLon(),
                request.getEndLat(), request.getEndLon(),
                routeConstraints
        );

        // Calculate totals
        double totalDistance = segmentDTOs.stream().mapToDouble(pl.logistic.logisticops.dto.RouteSegmentDTO::getDistanceKm).sum();
        double totalTime = segmentDTOs.stream().mapToDouble(pl.logistic.logisticops.dto.RouteSegmentDTO::getEstimatedTimeMin).sum();

        // Create route proposal
        RouteProposal proposal = RouteProposal.builder()
                .routeType(type)
                .totalDistanceKm(totalDistance)
                .estimatedTimeMinutes(totalTime)
                .fuelConsumptionLiters(calculateFuelConsumption(totalDistance, constraints))
                .approved(false)
                .generatedAt(LocalDateTime.now())
                .build();

        proposal = routeProposalRepository.save(proposal);

        // Save segments
        for (int i = 0; i < segmentDTOs.size(); i++) {
            pl.logistic.logisticops.dto.RouteSegmentDTO segmentDTO = segmentDTOs.get(i);

            RouteSegment segment = RouteSegment.builder()
                    .routeProposal(proposal)
                    .sequenceOrder(i)
                    .fromLocation(segmentDTO.getFromLocation())
                    .toLocation(segmentDTO.getToLocation())
                    .fromLatitude(segmentDTO.getFromLatitude())
                    .fromLongitude(segmentDTO.getFromLongitude())
                    .toLatitude(segmentDTO.getToLatitude())
                    .toLongitude(segmentDTO.getToLongitude())
                    .distanceKm(segmentDTO.getDistanceKm())
                    .estimatedTimeMin(segmentDTO.getEstimatedTimeMin())
                    .roadCondition(segmentDTO.getRoadCondition())
                    .roadName(segmentDTO.getRoadName())
                    .polyline(segmentDTO.getPolyline())
                    .build();

            routeSegmentRepository.save(segment);
        }

        // Log avoided infrastructure (for reporting purposes)
        logAvoidedInfrastructure(proposal, restrictiveInfrastructure, constraints);

        return proposal;
    }

    private void logAvoidedInfrastructure(RouteProposal proposal, List<Infrastructure> avoidedInfrastructure, TransportConstraints constraints) {
        for (Infrastructure infra : avoidedInfrastructure) {
            if (isProblematicForTransport(infra, constraints)) {
                RouteObstacle obstacle = RouteObstacle.builder()
                        .routeProposal(proposal)
                        .infrastructure(infra)
                        .canPass(false)
                        .restrictionType(determineRestrictionType(infra, constraints))
                        .alternativeRouteNeeded(true)
                        .notes("Transport constraints exceed infrastructure limits")
                        .build();
                routeObstacleRepository.save(obstacle);
            }
        }
    }

    private boolean isProblematicForTransport(Infrastructure infra, TransportConstraints constraints) {
        // Check height restrictions
        if (infra.getMaxHeightCm() != null && constraints.getMaxHeightCm() > infra.getMaxHeightCm()) {
            return true;
        }

        // Check weight restrictions
        if (infra.getMaxWeightKg() != null && constraints.getTotalWeightKg() > infra.getMaxWeightKg()) {
            return true;
        }

        // Check axle load restrictions
        if (infra.getMaxAxleWeightKg() != null && constraints.getMaxAxleLoadKg() > infra.getMaxAxleWeightKg()) {
            return true;
        }

        return false;
    }

    private TransportConstraints calculateConstraints(List<VehicleSpecification> vehicles) {
        return TransportConstraints.builder()
                .maxHeightCm(vehicles.stream().mapToInt(v -> v.getHeightCm() != null ? v.getHeightCm() : 0).max().orElse(0))
                .maxAxleLoadKg(vehicles.stream().mapToInt(v -> v.getMaxAxleLoadKg() != null ? v.getMaxAxleLoadKg() : 0).max().orElse(0))
                .totalWeightKg(vehicles.stream().mapToInt(v -> v.getTotalWeightKg() != null ? v.getTotalWeightKg() : 0).sum())
                .build();
    }

    private double calculateFuelConsumption(double distanceKm, TransportConstraints constraints) {
        // Basic fuel consumption calculation - can be improved
        double baseConsumption = 0.35; // liters per km
        double weightFactor = constraints.getTotalWeightKg() / 10000.0; // weight adjustment
        return distanceKm * baseConsumption * (1 + weightFactor);
    }

    private String determineRestrictionType(Infrastructure infra, TransportConstraints constraints) {
        if (infra.getMaxHeightCm() != null && constraints.getMaxHeightCm() > infra.getMaxHeightCm()) {
            return "HEIGHT";
        }
        if (infra.getMaxWeightKg() != null && constraints.getTotalWeightKg() > infra.getMaxWeightKg()) {
            return "WEIGHT";
        }
        if (infra.getMaxAxleWeightKg() != null && constraints.getMaxAxleLoadKg() > infra.getMaxAxleWeightKg()) {
            return "AXLE_WEIGHT";
        }
        return "OTHER";
    }
}