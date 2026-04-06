ALTER TABLE charging_stations
    ADD COLUMN IF NOT EXISTS assigned_vin VARCHAR(64) NULL;

COMMENT ON COLUMN charging_stations.assigned_vin IS 'VIN of the car that booked this station; NULL when FREE';
