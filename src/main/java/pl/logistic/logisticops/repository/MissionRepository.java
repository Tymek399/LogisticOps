package pl.logistic.logisticops.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.model.Mission;
import pl.logistic.logisticops.enums.MissionPriority;
import pl.logistic.logisticops.enums.MissionStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MissionRepository extends JpaRepository<Mission, Long> {

    List<Mission> findByStatus(MissionStatus status);

    Page<Mission> findByStatus(MissionStatus status, Pageable pageable);

    List<Mission> findByPriority(MissionPriority priority);

    List<Mission> findByCreatedByUserId(Long userId);

    Page<Mission> findByCreatedByUserId(Long userId, Pageable pageable);

    List<Mission> findByStartDateAfter(LocalDateTime date);

    List<Mission> findByEndDateBefore(LocalDateTime date);

    @Query("SELECT COUNT(m) FROM Mission m WHERE m.status = :status")
    Long countByStatus(@Param("status") MissionStatus status);

    @Query("SELECT m FROM Mission m WHERE m.status IN :statuses ORDER BY m.priority DESC, m.createdAt DESC")
    List<Mission> findByStatusInOrderByPriorityDescCreatedAtDesc(@Param("statuses") List<MissionStatus> statuses);
}