package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.model.Infrastructure;

import java.util.List;

@Repository
public interface InfrastructureRepository extends JpaRepository<Infrastructure, Long> {

    Infrastructure findByExternalId(String externalId);

    List<Infrastructure> findByType(String type);

    List<Infrastructure> findByIsActiveTrue();

    List<Infrastructure> findByIsActiveFalse();

    List<Infrastructure> findByRoadNumber(String roadNumber);

    @Query("SELECT i FROM Infrastructure i WHERE " +
            "(:maxHeightCm IS NULL OR i.maxHeightCm < :maxHeightCm) OR " +
            "(:maxWeightKg IS NULL OR i.maxWeightKg < :maxWeightKg) OR " +
            "(:maxAxleWeightKg IS NULL OR i.maxAxleWeightKg < :maxAxleWeightKg)")
    List<Infrastructure> findPotentialRestrictions(
            @Param("maxHeightCm") Integer maxHeightCm,
            @Param("maxWeightKg") Integer maxWeightKg,
            @Param("maxAxleWeightKg") Integer maxAxleWeightKg);

    @Query("SELECT i FROM Infrastructure i WHERE " +
            "SQRT((i.latitude - :latitude) * (i.latitude - :latitude) + " +
            "(i.longitude - :longitude) * (i.longitude - :longitude)) < :radiusKm / 111.32")
    List<Infrastructure> findNearPoint(@Param("latitude") Double latitude,
                                       @Param("longitude") Double longitude,
                                       @Param("radiusKm") Double radiusKm);

    @Query("SELECT i FROM Infrastructure i WHERE " +
            "i.type = 'BRIDGE' AND i.maxWeightKg < :weightKg")
    List<Infrastructure> findBridgesWithWeightRestriction(@Param("weightKg") Integer weightKg);

    @Query("SELECT i FROM Infrastructure i WHERE " +
            "i.type = 'TUNNEL' AND i.maxHeightCm < :heightCm")
    List<Infrastructure> findTunnelsWithHeightRestriction(@Param("heightCm") Integer heightCm);

    @Query("SELECT i FROM Infrastructure i WHERE " +
            "i.isActive = true AND i.type IN :types")
    List<Infrastructure> findActiveByTypes(@Param("types") List<String> types);
}