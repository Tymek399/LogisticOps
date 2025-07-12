package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import pl.logistic.logisticops.dto.TransportSetDTO;
import pl.logistic.logisticops.model.TransportSet;

@Mapper(componentModel = "spring")
public interface TransportSetMapper {

    @Mapping(target = "transporterId", source = "transporter.id")
    @Mapping(target = "cargoId", source = "cargo.id")
    TransportSetDTO toDTO(TransportSet transportSet);

    @Mapping(target = "transporter", ignore = true)
    @Mapping(target = "cargo", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    TransportSet toEntity(TransportSetDTO dto);
}