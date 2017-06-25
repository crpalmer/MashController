package org.crpalmer.mashcontroller;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.LinkedList;
import java.util.List;

/**
 * BrewBossConnection
 * <p>
 * Communication channel to/from the brew boss controller
 */

public class BrewBossConnection {
    private static final String TAG = "BrewBossConnection";

    private static final int SEND_COMMAND_MSG = 1;

    private static final String HEARTBEAT_CMD = "01";
    private static final String HEATER_POWER_CMD = "02";
    private static final String PUMP_ON_CMD = "03";
    private static final String TRIGGER_ALARM_CMD = "04";
    private static final String TEMPERATURE_UNIT_CMD = "06";

    private static final String HOST = "192.168.11.254";
    private static final int PORT = 8080;

    private final String host;
    private final int port;

    private Socket socket;
    private BufferedOutputStream out;
    private BufferedReader in;
    private long connectionCount;

    private final List<BrewStateChangeListener> listeners = new LinkedList<>();

    public BrewBossConnection() {
        this(HOST, PORT);
    }

    public BrewBossConnection(String host, int port) {
        this.host = host;
        this.port = port;
        looperThread.start();
    }

    public void addStateChangeListener(BrewStateChangeListener l) {
        synchronized (listeners) {
            listeners.add(l);
            l.onConnectionStateChanged(isConnected());
        }
    }

    private void notifyListeners(boolean isConnected) {
        synchronized (listeners) {
            for (BrewStateChangeListener l : listeners) {
                l.onConnectionStateChanged(isConnected);
            }
        }
    }

    public synchronized boolean isConnected() {
        return socket != null;
    }

    public String readLine() throws BrewBossConnectionException {
        BufferedReader currentIn;
        long currentConnectionCount;
        synchronized (this) {
            ensureConnectedLocked();
            currentIn = in;
            currentConnectionCount = connectionCount;
        }
        try {
            return currentIn.readLine();
        } catch (IOException e) {
            synchronized (this) {
                if (connectionCount == currentConnectionCount) {
                    resetConnectionLocked();
                }
            }
            throw new BrewBossConnectionException(TAG, "Failed to write data to [" + HOST + ":" + PORT + "]", e);
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

    public void setPumpOn(boolean isPumpOn) throws BrewBossConnectionException {
        sendCommand(PUMP_ON_CMD + (isPumpOn ? "1" : "0"));
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
                notifyListeners(true);
            }
        } catch (IOException e) {
            throw new BrewBossConnectionException(TAG, "Couldn't connect to [" + host + ":" + port + "]", e);
        }
    }

    private void resetConnectionLocked() {
        if (socket != null) {
            notifyListeners(false);
            try {
                socket.close();
            } catch (IOException ignored) {
            }
            socket = null;
        }
    }

    private void sendCommand(String command) {
        looperThread.sendCommand(command);
    }

    private LooperThread looperThread = new LooperThread();

    private class LooperThread extends Thread {
        public Handler handler;

        public void sendCommand(String command) {
            while (handler == null) {
            }
            handler.sendMessage(handler.obtainMessage(SEND_COMMAND_MSG, command));
        }

        public void run() {
            Looper.prepare();

            handler = new Handler() {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case SEND_COMMAND_MSG:
                            try {
                                String cmd = (String) msg.obj;

                                ensureConnectedLocked();
                                out.write(cmd.getBytes());
                                out.write(13);
                                out.write(10);
                                out.flush();
                            } catch (IOException e) {
                                resetConnectionLocked();
                                e.printStackTrace();
                            } catch (BrewBossConnectionException e) {
                                e.printStackTrace();
                            }
                            break;
                    }
                }
            };

            Looper.loop();
        }
    }

}