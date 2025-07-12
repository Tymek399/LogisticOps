package pl.logistic.logisticops.dto.request;

import lombok.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTransportRequestDTO {

    @NotNull(message = "Transport name is required")
    @Size(min = 2, max = 100, message = "Transport name must be between 2 and 100 characters")
    private String name;

    @NotNull(message = "Mission ID is required")
    private Long missionId;

    private Long routeProposalId;

    @NotEmpty(message = "At least one vehicle is required")
    @Valid
    private List<VehicleAssignmentDTO> vehicles;

    private LocalDateTime plannedDeparture;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VehicleAssignmentDTO {

        @NotNull(message = "Vehicle ID is required")
        private Long vehicleId;

        @NotNull(message = "Vehicle role is required")
        private String role; // TRANSPORTER, CARGO

        private Integer sequenceOrder;
    }
}
