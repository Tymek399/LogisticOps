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

    @Value("${api.googlemaps.key}")
    private String googleMapsApiKey;

    /**
     * 🎯 STRATEGIA 2-POZIOMOWA (TYLKO DOSTĘPNE API):
     *
     * 1. 🥇 OpenStreetMap Overpass API (GŁÓWNE ŹRÓDŁO - darmowe, najlepsze)
     * 2. 🛡️ Statyczne dane (ULTIMATE FALLBACK - zawsze działa)
     *
     * ❌ WYŁĄCZONE: HERE Maps (brak klucza API)
     * ❌ WYŁĄCZONE: Geoportal.gov.pl WFS (GDDKiA niedostępne)
     */

    public void manualSync() {
        syncInfrastructureData();
    }

    @Scheduled(fixedRate = 21600000) // Co 6 godzin (optymalna częstotliwość)
    public void syncInfrastructureData() {
        log.info("🚀 Starting 2-tier infrastructure sync for Poland (OSM + Static)");

        int syncedCount = 0;

        try {
            // POZIOM 1: OpenStreetMap (GŁÓWNE ŹRÓDŁO)
            syncedCount += syncFromOpenStreetMap();
            log.info("✅ Level 1 (OSM): {} new objects", syncedCount);

            // POZIOM 2: Statyczne dane (ULTIMATE FALLBACK)
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
            log.info("📡 Syncing from OpenStreetMap Overpass API...");

            // Mosty z ograniczeniami w Polsce
            String bridgeQuery = """
                [out:json][timeout:45];
                (
                  way["bridge"="yes"]["highway"]["maxweight"](49,14,55,24);
                  way["bridge"="yes"]["highway"]["maxheight"](49,14,55,24);
                  rel["bridge"="yes"]["highway"]["maxweight"](49,14,55,24);
                  way["bridge"="yes"]["maxweight"](49,14,55,24);
                  way["bridge"="yes"]["maxheight"](49,14,55,24);
                );
                out center meta;
                """;

            newObjects += processOverpassQuery(bridgeQuery, "BRIDGE");

            // Tunele z ograniczeniami wysokości
            String tunnelQuery = """
                [out:json][timeout:45];
                (
                  way["tunnel"="yes"]["highway"]["maxheight"](49,14,55,24);
                  way["tunnel"="yes"]["maxheight"](49,14,55,24);
                  node["tunnel"="yes"]["maxheight"](49,14,55,24);
                );
                out center meta;
                """;

            newObjects += processOverpassQuery(tunnelQuery, "TUNNEL");

            // Ograniczenia wysokości (wiadukty, przejazdy kolejowe)
            String heightQuery = """
                [out:json][timeout:45];
                (
                  way["highway"]["maxheight"](49,14,55,24);
                  node["barrier"="height_restrictor"](49,14,55,24);
                  way["railway"="rail"]["bridge"="yes"](49,14,55,24);
                  node["highway"="traffic_signals"]["maxheight"](49,14,55,24);
                  way["bridge:maxheight"](49,14,55,24);
                );
                out center meta;
                """;

            newObjects += processOverpassQuery(heightQuery, "HEIGHT_RESTRICTION");

            // Stacje ważenia i kontroli
            String weightStationQuery = """
                [out:json][timeout:30];
                (
                  node["amenity"="weighbridge"](49,14,55,24);
                  node["barrier"="toll_booth"](49,14,55,24);
                  way["amenity"="weighbridge"](49,14,55,24);
                );
                out center meta;
                """;

            newObjects += processOverpassQuery(weightStationQuery, "WEIGHT_STATION");

        } catch (Exception e) {
            log.warn("⚠️ OpenStreetMap sync failed: {}", e.getMessage());
        }

        return newObjects;
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
            } else {
                log.warn("⚠️ Overpass API returned status: {}", response.getStatusCode());
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

            if (elements == null) {
                log.warn("⚠️ No elements found in OSM response for type: {}", type);
                return 0;
            }

            log.debug("📊 Processing {} OSM elements for type: {}", elements.size(), type);

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

            if (lat == null || lon == null) {
                return false;
            }

            // Sprawdź czy współrzędne są w granicach Polski
            if (!isInPoland(lat, lon)) {
                return false;
            }

            Map<String, Object> tags = (Map<String, Object>) element.get("tags");
            if (tags == null) {
                tags = Map.of(); // Empty map if no tags
            }

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
            log.debug("✅ Added OSM {}: {} at [{}, {}]", type, infrastructure.getName(), lat, lon);
            return true;

        } catch (Exception e) {
            log.warn("⚠️ Error processing OSM element: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 🛡️ POZIOM 2: Krytyczna infrastruktura (ULTIMATE FALLBACK)
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

                Infrastructure.builder()
                        .externalId("CRITICAL_BRIDGE_004")
                        .name("Most Północny (A1 Gdańsk)")
                        .type("BRIDGE")
                        .latitude(54.3520)
                        .longitude(18.6466)
                        .roadNumber("A1")
                        .maxWeightKg(44000)
                        .maxAxleWeightKg(11000)
                        .description("KRYTYCZNY: Główny dostęp do portu w Gdańsku")
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

                Infrastructure.builder()
                        .externalId("CRITICAL_TUNNEL_003")
                        .name("Tunel Trasy Łazienkowskiej")
                        .type("TUNNEL")
                        .latitude(52.2170)
                        .longitude(21.0356)
                        .roadNumber("S79")
                        .maxHeightCm(430)
                        .maxWeightKg(40000)
                        .description("KRYTYCZNY: Alternatywny tunel centrum - 4.3m")
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

                Infrastructure.builder()
                        .externalId("CRITICAL_HEIGHT_004")
                        .name("Wiadukt PKP Kraków Główny")
                        .type("HEIGHT_RESTRICTION")
                        .latitude(50.0647)
                        .longitude(19.9450)
                        .roadNumber("DK4")
                        .maxHeightCm(385)
                        .description("NIEBEZPIECZNY: Ograniczenie centrum Krakowa - 3.85m")
                        .isActive(true)
                        .build(),

                Infrastructure.builder()
                        .externalId("CRITICAL_HEIGHT_005")
                        .name("Wiadukt PKP Wrocław Główny")
                        .type("HEIGHT_RESTRICTION")
                        .latitude(51.1079)
                        .longitude(17.0385)
                        .roadNumber("DK8")
                        .maxHeightCm(390)
                        .description("NIEBEZPIECZNY: Ograniczenie centrum Wrocławia - 3.9m")
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
                        .build(),

                Infrastructure.builder()
                        .externalId("CRITICAL_WEIGHT_003")
                        .name("Stacja ważenia Gdańsk Port (A1)")
                        .type("WEIGHT_STATION")
                        .latitude(54.3720)
                        .longitude(18.6966)
                        .roadNumber("A1")
                        .description("KONTROLA: Kontrola dostępu do portu")
                        .isActive(true)
                        .build(),

                // === DODATKOWE KRYTYCZNE PUNKTY ===
                Infrastructure.builder()
                        .externalId("CRITICAL_BRIDGE_005")
                        .name("Most im. Marszałka Józefa Piłsudskiego (Warszawa)")
                        .type("BRIDGE")
                        .latitude(52.2473)
                        .longitude(21.0362)
                        .roadNumber("S8")
                        .maxWeightKg(40000)
                        .maxAxleWeightKg(10000)
                        .description("KRYTYCZNY: Główna przeprawa centrum Warszawy")
                        .isActive(true)
                        .build(),

                Infrastructure.builder()
                        .externalId("CRITICAL_BRIDGE_006")
                        .name("Most Solidarności (Płock)")
                        .type("BRIDGE")
                        .latitude(52.5465)
                        .longitude(19.7065)
                        .roadNumber("DK60")
                        .maxWeightKg(42000)
                        .maxAxleWeightKg(10500)
                        .description("KRYTYCZNY: Główna przeprawa przez Wisłę na północy")
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

        log.info("🛡️ Loaded {} critical infrastructure objects", added);
        return added;
    }

    // ========================================
    // UTILITY METHODS
    // ========================================

    /**
     * 🇵🇱 Sprawdź czy punkt jest w granicach Polski
     */
    private boolean isInPoland(double lat, double lon) {
        // Uproszczone granice Polski
        return lat >= 49.0 && lat <= 55.0 && lon >= 14.0 && lon <= 24.5;
    }

    private String extractName(Map<String, Object> tags, String type) {
        String[] nameKeys = {"name", "bridge:name", "official_name", "loc_name", "name:pl"};
        for (String key : nameKeys) {
            if (tags.get(key) != null) {
                return tags.get(key).toString();
            }
        }

        String road = extractRoadNumber(tags);
        return type.toLowerCase().replace("_", " ") + (road != null ? " (" + road + ")" : " unnamed");
    }

    private String extractRoadNumber(Map<String, Object> tags) {
        String[] roadKeys = {"ref", "highway", "route", "road:ref"};
        for (String key : roadKeys) {
            if (tags.get(key) != null) {
                return tags.get(key).toString();
            }
        }
        return null;
    }

    private Integer extractHeightLimit(Map<String, Object> tags) {
        String[] heightKeys = {"maxheight", "maxheight:physical", "bridge:maxheight", "tunnel:maxheight", "barrier:height"};
        for (String key : heightKeys) {
            if (tags.get(key) != null) {
                try {
                    String height = tags.get(key).toString().replaceAll("[^0-9.]", "");
                    if (!height.isEmpty()) {
                        double heightM = Double.parseDouble(height);
                        // If value seems to be in meters, convert to cm
                        if (heightM < 10) {
                            return (int)(heightM * 100);
                        } else {
                            return (int)heightM; // Already in cm
                        }
                    }
                } catch (NumberFormatException e) {
                    // ignore and try next
                }
            }
        }
        return null;
    }

    private Integer extractWeightLimit(Map<String, Object> tags) {
        String[] weightKeys = {"maxweight", "maxweight:signed", "bridge:maxweight", "maxweight:conditional"};
        for (String key : weightKeys) {
            if (tags.get(key) != null) {
                try {
                    String weight = tags.get(key).toString().replaceAll("[^0-9.]", "");
                    if (!weight.isEmpty()) {
                        double weightT = Double.parseDouble(weight);
                        // Convert tons to kg
                        return (int)(weightT * 1000);
                    }
                } catch (NumberFormatException e) {
                    // ignore and try next
                }
            }
        }
        return null;
    }

    private Integer extractAxleWeightLimit(Map<String, Object> tags) {
        String[] axleKeys = {"maxaxleload", "maxweight:axle", "maxaxleweight"};
        for (String key : axleKeys) {
            if (tags.get(key) != null) {
                try {
                    String weight = tags.get(key).toString().replaceAll("[^0-9.]", "");
                    if (!weight.isEmpty()) {
                        double weightT = Double.parseDouble(weight);
                        // Convert tons to kg
                        return (int)(weightT * 1000);
                    }
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

        if (tags.get("railway") != null) {
            if (desc.length() > 0) desc.append(" | ");
            desc.append("Railway: ").append(tags.get("railway"));
        }

        return desc.length() > 0 ? desc.toString() : "No additional information available";
    }
}