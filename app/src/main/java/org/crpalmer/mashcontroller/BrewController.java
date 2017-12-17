package org.crpalmer.mashcontroller;

import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.LinkedList;

/**
 * BrewController
 * <p>
 * Interface to the controller.  It allows state inspection and updates to the state.
 */

public class BrewController {
    private static final String TAG = "BrewBoss";

    private static final int UPDATE_TEMPERATURE_MSG = 1;
    private static final int UPDATE_TEMPEATURE_MS = 900;
    private static final int START_STEP_MSG = 2;
    private static final int IS_STEP_READY_MSG = 3;
    private static final int IS_STEP_READY_MS = 1000;

    private final BrewBossConnection connection = new BrewBossConnection();
    private final BrewBossState state = new BrewBossState(connection);

    private HeaterPowerPredictor predictor;
    private double targetTemperature;

    private LinkedList<BrewStateChangeListener> listeners = new LinkedList<>();
    private ArrayList<BrewStep> brewSteps;
    private int currentStepNum = -1;
    private int nextStepNum = -1;
    private BrewStep currentStep;
    private CountDownTimer stepTimer;

    public BrewController() {
        HeaterPowerPredictor rampingPredictor = new RampingHeaterPowerPredictor();
        HeaterPowerPredictor maintainingPredictor = new MaintainingHeaterPowerPredictor(this);
        predictor = new HybridHeaterPowerPredictor(rampingPred`ictor, maintainingPredictor);
        looperThread.start();
    }

    public void loadBrewXml(File file) throws FileNotFoundException, XmlException {
        MashSession mashSession = new MashSession(file);
        brewSteps = new ArrayList<>();
        if (mashSession.getInfuseTemp() != null) {
            brewSteps.add(new MashInStep(mashSession.getInfuseAmount(), (int) Math.round(mashSession.getInfuseTemp())));
        }
        brewSteps.add(new TurnPumpOnStep());
        brewSteps.addAll(mashSession.getMashSteps());

        currentStepNum = -1;
        currentStep = null;
    }

    public int getHeaterPower() {
        return state.getHeaterPower();
    }

    public void setHeaterPower(int percent) throws BrewBossConnectionException {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("Temperature must be between 0 and 220");
        }
        connection.setHeaterPower(percent);
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
            predictor.start(state.getTemperature(), temperature);
            ensureTemperatureUpdateScheduled();
            synchronized (listeners) {
                for (BrewStateChangeListener l : listeners) {
                    l.onTargetTemperatureChanged(temperature);
                }
            }
        }
    }

    public boolean isAutomaticMode() {
        return state.isAutomaticMode();
    }

    public synchronized void setAutomaticMode(boolean on) {
        state.setAutomaticMode(on);
        looperThread.handler.removeMessages(UPDATE_TEMPERATURE_MSG);
        if (on) {
            scheduleUpdateTemperature();
            looperThread.handler.sendMessage(looperThread.handler.obtainMessage(START_STEP_MSG, (Integer) currentStepNum < 0 ? 0 : currentStepNum));
        } else {
            looperThread.handler.removeMessages(START_STEP_MSG);
            looperThread.handler.removeMessages(IS_STEP_READY_MSG);
        }
    }

    public boolean isConnected() {
        return connection.isConnected();
    }

    public boolean isHeaterOn() {
        return state.isHeaterOn();
    }

    public synchronized void setHeaterOn(boolean on) throws BrewBossConnectionException {
        connection.setHeaterPower(on ? state.getHeaterPower() : 0);
    }

    public boolean isPumpOn() {
        return state.isPumpOn();
    }

    public void setPumpOn(boolean isPumpOn) throws BrewBossConnectionException {
        connection.setPumpOn(isPumpOn);
    }

    public void alarm() throws BrewBossConnectionException {
        connection.alarm();
    }

    public void scheduleIsStepReady() {
        looperThread.handler.sendMessageDelayed(looperThread.handler.obtainMessage(IS_STEP_READY_MSG), IS_STEP_READY_MS);
    }

    public void addStateChangeListener(BrewStateChangeListener listener) {
        connection.addStateChangeListener(listener);
        state.addStateChangeListener(listener);
        synchronized (listeners) {
            listeners.add(listener);
            listener.onTargetTemperatureChanged(getTargetTemperature());
        }
    }

    private void scheduleUpdateTemperature() {
        looperThread.handler.sendMessageDelayed(looperThread.handler.obtainMessage(UPDATE_TEMPERATURE_MSG), UPDATE_TEMPEATURE_MS);
    }

    private void ensureTemperatureUpdateScheduled() {
        looperThread.handler.removeMessages(UPDATE_TEMPERATURE_MSG);
        scheduleUpdateTemperature();
    }

    private void updateTemperature() {
        if (state.isAutomaticMode() && targetTemperature > 0) {
            int power = predictor.predict(state.getTemperature());
            Log.v(TAG, "targetTemperature " + targetTemperature + " power " + power + " currentTemperate " + state.getTemperature());
            if (! state.isHeaterOn() || power != state.getHeaterPower()) {
                try {
                    connection.setHeaterPower(power);
                } catch (BrewBossConnectionException e) {
                    // TODO report this somehow
                }
            }
            scheduleUpdateTemperature();
        }
    }

    private void startStep(int stepNum) {
        nextStepNum = stepNum;
        finishStep();
    }

    private void finishStep() {
        try {
            if (currentStep == null || currentStep.finishStep(this)) {
                stepFinished();
            }
        } catch (BrewBossConnectionException e) {
        }
    }

    public void stepFinished() {
        try {
            currentStepNum = nextStepNum;
            if (currentStepNum >= brewSteps.size()) {
                setAutomaticMode(false);
                try {
                    alarm();
                } catch (BrewBossConnectionException e) {
                    App.toastException(e);
                }
            } else {
                currentStep = brewSteps.get(currentStepNum);
                stepTimer = new CountDownTimer(currentStep.getSeconds() * 1000, 1000) {
                    private void notifyTimeLeft(int secondsLeft) {
                        synchronized (listeners) {
                            for (BrewStateChangeListener listener : listeners) {
                                listener.onStepTick(secondsLeft);
                            }
                        }
                    }

                    @Override
                    public void onTick(long l) {
                        notifyTimeLeft((int) (l / 1000));
                    }

                    @Override
                    public void onFinish() {
                        notifyTimeLeft(0);
                        finishStep();
                    }
                };
                synchronized (listeners) {
                    for (BrewStateChangeListener listener : listeners) {
                        listener.onStepStart(currentStepNum, currentStep.getDescription());
                    }
                }
                if (currentStep.startStep(this)) {
                    stepStarted();
                }
            }
        } catch (BrewBossConnectionException e) {
            App.toastException(e);
            setAutomaticMode(false);
        }
    }

    public void stepStarted() {
        scheduleIsStepReady();
    }

    private LooperThread looperThread = new LooperThread();

    private class LooperThread extends Thread {
        public Handler handler;

        public void run() {
            Looper.prepare();

            handler = new Handler() {
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case UPDATE_TEMPERATURE_MSG:
                            updateTemperature();
                            break;
                        case START_STEP_MSG:
                            startStep((int) msg.obj);
                            break;
                        case IS_STEP_READY_MSG:
                            if (currentStep != null) {
                                if (currentStep.isStepReady(getTemperature())) {
                                    stepTimer.start();
                                    nextStepNum = currentStepNum + 1;
                                } else {
                                    scheduleIsStepReady();
                                }
                            }
                            break;

                    }

                }
            };

            Looper.loop();
        }
    }

}