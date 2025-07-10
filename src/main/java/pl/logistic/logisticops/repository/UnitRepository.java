package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.logistic.logisticops.Model.Transport;
import pl.logistic.logisticops.enums.UnitStatus;
import pl.logistic.logisticops.enums.UnitType;

import java.math.BigDecimal;
import java.util.List;

public interface UnitRepository extends JpaRepository<Transport, Long> {
    
    List<Transport> findByType(UnitType type);
    
    List<Transport> findByStatus(UnitStatus status);
    
    @Query("SELECT u FROM Transport u WHERE " +
            "u.latitude BETWEEN :minLat AND :maxLat AND " +
            "u.longitude BETWEEN :minLon AND :maxLon")
    List<Transport> findUnitsInArea(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLon") BigDecimal minLon,
            @Param("maxLon") BigDecimal maxLon);
    
    @Query(value = "SELECT * FROM unit u " +
            "WHERE ST_Distance(" +
            "    ST_SetSRID(ST_MakePoint(u.longitude, u.latitude), 4326)," +
            "    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)" +
            ") < :radiusKm / 111.32", nativeQuery = true)
    List<Transport> findUnitsNearPoint(
            @Param("lat") BigDecimal latitude,
            @Param("lon") BigDecimal longitude,
            @Param("radiusKm") double radiusKm);
}