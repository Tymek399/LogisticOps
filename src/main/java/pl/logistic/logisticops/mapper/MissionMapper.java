package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import pl.logistic.logisticops.model.Mission;
import pl.logistic.logisticops.dto.MissionDTO;

@Mapper(componentModel = "spring")
public interface MissionMapper {

    MissionDTO toDTO(Mission mission);

    Mission toEntity(MissionDTO dto);
}