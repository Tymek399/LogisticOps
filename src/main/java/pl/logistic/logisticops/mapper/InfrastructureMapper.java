package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import pl.logistic.logisticops.dto.InfrastructureDTO;
import pl.logistic.logisticops.model.Infrastructure;

@Mapper(componentModel = "spring")
public interface InfrastructureMapper {

    InfrastructureDTO toDTO(Infrastructure infrastructure);

    Infrastructure toEntity(InfrastructureDTO dto);
}
