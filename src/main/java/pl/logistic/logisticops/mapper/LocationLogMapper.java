package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import pl.logistic.logisticops.Model.VehicleTracking;
import pl.logistic.logisticops.dto.LocationLogDTO;

@Mapper(componentModel = "spring")
    public interface LocationLogMapper {
        LocationLogMapper INSTANCE = Mappers.getMapper(LocationLogMapper.class);

        LocationLogDTO toDTO(VehicleTracking vehicleTracking);
        VehicleTracking toEntity(LocationLogDTO dto);
    }
