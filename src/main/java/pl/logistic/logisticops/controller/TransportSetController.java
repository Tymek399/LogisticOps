package pl.logistic.logisticops.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.logistic.logisticops.dto.TransportSetDTO;
import pl.logistic.logisticops.service.TransportSetService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transport-sets")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TransportSetController {

    private final TransportSetService transportSetService;

    @GetMapping
    public ResponseEntity<List<TransportSetDTO>> getAllTransportSets() {
        return ResponseEntity.ok(transportSetService.getAllTransportSets());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransportSetDTO> getTransportSetById(@PathVariable Long id) {
        return transportSetService.getTransportSetById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<TransportSetDTO> createTransportSet(@RequestBody Map<String, Object> request) {
        Long transporterId = Long.valueOf(request.get("transporterId").toString());
        Long cargoId = Long.valueOf(request.get("cargoId").toString());
        String description = (String) request.get("description");

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transportSetService.createTransportSet(transporterId, cargoId, description));
    }

    @GetMapping("/{transportSetId}/can-pass/{obstacleId}")
    public ResponseEntity<Boolean> canPassObstacle(@PathVariable Long transportSetId,
                                                   @PathVariable Long obstacleId) {
        return ResponseEntity.ok(transportSetService.canPassObstacle(transportSetId, obstacleId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTransportSet(@PathVariable Long id) {
        transportSetService.deleteTransportSet(id);
        return ResponseEntity.noContent().build();
    }
}