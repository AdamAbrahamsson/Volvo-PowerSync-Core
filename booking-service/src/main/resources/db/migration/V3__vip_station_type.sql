ALTER TABLE charging_stations
    ADD COLUMN IF NOT EXISTS station_type VARCHAR(32) NOT NULL DEFAULT 'NORMAL';

INSERT INTO charging_stations (name, state, station_type, assigned_vin)
SELECT 'VIP Hub', 'FREE', 'VIP', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM charging_stations WHERE station_type = 'VIP'
);
