package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.Model.VehicleTracking;
import pl.logistic.logisticops.Model.Transport;
import pl.logistic.logisticops.repository.LocationLogRepository;
import pl.logistic.logisticops.repository.UnitRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LocationService {
    
    private final LocationLogRepository locationLogRepository;
    private final UnitRepository unitRepository;
    
    /**
     * Zapisuje nową lokalizację jednostki
     */
    public VehicleTracking logUnitLocation(Long unitId, BigDecimal latitude, BigDecimal longitude,
                                           BigDecimal speed, Integer heading) {
        
        // Sprawdź czy jednostka istnieje
        Transport transport = unitRepository.findById(unitId)
                .orElseThrow(() -> new IllegalArgumentException("Nie znaleziono jednostki o ID: " + unitId));
        
        // Aktualizuj lokalizację jednostki
        transport.setLatitude(latitude);
        transport.setLongitude(longitude);
        transport.setUpdatedAt(LocalDateTime.now());
        unitRepository.save(transport);
        
        // Zapisz log lokalizacji
        VehicleTracking vehicleTracking = VehicleTracking.builder()
                .unitId(unitId)
                .latitude(latitude)
                .longitude(longitude)
                .speed(speed)
                .heading(heading)
                .timestamp(LocalDateTime.now())
                .build();
        
        return locationLogRepository.save(vehicleTracking);
    }
    
    /**
     * Pobiera historię lokalizacji jednostki
     */
    public List<VehicleTracking> getUnitLocationHistory(Long unitId, LocalDateTime from, LocalDateTime to) {
        return locationLogRepository.findByUnitIdAndTimestampBetween(unitId, from, to);
    }
    
    /**
     * Pobiera ostatnią znaną lokalizację jednostki
     */
    public VehicleTracking getLastKnownLocation(Long unitId) {
        return locationLogRepository.findTopByUnitIdOrderByTimestampDesc(unitId)
                .orElse(null);
    }
}