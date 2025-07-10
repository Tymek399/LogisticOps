package pl.logistic.logisticops.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportSetDTO {
    private Long id;
    private Long transporterId;  // id VehicleSpecification dla ciężarówki
    private Long cargoId;        // id VehicleSpecification dla transportowanego pojazdu
    private String description;
}
