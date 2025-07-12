package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.logistic.logisticops.dto.TransportDTO;
import pl.logistic.logisticops.model.Transport;

@Mapper(componentModel = "spring", uses = {TransportVehicleMapper.class})
public interface TransportMapper {

    @Mapping(target = "missionId", source = "mission.id")
    @Mapping(target = "missionName", source = "mission.name")
    @Mapping(target = "approvedRouteId", source = "approvedRoute.id")
    TransportDTO toDTO(Transport transport);

    @Mapping(target = "mission", ignore = true)
    @Mapping(target = "approvedRoute", ignore = true)
    @Mapping(target = "vehicles", ignore = true)
    @Mapping(target = "trackingHistory", ignore = true)
    Transport toEntity(TransportDTO dto);
}
