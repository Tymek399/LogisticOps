package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.logistic.logisticops.dto.TransportVehicleDTO;
import pl.logistic.logisticops.model.TransportVehicle;

@Mapper(componentModel = "spring")
public interface TransportVehicleMapper {

    @Mapping(target = "transportId", source = "transport.id")
    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleModel", source = "vehicle.model")
    TransportVehicleDTO toDTO(TransportVehicle transportVehicle);

    @Mapping(target = "transport", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    TransportVehicle toEntity(TransportVehicleDTO dto);
}
