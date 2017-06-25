package org.crpalmer.mashcontroller;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static android.content.ContentValues.TAG;

/**
 * BrewBossState
 * <p>
 * Encapsulate the state and process updates from the controller to keep the view of the state correct.
 */

public class BrewBossState {
    private static final int UPDATE_STATE_MSG = 1;
    private static final int UPDATE_STATE_MS = 1000;

    private final BrewBossConnection connection;

    private final AtomicBoolean automaticMode = new AtomicBoolean();
    private final AtomicInteger temperatureTimes100 = new AtomicInteger();
    private final AtomicBoolean heaterOn = new AtomicBoolean();
    private final AtomicInteger heaterPower = new AtomicInteger();
    private final AtomicBoolean pumpOn = new AtomicBoolean();

    private final List<BrewStateChangeListener> listeners = new LinkedList<>();

    BrewBossState(BrewBossConnection connection) {
        this.connection = connection;
        looperThread.start();
        looperThread.scheduleUpdateState();
    }

    public int getHeaterPower() {
        return heaterPower.get();
    }

    public double getTemperature() {
        return temperatureTimes100.get() / 100.0;
    }

    public boolean isAutomaticMode() {
        return automaticMode.get();
    }

    public void setAutomaticMode(boolean on) {
        automaticMode.set(on);
    }

    public boolean isHeaterOn() {
        return heaterOn.get();
    }

    public boolean isPumpOn() {
        return pumpOn.get();
    }

    public void addStateChangeListener(BrewStateChangeListener l) {
        synchronized (listeners) {
            listeners.add(l);
            l.onHeaterChanged(isHeaterOn(), getHeaterPower());
            l.onPumpChanged(isPumpOn());
            l.onTemperatureChanged(getTemperature());
        }
    }

    private void processStateUpdate(String line) {
        boolean heaterChanged = false;
        boolean pumpChanged = false;
        boolean temperatureChanged = false;

        String values[] = line.split(",");
        double newTemperature = Double.valueOf(values[1]);
        int newTemperatureTimes100 = (int) Math.round(newTemperature * 100);
        int newHeaterPower = Integer.valueOf(values[2]);
        boolean newHeaterOn = newHeaterPower > 0;
        boolean newPumpOn = "1".equals(values[3]);
        heaterChanged |= heaterOn.getAndSet(newHeaterOn) != newHeaterOn;
        if (newHeaterOn) {
            heaterChanged |= heaterPower.getAndSet(newHeaterPower) != newHeaterPower;
        }
        pumpChanged |= pumpOn.getAndSet(newPumpOn) != newPumpOn;
        temperatureChanged |= temperatureTimes100.getAndSet(newTemperatureTimes100) != newTemperatureTimes100;

        if (temperatureChanged || heaterChanged || pumpChanged) {
            synchronized(listeners) {
                for (BrewStateChangeListener l : listeners) {
                    if (heaterChanged) {
                        l.onHeaterChanged(isHeaterOn(), getHeaterPower());
                    }
                    if (pumpChanged) {
                        l.onPumpChanged(isPumpOn());
                    }
                    if (temperatureChanged) {
                        l.onTemperatureChanged(getTemperature());
                    }
                }
            }
        }
    }

    private void updateState() {
        try {
            connection.heartBeat();
            String line = connection.readLine();
            processStateUpdate(line);
        } catch (BrewBossConnectionException e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
        } finally {
            looperThread.scheduleUpdateState();
        }
    }

    private LooperThread looperThread = new LooperThread();

    private class LooperThread extends Thread {
        public Handler handler;

        public void scheduleUpdateState() {
            while (handler == null) {}
            handler.sendMessageDelayed(handler.obtainMessage(UPDATE_STATE_MSG), UPDATE_STATE_MS);
        }

        public void run() {
            Looper.prepare();

            handler = new Handler() {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case UPDATE_STATE_MSG:
                            updateState();
                            break;
                    }
                }
            };

            Looper.loop();
        }
    }
}