package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.logistic.logisticops.dto.AlertDTO;
import pl.logistic.logisticops.model.Alert;

@Mapper(componentModel = "spring")
public interface AlertMapper {

    @Mapping(target = "relatedTransportId", source = "relatedTransport.id")
    @Mapping(target = "relatedTransportName", source = "relatedTransport.name")
    @Mapping(target = "relatedInfrastructureId", source = "relatedInfrastructure.id")
    @Mapping(target = "relatedInfrastructureName", source = "relatedInfrastructure.name")
    AlertDTO toDTO(Alert alert);

    @Mapping(target = "relatedTransport", ignore = true)
    @Mapping(target = "relatedInfrastructure", ignore = true)
    Alert toEntity(AlertDTO dto);
}