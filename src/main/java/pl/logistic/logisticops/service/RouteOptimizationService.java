package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.Model.*;
import pl.logistic.logisticops.api.HereMapsClient;
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
    private final HereMapsClient hereMapsClient;

    public List<RouteProposal> calculateOptimalRoutes(RouteRequest request) {
        List<VehicleSpecification> vehicles = vehicleRepository.findAllById(request.getVehicleIds());

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

        // Build restriction parameters for API call
        List<String> avoidanceParameters = buildAvoidanceParameters(constraints, restrictiveInfrastructure);

        List<RouteSegment> segments;

        switch (type) {
            case "OPTIMAL":
                segments = hereMapsClient.getOptimalRouteWithAvoidance(
                        request.getStartLat(), request.getStartLon(),
                        request.getEndLat(), request.getEndLon(),
                        constraints.getMaxHeightCm(), constraints.getMaxAxleLoadKg(),
                        avoidanceParameters
                );
                break;
            case "SAFE":
                segments = hereMapsClient.getSafeRouteWithAvoidance(
                        request.getStartLat(), request.getStartLon(),
                        request.getEndLat(), request.getEndLon(),
                        constraints.getMaxHeightCm(), constraints.getMaxAxleLoadKg(),
                        avoidanceParameters
                );
                break;
            case "ALTERNATIVE":
                segments = hereMapsClient.getAlternativeRouteWithAvoidance(
                        request.getStartLat(), request.getStartLon(),
                        request.getEndLat(), request.getEndLon(),
                        constraints.getMaxHeightCm(), constraints.getMaxAxleLoadKg(),
                        avoidanceParameters
                );
                break;
            default:
                throw new IllegalArgumentException("Unknown route type: " + type);
        }

        // Calculate totals
        double totalDistance = segments.stream().mapToDouble(RouteSegment::getDistanceKm).sum();
        double totalTime = segments.stream().mapToDouble(RouteSegment::getEstimatedTimeMin).sum();

        // Create route proposal
        RouteProposal proposal = RouteProposal.builder()
                .missionId(request.getMissionId())
                .routeType(type)
                .totalDistanceKm(totalDistance)
                .estimatedTimeMinutes(totalTime)
                .fuelConsumptionLiters(calculateFuelConsumption(totalDistance, constraints))
                .approved(false)
                .generatedAt(LocalDateTime.now())
                .build();

        proposal = routeProposalRepository.save(proposal);

        // Save segments
        for (int i = 0; i < segments.size(); i++) {
            RouteSegment segment = segments.get(i);
            segment.setRouteProposal(proposal);
            segment.setSequenceOrder(i);
            routeSegmentRepository.save(segment);
        }

        // Log avoided infrastructure (for reporting purposes)
        logAvoidedInfrastructure(proposal, restrictiveInfrastructure);

        return proposal;
    }

    private List<String> buildAvoidanceParameters(TransportConstraints constraints,
                                                  List<Infrastructure> restrictiveInfrastructure) {
        List<String> avoidanceParams = new ArrayList<>();

        for (Infrastructure infra : restrictiveInfrastructure) {
            // Check if this infrastructure would be problematic
            if (isProblematicForTransport(infra, constraints)) {
                // Add coordinates to avoidance list
                avoidanceParams.add(infra.getLatitude() + "," + infra.getLongitude());
            }
        }

        return avoidanceParams;
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

    private void logAvoidedInfrastructure(RouteProposal proposal, List<Infrastructure> avoidedInfrastructure) {
        for (Infrastructure infra : avoidedInfrastructure) {
            RouteObstacle obstacle = RouteObstacle.builder()
                    .routeProposal(proposal)
                    .canPass(false)
                    .restrictionType(determineRestrictionType(infra, null)) // We'll need constraints here
                    .alternativeRouteNeeded(true)
                    .build();
            routeObstacleRepository.save(obstacle);
        }
    }

    private TransportConstraints calculateConstraints(List<VehicleSpecification> vehicles) {
        return TransportConstraints.builder()
                .maxHeightCm(vehicles.stream().mapToInt(VehicleSpecification::getHeightCm).max().orElse(0))
                .maxAxleLoadKg(vehicles.stream().mapToInt(VehicleSpecification::getMaxAxleLoadKg).max().orElse(0))
                .totalWeightKg(vehicles.stream().mapToInt(VehicleSpecification::getTotalWeightKg).sum())
                .build();
    }

    private double calculateFuelConsumption(double distanceKm, TransportConstraints constraints) {
        // Basic fuel consumption calculation - can be improved
        double baseConsumption = 0.35; // liters per km
        double weightFactor = constraints.getTotalWeightKg() / 10000.0; // weight adjustment
        return distanceKm * baseConsumption * (1 + weightFactor);
    }

    private List<Infrastructure> findRestrictiveInfrastructure(List<RouteSegment> segments,
                                                               TransportConstraints constraints) {
        // This would use spatial queries to find infrastructure along the route
        // For now, return all infrastructure that might be restrictive
        return infrastructureRepository.findPotentialRestrictions(
                constraints.getMaxHeightCm(),
                constraints.getTotalWeightKg(),
                constraints.getMaxAxleLoadKg()
        );
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

