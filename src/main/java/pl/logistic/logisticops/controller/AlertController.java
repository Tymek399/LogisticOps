package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.dto.AlertDTO;
import pl.logistic.logisticops.enums.AlertLevel;
import pl.logistic.logisticops.service.AlertService;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public ResponseEntity<Page<AlertDTO>> getAllAlerts(Pageable pageable) {
        return ResponseEntity.ok(alertService.getAllAlerts(pageable));
    }

    @GetMapping("/active")
    public ResponseEntity<List<AlertDTO>> getActiveAlerts() {
        return ResponseEntity.ok(alertService.getActiveAlerts());
    }

    @GetMapping("/level/{level}")
    public ResponseEntity<List<AlertDTO>> getAlertsByLevel(@PathVariable AlertLevel level) {
        return ResponseEntity.ok(alertService.getAlertsByLevel(level));
    }

    @GetMapping("/transport/{transportId}")
    public ResponseEntity<List<AlertDTO>> getAlertsByTransport(@PathVariable Long transportId) {
        return ResponseEntity.ok(alertService.getAlertsByTransport(transportId));
    }

    @GetMapping("/recent")
    public ResponseEntity<List<AlertDTO>> getRecentAlerts(@RequestParam(defaultValue = "24") Integer hours) {
        return ResponseEntity.ok(alertService.getRecentAlerts(hours));
    }

    @PatchMapping("/{id}/resolve")
    public ResponseEntity<AlertDTO> resolveAlert(@PathVariable Long id) {
        return ResponseEntity.ok(alertService.resolveAlert(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAlert(@PathVariable Long id) {
        alertService.deleteAlert(id);
        return ResponseEntity.noContent().build();
    }
}