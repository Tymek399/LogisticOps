package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.logistic.logisticops.Model.VehicleTracking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LocationLogRepository extends JpaRepository<VehicleTracking, Long> {
    
    List<VehicleTracking> findByUnitId(Long unitId);
    
    List<VehicleTracking> findByUnitIdAndTimestampBetween(Long unitId, LocalDateTime from, LocalDateTime to);
    
    Optional<VehicleTracking> findTopByUnitIdOrderByTimestampDesc(Long unitId);
}