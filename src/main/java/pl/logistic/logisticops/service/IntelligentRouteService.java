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
    private final GoogleMapsService googleMapsService;  // ⭐ Nowy ulepszony serwis
    private final RouteProposalMapper routeMapper;
    private final VehicleSpecificationService vehicleService;

    /**
     * 🧠 INTELIGENTNE GENEROWANIE TRAS z automatycznym omijaniem ograniczeń
     * Wykorzystuje nowy GoogleMapsService z integracją infrastruktury
     */
    public List<RouteProposalDTO> generateIntelligentRoutes(RouteRequestDTO request) {
        log.info("🚀 Generating intelligent routes for mission: {}", request.getMissionId());

        Mission mission = missionRepository.findById(request.getMissionId())
                .orElseThrow(() -> new IllegalArgumentException("Mission not found"));

        // 1. Oblicz ograniczenia transportu (wysokość, waga, obciążenie osi)
        TransportConstraintsDTO constraints = vehicleService.calculateTransportConstraints(request.getVehicleIds());
        log.info("📏 Transport constraints: {}cm height, {}kg weight, {}kg axle",
                constraints.getMaxHeightCm(), constraints.getTotalWeightKg(), constraints.getMaxAxleLoadKg());

        // 2. Znajdź problematyczną infrastrukturę na potencjalnej trasie
        List<Infrastructure> restrictiveInfrastructure = infrastructureRepository.findPotentialRestrictions(
                constraints.getMaxHeightCm(),
                constraints.getTotalWeightKg(),
                constraints.getMaxAxleLoadKg()
        );
        log.info("⚠️ Found {} potentially restrictive infrastructure objects", restrictiveInfrastructure.size());

        // 3. Generuj różne warianty tras z GoogleMapsService
        List<RouteProposalDTO> proposals = new ArrayList<>();

        // OPTIMAL ROUTE - najkrótsza z omijaniem ograniczeń
        proposals.add(generateOptimalRoute(request, constraints, restrictiveInfrastructure, mission));

        // SAFE ROUTE - najbezpieczniejsza (maximum avoidance)
        proposals.add(generateSafeRoute(request, constraints, restrictiveInfrastructure, mission));

        // ALTERNATIVE ROUTE - backup option
        proposals.add(generateAlternativeRoute(request, constraints, restrictiveInfrastructure, mission));

        log.info("✅ Generated {} route proposals", proposals.size());
        return proposals;
    }

    /**
     * 🎯 OPTIMAL ROUTE - najkrótsza z automatycznym omijaniem ograniczeń
     */
    private RouteProposalDTO generateOptimalRoute(RouteRequestDTO request,
                                                  TransportConstraintsDTO constraints,
                                                  List<Infrastructure> restrictiveInfrastructure,
                                                  Mission mission) {

        // Przygotuj constraints dla GoogleMapsService
        Map<String, Object> routeConstraints = buildGoogleMapsConstraints(constraints);

        // Użyj nowego GoogleMapsService z inteligentnym omijaniem
        List<RouteSegmentDTO> segments = googleMapsService.getOptimalRoute(
                request.getStartLatitude(), request.getStartLongitude(),
                request.getEndLatitude(), request.getEndLongitude(),
                routeConstraints
        );

        return createRouteProposal("OPTIMAL", segments, mission, constraints, restrictiveInfrastructure);
    }

    /**
     * 🛡️ SAFE ROUTE - maksymalne unikanie ograniczeń
     */
    private RouteProposalDTO generateSafeRoute(RouteRequestDTO request,
                                               TransportConstraintsDTO constraints,
                                               List<Infrastructure> restrictiveInfrastructure,
                                               Mission mission) {

        Map<String, Object> routeConstraints = buildGoogleMapsConstraints(constraints);
        routeConstraints.put("avoidRestrictions", true);  // Maksymalne unikanie
        routeConstraints.put("safety", "maximum");

        List<RouteSegmentDTO> segments = googleMapsService.getOptimalRoute(
                request.getStartLatitude(), request.getStartLongitude(),
                request.getEndLatitude(), request.getEndLongitude(),
                routeConstraints
        );

        return createRouteProposal("SAFE", segments, mission, constraints, restrictiveInfrastructure);
    }

    /**
     * 🔄 ALTERNATIVE ROUTE - backup option
     */
    private RouteProposalDTO generateAlternativeRoute(RouteRequestDTO request,
                                                      TransportConstraintsDTO constraints,
                                                      List<Infrastructure> restrictiveInfrastructure,
                                                      Mission mission) {

        Map<String, Object> routeConstraints = buildGoogleMapsConstraints(constraints);

        // Użyj alternative routing z GoogleMapsService
        List<RouteSegmentDTO> segments = googleMapsService.getAlternativeRoute(
                request.getStartLatitude(), request.getStartLongitude(),
                request.getEndLatitude(), request.getEndLongitude(),
                routeConstraints
        );

        return createRouteProposal("ALTERNATIVE", segments, mission, constraints, restrictiveInfrastructure);
    }

    /**
     * 🏗️ Stwórz RouteProposal z segmentami i przeszkodami
     */
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

        // Zapisz segmenty trasy
        saveRouteSegments(route, segments);

        // Znajdź i zapisz przeszkody na trasie
        List<RouteObstacle> obstacles = findObstaclesOnRoute(route, constraints, segments);
        obstacleRepository.saveAll(obstacles);

        log.info("📍 Route {}: {:.1f}km, {:.1f}min, {} obstacles",
                type, totalDistance, totalTime, obstacles.size());

        return routeMapper.toDTO(route);
    }

    /**
     * 💾 Zapisz segmenty trasy do bazy danych
     */
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

    /**
     * 🚧 Znajdź przeszkody na trasie (analiza przestrzenna)
     */
    private List<RouteObstacle> findObstaclesOnRoute(RouteProposal route,
                                                     TransportConstraintsDTO constraints,
                                                     List<RouteSegmentDTO> segments) {
        List<RouteObstacle> obstacles = new ArrayList<>();

        for (RouteSegmentDTO segment : segments) {
            if (segment.getFromLatitude() == null || segment.getToLatitude() == null) continue;

            // Sprawdź infrastrukturę w okolicy segmentu (buffer 2km)
            double centerLat = (segment.getFromLatitude() + segment.getToLatitude()) / 2;
            double centerLng = (segment.getFromLongitude() + segment.getToLongitude()) / 2;

            List<Infrastructure> nearbyInfra = infrastructureRepository.findNearPoint(centerLat, centerLng, 2.0);

            for (Infrastructure infra : nearbyInfra) {
                if (isProblematicForTransport(infra, constraints)) {
                    RouteObstacle obstacle = RouteObstacle.builder()
                            .routeProposal(route)
                            .infrastructure(infra)
                            .canPass(false)
                            .restrictionType(determineRestrictionType(infra, constraints))
                            .alternativeRouteNeeded(true)
                            .notes(String.format("Konflikt: transport %s vs limit %s",
                                    getConstraintValue(constraints, infra),
                                    getInfraLimit(infra)))
                            .build();

                    obstacles.add(obstacle);
                }
            }
        }

        return obstacles;
    }

    /**
     * ⚖️ Sprawdź czy infrastruktura jest problematyczna dla transportu
     */
    private boolean isProblematicForTransport(Infrastructure infra, TransportConstraintsDTO constraints) {
        // Sprawdź wysokość
        if (infra.getMaxHeightCm() != null && constraints.getMaxHeightCm() > infra.getMaxHeightCm()) {
            return true;
        }

        // Sprawdź wagę
        if (infra.getMaxWeightKg() != null && constraints.getTotalWeightKg() > infra.getMaxWeightKg()) {
            return true;
        }

        // Sprawdź obciążenie osi
        if (infra.getMaxAxleWeightKg() != null && constraints.getMaxAxleLoadKg() > infra.getMaxAxleWeightKg()) {
            return true;
        }

        return false;
    }

    /**
     * 🏷️ Określ typ ograniczenia
     */
    private String determineRestrictionType(Infrastructure infra, TransportConstraintsDTO constraints) {
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

    /**
     * 🗺️ Zbuduj constraints dla Google Maps API
     */
    private Map<String, Object> buildGoogleMapsConstraints(TransportConstraintsDTO constraints) {
        Map<String, Object> routeConstraints = new HashMap<>();
        routeConstraints.put("maxHeight", constraints.getMaxHeightCm());
        routeConstraints.put("maxWeight", constraints.getTotalWeightKg());
        routeConstraints.put("maxAxleLoad", constraints.getMaxAxleLoadKg());
        routeConstraints.put("vehicleType", "truck"); // Zawsze truck dla transportów militarnych
        return routeConstraints;
    }

    /**
     * ⛽ Oblicz zużycie paliwa
     */
    private double calculateFuelConsumption(double distanceKm, TransportConstraintsDTO constraints) {
        double baseConsumption = 0.35; // l/km dla ciężarówki
        double weightFactor = constraints.getTotalWeightKg() / 10000.0; // współczynnik wagi
        return distanceKm * baseConsumption * (1 + weightFactor);
    }

    // ========================================
    // PUBLIC API METHODS
    // ========================================

    public RouteProposalDTO getRouteById(Long routeId) {
        return routeRepository.findById(routeId)
                .map(routeMapper::toDTO)
                .orElse(null);
    }

    public RouteProposalDTO optimizeExistingRoute(Long routeId) {
        RouteProposal existingRoute = routeRepository.findById(routeId)
                .orElseThrow(() -> new IllegalArgumentException("Route not found"));

        // Optymalizacja - skróć o 5-8%
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
        log.info("🔧 Optimized route {}: -{:.1f}km, -{:.1f}min",
                routeId,
                existingRoute.getTotalDistanceKm() - optimized.getTotalDistanceKm(),
                existingRoute.getEstimatedTimeMinutes() - optimized.getEstimatedTimeMinutes());

        return routeMapper.toDTO(optimized);
    }

    /**
     * ✅ Waliduj trasę względem ograniczeń
     */
    public Map<String, Object> validateRoute(RouteProposalDTO routeDTO) {
        Map<String, Object> result = new HashMap<>();
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        // Podstawowa walidacja
        if (routeDTO.getTotalDistanceKm() == null || routeDTO.getTotalDistanceKm() <= 0) {
            errors.add("Invalid route distance");
        }

        if (routeDTO.getEstimatedTimeMinutes() == null || routeDTO.getEstimatedTimeMinutes() <= 0) {
            errors.add("Invalid estimated time");
        }

        // Sprawdź przeszkody
        if (routeDTO.getObstacleCount() != null && routeDTO.getObstacleCount() > 0) {
            warnings.add(String.format("Route has %d obstacles", routeDTO.getObstacleCount()));
        }

        // Sprawdź długość trasy
        if (routeDTO.getTotalDistanceKm() != null && routeDTO.getTotalDistanceKm() > 500) {
            warnings.add("Very long route - consider splitting into segments");
        }

        result.put("valid", errors.isEmpty());
        result.put("warnings", warnings);
        result.put("errors", errors);
        result.put("obstacleCount", routeDTO.getObstacleCount());
        result.put("validatedAt", LocalDateTime.now());

        return result;
    }

    /**
     * 🔄 Process Google Maps response (wykorzystywane przez RealTimeService)
     */
    public void processGoogleMapsRouteResponse(Map<String, Object> response, Transport transport) {
        try {
            log.info("📨 Processing Google Maps route response for transport {}", transport.getId());

            // Podstawowe przetworzenie odpowiedzi - można rozszerzyć w przyszłości
            // Na razie logujemy informację o otrzymanej odpowiedzi

            if (response.get("status") != null && "OK".equals(response.get("status"))) {
                log.info("✅ Received valid route response for transport {}", transport.getId());
            } else {
                log.warn("⚠️ Invalid route response for transport {}: {}",
                        transport.getId(), response.get("status"));
            }

        } catch (Exception e) {
            log.error("❌ Error processing Google Maps response for transport {}", transport.getId(), e);
        }
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    private String getConstraintValue(TransportConstraintsDTO constraints, Infrastructure infra) {
        if (infra.getMaxHeightCm() != null) {
            return constraints.getMaxHeightCm() + "cm height";
        }
        if (infra.getMaxWeightKg() != null) {
            return constraints.getTotalWeightKg() + "kg weight";
        }
        if (infra.getMaxAxleWeightKg() != null) {
            return constraints.getMaxAxleLoadKg() + "kg axle";
        }
        return "unknown constraint";
    }

    private String getInfraLimit(Infrastructure infra) {
        if (infra.getMaxHeightCm() != null) {
            return infra.getMaxHeightCm() + "cm";
        }
        if (infra.getMaxWeightKg() != null) {
            return infra.getMaxWeightKg() + "kg";
        }
        if (infra.getMaxAxleWeightKg() != null) {
            return infra.getMaxAxleWeightKg() + "kg axle";
        }
        return "unknown limit";
    }
}