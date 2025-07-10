package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import pl.logistic.logisticops.Model.VehicleSpecification;
import pl.logistic.logisticops.dto.VehicleSpecificationDTO;

@Mapper(componentModel = "spring")
    public interface VehicleSpecificationMapper {
        VehicleSpecificationMapper INSTANCE = Mappers.getMapper(VehicleSpecificationMapper.class);

        VehicleSpecification toEntity(VehicleSpecificationDTO dto);
        VehicleSpecificationDTO toDTO(VehicleSpecification vehicleSpecification);
    }
