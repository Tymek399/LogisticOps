package pl.logistic.logisticops.dto.request;

import lombok.*;
import jakarta.validation.constraints.*;
import pl.logistic.logisticops.enums.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateUserRequestDTO {

    @NotNull(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @NotNull(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @NotNull(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @Size(max = 50, message = "First name cannot exceed 50 characters")
    private String firstName;

    @Size(max = 50, message = "Last name cannot exceed 50 characters")
    private String lastName;

    @NotNull(message = "Role is required")
    private Role role;
}