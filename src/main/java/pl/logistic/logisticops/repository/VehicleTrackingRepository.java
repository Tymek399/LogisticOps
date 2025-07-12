package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.model.VehicleTracking;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VehicleTrackingRepository extends JpaRepository<VehicleTracking, Long> {

    List<VehicleTracking> findByTransportId(Long transportId);

    List<VehicleTracking> findByVehicleId(Long vehicleId);

    List<VehicleTracking> findByVehicleIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            Long vehicleId, LocalDateTime from, LocalDateTime to);

    List<VehicleTracking> findByTransportIdAndRecordedAtBetweenOrderByRecordedAtDesc(
            Long transportId, LocalDateTime from, LocalDateTime to);

    @Query("SELECT vt FROM VehicleTracking vt WHERE vt.transport.id = :transportId " +
            "AND vt.recordedAt = (SELECT MAX(vt2.recordedAt) FROM VehicleTracking vt2 " +
            "WHERE vt2.vehicle.id = vt.vehicle.id AND vt2.transport.id = :transportId)")
    List<VehicleTracking> findLatestByTransportId(@Param("transportId") Long transportId);

    @Query("SELECT vt FROM VehicleTracking vt WHERE vt.vehicle.id = :vehicleId " +
            "ORDER BY vt.recordedAt DESC LIMIT 1")
    Optional<VehicleTracking> findLatestByVehicleId(@Param("vehicleId") Long vehicleId);

    @Query("SELECT vt FROM VehicleTracking vt WHERE " +
            "vt.fuelLevelPercentage < :threshold ORDER BY vt.recordedAt DESC")
    List<VehicleTracking> findByLowFuelLevel(@Param("threshold") Double threshold);

    @Query("SELECT vt FROM VehicleTracking vt WHERE " +
            "vt.speedKmh > :speedLimit ORDER BY vt.recordedAt DESC")
    List<VehicleTracking> findByExcessiveSpeed(@Param("speedLimit") Double speedLimit);
}