package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import pl.logistic.logisticops.Model.SystemPerformance;

import java.time.LocalDateTime;
import java.util.List;

public interface SystemPerformanceRepository extends JpaRepository<SystemPerformance, Long> {

    List<SystemPerformance> findByRecordedAtAfter(LocalDateTime time);

    List<SystemPerformance> findByMetricNameAndRecordedAtBetween(String metricName, LocalDateTime start, LocalDateTime end);

    @Query("SELECT s.category, AVG(s.metricValue) FROM SystemPerformance s " +
            "WHERE s.recordedAt >= :startTime GROUP BY s.category")
    List<Object[]> getAverageMetricsByCategory(LocalDateTime startTime);
}