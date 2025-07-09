package pl.logistic.logisticops.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.logistic.logisticops.Model.VehicleSpecification;
import pl.logistic.logisticops.repository.VehicleSpecificationRepository;

import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class VehicleSpecificationService {

    private final VehicleSpecificationRepository vehicleSpecRepo;

    public VehicleSpecificationService(VehicleSpecificationRepository vehicleSpecRepo) {
        this.vehicleSpecRepo = vehicleSpecRepo;
    }

    public List<VehicleSpecification> findAll() {
        return vehicleSpecRepo.findAll();
    }

    public Optional<VehicleSpecification> findById(Long id) {
        return vehicleSpecRepo.findById(id);
    }

    public VehicleSpecification save(VehicleSpecification vehicleSpecification) {
        return vehicleSpecRepo.save(vehicleSpecification);
    }

    public void deleteById(Long id) {
        vehicleSpecRepo.deleteById(id);
    }
}
