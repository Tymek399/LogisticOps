package pl.logistic.logisticops.service;

public class InfrastructureAlertService {
    package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.Model.*;
import pl.logistic.logisticops.enums.AlertLevel;
import pl.logistic.logisticops.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

    @Service
    @RequiredArgsConstructor
    public class InfrastructureAlertService {

        private final AlertRepository alertRepository;
        private final TransportRepository transportRepository;
        private final SimpMessagingTemplate messagingTemplate;

        public void processInfrastructureAlert(Infrastructure infrastructure, String alertType, String details) {
            // Create infrastructure alert
            Alert infrastructureAlert = Alert.builder()
                    .message(String.format("%s alert for %s: %s", alertType, infrastructure.getName(), details))
                    .level(determineAlertLevel(alertType))
                    .relatedUnitId(null)
                    .timestamp(LocalDateTime.now())
                    .build();

            alertRepository.save(infrastructureAlert);

            // Find affected transports
            List<Transport> affectedTransports = findTransportsAffectedByInfrastructure(infrastructure);

            for (Transport transport : affectedTransports) {
                // Create transport-specific alert
                Alert transportAlert = Alert.builder()
                        .message(String.format("Route affected: %s", infrastructureAlert.getMessage()))
                        .level(AlertLevel.High)
                        .relatedUnitId(transport.getId())
                        .timestamp(LocalDateTime.now())
                        .build();

                alertRepository.save(transportAlert);

                // Send real-time notification
                messagingTemplate.convertAndSend("/topic/transport/" + transport.getId() + "/alerts",
                        Map.of(
                                "alert", transportAlert,
                                "infrastructure", infrastructure,
                                "actionRequired", true,
                                "suggestedActions", generateSuggestedActions(infrastructure, alertType)
                        ));
            }

            // Broadcast general infrastructure alert
            messagingTemplate.convertAndSend("/topic/infrastructure/alerts",
                    Map.of(
                            "alert", infrastructureAlert,
                            "infrastructure", infrastructure,
                            "affectedTransports", affectedTransports.size()
                    ));
        }

        private AlertLevel determineAlertLevel(String alertType) {
            return switch (alertType.toUpperCase()) {
                case "CLOSURE", "EMERGENCY" -> AlertLevel.High;
                case "RESTRICTION", "MAINTENANCE" -> AlertLevel.Medium;
                default -> AlertLevel.Low;
            };
        }

        private List<Transport> findTransportsAffectedByInfrastructure(Infrastructure infrastructure) {
            // Find transports whose routes might be affected
            return transportRepository.findActiveWithLocation().stream()
                    .filter(transport -> isTransportRouteAffected(transport, infrastructure))
                    .toList();
        }

        private boolean isTransportRouteAffected(Transport transport, Infrastructure infrastructure) {
            // Implement logic to determine if transport route is affected
            // This would involve spatial analysis of the route vs infrastructure location
            return true; // Simplified for demo
        }

        private List<String> generateSuggestedActions(Infrastructure infrastructure, String alertType) {
            return switch (alertType.toUpperCase()) {
                case "CLOSURE" -> List.of(
                        "Find alternative route",
                        "Calculate detour via secondary roads",
                        "Estimate additional time and fuel costs"
                );
                case "RESTRICTION" -> List.of(
                        "Check if transport set meets new restrictions",
                        "Consider route modification if needed",
                        "Update ETA calculations"
                );
                case "MAINTENANCE" -> List.of(
                        "Monitor for potential delays",
                        "Consider alternative timing",
                        "Prepare contingency routes"
                );
                default -> List.of("Monitor situation", "Be prepared for route changes");
            };
        }
    }
}
