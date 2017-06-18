package org.crpalmer.mashcontroller;

import android.util.Log;

/**
 * Created by crpalmer on 6/17/17.
 */

public class BrewBoss {
    private static final String TAG = "BrewBoss";
    private final BrewBossConnection connection = new BrewBossConnection();
    private final BrewBossState state = new BrewBossState(connection);

    private HeaterPowerPredictor predictor = new HybridHeaterPowerPredictor();
    private double targetTemperature;

    public boolean isConnected() {
        return connection.isConnected();
    }

    public double getTemperature() {
        return state.getTemperature();
    }

    public synchronized double getTargetTemperature() {
        return targetTemperature;
    }

    public synchronized void setTargetTemperature(double temperature) {
        if (targetTemperature != temperature) {
            targetTemperature = temperature;
            predictor.start(temperature);
        }
    }

    public int getHeaterPower() {
        return state.getHeaterPower();
    }

    public boolean isPumpOn() {
        return state.isPumpOn();
    }

    public void setPumpEnabled(boolean isEnabled) throws BrewBossConnectionException {
        connection.setPumpEnabled(isEnabled);
    }

    public void heatTo(double temperature) {
        setTargetTemperature(temperature);
        while (targetTemperature > 0 && Math.abs(getTemperature() - targetTemperature) < 0.1) {
            pause();
        }
    }

    public void heaterOff() {
        heatTo(0);
    }

    private final Thread thread = new Thread() {
        @Override
        public void run() {
            while (true) {
                if (targetTemperature > 0) {
                    int power = predictor.predict(state.getTemperature());
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