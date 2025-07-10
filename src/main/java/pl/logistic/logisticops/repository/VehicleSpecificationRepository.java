package pl.logistic.logisticops.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.logistic.logisticops.Model.VehicleSpecification;

import java.util.List;

public interface VehicleSpecificationRepository extends JpaRepository<VehicleSpecification, Long> {
    
    List<VehicleSpecification> findByType(String type);
    
    List<VehicleSpecification> findByModelContaining(String model);
    
    List<VehicleSpecification> findByTotalWeightKgLessThan(Integer weight);
    
    List<VehicleSpecification> findByHeightCmLessThan(Integer height);
}