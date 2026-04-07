package com.volvo.powersync.simulator;

import java.util.Objects;

/** In-memory car: VIN is the id; battery and state change each tick. */
public final class Car {

    private final String vin;
    private volatile int batteryPercentage;
    private volatile CarState state;
    private volatile String assignedChargingStationId;
    private volatile boolean lowBatteryBookingAttempted;
    private final boolean vipEligible;

    public Car(String vin, int batteryPercentage, CarState state, String assignedChargingStationId, boolean vipEligible) {
        this.vin = Objects.requireNonNull(vin, "vin");
        this.batteryPercentage = clampBattery(batteryPercentage);
        this.state = Objects.requireNonNull(state, "state");
        this.assignedChargingStationId = assignedChargingStationId;
        this.vipEligible = vipEligible;
    }

    /** New cars start driving with no charging station. */
    public static Car create(String vin, int batteryPercentage) {
        return new Car(vin, batteryPercentage, CarState.DRIVING, null, false);
    }

    public static Car createVip(String vin, int batteryPercentage) {
        return new Car(vin, batteryPercentage, CarState.DRIVING, null, true);
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

    public boolean vipEligible() {
        return vipEligible;
    }

    public void setState(CarState newState) {
        this.state = Objects.requireNonNull(newState, "state");
    }

    public void setAssignedChargingStationId(String assignedChargingStationId) {
        this.assignedChargingStationId = assignedChargingStationId;
    }

    public boolean isLowBatteryBookingAttempted() {
        return lowBatteryBookingAttempted;
    }

    public void markLowBatteryBookingAttempted() {
        lowBatteryBookingAttempted = true;
    }

    public void clearLowBatteryBookingAttempted() {
        lowBatteryBookingAttempted = false;
    }

    /** One simulation step: battery up/down depends on {@link #state()}. */
    public void applyBatteryTick() {
        switch (state) {
            case DRIVING -> {
                batteryPercentage = Math.max(0, batteryPercentage - 2);
                if (batteryPercentage == 0) {
                    state = CarState.STOPPED;
                }
            }
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
