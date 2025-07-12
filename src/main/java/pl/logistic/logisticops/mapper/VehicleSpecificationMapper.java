package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import pl.logistic.logisticops.model.VehicleSpecification;
import pl.logistic.logisticops.dto.VehicleSpecificationDTO;

@Mapper(componentModel = "spring")
public interface VehicleSpecificationMapper {

    VehicleSpecificationDTO toDTO(VehicleSpecification vehicleSpecification);

    VehicleSpecification toEntity(VehicleSpecificationDTO dto);
}