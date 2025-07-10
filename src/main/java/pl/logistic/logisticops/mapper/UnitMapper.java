package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import pl.logistic.logisticops.Model.Transport;
import pl.logistic.logisticops.dto.UnitDTO;

@Mapper(componentModel = "spring")
    public interface UnitMapper {
        UnitMapper INSTANCE = Mappers.getMapper(UnitMapper.class);

        UnitDTO toDTO(Transport transport);
        Transport toEntity(UnitDTO dto);
    }
