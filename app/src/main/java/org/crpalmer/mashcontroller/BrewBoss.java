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

    public BrewBoss()     {
        predictor = new HybridHeaterPowerPredictor(rampingPredictor, maintainPredictor);
    }

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
        if (temperature < 0 || temperature > 220) {
            throw new IllegalArgumentException("Temperature must be between 0 and 220");
        }
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

    public void setPumpOn(boolean isPumpOn) throws BrewBossConnectionException {
        connection.setPumpOn(isPumpOn);
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