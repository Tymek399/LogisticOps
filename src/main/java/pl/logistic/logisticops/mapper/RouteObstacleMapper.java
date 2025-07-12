package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.logistic.logisticops.dto.RouteObstacleDTO;
import pl.logistic.logisticops.model.RouteObstacle;

@Mapper(componentModel = "spring")
public interface RouteObstacleMapper {

    @Mapping(target = "routeProposalId", source = "routeProposal.id")
    @Mapping(target = "infrastructureId", source = "infrastructure.id")
    @Mapping(target = "infrastructureName", source = "infrastructure.name")
    @Mapping(target = "infrastructureType", source = "infrastructure.type")
    RouteObstacleDTO toDTO(RouteObstacle routeObstacle);

    @Mapping(target = "routeProposal", ignore = true)
    @Mapping(target = "infrastructure", ignore = true)
    RouteObstacle toEntity(RouteObstacleDTO dto);
}