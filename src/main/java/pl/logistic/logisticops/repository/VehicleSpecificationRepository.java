package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.enums.VehicleType;
import pl.logistic.logisticops.model.VehicleSpecification;

import java.util.List;

@Repository
public interface VehicleSpecificationRepository extends JpaRepository<VehicleSpecification, Long> {

    List<VehicleSpecification> findByType(VehicleType type);

    List<VehicleSpecification> findByType(String type);

    List<VehicleSpecification> findByModelContaining(String model);

    List<VehicleSpecification> findByTotalWeightKgLessThan(Integer weight);

    List<VehicleSpecification> findByHeightCmLessThan(Integer height);

    List<VehicleSpecification> findByActiveTrue();

    List<VehicleSpecification> findByActiveTrueAndType(VehicleType type);

    @Query("SELECT COUNT(v) FROM VehicleSpecification v WHERE v.active = true")
    Long countByActiveTrue();
}