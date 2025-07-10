package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.logistic.logisticops.Model.Infrastructure;

import java.util.List;

public interface ObstacleRepository extends JpaRepository<Infrastructure, Long> {
    
    List<Infrastructure> findByType(String type);
    
    List<Infrastructure> findByMaxHeightCmLessThan(int height);
    
    List<Infrastructure> findByMaxWeightTonsLessThan(double weight);
    
    @Query("SELECT o FROM Infrastructure o WHERE " +
            "o.type = 'BRIDGE' AND o.maxWeightTons < :totalWeightTons OR " +
            "o.type = 'TUNNEL' AND o.maxHeightCm < :maxHeightCm")
    List<Infrastructure> findPotentialObstacles(
            @Param("totalWeightTons") double totalWeightTons,
            @Param("maxHeightCm") int maxHeightCm);
    
    @Query(value = "SELECT * FROM obstacles o " +
            "WHERE ST_Distance(" +
            "    ST_SetSRID(ST_MakePoint(o.longitude, o.latitude), 4326)," +
            "    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)" +
            ") < :radiusKm / 111.32", nativeQuery = true)
    List<Infrastructure> findObstaclesNearPoint(
            @Param("lat") double latitude,
            @Param("lon") double longitude,
            @Param("radiusKm") double radiusKm);
    
    // To jest metoda, która powinna być zaimplementowana z użyciem PostGIS lub innej bazy danych
    // obsługującej dane przestrzenne. Tutaj jest tylko przykładowa sygnatura.
    @Query(value = "/* Tu byłoby zapytanie SQL używające PostGIS */", nativeQuery = true)
    List<Infrastructure> findObstaclesNearRoute(
            @Param("fromPoints") List<String> fromPoints,
            @Param("toPoints") List<String> toPoints,
            @Param("maxHeight") int maxHeight,
            @Param("maxAxleLoad") int maxAxleLoad,
            @Param("totalWeight") int totalWeight);
}