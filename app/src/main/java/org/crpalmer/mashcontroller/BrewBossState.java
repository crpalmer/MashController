package org.crpalmer.mashcontroller;

import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by crpalmer on 6/17/17.
 */

public class BrewBossState {
    private static final String TAG = "BrewBossState";

    private final BrewBossConnection connection;

    private AtomicBoolean automaticMode = new AtomicBoolean(true);
    private AtomicInteger temperatureTimes100 = new AtomicInteger();
    private AtomicBoolean heaterOn = new AtomicBoolean();
    private AtomicInteger heaterPower = new AtomicInteger();
    private AtomicBoolean pumpOn = new AtomicBoolean();

    BrewBossState(BrewBossConnection connection) {
        this.connection = connection;
    }

    public int getHeaterPower() {
        return heaterPower.get();
    }
    public double getTemperature() {
        return temperatureTimes100.get() / 100.0;
    }
    public boolean isAutomaticMode() { return automaticMode.get(); }
    public boolean isHeaterOn() { return heaterOn.get(); }
    public boolean isPumpOn() {
        return pumpOn.get();
    }

    public void setHeaterOn(boolean on) {
        heaterOn.set(on);
    }

    public void setAutomaticMode(boolean on) {
        automaticMode.set(on);
    }

    private void updateState(String line) {
        // TODO: figure out the format of this string

        // NOTE: if heaterOn == true && heaterPower != reported heater power then resend the heater power
        //       if heaterOn == false && reported heater power > 0 then set heater power to 9
        // (aka: treat our state as authority)  Is this right? Is this possible?
    }

    private void pause() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Log.e(TAG, "Sleep interrupted: " + e.getLocalizedMessage());
        }
    }

    private final Thread heartbeatThread = new Thread() {
        @Override
        public void run() {
            while (true) {
                try {
                    connection.heartBeat();
                } catch (BrewBossConnectionException e) {
                }
                pause();
            }
        }
    };

    private final Thread readerThread = new Thread() {
        @Override
        public void run() {
            while (true) {
                try {
                    updateState(connection.readLine());
                } catch (BrewBossConnectionException e) {
                    pause();
                }
            }
        }

    };
}