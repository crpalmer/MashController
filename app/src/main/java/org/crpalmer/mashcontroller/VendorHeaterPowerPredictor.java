package org.crpalmer.mashcontroller;

/**
 * VendorHeaterPowerPredictor
 * <p>
 * This is the prediction algorithm used by the Brew Boss vendor in their product.
 */

public class VendorHeaterPowerPredictor implements HeaterPowerPredictor {
    private static final int DELTA_T1 = 4;
    private static final int DELTA_T2 = 2;
    private static final int SLOW_POWER = 75;
    private static final int LOW_POWER = 50;
    private static final int MAINTAIN_POWER = 25;

    private boolean isMaintaining;
    private double targetTemperature;

    @Override
    public synchronized void start(double targetTemperature) {
        isMaintaining = false;
        this.targetTemperature = targetTemperature;
    }

    @Override
    public synchronized int predict(double currentTemperature) {
        if (currentTemperature < targetTemperature - DELTA_T1) {
            return 100;
        } else if (currentTemperature < targetTemperature - DELTA_T2) {
            return SLOW_POWER;
        } else if (currentTemperature < targetTemperature) {
            return isMaintaining ? MAINTAIN_POWER : LOW_POWER;
        } else {
            isMaintaining = true;
            return 0;
        }
    }
}
