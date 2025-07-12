package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.logistic.logisticops.dto.VehicleTrackingDTO;
import pl.logistic.logisticops.model.VehicleTracking;

@Mapper(componentModel = "spring")
public interface VehicleTrackingMapper {

    @Mapping(target = "transportId", source = "transport.id")
    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "vehicleModel", source = "vehicle.model")
    VehicleTrackingDTO toDTO(VehicleTracking vehicleTracking);

    @Mapping(target = "transport", ignore = true)
    @Mapping(target = "vehicle", ignore = true)
    VehicleTracking toEntity(VehicleTrackingDTO dto);
}
