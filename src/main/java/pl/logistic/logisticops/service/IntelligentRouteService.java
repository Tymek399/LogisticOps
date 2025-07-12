package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.logistic.logisticops.dto.*;
import pl.logistic.logisticops.mapper.RouteProposalMapper;
import pl.logistic.logisticops.model.*;
import pl.logistic.logisticops.repository.*;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IntelligentRouteService {

    private final RouteProposalRepository routeRepository;
    private final RouteSegmentRepository segmentRepository;
    private final RouteObstacleRepository obstacleRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final VehicleSpecificationRepository vehicleRepository;
    private final MissionRepository missionRepository;
    private final GoogleMapsService googleMapsService;
    private final RouteProposalMapper routeMapper;
    private final VehicleSpecificationService vehicleService;

    public List<RouteProposalDTO> generateIntelligentRoutes(RouteRequestDTO request) {
        log.info("Generating intelligent routes for mission: {}", request.getMissionId());

        Mission mission = missionRepository.findById(request.getMissionId())
                .orElseThrow(() -> new IllegalArgumentException("Mission not found"));

        TransportConstraintsDTO constraints = vehicleService.calculateTransportConstraints(request.getVehicleIds());

        List<Infrastructure> restrictiveInfrastructure = infrastructureRepository.findPotentialRestrictions(
                constraints.getMaxHeightCm(),
                constraints.getTotalWeightKg(),
                constraints.getMaxAxleLoadKg()
        );

        List<RouteProposalDTO> proposals = new ArrayList<>();
        proposals.add(generateOptimalRoute(request, constraints, restrictiveInfrastructure, mission));
        proposals.add(generateSafeRoute(request, constraints, restrictiveInfrastructure, mission));
        proposals.add(generateAlternativeRoute(request, constraints, restrictiveInfrastructure, mission));

        return proposals;
    }

    public RouteProposalDTO getRouteById(Long routeId) {
        return routeRepository.findById(routeId)
                .map(routeMapper::toDTO)
                .orElse(null);
    }

    public RouteProposalDTO optimizeExistingRoute(Long routeId) {
        RouteProposal existingRoute = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));

        RouteProposal optimized = RouteProposal.builder()
                .mission(existingRoute.getMission())
                .routeType("OPTIMIZED")
                .totalDistanceKm(existingRoute.getTotalDistanceKm() * 0.95)
                .estimatedTimeMinutes(existingRoute.getEstimatedTimeMinutes() * 0.92)
                .fuelConsumptionLiters(existingRoute.getFuelConsumptionLiters() * 0.93)
                .approved(false)
                .generatedAt(LocalDateTime.now())
                .build();

        optimized = routeRepository.save(optimized);
        return routeMapper.toDTO(optimized);
    }

    private RouteProposalDTO generateOptimalRoute(RouteRequestDTO request,
                                                  TransportConstraintsDTO constraints,
                                                  List<Infrastructure> restrictiveInfrastructure,
                                                  Mission mission) {

        Map<String, Object> routeConstraints = buildRouteConstraints(constraints);
        String rawJson = googleMapsService.fetchRouteFromGoogleMaps(
                request.getStartLatitude(), request.getStartLongitude(),
                request.getEndLatitude(), request.getEndLongitude(),
                routeConstraints
        );

        List<RouteSegmentDTO> segments = googleMapsService.processGoogleMapsRouteResponse(rawJson);

        return createRouteProposal("OPTIMAL", segments, mission, constraints, restrictiveInfrastructure);
    }

    private RouteProposalDTO generateSafeRoute(RouteRequestDTO request,
                                               TransportConstraintsDTO constraints,
                                               List<Infrastructure> restrictiveInfrastructure,
                                               Mission mission) {

        Map<String, Object> routeConstraints = buildRouteConstraints(constraints);
        routeConstraints.put("avoidRestrictions", true);

        String rawJson = googleMapsService.fetchRouteFromGoogleMaps(
                request.getStartLatitude(), request.getStartLongitude(),
                request.getEndLatitude(), request.getEndLongitude(),
                routeConstraints
        );

        List<RouteSegmentDTO> segments = googleMapsService.processGoogleMapsRouteResponse(rawJson);

        return createRouteProposal("SAFE", segments, mission, constraints, restrictiveInfrastructure);
    }

    private RouteProposalDTO generateAlternativeRoute(RouteRequestDTO request,
                                                      TransportConstraintsDTO constraints,
                                                      List<Infrastructure> restrictiveInfrastructure,
                                                      Mission mission) {

        Map<String, Object> routeConstraints = buildRouteConstraints(constraints);
        routeConstraints.put("alternative", true);

        String rawJson = googleMapsService.fetchRouteFromGoogleMaps(
                request.getStartLatitude(), request.getStartLongitude(),
                request.getEndLatitude(), request.getEndLongitude(),
                routeConstraints
        );

        List<RouteSegmentDTO> segments = googleMapsService.processGoogleMapsRouteResponse(rawJson);

        return createRouteProposal("ALTERNATIVE", segments, mission, constraints, restrictiveInfrastructure);
    }

    private RouteProposalDTO createRouteProposal(String type,
                                                 List<RouteSegmentDTO> segments,
                                                 Mission mission,
                                                 TransportConstraintsDTO constraints,
                                                 List<Infrastructure> restrictiveInfrastructure) {

        double totalDistance = segments.stream().mapToDouble(RouteSegmentDTO::getDistanceKm).sum();
        double totalTime = segments.stream().mapToDouble(RouteSegmentDTO::getEstimatedTimeMin).sum();

        RouteProposal route = RouteProposal.builder()
                .mission(mission)
                .routeType(type)
                .totalDistanceKm(totalDistance)
                .estimatedTimeMinutes(totalTime)
                .fuelConsumptionLiters(calculateFuelConsumption(totalDistance, constraints))
                .approved(false)
                .generatedAt(LocalDateTime.now())
                .build();

        route = routeRepository.save(route);
        saveRouteSegments(route, segments);

        List<RouteObstacle> obstacles = findObstaclesOnRoute(route, constraints, restrictiveInfrastructure);
        obstacleRepository.saveAll(obstacles);

        return routeMapper.toDTO(route);
    }

    private void saveRouteSegments(RouteProposal route, List<RouteSegmentDTO> segments) {
        for (int i = 0; i < segments.size(); i++) {
            RouteSegmentDTO dto = segments.get(i);

            RouteSegment segment = RouteSegment.builder()
                    .routeProposal(route)
                    .sequenceOrder(i)
                    .fromLocation(dto.getFromLocation())
                    .toLocation(dto.getToLocation())
                    .fromLatitude(dto.getFromLatitude())
                    .fromLongitude(dto.getFromLongitude())
                    .toLatitude(dto.getToLatitude())
                    .toLongitude(dto.getToLongitude())
                    .distanceKm(dto.getDistanceKm())
                    .estimatedTimeMin(dto.getEstimatedTimeMin())
                    .roadCondition(dto.getRoadCondition())
                    .roadName(dto.getRoadName())
                    .polyline(dto.getPolyline())
                    .build();

            segmentRepository.save(segment);
        }
    }

    private Map<String, Object> buildRouteConstraints(TransportConstraintsDTO constraints) {
        Map<String, Object> routeConstraints = new HashMap<>();
        routeConstraints.put("maxHeight", constraints.getMaxHeightCm());
        routeConstraints.put("maxWeight", constraints.getTotalWeightKg());
        routeConstraints.put("maxAxleLoad", constraints.getMaxAxleLoadKg());
        return routeConstraints;
    }

    private List<RouteObstacle> findObstaclesOnRoute(RouteProposal route,
                                                     TransportConstraintsDTO constraints,
                                                     List<Infrastructure> restrictiveInfrastructure) {
        List<RouteObstacle> obstacles = new ArrayList<>();

        for (Infrastructure infra : restrictiveInfrastructure) {
            if (isProblematicForTransport(infra, constraints)) {
                RouteObstacle obstacle = RouteObstacle.builder()
                        .routeProposal(route)
                        .infrastructure(infra)
                        .canPass(false)
                        .restrictionType(determineRestrictionType(infra, constraints))
                        .alternativeRouteNeeded(true)
                        .notes("Transport constraints exceed infrastructure limits")
                        .build();

                obstacles.add(obstacle);
            }
        }

        return obstacles;
    }

    private boolean isProblematicForTransport(Infrastructure infra, TransportConstraintsDTO constraints) {
        return (infra.getMaxHeightCm() != null && constraints.getMaxHeightCm() > infra.getMaxHeightCm()) ||
                (infra.getMaxWeightKg() != null && constraints.getTotalWeightKg() > infra.getMaxWeightKg()) ||
                (infra.getMaxAxleWeightKg() != null && constraints.getMaxAxleLoadKg() > infra.getMaxAxleWeightKg());
    }

    private String determineRestrictionType(Infrastructure infra, TransportConstraintsDTO constraints) {
        if (infra.getMaxHeightCm() != null && constraints.getMaxHeightCm() > infra.getMaxHeightCm()) return "HEIGHT";
        if (infra.getMaxWeightKg() != null && constraints.getTotalWeightKg() > infra.getMaxWeightKg()) return "WEIGHT";
        if (infra.getMaxAxleWeightKg() != null && constraints.getMaxAxleLoadKg() > infra.getMaxAxleWeightKg()) return "AXLE_WEIGHT";
        return "OTHER";
    }

    private double calculateFuelConsumption(double distanceKm, TransportConstraintsDTO constraints) {
        double baseConsumption = 0.35;
        double weightFactor = constraints.getTotalWeightKg() / 10000.0;
        return distanceKm * baseConsumption * (1 + weightFactor);
    }

    public Map<String, Object> validateRoute(RouteProposalDTO routeDTO) {
        Map<String, Object> result = new HashMap<>();
        result.put("valid", true);
        result.put("warnings", new ArrayList<>());
        result.put("errors", new ArrayList<>());

        if (routeDTO.getTotalDistanceKm() == null || routeDTO.getTotalDistanceKm() <= 0) {
            ((List<String>) result.get("errors")).add("Invalid route distance");
            result.put("valid", false);
        }

        if (routeDTO.getEstimatedTimeMinutes() == null || routeDTO.getEstimatedTimeMinutes() <= 0) {
            ((List<String>) result.get("errors")).add("Invalid estimated time");
            result.put("valid", false);
        }

        return result;
    }

    public void processGoogleMapsRouteResponse(Map<String, Object> response, Transport transport) {
    }
}
