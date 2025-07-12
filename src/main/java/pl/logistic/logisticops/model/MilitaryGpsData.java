package pl.logistic.logisticops.model;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class MilitaryGpsData {
    private String deviceId;              // ID urządzenia (GTX-12345678)
    private String securityCertificate;   // Certyfikat bezpieczeństwa
    private String checksum;              // Suma kontrolna danych
    private Double encryptedLatitude;     // Zaszyfrowana szerokość
    private Double encryptedLongitude;    // Zaszyfrowana długość
    private Double altitude;              // Wysokość nad poziomem morza
    private Double speed;                 // Prędkość w km/h
    private Integer heading;              // Kurs w stopniach
    private Double accuracy;              // Dokładność w metrach
    private LocalDateTime timestamp;      // Czas pobrania GPS
    private Long vehicleId;               // ID pojazdu w systemie
    private Long transportId;             // ID transportu


}

