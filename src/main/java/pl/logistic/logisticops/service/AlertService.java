package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.dto.AlertDTO;
import pl.logistic.logisticops.enums.AlertLevel;
import pl.logistic.logisticops.mapper.AlertMapper;
import pl.logistic.logisticops.model.*;
import pl.logistic.logisticops.repository.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final TransportRepository transportRepository;
    private final InfrastructureRepository infrastructureRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AlertMapper alertMapper;

    public AlertDTO createAlert(String message, AlertLevel level, Long relatedTransportId,
                                Long relatedInfrastructureId, String type) {
        Transport transport = null;
        Infrastructure infrastructure = null;

        if (relatedTransportId != null) {
            transport = transportRepository.findById(relatedTransportId).orElse(null);
        }

        if (relatedInfrastructureId != null) {
            infrastructure = infrastructureRepository.findById(relatedInfrastructureId).orElse(null);
        }

        Alert alert = Alert.builder()
                .message(message)
                .level(level)
                .type(type)
                .relatedTransport(transport)
                .relatedInfrastructure(infrastructure)
                .resolved(false)
                .timestamp(LocalDateTime.now())
                .build();

        alert = alertRepository.save(alert);

        AlertDTO dto = alertMapper.toDTO(alert);

        // Send real-time alert
        messagingTemplate.convertAndSend("/topic/alerts/new", dto);

        if (relatedTransportId != null) {
            messagingTemplate.convertAndSend("/topic/transport/" + relatedTransportId + "/alerts", dto);
        }

        return dto;
    }

    public Page<AlertDTO> getAllAlerts(Pageable pageable) {
        return alertRepository.findAll(pageable)
                .map(alertMapper::toDTO);
    }

    public List<AlertDTO> getActiveAlerts() {
        return alertRepository.findByResolvedFalseOrderByTimestampDesc()
                .stream()
                .map(alertMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<AlertDTO> getAlertsByLevel(AlertLevel level) {
        return alertRepository.findByLevelOrderByTimestampDesc(level)
                .stream()
                .map(alertMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<AlertDTO> getAlertsByTransport(Long transportId) {
        return alertRepository.findByRelatedTransportIdOrderByTimestampDesc(transportId)
                .stream()
                .map(alertMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<AlertDTO> getRecentAlerts(Integer hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return alertRepository.findByTimestampAfterOrderByTimestampDesc(since)
                .stream()
                .map(alertMapper::toDTO)
                .collect(Collectors.toList());
    }

    public AlertDTO resolveAlert(Long id) {
        Alert alert = alertRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));

        alert.setResolved(true);
        alert.setResolvedAt(LocalDateTime.now());

        alert = alertRepository.save(alert);

        AlertDTO dto = alertMapper.toDTO(alert);

        // Send notification
        messagingTemplate.convertAndSend("/topic/alerts/resolved", dto);

        return dto;
    }

    public void deleteAlert(Long id) {
        alertRepository.deleteById(id);

        // Send notification
        messagingTemplate.convertAndSend("/topic/alerts/deleted",
                Map.of("alertId", id));
    }
}
