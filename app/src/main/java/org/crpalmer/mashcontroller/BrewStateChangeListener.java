package org.crpalmer.mashcontroller;

import android.os.CountDownTimer;

/**
 * Created by crpalmer on 6/21/17.
 */

public interface BrewStateChangeListener {
    void onConnectionStateChanged(boolean connected);

    void onHeaterChanged(boolean on, int power);

    void onPumpChanged(boolean on);

    void onStepStart(int num, String description);

    void onStepTick(int secondsLeft);

    void onTemperatureChanged(double temperature);

    void onTargetTemperatureChanged(double targetTemperature);
}
