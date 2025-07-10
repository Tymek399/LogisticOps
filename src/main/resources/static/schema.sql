-- Tabela przechowująca specyfikacje pojazdów i ładunków
CREATE TABLE vehicle_specification (
                                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                       model VARCHAR(100) NOT NULL,
                                       type VARCHAR(50) NOT NULL, -- np. Czołg, Transporter, Pojazd wsparcia, Ciężarówka
                                       total_weight_kg INT NOT NULL,
                                       axle_count INT NOT NULL,
                                       max_axle_load_kg INT NOT NULL,
                                       height_cm INT NOT NULL,
                                       width_cm INT NOT NULL,
                                       length_cm INT NOT NULL,
                                       description TEXT,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Tabela łącząca transporter (np. ciężarówka) z ładunkiem (np. czołg)
CREATE TABLE transport_set (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               transporter_id BIGINT NOT NULL,
                               cargo_id BIGINT NOT NULL,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               CONSTRAINT fk_transporter FOREIGN KEY (transporter_id) REFERENCES vehicle_specification(id),
                               CONSTRAINT fk_cargo FOREIGN KEY (cargo_id) REFERENCES vehicle_specification(id)
);
