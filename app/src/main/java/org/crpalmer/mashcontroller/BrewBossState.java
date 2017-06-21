package org.crpalmer.mashcontroller;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by crpalmer on 6/17/17.
 */

public class BrewBossState {
    private static final String TAG = "BrewBossState";
    private static final int UPDATE_STATE_MSG = 1;
    private static final int UPDATE_STATE_MS = 1000;

    private final BrewBossConnection connection;

    private AtomicBoolean automaticMode = new AtomicBoolean();
    private AtomicInteger temperatureTimes100 = new AtomicInteger();
    private AtomicBoolean heaterOn = new AtomicBoolean();
    private AtomicInteger heaterPower = new AtomicInteger();
    private AtomicBoolean pumpOn = new AtomicBoolean();

    private List<BrewBossStateChangeListener> listeners = new LinkedList<BrewBossStateChangeListener>();

    BrewBossState(BrewBossConnection connection) {
        this.connection = connection;
        scheduleUpdateState();
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

    public boolean isHeaterOn() {
        return heaterOn.get();
    }

    public boolean isPumpOn() {
        return pumpOn.get();
    }

    public void setHeaterOn(boolean on) {
        heaterOn.set(on);
    }

    public void setAutomaticMode(boolean on) {
        automaticMode.set(on);
    }

    public void addStateChangeListener(BrewBossStateChangeListener l) {
        synchronized (listeners) {
            listeners.add(l);
        }
    }

    private void updateState() {
        try {
            connection.heartBeat();
            String line = connection.readLine();
            // TODO: figure out the format of this string

            // NOTE: if heaterOn == true && heaterPower != reported heater power then resend the heater power
            //       if heaterOn == false && reported heater power > 0 then set heater power to 9
            // (aka: treat our state as authority)  Is this right? Is this possible?
        } catch (BrewBossConnectionException e) {
        } finally {
            scheduleUpdateState();
        }
    }

    private void scheduleUpdateState() {
        handler.sendMessageDelayed(handler.obtainMessage(UPDATE_STATE_MSG), UPDATE_STATE_MS);
    }

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_STATE_MSG:
                    updateState();
                    break;
            }
        }
    };
}