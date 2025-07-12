package pl.logistic.logisticops.mapper;

import org.mapstruct.Mapper;
import pl.logistic.logisticops.model.User;
import pl.logistic.logisticops.dto.UserDTO;

@Mapper(componentModel = "spring")
public interface UserMapper {

    UserDTO toDTO(User user);

    User toEntity(UserDTO dto);
}
