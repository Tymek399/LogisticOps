package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.logistic.logisticops.Model.FuelConsumptionData;

import java.util.List;
import java.util.Optional;

public interface FuelConsumptionDataRepository extends JpaRepository<FuelConsumptionData, Long> {

    Optional<FuelConsumptionData> findByTransportSetIdAndRouteProposalId(Long transportSetId, Long routeProposalId);

    List<FuelConsumptionData> findByTransportSetIdAndActualConsumptionLitersIsNotNull(Long transportSetId);

    @Query("SELECT AVG(f.accuracyPercentage) FROM FuelConsumptionData f WHERE f.accuracyPercentage IS NOT NULL")
    Double getAverageAccuracy();

    @Query("SELECT f FROM FuelConsumptionData f WHERE f.transportSetId = :transportSetId " +
            "AND f.actualConsumptionLiters IS NOT NULL " +
            "ORDER BY f.createdAt DESC")
    List<FuelConsumptionData> findRecentByTransportSetId(@Param("transportSetId") Long transportSetId);
}
