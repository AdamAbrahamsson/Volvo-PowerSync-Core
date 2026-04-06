CREATE TABLE charging_stations (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    state VARCHAR(32) NOT NULL
);

INSERT INTO charging_stations (name, state) VALUES ('Station Nord', 'FREE');
INSERT INTO charging_stations (name, state) VALUES ('Station Syd', 'FREE');
