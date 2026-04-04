package com.volvo.powersync.simulator.domain;

import java.util.Objects;

/**
 * In-memory vehicle model. {@code vin} is the primary key.
 */
public final class Car {

    private final String vin;
    private volatile int batteryPercentage;
    private volatile CarState state;
    private volatile String assignedChargingStationId;

    public Car(String vin, int batteryPercentage, CarState state, String assignedChargingStationId) {
        this.vin = Objects.requireNonNull(vin, "vin");
        this.batteryPercentage = clampBattery(batteryPercentage);
        this.state = Objects.requireNonNull(state, "state");
        this.assignedChargingStationId = assignedChargingStationId;
    }

    /**
     * New vehicles start in {@link CarState#DRIVING} with no assigned charging station.
     */
    public static Car create(String vin, int batteryPercentage) {
        return new Car(vin, batteryPercentage, CarState.DRIVING, null);
    }

    public String vin() {
        return vin;
    }

    public int batteryPercentage() {
        return batteryPercentage;
    }

    public CarState state() {
        return state;
    }

    public String assignedChargingStationId() {
        return assignedChargingStationId;
    }

    public void setState(CarState newState) {
        this.state = Objects.requireNonNull(newState, "state");
    }

    public void setAssignedChargingStationId(String assignedChargingStationId) {
        this.assignedChargingStationId = assignedChargingStationId;
    }

    /**
     * Applies one simulation tick for this car based on its current {@link CarState}.
     * When driving, battery drops by exactly 1 point (e.g. 50% → 49%). Bounds only apply at 0% / 100%.
     */
    public void applyBatteryTick() {
        switch (state) {
            case DRIVING -> batteryPercentage = Math.max(0, batteryPercentage - 1);
            case CHARGING -> batteryPercentage = Math.min(100, batteryPercentage + 2);
            case STOPPED, WAITING_FOR_CHARGE -> {
                // no change
            }
        }
    }

    private static int clampBattery(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
