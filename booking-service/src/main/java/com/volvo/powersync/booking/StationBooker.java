package com.volvo.powersync.booking;

import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * All database logic for “give me one free charging station” lives here.
 * The gRPC class only talks to the outside world; this class talks to PostgreSQL.
 */
@Service
public class StationBooker {

    private final ChargingStationRepository stations;
    private final BookingEventsPublisher eventsPublisher;
    private final BookingMetrics bookingMetrics;

    public StationBooker(
            ChargingStationRepository stations,
            BookingEventsPublisher eventsPublisher,
            BookingMetrics bookingMetrics) {
        this.stations = stations;
        this.eventsPublisher = eventsPublisher;
        this.bookingMetrics = bookingMetrics;
    }

    /**
     * Tries to book one free station for the given car.
     *
     * @return the station database id as text, or null if every station is already BOOKED
     */
    @Transactional
    public String tryBookOneFreeStation(String vin) {
        return tryBookOneFreeStationByType(vin, StationType.NORMAL);
    }

    @Transactional
    public String tryBookOneFreeStationByType(String vin, StationType stationType) {
        List<ChargingStation> freeStations =
                stations.findTop1ByStateAndStationTypeOrderByIdAsc(StationState.FREE, stationType);

        if (freeStations.isEmpty()) {
            return null;
        }

        ChargingStation station = freeStations.get(0);
        station.setState(StationState.BOOKED);
        station.setAssignedVin(vin);
        stations.save(station);
        eventsPublisher.publishStationStatus(station);
        bookingMetrics.bookingSuccess(station);

        return String.valueOf(station.getId());
    }

    /**
     * Frees the station if it is booked by this VIN. Otherwise does nothing and returns false.
     */
    @Transactional
    public boolean releaseStation(String vin, String stationIdText) {
        long id;
        try {
            id = Long.parseLong(stationIdText);
        } catch (NumberFormatException e) {
            return false;
        }
        Optional<ChargingStation> locked = stations.findByIdWithLock(id);
        if (locked.isEmpty()) {
            return false;
        }
        ChargingStation station = locked.get();
        if (station.getState() != StationState.BOOKED) {
            return false;
        }
        if (station.getAssignedVin() == null || !station.getAssignedVin().equals(vin)) {
            return false;
        }
        station.setState(StationState.FREE);
        station.setAssignedVin(null);
        stations.save(station);
        eventsPublisher.publishStationStatus(station);
        return true;
    }

    /**
     * Frees whichever station is booked for this VIN (ignores station id). Use when the client’s id may be stale.
     */
    @Transactional
    public boolean releaseStationForVin(String vin) {
        Optional<ChargingStation> locked =
                stations.findFirstByAssignedVinAndStateOrderByIdAsc(vin, StationState.BOOKED);
        if (locked.isEmpty()) {
            return false;
        }
        ChargingStation station = locked.get();
        station.setState(StationState.FREE);
        station.setAssignedVin(null);
        stations.save(station);
        eventsPublisher.publishStationStatus(station);
        return true;
    }

    /**
     * Startup safety reset:
     * if simulator cars restart from fresh in-memory state, persisted BOOKED rows become stale.
     * We clear every assignment so the system starts from a consistent baseline.
     */
    @Transactional
    public void resetAllStationsToFree() {
        List<ChargingStation> allStations = stations.findAll();
        for (ChargingStation station : allStations) {
            station.setState(StationState.FREE);
            station.setAssignedVin(null);
            stations.save(station);
            eventsPublisher.publishStationStatus(station);
        }
    }
}
