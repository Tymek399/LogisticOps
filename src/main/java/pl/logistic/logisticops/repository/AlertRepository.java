package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.enums.AlertLevel;
import pl.logistic.logisticops.model.Alert;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {

    List<Alert> findByResolvedFalseOrderByTimestampDesc();

    List<Alert> findByLevelOrderByTimestampDesc(AlertLevel level);

    List<Alert> findByTypeOrderByTimestampDesc(String type);

    List<Alert> findByRelatedTransportIdOrderByTimestampDesc(Long transportId);

    List<Alert> findByRelatedInfrastructureIdOrderByTimestampDesc(Long infrastructureId);

    List<Alert> findByTimestampAfterOrderByTimestampDesc(LocalDateTime timestamp);

    List<Alert> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end);

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.resolved = false")
    Long countUnresolved();

    @Query("SELECT COUNT(a) FROM Alert a WHERE a.level = :level AND a.resolved = false")
    Long countUnresolvedByLevel(@Param("level") AlertLevel level);

    @Query("SELECT a FROM Alert a WHERE a.relatedTransport.id = :transportId " +
            "AND a.resolved = false ORDER BY a.timestamp DESC")
    List<Alert> findUnresolvedByTransportId(@Param("transportId") Long transportId);

    @Query("SELECT a FROM Alert a WHERE a.level IN :levels " +
            "AND a.resolved = false ORDER BY a.timestamp DESC")
    List<Alert> findUnresolvedByLevels(@Param("levels") List<AlertLevel> levels);
}
