package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.Model.*;
import pl.logistic.logisticops.repository.*;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TransportSetOptimizationService {

    private final TransportSetRepository transportSetRepository;
    private final VehicleSpecificationRepository vehicleRepository;
    private final InfrastructureRepository infrastructureRepository;

    /**
     * Validates transport set against route infrastructure
     * Returns optimization recommendations
     */
    public TransportSetValidationResult validateTransportSet(Long transportSetId, List<Infrastructure> routeInfrastructure) {
        TransportSet transportSet = transportSetRepository.findById(transportSetId)
                .orElseThrow(() -> new IllegalArgumentException("Transport set not found"));

        TransportConstraints constraints = calculateSetConstraints(transportSet);

        List<Infrastructure> problematicInfrastructure = routeInfrastructure.stream()
                .filter(infra -> isProblematicForSet(infra, constraints))
                .toList();

        return TransportSetValidationResult.builder()
                .transportSetId(transportSetId)
                .canPassDirectly(problematicInfrastructure.isEmpty())
                .problematicInfrastructure(problematicInfrastructure)
                .constraints(constraints)
                .recommendations(generateRecommendations(problematicInfrastructure))
                .build();
    }

    /**
     * Calculates combined constraints for transport set (transporter + cargo)
     */
    public TransportConstraints calculateSetConstraints(TransportSet transportSet) {
        VehicleSpecification transporter = transportSet.getTransporter();
        VehicleSpecification cargo = transportSet.getCargo();

        // Height is maximum of both vehicles
        int maxHeight = Math.max(
                transporter.getHeightCm() != null ? transporter.getHeightCm() : 0,
                cargo.getHeightCm() != null ? cargo.getHeightCm() : 0
        );

        // Weight is sum of both vehicles
        int totalWeight = (transporter.getTotalWeightKg() != null ? transporter.getTotalWeightKg() : 0) +
                (cargo.getTotalWeightKg() != null ? cargo.getTotalWeightKg() : 0);

        // Axle load is maximum of both (considering load distribution)
        int maxAxleLoad = Math.max(
                transporter.getMaxAxleLoadKg() != null ? transporter.getMaxAxleLoadKg() : 0,
                cargo.getMaxAxleLoadKg() != null ? cargo.getMaxAxleLoadKg() : 0
        );

        return TransportConstraints.builder()
                .maxHeightCm(maxHeight)
                .totalWeightKg(totalWeight)
                .maxAxleLoadKg(maxAxleLoad)
                .transporterModel(transporter.getModel())
                .cargoModel(cargo.getModel())
                .build();
    }

    private boolean isProblematicForSet(Infrastructure infra, TransportConstraints constraints) {
        // Height check for tunnels
        if ("TUNNEL".equals(infra.getType()) &&
                infra.getMaxHeightCm() != null &&
                constraints.getMaxHeightCm() > infra.getMaxHeightCm()) {
            return true;
        }

        // Weight check for bridges
        if ("BRIDGE".equals(infra.getType()) &&
                infra.getMaxWeightKg() != null &&
                constraints.getTotalWeightKg() > infra.getMaxWeightKg()) {
            return true;
        }

        // Axle load check
        if (infra.getMaxAxleWeightKg() != null &&
                constraints.getMaxAxleLoadKg() > infra.getMaxAxleWeightKg()) {
            return true;
        }

        return false;
    }

    private List<String> generateRecommendations(List<Infrastructure> problematicInfrastructure) {
        List<String> recommendations = new ArrayList<>();

        for (Infrastructure infra : problematicInfrastructure) {
            switch (infra.getType()) {
                case "BRIDGE":
                    recommendations.add("Ominąć " + infra.getName() + " - alternatywna przeprawa");
                    break;
                case "TUNNEL":
                    recommendations.add("Ominąć " + infra.getName() + " - trasa powierzchniowa");
                    break;
                default:
                    recommendations.add("Ominąć " + infra.getName() + " - znaleźć objazd");
            }
        }

        return recommendations;
    }
}