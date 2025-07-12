package pl.logistic.logisticops.model;

import lombok.*;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransportSetValidationResult {
    private Long transportSetId;
    private Boolean canPassDirectly;
    private List<Infrastructure> problematicInfrastructure;
    private TransportConstraints constraints;
    private List<String> recommendations;
}