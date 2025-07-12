package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.enums.TransportStatus;
import pl.logistic.logisticops.model.Transport;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransportRepository extends JpaRepository<Transport, Long> {

    List<Transport> findByStatus(TransportStatus status);

    List<Transport> findByStatusIn(List<TransportStatus> statuses);

    List<Transport> findByMissionId(Long missionId);

    List<Transport> findByCreatedByUserId(Long userId);

    List<Transport> findByPlannedDepartureBetween(LocalDateTime start, LocalDateTime end);

    List<Transport> findByEstimatedArrivalBetween(LocalDateTime start, LocalDateTime end);

    @Query("SELECT t FROM Transport t WHERE t.currentLatitude IS NOT NULL AND t.currentLongitude IS NOT NULL")
    List<Transport> findActiveWithLocation();

    @Query("SELECT t FROM Transport t WHERE t.status IN :statuses AND t.currentLatitude IS NOT NULL")
    List<Transport> findByStatusInWithLocation(@Param("statuses") List<TransportStatus> statuses);

    @Query("SELECT COUNT(t) FROM Transport t WHERE t.status = :status")
    Long countByStatus(@Param("status") TransportStatus status);

    @Query("SELECT t FROM Transport t WHERE t.progressPercentage BETWEEN :minProgress AND :maxProgress")
    List<Transport> findByProgressBetween(@Param("minProgress") Double minProgress, @Param("maxProgress") Double maxProgress);
}