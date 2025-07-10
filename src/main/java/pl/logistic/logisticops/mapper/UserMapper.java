package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;
import pl.logistic.logisticops.Model.User;
import pl.logistic.logisticops.dto.UserDTO;

@Mapper(componentModel = "spring")
    public interface UserMapper {


    @Mapping(target = "role", ignore = true)
        UserDTO toDTO(User user);
    @Mapping(target = "role", ignore = true)
        User toEntity(UserDTO dto);
    }
