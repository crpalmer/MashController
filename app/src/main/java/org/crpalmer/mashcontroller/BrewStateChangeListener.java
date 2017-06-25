package org.crpalmer.mashcontroller;

/**
 * Created by crpalmer on 6/21/17.
 */

public interface BrewStateChangeListener {
    void onConnectionStateChanged(boolean connected);

    void onHeaterChanged(boolean on, int power);

    void onPumpChanged(boolean on);

    void onTemperatureChanged(double temperature);

    void onTargetTemperatureChanged(double targetTemperature);
}