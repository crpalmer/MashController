package org.crpalmer.mashcontroller;

/**
 * Created by crpalmer on 6/17/17.
 */

public interface HeaterPowerPredictor {
    public int predict(double currentTemperature, double targetTemperature);
}
