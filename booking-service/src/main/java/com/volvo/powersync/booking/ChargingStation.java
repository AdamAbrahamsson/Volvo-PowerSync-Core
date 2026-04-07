package com.volvo.powersync.booking;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One row in the charging_stations table. */
@Entity
@Table(name = "charging_stations")
public class ChargingStation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private StationState state;

    @Enumerated(EnumType.STRING)
    @Column(name = "station_type", nullable = false, length = 32)
    private StationType stationType;

    /** Which car booked this spot; null while the station is FREE. */
    @Column(name = "assigned_vin", length = 64)
    private String assignedVin;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public StationState getState() {
        return state;
    }

    public void setState(StationState state) {
        this.state = state;
    }

    public StationType getStationType() {
        return stationType;
    }

    public void setStationType(StationType stationType) {
        this.stationType = stationType;
    }

    public String getAssignedVin() {
        return assignedVin;
    }

    public void setAssignedVin(String assignedVin) {
        this.assignedVin = assignedVin;
    }
}
