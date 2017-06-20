package org.crpalmer.mashcontroller;

import android.util.Log;

/**
 * Created by crpalmer on 6/17/17.
 */

public class BrewBoss {
    private static final String TAG = "BrewBoss";
    private final BrewBossConnection connection = new BrewBossConnection();
    private final BrewBossState state = new BrewBossState(connection);

    private HeaterPowerPredictor rampingPredictor = new VendorHeaterPowerPredictor();
    private HeaterPowerPredictor maintainPredictor = new PIDHeaterPowerPredictor();
    private HeaterPowerPredictor predictor;
    private double targetTemperature;

    public BrewBoss()     {
        predictor = new HybridHeaterPowerPredictor(rampingPredictor, maintainPredictor);
        thread.start();
    }

    public int getHeaterPower() {
        return state.getHeaterPower();
    }
    public double getTemperature() {
        return state.getTemperature();
    }
    public synchronized double getTargetTemperature() {
        return targetTemperature;
    }
    public boolean isAutomaticMode() { return state.isAutomaticMode(); }
    public boolean isConnected() {
        return connection.isConnected();
    }
    public boolean isHeaterOn() { return state.isHeaterOn(); }
    public boolean isPumpOn() {
        return state.isPumpOn();
    }

    public synchronized void setAutomaticMode(boolean on) {
        state.setAutomaticMode(on);
        // TODO: Stop the auto updates
    }

    public synchronized void setHeaterOn(boolean on) throws BrewBossConnectionException {
        connection.setHeaterPower(on ? state.getHeaterPower() : 0);
        state.setHeaterOn(on);
    }

    public void setHeaterPower(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Temperature must be between 0 and 220");
        }

    }
    public synchronized void setTargetTemperature(double temperature) {
        if (temperature < 0 || temperature > 220) {
            throw new IllegalArgumentException("Temperature must be between 0 and 220");
        }
        if (targetTemperature != temperature) {
            targetTemperature = temperature;
            predictor.start(temperature);
        }
    }

    public void setPumpOn(boolean isPumpOn) throws BrewBossConnectionException {
        connection.setPumpOn(isPumpOn);
    }

    public void heatTo(double temperature) throws BrewBossConnectionException {
        setHeaterOn(true);
        setTargetTemperature(temperature);
        while (targetTemperature > 0 && Math.abs(getTemperature() - targetTemperature) < 0.1) {
            pause();
        }
    }

    private final Thread thread = new Thread() {
        @Override
        public void run() {
            while (true) {
                if (state.isAutomaticMode() && targetTemperature > 0) {
                    int power = predictor.predict(state.getTemperature());
                    Log.e("CRP", "targetTemperature " + targetTemperature + " power " + power);
                    if (power != state.getHeaterPower()) {
                        try {
                            connection.setHeaterPower(power);
                        } catch (BrewBossConnectionException e) {
                            // TODO report this somehow
                        }
                    }
                }
                pause();
            }
        }
    };

    private void pause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted: " + e.getLocalizedMessage());
        }
    }
}