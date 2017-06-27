package org.crpalmer.mashcontroller;

import android.os.CountDownTimer;
import android.support.annotation.VisibleForTesting;

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
    private static final int AT_TEMP_ONE_DEGREE_MS = 15*60*1000;
    private static final int ONE_HOUR_MS = 60*60*1000;
    private static final int RESTORE_TEMP_MS = 1*60*1000;

    private static final int NUM_OBSERVATIONS = 10;

    private double observations[] = new double[NUM_OBSERVATIONS];
    private int nObservations;
    private int nextObservation;
    private double sumObservations;
    private boolean wasOn;
    private int heaterStartPower;
    private long temperatureStartMs;
    private double temperatureStart;
    private double currentTemperature;
    private long currentTemperatureMs;
    private Double smoothedMsPerDegree;
    private double targetTemperature;

    public MaintainingHeaterPowerPredictor(BrewController brewController) {
        brewController.addStateChangeListener(this);
    }

    @Override
    public void start(double currentTemperature, double targetTemperature) {
        this.targetTemperature = targetTemperature;
        this.currentTemperature = currentTemperature;
    }

    @Override
    public int predict(double currentTemperature) {
        // TODO: What if we start at the target temperature and then drop??
        if (smoothedMsPerDegree == null) {
            return 0;
        }

        double power = 0;

        // TODO: Use the median instead

        if (currentTemperature < targetTemperature) {
            power = 100 * (targetTemperature - currentTemperature) * smoothedMsPerDegree / RESTORE_TEMP_MS;
        } else if (currentTemperature == targetTemperature) {
            power = 100 * (smoothedMsPerDegree / AT_TEMP_ONE_DEGREE_MS);
        } else if (currentTemperature == targetTemperature+1) {
            power = 100 * (smoothedMsPerDegree / ONE_HOUR_MS);
        }
        int powerInt = (int) Math.ceil(power);
        if (powerInt < 0) return 0;
        else if (powerInt > 100) return 100;
        else return powerInt;
    }

    @Override
    public void onConnectionStateChanged(boolean connected) {
    }

    @Override
    public void onHeaterChanged(boolean on, int power) {
        if ((on && ! wasOn) || heaterStartPower != power) {
            heaterStartPower = power;
            temperatureStartMs = System.currentTimeMillis();
            temperatureStart = currentTemperature;
            currentTemperatureMs = temperatureStartMs;
        }
        wasOn = on;
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
        onTemperatureChanged(temperature, System.currentTimeMillis());
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public void onTemperatureChanged(double temperature, long ms) {
        // TODO: What if the current power is too low and temperature is dropping?  How does this
        // need to accommodate that?
        // TODO: Specifically, if nothing else we need to update currentTemperature & CTMs

        // Only update the estimate when we have done more than 1 degree difference because of the numeric imprecision of the temperature
        if (currentTemperature < temperature && temperatureStartMs > 0) {
            // TODO: Only skip the update when the pump has cycled off, not just changed temperatures
            // For example, ramping to 120 it goes to 116 and then changes temp which ignores the 116 -> 117 update, does 117 -> 118 and
            // the again changes temperature so it ignores 118 -> 119 but does 119 -> 120
            if (temperatureStart+1 < temperature) {
                updatePrediction(temperature, ms);
            }
            currentTemperature = temperature;
            currentTemperatureMs = ms;
        }
    }

    private void updatePrediction(double temperature, long ms) {
        double deltaDegree = (temperature - currentTemperature);
        long deltaMs = ms - currentTemperatureMs;
        double newMsPerDegree = deltaMs / deltaDegree * (heaterStartPower / 100.0);

        if (nObservations == NUM_OBSERVATIONS) {
            sumObservations -= observations[nextObservation];
        } else {
            nObservations++;
        }

        sumObservations += newMsPerDegree;
        observations[nextObservation] = newMsPerDegree;

        nextObservation = (nextObservation + 1) % NUM_OBSERVATIONS;

        smoothedMsPerDegree = sumObservations / nObservations;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public double getMsPerDegree() {
        return smoothedMsPerDegree != null ? smoothedMsPerDegree : 0;
    }

    @Override
    public void onTargetTemperatureChanged(double targetTemperature) {

    }
}
