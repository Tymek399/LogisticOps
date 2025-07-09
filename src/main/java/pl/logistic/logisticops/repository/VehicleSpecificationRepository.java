package pl.logistic.logisticops.reposiotry;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.logistic.logisticops.Model.VehicleSpecification;

@Repository
public interface VehicleSpecificationRepository extends JpaRepository<VehicleSpecification, Long> {
    // Możesz dodać metody wyszukiwania jeśli potrzebujesz, np. findByType itp.
}
