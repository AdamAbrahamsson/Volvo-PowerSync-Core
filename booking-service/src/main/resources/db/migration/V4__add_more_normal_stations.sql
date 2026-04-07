INSERT INTO charging_stations (name, state, station_type, assigned_vin)
SELECT 'Station Ost', 'FREE', 'NORMAL', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM charging_stations WHERE name = 'Station Ost'
);

INSERT INTO charging_stations (name, state, station_type, assigned_vin)
SELECT 'Station Vast', 'FREE', 'NORMAL', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM charging_stations WHERE name = 'Station Vast'
);
