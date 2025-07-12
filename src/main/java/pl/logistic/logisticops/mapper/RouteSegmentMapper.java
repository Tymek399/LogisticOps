package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import pl.logistic.logisticops.dto.RouteSegmentDTO;
import pl.logistic.logisticops.model.RouteSegment;

@Mapper(componentModel = "spring")
public interface RouteSegmentMapper {

    RouteSegmentDTO toDTO(RouteSegment routeSegment);

    RouteSegment toEntity(RouteSegmentDTO dto);
}
