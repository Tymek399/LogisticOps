package pl.logistic.logisticops.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportVehicleDTO {
    private Long id;
    private Long transportId;
    private Long vehicleId;
    private String vehicleModel;
    private String role;
    private Integer sequenceOrder;
}
