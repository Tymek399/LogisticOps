package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.model.TransportVehicle;

import java.util.List;

@Repository
public interface TransportVehicleRepository extends JpaRepository<TransportVehicle, Long> {

    List<TransportVehicle> findByTransportId(Long transportId);

    List<TransportVehicle> findByVehicleId(Long vehicleId);

    List<TransportVehicle> findByRole(String role);

    List<TransportVehicle> findByTransportIdOrderBySequenceOrder(Long transportId);

    @Query("SELECT tv FROM TransportVehicle tv WHERE tv.transport.id = :transportId " +
            "AND tv.role = :role")
    List<TransportVehicle> findByTransportIdAndRole(@Param("transportId") Long transportId,
                                                    @Param("role") String role);

    @Query("SELECT COUNT(tv) FROM TransportVehicle tv WHERE tv.transport.id = :transportId")
    Long countByTransportId(@Param("transportId") Long transportId);

    @Query("SELECT tv FROM TransportVehicle tv WHERE tv.vehicle.id = :vehicleId " +
            "AND tv.transport.status IN ('PLANNED', 'APPROVED', 'READY_TO_DEPART', 'IN_TRANSIT')")
    List<TransportVehicle> findActiveAssignmentsByVehicleId(@Param("vehicleId") Long vehicleId);
}
