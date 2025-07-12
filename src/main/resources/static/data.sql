-- Ciężarówki i ciągniki wojskowe
INSERT INTO vehicle_specification (model, type, total_weight_kg, axle_count, max_axle_load_kg, height_cm, width_cm, length_cm, description) VALUES
('MAN TGS 18.440', 'Ciężarówka', 12000, 3, 6000, 350, 250, 700, 'Ciężarówka transportowa używana w wojsku polskim'),
('Scania R 500', 'Ciężarówka', 13000, 3, 6500, 360, 255, 720, 'Ciężarówka wojskowa do transportu sprzętu'),
('Volvo FMX 460', 'Ciężarówka', 12500, 3, 6200, 355, 250, 710, 'Ciężarówka terenowa, bardzo wytrzymała'),
('Tatra 815', 'Ciężarówka', 15000, 3, 7000, 370, 260, 730, 'Ciężarówka terenowa, często wykorzystywana w wojsku'),

-- Sprzęt transportowany (czołgi, transportery, pojazdy wsparcia)
('PT-91 Twardy', 'Czołg', 45000, 7, 8000, 320, 350, 700, 'Polski czołg podstawowy'),
('Rosomak', 'Transporter', 24000, 6, 7000, 300, 280, 700, 'Wielozadaniowy transporter opancerzony'),
('BMP-1', 'Transporter', 13000, 5, 5000, 250, 280, 620, 'Rosyjski transporter opancerzony'),
('Woz Sztabowy', 'Pojazd wsparcia', 8000, 2, 4000, 270, 200, 500, 'Lekki pojazd wsparcia'),
('Haubica 155mm', 'Sprzęt wsparcia', 18000, 4, 6000, 250, 300, 650, 'Działo samobieżne'),
('Most Samobieżny', 'Sprzęt wsparcia', 20000, 5, 6000, 280, 320, 700, 'Mobilny most wojskowy');

-- Przykładowe zestawy (ciągnik + ładunek)
INSERT INTO transport_set (transporter_id, cargo_id) VALUES
(1, 5),  -- MAN TGS 18.440 + PT-91 Twardy
(2, 6),  -- Scania R 500 + Rosomak
(3, 7),  -- Volvo FMX 460 + BMP-1
(4, 8),  -- Tatra 815 + Woz Sztabowy
(2, 9),  -- Scania R 500 + Haubica 155mm
(1, 10); -- MAN TGS 18.440 + Most Samobieżny
