package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.logistic.logisticops.model.Infrastructure;
import pl.logistic.logisticops.repository.InfrastructureRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolishInfrastructureService {

    private final InfrastructureRepository infrastructureRepository;
    private final RestTemplate restTemplate;

    /**
     * Synchronizuje dane o infrastrukturze Polski z zewnętrznych źródeł
     */
    @Scheduled(fixedRate = 3600000) // Co godzinę
    public void syncInfrastructureData() {
        log.info("Starting infrastructure data synchronization");

        try {
            syncBridges();
            syncTunnels();
            syncWeightStations();
            log.info("Infrastructure data synchronization completed successfully");
        } catch (Exception e) {
            log.error("Error during infrastructure synchronization", e);
        }
    }

    public void manualSync() {
        syncInfrastructureData();
    }

    private void syncBridges() {
        try {
            // API GDDKiA dla mostów
            String url = "https://api.gddkia.gov.pl/infrastructure/bridges";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("bridges")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> bridges = (List<Map<String, Object>>) response.get("bridges");

                for (Map<String, Object> bridgeData : bridges) {
                    processBridgeData(bridgeData);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to sync bridges data: {}", e.getMessage());
            // Fallback - załaduj przykładowe dane
            loadSampleBridges();
        }
    }

    private void syncTunnels() {
        try {
            // API GDDKiA dla tuneli
            String url = "https://api.gddkia.gov.pl/infrastructure/tunnels";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("tunnels")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> tunnels = (List<Map<String, Object>>) response.get("tunnels");

                for (Map<String, Object> tunnelData : tunnels) {
                    processTunnelData(tunnelData);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to sync tunnels data: {}", e.getMessage());
            // Fallback - załaduj przykładowe dane
            loadSampleTunnels();
        }
    }

    private void syncWeightStations() {
        try {
            // API dla stacji ważenia
            String url = "https://api.gddkia.gov.pl/infrastructure/weight-stations";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("stations")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> stations = (List<Map<String, Object>>) response.get("stations");

                for (Map<String, Object> stationData : stations) {
                    processWeightStationData(stationData);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to sync weight stations data: {}", e.getMessage());
        }
    }

    private void processBridgeData(Map<String, Object> bridgeData) {
        String externalId = "GDDKIA_BRIDGE_" + bridgeData.get("id");

        Infrastructure existing = infrastructureRepository.findByExternalId(externalId);

        if (existing == null) {
            Infrastructure bridge = Infrastructure.builder()
                    .externalId(externalId)
                    .name((String) bridgeData.get("name"))
                    .type("BRIDGE")
                    .latitude(((Number) bridgeData.get("latitude")).doubleValue())
                    .longitude(((Number) bridgeData.get("longitude")).doubleValue())
                    .roadNumber((String) bridgeData.get("roadNumber"))
                    .maxWeightKg(parseInteger(bridgeData.get("maxWeight")))
                    .maxAxleWeightKg(parseInteger(bridgeData.get("maxAxleWeight")))
                    .isActive(true)
                    .description("Most na drodze " + bridgeData.get("roadNumber"))
                    .build();

            infrastructureRepository.save(bridge);
            log.debug("Added new bridge: {}", bridge.getName());
        } else {
            // Aktualizuj istniejący
            existing.setMaxWeightKg(parseInteger(bridgeData.get("maxWeight")));
            existing.setMaxAxleWeightKg(parseInteger(bridgeData.get("maxAxleWeight")));
            existing.setUpdatedAt(LocalDateTime.now());
            infrastructureRepository.save(existing);
        }
    }

    private void processTunnelData(Map<String, Object> tunnelData) {
        String externalId = "GDDKIA_TUNNEL_" + tunnelData.get("id");

        Infrastructure existing = infrastructureRepository.findByExternalId(externalId);

        if (existing == null) {
            Infrastructure tunnel = Infrastructure.builder()
                    .externalId(externalId)
                    .name((String) tunnelData.get("name"))
                    .type("TUNNEL")
                    .latitude(((Number) tunnelData.get("latitude")).doubleValue())
                    .longitude(((Number) tunnelData.get("longitude")).doubleValue())
                    .roadNumber((String) tunnelData.get("roadNumber"))
                    .maxHeightCm(parseInteger(tunnelData.get("maxHeight")))
                    .maxWeightKg(parseInteger(tunnelData.get("maxWeight")))
                    .isActive(true)
                    .description("Tunel na drodze " + tunnelData.get("roadNumber"))
                    .build();

            infrastructureRepository.save(tunnel);
            log.debug("Added new tunnel: {}", tunnel.getName());
        } else {
            // Aktualizuj istniejący
            existing.setMaxHeightCm(parseInteger(tunnelData.get("maxHeight")));
            existing.setMaxWeightKg(parseInteger(tunnelData.get("maxWeight")));
            existing.setUpdatedAt(LocalDateTime.now());
            infrastructureRepository.save(existing);
        }
    }

    private void processWeightStationData(Map<String, Object> stationData) {
        String externalId = "GDDKIA_STATION_" + stationData.get("id");

        Infrastructure existing = infrastructureRepository.findByExternalId(externalId);

        if (existing == null) {
            Infrastructure station = Infrastructure.builder()
                    .externalId(externalId)
                    .name((String) stationData.get("name"))
                    .type("WEIGHT_STATION")
                    .latitude(((Number) stationData.get("latitude")).doubleValue())
                    .longitude(((Number) stationData.get("longitude")).doubleValue())
                    .roadNumber((String) stationData.get("roadNumber"))
                    .isActive(true)
                    .description("Stacja ważenia na drodze " + stationData.get("roadNumber"))
                    .build();

            infrastructureRepository.save(station);
            log.debug("Added new weight station: {}", station.getName());
        }
    }

    private void loadSampleBridges() {
        // Przykładowe mosty w Polsce - rzeczywiste lokalizacje
        List<Infrastructure> sampleBridges = List.of(
                Infrastructure.builder()
                        .externalId("SAMPLE_BRIDGE_001")
                        .name("Most im. Marii Skłodowskiej-Curie")
                        .type("BRIDGE")
                        .latitude(52.2473)
                        .longitude(21.0362)
                        .roadNumber("S8")
                        .maxWeightKg(40000)
                        .maxAxleWeightKg(10000)
                        .isActive(true)
                        .description("Most w Warszawie")
                        .build(),

                Infrastructure.builder()
                        .externalId("SAMPLE_BRIDGE_002")
                        .name("Most Grunwaldzki")
                        .type("BRIDGE")
                        .latitude(51.1158)
                        .longitude(17.0560)
                        .roadNumber("A4")
                        .maxWeightKg(44000)
                        .maxAxleWeightKg(11000)
                        .isActive(true)
                        .description("Most we Wrocławiu")
                        .build(),

                Infrastructure.builder()
                        .externalId("SAMPLE_BRIDGE_003")
                        .name("Most Wandy")
                        .type("BRIDGE")
                        .latitude(50.0776)
                        .longitude(20.0710)
                        .roadNumber("A4")
                        .maxWeightKg(38000)
                        .maxAxleWeightKg(9500)
                        .isActive(true)
                        .description("Most w Krakowie")
                        .build()
        );

        for (Infrastructure bridge : sampleBridges) {
            if (infrastructureRepository.findByExternalId(bridge.getExternalId()) == null) {
                infrastructureRepository.save(bridge);
            }
        }

        log.info("Loaded {} sample bridges", sampleBridges.size());
    }

    private void loadSampleTunnels() {
        // Przykładowe tunele w Polsce
        List<Infrastructure> sampleTunnels = List.of(
                Infrastructure.builder()
                        .externalId("SAMPLE_TUNNEL_001")
                        .name("Tunel Ursynowski")
                        .type("TUNNEL")
                        .latitude(52.1465)
                        .longitude(21.0520)
                        .roadNumber("S2")
                        .maxHeightCm(420)
                        .maxWeightKg(40000)
                        .isActive(true)
                        .description("Tunel w Warszawie")
                        .build(),

                Infrastructure.builder()
                        .externalId("SAMPLE_TUNNEL_002")
                        .name("Tunel Laliki")
                        .type("TUNNEL")
                        .latitude(49.3845)
                        .longitude(20.8231)
                        .roadNumber("S7")
                        .maxHeightCm(450)
                        .maxWeightKg(44000)
                        .isActive(true)
                        .description("Tunel w Zakopanem")
                        .build()
        );

        for (Infrastructure tunnel : sampleTunnels) {
            if (infrastructureRepository.findByExternalId(tunnel.getExternalId()) == null) {
                infrastructureRepository.save(tunnel);
            }
        }

        log.info("Loaded {} sample tunnels", sampleTunnels.size());
    }

    private Integer parseInteger(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}