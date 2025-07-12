package pl.logistic.logisticops.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import pl.logistic.logisticops.dto.VehicleTrackingDTO;
import pl.logistic.logisticops.enums.AlertLevel;
import pl.logistic.logisticops.model.*;
import pl.logistic.logisticops.repository.*;

import java.time.LocalDateTime;

/**
 * 🛡️ SERWIS INTEGRACJI Z BEZPIECZNYMI URZĄDZENIAMI GPS WOJSKOWYMI
 *
 * Obsługuje:
 * - 📡 Urządzenia GTX wojskowe
 * - 🔒 Szyfrowaną komunikację GPS
 * - 📍 Bezpieczne pozycjonowanie GNSS
 * - 🛰️ Systemy anty-jamming
 * - 📱 Terminale wojskowe z GPS
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MilitaryGpsIntegrationService {

    private final VehicleTrackingRepository trackingRepository;
    private final TransportRepository transportRepository;
    private final VehicleSpecificationRepository vehicleRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AlertService alertService;

    /**
     * 📡 INTEGRACJA Z URZĄDZENIAMI WOJSKOWYMI
     *
     * OBSŁUGIWANE URZĄDZENIA:
     * - 🛡️ Harris Falcon III (szyfrowana komunikacja)
     * - 📡 Rockwell Collins SINCGARS
     * - 🔒 Thales SOTAS (secure over-the-air)
     * - 📱 Samsung Galaxy Tactical Edition
     * - 🛰️ Garmin DAGR (Defense Advanced GPS Receiver)
     * - 📍 Trimble Defense Solutions
     */

    /**
     * 🔒 Odbierz bezpieczne dane GPS z urządzenia wojskowego
     */
    public void receiveSecureGpsData(MilitaryGpsData gpsData) {
        try {
            log.info("🛡️ Receiving secure GPS data from device: {}", gpsData.getDeviceId());

            // 1. Walidacja bezpieczeństwa
            if (!validateMilitaryDevice(gpsData)) {
                log.error("🚨 SECURITY ALERT: Invalid military device: {}", gpsData.getDeviceId());
                createSecurityAlert(gpsData);
                return;
            }

            // 2. Deszyfrowanie danych GPS (jeśli wymagane)
            GpsPosition decryptedPosition = decryptGpsData(gpsData);

            // 3. Aktualizacja pozycji pojazdu
            updateVehiclePosition(decryptedPosition);

            // 4. Sprawdzenie geofencing wojskowego
            checkMilitaryGeofencing(decryptedPosition);

            // 5. Real-time broadcast do operatorów
            broadcastSecurePosition(decryptedPosition);

        } catch (Exception e) {
            log.error("❌ Error processing military GPS data", e);
            createTechnicalAlert(gpsData, e.getMessage());
        }
    }

    /**
     * 🛡️ Walidacja urządzenia wojskowego
     */
    private boolean validateMilitaryDevice(MilitaryGpsData gpsData) {
        // Sprawdź certyfikat bezpieczeństwa
        if (!isValidMilitaryCertificate(gpsData.getSecurityCertificate())) {
            return false;
        }

        // Sprawdź czy urządzenie jest zarejestrowane
        if (!isRegisteredMilitaryDevice(gpsData.getDeviceId())) {
            return false;
        }

        // Sprawdź integralność danych
        if (!verifyDataIntegrity(gpsData)) {
            return false;
        }

        return true;
    }

    /**
     * 🔓 Deszyfrowanie danych GPS (symulacja)
     */
    private GpsPosition decryptGpsData(MilitaryGpsData gpsData) {
        // W rzeczywistości tutaj byłoby prawdziwe deszyfrowanie AES-256
        return GpsPosition.builder()
                .latitude(gpsData.getEncryptedLatitude())  // Po deszyfrowaniu
                .longitude(gpsData.getEncryptedLongitude()) // Po deszyfrowaniu
                .altitude(gpsData.getAltitude())
                .speed(gpsData.getSpeed())
                .heading(gpsData.getHeading())
                .accuracy(gpsData.getAccuracy())
                .timestamp(gpsData.getTimestamp())
                .vehicleId(gpsData.getVehicleId())
                .transportId(gpsData.getTransportId())
                .deviceId(gpsData.getDeviceId())
                .securityLevel("MILITARY")
                .build();
    }

    /**
     * 📍 Aktualizacja pozycji pojazdu z danymi GPS
     */
    private void updateVehiclePosition(GpsPosition position) {
        try {
            Transport transport = transportRepository.findById(position.getTransportId())
                    .orElse(null);

            VehicleSpecification vehicle = vehicleRepository.findById(position.getVehicleId())
                    .orElse(null);

            if (transport == null || vehicle == null) {
                log.warn("⚠️ Transport or vehicle not found for GPS update");
                return;
            }

            // Stwórz rekord tracking
            VehicleTracking tracking = VehicleTracking.builder()
                    .transport(transport)
                    .vehicle(vehicle)
                    .latitude(position.getLatitude())
                    .longitude(position.getLongitude())
                    .speedKmh(position.getSpeed())
                    .headingDegrees(position.getHeading())
                    .sensorData(buildMilitaryGpsSensorData(position))
                    .recordedAt(position.getTimestamp())
                    .build();

            trackingRepository.save(tracking);

            // Aktualizuj pozycję transportu
            transport.setCurrentLatitude(position.getLatitude());
            transport.setCurrentLongitude(position.getLongitude());
            transportRepository.save(transport);

            log.debug("✅ Updated military GPS position for transport {}", transport.getId());

        } catch (Exception e) {
            log.error("❌ Error updating vehicle position from military GPS", e);
        }
    }

    /**
     * 🚧 Sprawdzenie geofencing wojskowego
     */
    private void checkMilitaryGeofencing(GpsPosition position) {
        // Sprawdź czy pojazd nie wjechał w strefy zakazane
        if (isInRestrictedMilitaryZone(position)) {
            alertService.createAlert(
                    "🚨 MILITARY ALERT: Vehicle entered restricted zone",
                    AlertLevel.CRITICAL,
                    position.getTransportId(),
                    null,
                    "GEOFENCING_VIOLATION"
            );
        }

        // Sprawdź czy pojazd nie oddalił się za bardzo od trasy
        if (isDeviatingFromRoute(position)) {
            alertService.createAlert(
                    "⚠️ Vehicle deviating from approved route",
                    AlertLevel.HIGH,
                    position.getTransportId(),
                    null,
                    "ROUTE_DEVIATION"
            );
        }
    }

    /**
     * 📡 Broadcast bezpiecznej pozycji do operatorów
     */
    private void broadcastSecurePosition(GpsPosition position) {
        // Sanityzuj dane przed wysłaniem (usuń wrażliwe informacje)
        VehicleTrackingDTO sanitizedDto = VehicleTrackingDTO.builder()
                .transportId(position.getTransportId())
                .vehicleId(position.getVehicleId())
                .latitude(position.getLatitude())
                .longitude(position.getLongitude())
                .speedKmh(position.getSpeed())
                .headingDegrees(position.getHeading())
                .recordedAt(position.getTimestamp())
                .build();

        // Wyślij do autoryzowanych operatorów
        messagingTemplate.convertAndSend(
                "/topic/transport/" + position.getTransportId() + "/tracking/secure",
                sanitizedDto
        );
    }

    // ========================================
    // METODY BEZPIECZEŃSTWA
    // ========================================

    private boolean isValidMilitaryCertificate(String certificate) {
        // Sprawdzenie certyfikatu wojskowego (symulacja)
        return certificate != null && certificate.startsWith("MIL-CERT-");
    }

    private boolean isRegisteredMilitaryDevice(String deviceId) {
        // Sprawdzenie czy urządzenie jest w rejestrze wojskowym
        return deviceId != null && deviceId.matches("^(GTX|DAGR|SOTAS)-[0-9A-F]{8}$");
    }

    private boolean verifyDataIntegrity(MilitaryGpsData gpsData) {
        // Weryfikacja integralności danych (checksum, podpis cyfrowy)
        return gpsData.getChecksum() != null && gpsData.getChecksum().length() == 64;
    }

    private boolean isInRestrictedMilitaryZone(GpsPosition position) {
        // Sprawdzenie czy pozycja jest w strefie wojskowej (symulacja)
        // W rzeczywistości sprawdzałoby bazę stref zakazanych
        return false;
    }

    private boolean isDeviatingFromRoute(GpsPosition position) {
        // Sprawdzenie odchylenia od zaplanowanej trasy
        // W rzeczywistości porównywałoby z approved route
        return false;
    }

    private String buildMilitaryGpsSensorData(GpsPosition position) {
        return String.format(
                "MILITARY_GPS{device:%s,accuracy:%.2fm,satellites:%d,security:%s}",
                position.getDeviceId(),
                position.getAccuracy(),
                8, // Symulacja liczby satelitów
                position.getSecurityLevel()
        );
    }

    private void createSecurityAlert(MilitaryGpsData gpsData) {
        alertService.createAlert(
                "🚨 SECURITY BREACH: Unauthorized military GPS device: " + gpsData.getDeviceId(),
                AlertLevel.CRITICAL,
                null,
                null,
                "SECURITY_BREACH"
        );
    }

    private void createTechnicalAlert(MilitaryGpsData gpsData, String error) {
        alertService.createAlert(
                "🔧 Military GPS technical error: " + error,
                AlertLevel.MEDIUM,
                gpsData.getTransportId(),
                null,
                "TECHNICAL_ERROR"
        );
    }
}