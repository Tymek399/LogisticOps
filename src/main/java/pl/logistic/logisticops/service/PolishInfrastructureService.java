package pl.logistic.logisticops.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import pl.logistic.logisticops.model.Infrastructure;
import pl.logistic.logisticops.repository.InfrastructureRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolishInfrastructureService {

    private final InfrastructureRepository infrastructureRepository;
    private final RestTemplate restTemplate;

    @Value("${api.here.api-key:}")
    private String hereApiKey;

    /**
     * 🎯 STRATEGIA 3-POZIOMOWA:
     *
     * 1. 🥇 OpenStreetMap Overpass API (GŁÓWNE ŹRÓDŁO - darmowe, najlepsze)
     * 2. 🥈 Geoportal.gov.pl WFS (WERYFIKACJA - oficjalne polskie)
     * 3. 🥉 HERE Maps API (COMMERCIAL BACKUP - płatne, precyzyjne)
     * 4. 🛡️ Statyczne dane (ULTIMATE FALLBACK - zawsze działa)
     */

    public void manualSync() {
        syncInfrastructureData();
    }

    @Scheduled(fixedRate = 21600000) // Co 6 godzin (optymalna częstotliwość)
    public void syncInfrastructureData() {
        log.info("🚀 Starting 3-tier infrastructure sync for Poland");

        int syncedCount = 0;

        try {
            // POZIOM 1: OpenStreetMap (GŁÓWNE ŹRÓDŁO)
            syncedCount += syncFromOpenStreetMap();
            log.info("✅ Level 1 (OSM): {} new objects", syncedCount);

            // POZIOM 2: Geoportal.gov.pl (WERYFIKACJA)
            syncedCount += syncFromGeoportal();
            log.info("✅ Level 2 (Geoportal): total {} objects", syncedCount);

            // POZIOM 3: HERE Maps (COMMERCIAL BACKUP)
            if (!hereApiKey.isEmpty()) {
                syncedCount += syncFromHereMaps();
                log.info("✅ Level 3 (HERE): total {} objects", syncedCount);
            }

            // POZIOM 4: Statyczne dane (ULTIMATE FALLBACK)
            syncedCount += loadCriticalInfrastructure();

            log.info("🎉 Infrastructure sync completed: {} total objects in database",
                    infrastructureRepository.count());

        } catch (Exception e) {
            log.error("❌ Error during infrastructure synchronization", e);
            // Emergency fallback
            loadCriticalInfrastructure();
        }
    }

    /**
     * 🥇 POZIOM 1: OpenStreetMap Overpass API
     * Najlepsze darmowe źródło danych o infrastrukturze
     */
    private int syncFromOpenStreetMap() {
        int newObjects = 0;

        try {
            log.info("📡 Syncing from OpenStreetMap...");

            // Mosty z ograniczeniami w Polsce
            String bridgeQuery = """
                [out:json][timeout:30];
                (
                  way["bridge"="yes"]["highway"]["maxweight"](49,14,55,24);
                  way["bridge"="yes"]["highway"]["maxheight"](49,14,55,24);
                  rel["bridge"="yes"]["highway"]["maxweight"](49,14,55,24);
                );
                out center meta;
                """;

            newObjects += processOverpassQuery(bridgeQuery, "BRIDGE");

            // Tunele z ograniczeniami wysokości
            String tunnelQuery = """
                [out:json][timeout:30];
                (
                  way["tunnel"="yes"]["highway"]["maxheight"](49,14,55,24);
                  way["tunnel"="yes"]["maxheight"](49,14,55,24);
                );
                out center meta;
                """;

            newObjects += processOverpassQuery(tunnelQuery, "TUNNEL");

            // Ograniczenia wysokości (wiadukty, przejazdy kolejowe)
            String heightQuery = """
                [out:json][timeout:30];
                (
                  way["highway"]["maxheight"](49,14,55,24);
                  node["barrier"="height_restrictor"](49,14,55,24);
                  way["railway"="rail"]["bridge"="yes"](49,14,55,24);
                );
                out center meta;
                """;

            newObjects += processOverpassQuery(heightQuery, "HEIGHT_RESTRICTION");

        } catch (Exception e) {
            log.warn("⚠️ OpenStreetMap sync failed: {}", e.getMessage());
        }

        return newObjects;
    }

    /**
     * 🥈 POZIOM 2: Geoportal.gov.pl WFS
     * Oficjalne polskie dane państwowe
     */
    private int syncFromGeoportal() {
        try {
            log.info("🇵🇱 Syncing from Geoportal.gov.pl...");

            // WFS dla infrastruktury komunikacyjnej BDOT10k
            String wfsUrl = "https://mapy.geoportal.gov.pl/wss/service/PZGIK/BDOT10k/WFS/Komunikacja?" +
                    "service=WFS&version=2.0.0&request=GetFeature&" +
                    "typeName=ms:PTWP_A&" + // mosty (punktowe obiekty transportu wodnego)
                    "outputFormat=application/json&" +
                    "bbox=14,49,24,55,EPSG:4326&" +
                    "maxFeatures=500";

            ResponseEntity<String> response = restTemplate.getForEntity(wfsUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return processGeoportalData(response.getBody());
            }

        } catch (Exception e) {
            log.warn("⚠️ Geoportal sync failed: {}", e.getMessage());
        }

        return 0;
    }

    /**
     * 🥉 POZIOM 3: HERE Maps API
     * Commercial backup dla high-precision data
     */
    private int syncFromHereMaps() {
        try {
            log.info("💼 Syncing from HERE Maps...");

            // HERE Places API - infrastruktura z ograniczeniami
            String hereUrl = "https://discover.search.hereapi.com/v1/discover?" +
                    "in=countryCode:POL&" +
                    "q=bridge,tunnel,height restriction&" +
                    "limit=200&" +
                    "apiKey=" + hereApiKey;

            ResponseEntity<String> response = restTemplate.getForEntity(hereUrl, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return processHereData(response.getBody());
            }

        } catch (Exception e) {
            log.warn("⚠️ HERE Maps sync failed: {}", e.getMessage());
        }

        return 0;
    }

    // ========================================
    // PROCESSING METHODS
    // ========================================

    private int processOverpassQuery(String query, String type) {
        try {
            String overpassUrl = "https://overpass-api.de/api/interpreter";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<String> request = new HttpEntity<>("data=" +
                    URLEncoder.encode(query, StandardCharsets.UTF_8), headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    overpassUrl, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return processOpenStreetMapData(response.getBody(), type);
            }

        } catch (Exception e) {
            log.error("❌ Overpass query failed for type: {}", type, e);
        }

        return 0;
    }

    @SuppressWarnings("unchecked")
    private int processOpenStreetMapData(String jsonData, String type) {
        int newObjects = 0;

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(jsonData, Map.class);

            List<Map<String, Object>> elements = (List<Map<String, Object>>) data.get("elements");

            for (Map<String, Object> element : elements) {
                if (processOSMElement(element, type)) {
                    newObjects++;
                }
            }

        } catch (Exception e) {
            log.error("❌ Error processing OpenStreetMap data for type: {}", type, e);
        }

        return newObjects;
    }

    @SuppressWarnings("unchecked")
    private boolean processOSMElement(Map<String, Object> element, String type) {
        try {
            // Get coordinates (center for ways, direct lat/lon for nodes)
            Double lat = null, lon = null;

            if (element.get("center") != null) {
                Map<String, Object> center = (Map<String, Object>) element.get("center");
                lat = ((Number) center.get("lat")).doubleValue();
                lon = ((Number) center.get("lon")).doubleValue();
            } else if (element.get("lat") != null) {
                lat = ((Number) element.get("lat")).doubleValue();
                lon = ((Number) element.get("lon")).doubleValue();
            }

            if (lat == null || lon == null) return false;

            Map<String, Object> tags = (Map<String, Object>) element.get("tags");
            if (tags == null) return false;

            String externalId = "OSM_" + type + "_" + element.get("id");

            // Skip if already exists
            if (infrastructureRepository.findByExternalId(externalId) != null) {
                return false;
            }

            Infrastructure infrastructure = Infrastructure.builder()
                    .externalId(externalId)
                    .name(extractName(tags, type))
                    .type(type)
                    .latitude(lat)
                    .longitude(lon)
                    .roadNumber(extractRoadNumber(tags))
                    .maxHeightCm(extractHeightLimit(tags))
                    .maxWeightKg(extractWeightLimit(tags))
                    .maxAxleWeightKg(extractAxleWeightLimit(tags))
                    .description("OSM: " + extractDescription(tags))
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            infrastructureRepository.save(infrastructure);
            log.debug("✅ Added OSM {}: {}", type, infrastructure.getName());
            return true;

        } catch (Exception e) {
            log.warn("⚠️ Error processing OSM element: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private int processGeoportalData(String jsonData) {
        int newObjects = 0;

        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(jsonData, Map.class);

            List<Map<String, Object>> features = (List<Map<String, Object>>) data.get("features");
            if (features == null) return 0;

            for (Map<String, Object> feature : features) {
                Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
                Map<String, Object> properties = (Map<String, Object>) feature.get("properties");

                if (geometry != null && properties != null) {
                    if (processGeoportalFeature(geometry, properties)) {
                        newObjects++;
                    }
                }
            }

        } catch (Exception e) {
            log.error("❌ Error processing Geoportal data", e);
        }

        return newObjects;
    }

    @SuppressWarnings("unchecked")
    private boolean processGeoportalFeature(Map<String, Object> geometry, Map<String, Object> properties) {
        try {
            List<Double> coordinates = (List<Double>) geometry.get("coordinates");
            if (coordinates == null || coordinates.size() < 2) return false;

            String externalId = "GEOPORTAL_" + properties.get("id");
            if (infrastructureRepository.findByExternalId(externalId) != null) {
                return false;
            }

            Infrastructure infrastructure = Infrastructure.builder()
                    .externalId(externalId)
                    .name(properties.get("name") != null ? properties.get("name").toString() : "Most/Przeprawa")
                    .type("BRIDGE")
                    .latitude(coordinates.get(1))
                    .longitude(coordinates.get(0))
                    .description("Geoportal: " + (properties.get("opis") != null ? properties.get("opis") : "Oficjalne dane"))
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();

            infrastructureRepository.save(infrastructure);
            log.debug("✅ Added Geoportal bridge: {}", infrastructure.getName());
            return true;

        } catch (Exception e) {
            log.warn("⚠️ Error processing Geoportal feature: {}", e.getMessage());
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private int processHereData(String jsonData) {
        // Simplified HERE processing - focus on places with restrictions
        try {
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> data = mapper.readValue(jsonData, Map.class);

            List<Map<String, Object>> items = (List<Map<String, Object>>) data.get("items");
            if (items == null) return 0;

            // HERE data processing would go here
            // For now, return 0 as it requires specific HERE response format analysis
            log.info("📊 HERE Maps returned {} items (processing not fully implemented)", items.size());

        } catch (Exception e) {
            log.error("❌ Error processing HERE data", e);
        }

        return 0;
    }

    /**
     * 🛡️ POZIOM 4: Krytyczna infrastruktura (ULTIMATE FALLBACK)
     * Kluczowe obiekty które MUSZĄ być w systemie
     */
    private int loadCriticalInfrastructure() {
        log.info("🛡️ Loading critical infrastructure (fallback)...");

        List<Infrastructure> criticalData = List.of(
                // === KLUCZOWE MOSTY MILITARNE ===
                Infrastructure.builder()
                        .externalId("CRITICAL_BRIDGE_001")
                        .name("Most Siennicki (S8 Warszawa)")
                        .type("BRIDGE")
                        .latitude(52.2297)
                        .longitude(21.0122)
                        .roadNumber("S8")
                        .maxWeightKg(44000)
                        .maxAxleWeightKg(11000)
                        .description("KRYTYCZNY: Główna przeprawa ciężkich zestawów przez Wisłę")
                        .isActive(true)
                        .build(),

                Infrastructure.builder()
                        .externalId("CRITICAL_BRIDGE_002")
                        .name("Most Grunwaldzki (A4 Wrocław)")
                        .type("BRIDGE")
                        .latitude(51.1079)
                        .longitude(17.0385)
                        .roadNumber("A4")
                        .maxWeightKg(40000)
                        .maxAxleWeightKg(10000)
                        .description("KRYTYCZNY: Główny korytarz wschód-zachód przez Odrę")
                        .isActive(true)
                        .build(),

                Infrastructure.builder()
                        .externalId("CRITICAL_BRIDGE_003")
                        .name("Most Dębicki (A4 Wisła)")
                        .type("BRIDGE")
                        .latitude(50.0516)
                        .longitude(21.4122)
                        .roadNumber("A4")
                        .maxWeightKg(42000)
                        .maxAxleWeightKg(10500)
                        .description("KRYTYCZNY: Główna przeprawa wschodnia A4")
                        .isActive(true)
                        .build(),

                // === TUNELE Z OGRANICZENIAMI ===
                Infrastructure.builder()
                        .externalId("CRITICAL_TUNNEL_001")
                        .name("Tunel Ursynowski (S2)")
                        .type("TUNNEL")
                        .latitude(52.1465)
                        .longitude(21.0520)
                        .roadNumber("S2")
                        .maxHeightCm(420)
                        .maxWeightKg(44000)
                        .description("KRYTYCZNY: Główne ograniczenie wysokości w Warszawie - 4.2m")
                        .isActive(true)
                        .build(),

                Infrastructure.builder()
                        .externalId("CRITICAL_TUNNEL_002")
                        .name("Tunel Wisłostrady (S2)")
                        .type("TUNNEL")
                        .latitude(52.2156)
                        .longitude(21.0348)
                        .roadNumber("S2")
                        .maxHeightCm(450)
                        .maxWeightKg(40000)
                        .description("KRYTYCZNY: Tunel centrum Warszawy - 4.5m")
                        .isActive(true)
                        .build(),

                // === NIEBEZPIECZNE OGRANICZENIA WYSOKOŚCI ===
                Infrastructure.builder()
                        .externalId("CRITICAL_HEIGHT_001")
                        .name("Wiadukt PKP Praga (DK50)")
                        .type("HEIGHT_RESTRICTION")
                        .latitude(52.2504)
                        .longitude(21.0348)
                        .roadNumber("DK50")
                        .maxHeightCm(380)
                        .description("NIEBEZPIECZNY: Bardzo niski przejazd pod PKP - 3.8m!")
                        .isActive(true)
                        .build(),

                Infrastructure.builder()
                        .externalId("CRITICAL_HEIGHT_002")
                        .name("Wiadukt A1 Częstochowa")
                        .type("HEIGHT_RESTRICTION")
                        .latitude(50.8118)
                        .longitude(19.1203)
                        .roadNumber("DK1")
                        .maxHeightCm(390)
                        .description("NIEBEZPIECZNY: Niski przejazd pod A1 - 3.9m")
                        .isActive(true)
                        .build(),

                Infrastructure.builder()
                        .externalId("CRITICAL_HEIGHT_003")
                        .name("Most kolejowy Gdańsk Główny")
                        .type("HEIGHT_RESTRICTION")
                        .latitude(54.3520)
                        .longitude(18.6466)
                        .roadNumber("DK7")
                        .maxHeightCm(375)
                        .description("NIEBEZPIECZNY: Najniższy przejazd na trasie północnej - 3.75m!")
                        .isActive(true)
                        .build(),

                // === STACJE KONTROLI ===
                Infrastructure.builder()
                        .externalId("CRITICAL_WEIGHT_001")
                        .name("Stacja ważenia Grodzisk Maz. (A2)")
                        .type("WEIGHT_STATION")
                        .latitude(52.1097)
                        .longitude(20.6347)
                        .roadNumber("A2")
                        .description("KONTROLA: Automatyczne ważenie - korytarz zachodni")
                        .isActive(true)
                        .build(),

                Infrastructure.builder()
                        .externalId("CRITICAL_WEIGHT_002")
                        .name("Stacja ważenia Mysłowice (A4)")
                        .type("WEIGHT_STATION")
                        .latitude(50.2079)
                        .longitude(19.1314)
                        .roadNumber("A4")
                        .description("KONTROLA: Automatyczne ważenie - korytarz południowy")
                        .isActive(true)
                        .build()
        );

        int added = 0;
        for (Infrastructure infra : criticalData) {
            if (infrastructureRepository.findByExternalId(infra.getExternalId()) == null) {
                infra.setCreatedAt(LocalDateTime.now());
                infra.setUpdatedAt(LocalDateTime.now());
                infrastructureRepository.save(infra);
                added++;
                log.info("🛡️ Added critical infrastructure: {}", infra.getName());
            }
        }

        return added;
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    private String extractName(Map<String, Object> tags, String type) {
        String[] nameKeys = {"name", "bridge:name", "official_name", "loc_name"};
        for (String key : nameKeys) {
            if (tags.get(key) != null) {
                return tags.get(key).toString();
            }
        }

        String road = extractRoadNumber(tags);
        return type.toLowerCase() + (road != null ? " (" + road + ")" : " unnamed");
    }

    private String extractRoadNumber(Map<String, Object> tags) {
        String[] roadKeys = {"ref", "highway", "route"};
        for (String key : roadKeys) {
            if (tags.get(key) != null) {
                return tags.get(key).toString();
            }
        }
        return null;
    }

    private Integer extractHeightLimit(Map<String, Object> tags) {
        String[] heightKeys = {"maxheight", "maxheight:physical", "bridge:maxheight", "tunnel:maxheight"};
        for (String key : heightKeys) {
            if (tags.get(key) != null) {
                try {
                    String height = tags.get(key).toString().replaceAll("[^0-9.]", "");
                    return (int)(Double.parseDouble(height) * 100); // convert to cm
                } catch (NumberFormatException e) {
                    // ignore and try next
                }
            }
        }
        return null;
    }

    private Integer extractWeightLimit(Map<String, Object> tags) {
        String[] weightKeys = {"maxweight", "maxweight:signed", "bridge:maxweight"};
        for (String key : weightKeys) {
            if (tags.get(key) != null) {
                try {
                    String weight = tags.get(key).toString().replaceAll("[^0-9.]", "");
                    return (int)(Double.parseDouble(weight) * 1000); // convert to kg
                } catch (NumberFormatException e) {
                    // ignore and try next
                }
            }
        }
        return null;
    }

    private Integer extractAxleWeightLimit(Map<String, Object> tags) {
        String[] axleKeys = {"maxaxleload", "maxweight:axle"};
        for (String key : axleKeys) {
            if (tags.get(key) != null) {
                try {
                    String weight = tags.get(key).toString().replaceAll("[^0-9.]", "");
                    return (int)(Double.parseDouble(weight) * 1000); // convert to kg
                } catch (NumberFormatException e) {
                    // ignore and try next
                }
            }
        }
        return null;
    }

    private String extractDescription(Map<String, Object> tags) {
        StringBuilder desc = new StringBuilder();

        if (tags.get("description") != null) {
            desc.append(tags.get("description"));
        }

        if (tags.get("note") != null) {
            if (desc.length() > 0) desc.append(" | ");
            desc.append(tags.get("note"));
        }

        if (tags.get("highway") != null) {
            if (desc.length() > 0) desc.append(" | ");
            desc.append("Highway: ").append(tags.get("highway"));
        }

        if (tags.get("barrier") != null) {
            if (desc.length() > 0) desc.append(" | ");
            desc.append("Barrier: ").append(tags.get("barrier"));
        }

        return desc.length() > 0 ? desc.toString() : "No additional information";
    }
}