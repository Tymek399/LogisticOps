
package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.Model.Infrastructure;
import pl.logistic.logisticops.Model.RouteProposal;
import pl.logistic.logisticops.Model.RouteSegment;
import pl.logistic.logisticops.Model.VehicleSpecification;
import pl.logistic.logisticops.api.HereMapsClient;
import pl.logistic.logisticops.api.TomTomTrafficClient;
import pl.logistic.logisticops.repository.ObstacleRepository;
import pl.logistic.logisticops.repository.RouteProposalRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RouteService {
    
    private final VehicleSpecificationService vehicleService;
    private final RouteProposalRepository routeProposalRepository;
    private final ObstacleRepository obstacleRepository;
    private final HereMapsClient hereMapsClient;
    private final TomTomTrafficClient tomTomTrafficClient;
    
    /**
     * Generuje propozycje tras dla zestawu pojazdów
     * @param vehicleIds lista ID pojazdów w konwoju
     * @param startLat szerokość geograficzna punktu startowego
     * @param startLon długość geograficzna punktu startowego
     * @param endLat szerokość geograficzna punktu docelowego
     * @param endLon długość geograficzna punktu docelowego
     * @param missionId ID misji
     * @return lista propozycji tras
     */
    public List<RouteProposal> generateRouteProposals(
            List<Long> vehicleIds, 
            double startLat, 
            double startLon, 
            double endLat, 
            double endLon, 
            Long missionId) {
        
        // Pobierz specyfikacje pojazdów
        List<VehicleSpecification> vehicles = vehicleIds.stream()
                .map(vehicleService::getVehicleById)
                .filter(java.util.Optional::isPresent)
                .map(java.util.Optional::get)
                .collect(Collectors.toList());
        
        if (vehicles.isEmpty()) {
            throw new IllegalArgumentException("Nie znaleziono pojazdów o podanych ID");
        }
        
        // Znajdź parametry ograniczające konwój
        int maxHeight = vehicles.stream()
                .mapToInt(VehicleSpecification::getHeightCm)
                .max()
                .orElse(0);
        
        int maxAxleLoad = vehicles.stream()
                .mapToInt(VehicleSpecification::getMaxAxleLoadKg)
                .max()
                .orElse(0);
        
        int totalWeight = vehicles.stream()
                .mapToInt(VehicleSpecification::getTotalWeightKg)
                .sum();
        
        // Pobierz dane o trasie z HERE Maps API
        List<RouteSegment> optimalRouteSegments = hereMapsClient.getOptimalRoute(
                startLat, startLon, endLat, endLon, maxHeight, maxAxleLoad);
        
        // Pobierz dane o ruchu drogowym z TomTom API
        List<RouteSegment> safeRouteSegments = hereMapsClient.getSafeRoute(
                startLat, startLon, endLat, endLon, maxHeight, maxAxleLoad);
        
        // Pobierz dane o ruchu drogowym z TomTom API
        List<RouteSegment> alternativeRouteSegments = hereMapsClient.getAlternativeRoute(
                startLat, startLon, endLat, endLon, maxHeight, maxAxleLoad);
        
        // Pobierz przeszkody na trasie
        List<Infrastructure> infrastructures = findObstaclesOnRoute(
                optimalRouteSegments, maxHeight, maxAxleLoad, totalWeight);
        
        // Utworzenie propozycji tras
        RouteProposal optimalRoute = RouteProposal.builder()
                .missionId(missionId)
                .segments(optimalRouteSegments)
                .infrastructures(infrastructures)
                .approved(false)
                .generatedAt(LocalDateTime.now())
                .build();
        
        RouteProposal safeRoute = RouteProposal.builder()
                .missionId(missionId)
                .segments(safeRouteSegments)
                .infrastructures(new ArrayList<>())
                .approved(false)
                .generatedAt(LocalDateTime.now())
                .build();
        
        RouteProposal alternativeRoute = RouteProposal.builder()
                .missionId(missionId)
                .segments(alternativeRouteSegments)
                .infrastructures(new ArrayList<>())
                .approved(false)
                .generatedAt(LocalDateTime.now())
                .build();
        
        // Zapisz propozycje tras
        List<RouteProposal> proposals = List.of(
                routeProposalRepository.save(optimalRoute),
                routeProposalRepository.save(safeRoute),
                routeProposalRepository.save(alternativeRoute)
        );
        
        return proposals;
    }
    
    /**
     * Znajduje przeszkody na trasie (mosty, tunele itp.)
     */
    private List<Infrastructure> findObstaclesOnRoute(
            List<RouteSegment> segments, int maxHeight, int maxAxleLoad, int totalWeight) {
        
        // Tutaj powinna być implementacja do wykrywania przeszkód na trasie
        // np. przez odpytanie bazy danych lub HERE Maps API
        
        // Przykładowa implementacja
        return obstacleRepository.findObstaclesNearRoute(
                segments.stream()
                        .map(RouteSegment::getFrom)
                        .collect(Collectors.toList()),
                segments.stream()
                        .map(RouteSegment::getTo)
                        .collect(Collectors.toList()),
                maxHeight,
                maxAxleLoad,
                totalWeight
        );
    }
    
    /**
     * Zatwierdza propozycję trasy
     */
    public RouteProposal approveRouteProposal(Long proposalId) {
        RouteProposal proposal = routeProposalRepository.findById(proposalId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono propozycji trasy"));
        
        proposal.setApproved(true);
        return routeProposalRepository.save(proposal);
    }
    
    /**
     * Pobiera wszystkie propozycje tras dla danej misji
     */
    public List<RouteProposal> getRouteProposalsForMission(Long missionId) {
        return routeProposalRepository.findByMissionId(missionId);
    }
}