package org.crpalmer.mashcontroller;

import android.os.CountDownTimer;

/**
 * MaintainingHeaterPowerPredictor
 *
 * A power predictor that attempts to maintain the temperature over a window of time (or restore the
 * temperature over a window of time).
 *
 * Times here are scaled to be relative to 100% heater power (aka, we assume that 50% heater power
 * heats half as fast as 100% power and 10x faster than 5% power).
 *
 * If we've dropped below the target temperature, we set the heater to the power we think it will
 * need to restore that 1 degree over RESTORE_TEMP_MS milliseconds.  If we are at or above the target
 * temperature we set the power to be the poewr we predict we would need to raise the temperature
 * 1 degree over PREDICT_ONE_DEGREE_MS.
 */

public class MaintainingHeaterPowerPredictor implements HeaterPowerPredictor, BrewStateChangeListener {
    private static final double ALPHA = 0.5;
    private static final int PREDICT_ONE_DEGREE_MS = 2*60*60*1000;
    private static final int RESTORE_TEMP_MS = 1*60*1000;

    private int heaterStartPower;
    private long temperatureStartMs;
    private double temperatureStart;
    private double currentTemperature;
    private int smoothedMsPerDegree;
    private double targetTemperature;

    public MaintainingHeaterPowerPredictor(BrewController brewController) {
        brewController.addStateChangeListener(this);
    }

    @Override
    public void start(double targetTemperature) {
        this.targetTemperature = targetTemperature;
    }

    @Override
    public int predict(double currentTemperature) {
        double power;
        if (currentTemperature < targetTemperature) {
            power = (targetTemperature - currentTemperature) * smoothedMsPerDegree / RESTORE_TEMP_MS;
        } else {
            power = 100.0 / (smoothedMsPerDegree / PREDICT_ONE_DEGREE_MS);
        }
        int powerInt = (int) Math.round(power);
        if (powerInt < 0) return 0;
        else if (powerInt > 100) return 100;
        else return powerInt;
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
    }

    @Override
    public void onHeaterChanged(boolean on, int power) {
        if (on && power != heaterStartPower) {
            heaterStartPower = power;
            temperatureStartMs = System.currentTimeMillis();
            temperatureStart = currentTemperature;
        }

    }

    @Override
    public void onPumpChanged(boolean on) {
    }

    @Override
    public void onStepStart(int num, String description) {

    }

    @Override
    public void onStepTick(int secondsLeft) {

    }

    @Override
    public void onTemperatureChanged(double temperature) {
        long ms = System.currentTimeMillis();

        // TODO: What if the current power is too low and temperature is dropping?  How does this
        // need to accommodate that?

        if (currentTemperature < temperature && temperatureStartMs > 0) {
            long neWMsPerDegree = ms - temperatureStartMs;
            double deltaDegree = (temperature - temperatureStart);
            long deltaMs = System.currentTimeMillis() - temperatureStartMs;
            double newMsPerDegree = deltaMs / deltaDegree * 100 / heaterStartPower;
            smoothedMsPerDegree = (int) Math.round(ALPHA*newMsPerDegree + (1-ALPHA) * smoothedMsPerDegree);
        }
        currentTemperature = temperature;
        temperatureStartMs = ms;
    }

    @Override
    public void onTargetTemperatureChanged(double targetTemperature) {

    }
}
