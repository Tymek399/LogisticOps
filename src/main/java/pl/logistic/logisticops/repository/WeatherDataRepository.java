package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.logistic.logisticops.Model.WeatherData;

import java.time.LocalDateTime;
import java.util.List;

public interface WeatherDataRepository extends JpaRepository<WeatherData, Long> {

    List<WeatherData> findByForecastTimeBetween(LocalDateTime start, LocalDateTime end);

    @Query(value = "SELECT * FROM weather_data w " +
            "WHERE ST_Distance(" +
            "    ST_SetSRID(ST_MakePoint(w.longitude, w.latitude), 4326)," +
            "    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)" +
            ") < :radiusKm / 111.32 " +
            "AND w.forecast_time >= :startTime " +
            "ORDER BY w.forecast_time DESC LIMIT 1", nativeQuery = true)
    WeatherData findLatestWeatherNear(
            @Param("lat") Double latitude,
            @Param("lon") Double longitude,
            @Param("radiusKm") Double radiusKm,
            @Param("startTime") LocalDateTime startTime
    );

    List<WeatherData> findByRoadConditionsAndForecastTimeAfter(String roadConditions, LocalDateTime time);
}