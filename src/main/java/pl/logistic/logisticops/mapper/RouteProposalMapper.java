package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.logistic.logisticops.dto.RouteProposalDTO;
import pl.logistic.logisticops.model.RouteProposal;

@Mapper(componentModel = "spring", uses = {RouteSegmentMapper.class, RouteObstacleMapper.class})
public interface RouteProposalMapper {

    @Mapping(target = "missionName", source = "mission.name")
    @Mapping(target = "obstacleCount", expression = "java(routeProposal.getObstacles() != null ? routeProposal.getObstacles().size() : 0)")
    @Mapping(target = "summary", expression = "java(generateSummary(routeProposal))")
    RouteProposalDTO toDTO(RouteProposal routeProposal);

    @Mapping(target = "mission", ignore = true)
    @Mapping(target = "segments", ignore = true)
    @Mapping(target = "obstacles", ignore = true)
    @Mapping(target = "transports", ignore = true)
    RouteProposal toEntity(RouteProposalDTO dto);

    default String generateSummary(RouteProposal routeProposal) {
        if (routeProposal == null) return "";

        StringBuilder summary = new StringBuilder();
        if (routeProposal.getTotalDistanceKm() != null) {
            summary.append(String.format("%.1f km", routeProposal.getTotalDistanceKm()));
        }
        if (routeProposal.getEstimatedTimeMinutes() != null) {
            if (summary.length() > 0) summary.append(" • ");
            summary.append(String.format("%.0f min", routeProposal.getEstimatedTimeMinutes()));
        }
        if (routeProposal.getObstacles() != null && !routeProposal.getObstacles().isEmpty()) {
            if (summary.length() > 0) summary.append(" • ");
            summary.append(routeProposal.getObstacles().size()).append(" obstacles");
        }
        return summary.toString();
    }
}
