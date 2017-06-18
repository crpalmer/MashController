package org.crpalmer.mashcontroller;

/**
 * Created by crpalmer on 6/17/17.
 */

public class VendorHeaterPowerPredictor implements HeaterPowerPredictor {
    private static final int DELTA_T1 = 4;
    private static final int DELTA_T2 = 2;
    private static final int SLOW_POWER = 75;
    private static final int LOW_POWER = 50;
    private static final int MAINTAIN_POWER = 25;

    private boolean isMaintaining;

    @Override
    public int predict(double currentTemperature, double targetTemperature) {
        if (currentTemperature < targetTemperature - DELTA_T1) {
            isMaintaining = false;
            return 100;
        } else if (currentTemperature < targetTemperature - DELTA_T2) {
            isMaintaining = false;
            return SLOW_POWER;
        } else if (currentTemperature < targetTemperature) {
            return isMaintaining ? MAINTAIN_POWER : LOW_POWER;
        } else {
            isMaintaining = true;
            return 0;
        }
    }
}
