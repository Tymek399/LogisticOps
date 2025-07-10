package pl.logistic.logisticops.Model;

public class TransportSetValidationResult {
    @Data
    @Builder
    class TransportSetValidationResult {
        private Long transportSetId;
        private boolean canPassDirectly;
        private List<Infrastructure> problematicInfrastructure;
        private TransportConstraints constraints;
        private List<String> recommendations;
    }

}
