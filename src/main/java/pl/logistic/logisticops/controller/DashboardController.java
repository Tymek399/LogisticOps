package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.dto.DashboardDTO;
import pl.logistic.logisticops.dto.TransportStatisticsDTO;
import pl.logistic.logisticops.service.DashboardService;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ResponseEntity<DashboardDTO> getDashboardData() {
        return ResponseEntity.ok(dashboardService.getDashboardData());
    }

    @GetMapping("/statistics")
    public ResponseEntity<TransportStatisticsDTO> getTransportStatistics() {
        return ResponseEntity.ok(dashboardService.getTransportStatistics());
    }

    @GetMapping("/refresh")
    public ResponseEntity<DashboardDTO> refreshDashboard() {
        return ResponseEntity.ok(dashboardService.refreshDashboardData());
    }
}