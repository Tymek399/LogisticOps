package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import pl.logistic.logisticops.model.Infrastructure;
import pl.logistic.logisticops.repository.InfrastructureRepository;
import pl.logistic.logisticops.service.RealTimeInfrastructureService;

import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class WebSocketInfrastructureController {

    private final InfrastructureRepository infrastructureRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final RealTimeInfrastructureService realTimeService;

    @MessageMapping("/infrastructure/subscribe")
    @SendTo("/topic/infrastructure/all")
    public List<Infrastructure> subscribeToInfrastructureUpdates() {
        return infrastructureRepository.findByIsActiveTrue();
    }

    @MessageMapping("/infrastructure/check-status")
    public void checkInfrastructureStatus(Map<String, Object> request) {
        // Trigger manual infrastructure status check
        realTimeService.monitorInfrastructureStatus();

        messagingTemplate.convertAndSend("/topic/infrastructure/status-check",
                Map.of("status", "completed", "timestamp", System.currentTimeMillis()));
    }

    @MessageMapping("/infrastructure/get-near-point")
    public void getInfrastructureNearPoint(Map<String, Object> request) {
        Double latitude = Double.valueOf(request.get("latitude").toString());
        Double longitude = Double.valueOf(request.get("longitude").toString());
        Double radius = request.get("radius") != null ?
                Double.valueOf(request.get("radius").toString()) : 10.0;

        List<Infrastructure> nearbyInfrastructure =
                infrastructureRepository.findNearPoint(latitude, longitude, radius);

        messagingTemplate.convertAndSend("/topic/infrastructure/nearby", Map.of(
                "requestId", request.get("requestId"),
                "infrastructure", nearbyInfrastructure,
                "searchCenter", Map.of("lat", latitude, "lng", longitude),
                "radius", radius
        ));
    }
}