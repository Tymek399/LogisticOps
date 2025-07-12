package pl.logistic.logisticops.dto;

import lombok.*;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RouteValidationDTO {
    private Long routeId;
    private Boolean isValid;
    private List<String> warnings;
    private List<String> errors;
    private Map<String, Object> validationDetails;
    private List<InfrastructureDTO> problematicInfrastructure;
}
