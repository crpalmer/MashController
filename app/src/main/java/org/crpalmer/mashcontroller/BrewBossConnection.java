package org.crpalmer.mashcontroller;

import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.NumberFormat;

/**
 * Created by crpalmer on 6/17/17.
 */

public class BrewBossConnection {
    private static final String TAG = "BrewBossConnection";

    private static final String HEARTBEAT_CMD = "01";
    private static final String HEATER_POWER_CMD = "02";
    private static final String PUMP_ENABLED_CMD = "03";
    private static final String TRIGGER_ALARM_CMD = "04";
    private static final String TEMPERATURE_UNIT_CMD = "06";

    private static final String HOST = "192.168.11.254";
    private static final int PORT = 5000;

    private final String host;
    private final int port;

    private Socket socket;
    private BufferedOutputStream out;
    private BufferedReader in;
    private long connectionCount;

    BrewBossConnection() {
        this(HOST, PORT);
    }

    BrewBossConnection(String host_, int port_) {
        host = host_;
        port = port_;
    }

    public synchronized  boolean isConnected() {
        return socket != null;
    }

    public String readLine() throws BrewBossConnectionException {
        BufferedReader currentIn = in;
        long currentConnectionCount;
        synchronized (this) {
            ensureConnectedLocked();
            currentIn = in;
            currentConnectionCount = connectionCount;
        }
        try {
            return in.readLine();
        } catch (IOException e) {
            synchronized (this) {
                if (connectionCount == currentConnectionCount) {
                    resetConnectionLocked();
                }
            }
            throw new BrewBossConnectionException(TAG, "Failed to write data to [" + HOST + ":" + PORT, e);
        }
    }

    public void heartBeat() throws BrewBossConnectionException {
        sendCommand(HEARTBEAT_CMD);
    }

    public void alarm() throws BrewBossConnectionException {
        sendCommand(TRIGGER_ALARM_CMD);
    }

    public void setHeaterPower(int power) throws BrewBossConnectionException {
        if (power < 0 || power > 100) {
            throw new IllegalArgumentException("Power " + power + " is not in range [0,100]");
        }
        NumberFormat nf = new DecimalFormat("000");
        sendCommand(HEATER_POWER_CMD + nf.format(power));
    }

    public void setPumpEnabled(boolean isEnabled) throws BrewBossConnectionException {
        sendCommand(PUMP_ENABLED_CMD + (isEnabled ? "1" : "0"));
    }

    public void setTemperatureUnit(boolean isF) throws BrewBossConnectionException {
        sendCommand(TEMPERATURE_UNIT_CMD + (isF ? "F" : "C"));
    }

    private void ensureConnectedLocked() throws BrewBossConnectionException {
        try {
            if (socket == null) {
                socket = new Socket(host, port);
                out = new BufferedOutputStream(socket.getOutputStream());
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                connectionCount++;
            }
        } catch (UnknownHostException e) {
            throw new BrewBossConnectionException(TAG, "Couldn't connect to [" + host + ":" + port, e);
        } catch (IOException e) {
            throw new BrewBossConnectionException(TAG, "Couldn't connect to [" + host + ":" + port, e);
        }
    }

    private void resetConnectionLocked() {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ee) {
            }
            socket = null;
        }
    }

    private synchronized void sendCommand(String cmd) throws BrewBossConnectionException {
        ensureConnectedLocked();
        try {
            out.write(cmd.getBytes());
            out.write(13);
            out.write(10);
            out.flush();
        } catch (IOException e) {
            resetConnectionLocked();
            throw new BrewBossConnectionException(TAG, "Failed to write data to [" + HOST + ":" + PORT, e);
        }
    }

}