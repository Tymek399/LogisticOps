package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;
import pl.logistic.logisticops.Model.Mission;
import pl.logistic.logisticops.dto.MissionDTO;

@Mapper(componentModel = "spring")
    public interface MissionMapper {


        MissionDTO toDTO(Mission mission);
        Mission toEntity(MissionDTO dto);
    }
