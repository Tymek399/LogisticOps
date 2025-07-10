package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.logistic.logisticops.Model.Mission;
import pl.logistic.logisticops.enums.MissionPriority;
import pl.logistic.logisticops.enums.MissionStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface MissionRepository extends JpaRepository<Mission, Long> {
    
    List<Mission> findByStatus(MissionStatus status);
    
    List<Mission> findByPriority(MissionPriority priority);
    
    List<Mission> findByCreatedByUserId(Long userId);
    
    List<Mission> findByStartDateAfter(LocalDateTime date);
    
    List<Mission> findByEndDateBefore(LocalDateTime date);
}