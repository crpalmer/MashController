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

    private AtomicInteger temperatureTimes100 = new AtomicInteger();
    private AtomicInteger heaterPower = new AtomicInteger();
    private AtomicBoolean pumpOn = new AtomicBoolean();

    BrewBossState(BrewBossConnection connection) {
        this.connection = connection;
    }

    public double getTemperature() {
        return temperatureTimes100.get() / 100.0;
    }

    public int getHeaterPower() {
        return heaterPower.get();
    }

    public boolean isPumpOn() {
        return pumpOn.get();
    }


    private void updateState(String line) {
        // TODO: figure out the format of this string
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
