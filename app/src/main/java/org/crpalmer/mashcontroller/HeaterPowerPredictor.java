package org.crpalmer.mashcontroller;

/**
 * HeaterPowerPredictor
 *
 * Predict the heater power based on the current and target temperatures.
 */

public interface HeaterPowerPredictor {
    void start(double targetTemperature);
    int predict(double currentTemperature);
}
