package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.Model.Alert;
import pl.logistic.logisticops.Model.Transport;
import pl.logistic.logisticops.enums.AlertLevel;
import pl.logistic.logisticops.repository.AlertRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RouteOptimizationService routeOptimizationService;

    public Alert createAlert(String message, AlertLevel level, Long relatedUnitId, String type) {
        Alert alert = Alert.builder()
                .message(message)
                .level(level)
                .relatedUnitId(relatedUnitId)
                .timestamp(LocalDateTime.now())
                .build();

        alert = alertRepository.save(alert);

        // Send real-time alert
        messagingTemplate.convertAndSend("/topic/alerts", alert);

        return alert;
    }

    public void processTrafficAlert(Long transportId, String location) {
        Alert alert = createAlert(
                "Wysokie natężenie ruchu w " + location,
                AlertLevel.High,
                transportId,
                "TRAFFIC"
        );

        // Generate alternative route suggestion
        generateAlternativeRouteSuggestion(transportId);
    }

    public void processInfrastructureAlert(Long transportId, String infrastructureName, String issue) {
        Alert alert = createAlert(
                infrastructureName + " - " + issue,
                AlertLevel.High,
                transportId,
                "INFRASTRUCTURE"
        );
    }

    private void generateAlternativeRouteSuggestion(Long transportId) {
        // This would generate a new route from current location to destination
        // avoiding traffic congestion
        messagingTemplate.convertAndSend("/topic/transport/" + transportId + "/route-suggestion",
                Map.of("type", "ALTERNATIVE", "reason", "TRAFFIC_AVOIDANCE"));
    }

    public List<Alert> getActiveAlerts() {
        return alertRepository.findByTimestampAfterOrderByTimestampDesc(
                LocalDateTime.now().minusHours(24)
        );
    }
}
