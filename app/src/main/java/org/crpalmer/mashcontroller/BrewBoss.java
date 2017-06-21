package org.crpalmer.mashcontroller;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 * BrewBoss
 *
 * Interface to the controller.  It allows state inspection and updates to the state.
 */

public class BrewBoss {
    private static final String TAG = "BrewBoss";
    private static final int UPDATE_TEMPERATURE_MSG = 1;
    private static final int UPDATE_TEMPEATURE_MS = 900;

    private final BrewBossConnection connection = new BrewBossConnection();
    private final BrewBossState state = new BrewBossState(connection);

    private HeaterPowerPredictor predictor;
    private double targetTemperature;

    public BrewBoss() {
        HeaterPowerPredictor rampingPredictor = new VendorHeaterPowerPredictor();
        HeaterPowerPredictor maintainPredictor = new PIDHeaterPowerPredictor();
        predictor = new HybridHeaterPowerPredictor(rampingPredictor, maintainPredictor);
        setAutomaticMode(true);
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

    public boolean isAutomaticMode() {
        return state.isAutomaticMode();
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public boolean isHeaterOn() {
        return state.isHeaterOn();
    }

    public boolean isPumpOn() {
        return state.isPumpOn();
    }

    public void addStateChangeListener(BrewBossStateChangeListener listener) {
        state.addStateChangeListener(listener);
    }

    public synchronized void setAutomaticMode(boolean on) {
        state.setAutomaticMode(on);
        handler.removeMessages(UPDATE_TEMPERATURE_MSG);
        if (on) {
            scheduleUpdateTemperature();
        }
    }

    public synchronized void setHeaterOn(boolean on) throws BrewBossConnectionException {
        connection.setHeaterPower(on ? state.getHeaterPower() : 0);
    }

    public void setHeaterPower(int percent) throws BrewBossConnectionException {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Temperature must be between 0 and 220");
        }
        connection.setHeaterPower(percent);
    }

    public synchronized void setTargetTemperature(double temperature) {
        if (temperature < 0 || temperature > 220) {
            throw new IllegalArgumentException("Temperature must be between 0 and 220");
        }
        if (targetTemperature != temperature) {
            targetTemperature = temperature;
            predictor.start(temperature);
            ensureTemperatureUpdateScheduled();
        }
    }

    public void setPumpOn(boolean isPumpOn) throws BrewBossConnectionException {
        connection.setPumpOn(isPumpOn);
    }

    private void scheduleUpdateTemperature() {
        handler.sendMessageDelayed(handler.obtainMessage(UPDATE_TEMPERATURE_MSG), UPDATE_TEMPEATURE_MS);
    }

    private void ensureTemperatureUpdateScheduled() {
        handler.removeMessages(UPDATE_TEMPERATURE_MSG);
        scheduleUpdateTemperature();
    }

    private void updateTemperature() {
        if (state.isAutomaticMode() && targetTemperature > 0) {
            int power = predictor.predict(state.getTemperature());
            Log.v(TAG, "targetTemperature " + targetTemperature + " power " + power);
            if (power != state.getHeaterPower()) {
                try {
                    connection.setHeaterPower(power);
                } catch (BrewBossConnectionException e) {
                    // TODO report this somehow
                }
            }
            scheduleUpdateTemperature();
        }
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TEMPERATURE_MSG:
                    updateTemperature();
            }
        }
    };
}