package pl.logistic.logisticops.dto;


import java.time.LocalDateTime;
import lombok.*;
import pl.logistic.logisticops.enums.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserDTO {
    private Long id;
    private String username;
    private String email;
    private Role role;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;


}

