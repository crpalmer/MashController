package org.crpalmer.mashcontroller;

/**
 * HeaterPowerPredictor
 * <p>
 * Predict the heater power based on the current and target temperatures.
 */

public interface HeaterPowerPredictor {
    void start(double currentTemperature, double targetTemperature);

    int predict(double currentTemperature);
}
