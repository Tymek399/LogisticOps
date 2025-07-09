package pl.logistic.logisticops.mapper;


import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import pl.logistic.logisticops.Model.*;
import pl.logistic.logisticops.dto.*;


@Mapper(componentModel = "spring")
    public interface TransportSetMapper {
        TransportSetMapper INSTANCE = Mappers.getMapper(TransportSetMapper.class);


        TransportSetDTO toDTO(TransportSet transportSet);

        TransportSet toEntity(TransportSetDTO dto);
    }

