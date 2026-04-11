-- Rename stations for demo naming
UPDATE charging_stations
SET name = 'Lindholmen'
WHERE station_type = 'VIP'
  AND name <> 'Lindholmen';

UPDATE charging_stations
SET name = 'Torslanda'
WHERE station_type = 'NORMAL'
  AND name = 'Station Nord';

UPDATE charging_stations
SET name = 'Mölndal'
WHERE station_type = 'NORMAL'
  AND name = 'Station Syd';

-- Remove extra normal stations so we keep exactly two normal slots
DELETE FROM charging_stations
WHERE station_type = 'NORMAL'
  AND name IN ('Station Ost', 'Station Vast');

-- Safety inserts if database did not contain the expected originals
INSERT INTO charging_stations (name, state, station_type, assigned_vin)
SELECT 'Torslanda', 'FREE', 'NORMAL', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM charging_stations
    WHERE station_type = 'NORMAL' AND name = 'Torslanda'
);

INSERT INTO charging_stations (name, state, station_type, assigned_vin)
SELECT 'Mölndal', 'FREE', 'NORMAL', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM charging_stations
    WHERE station_type = 'NORMAL' AND name = 'Mölndal'
);
