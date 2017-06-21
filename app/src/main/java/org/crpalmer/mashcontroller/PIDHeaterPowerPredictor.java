package org.crpalmer.mashcontroller;

/**
 * PIDHeaterPowerPredictor
 * <p>
 * Use a PID algorithm to predict the heater power.
 */

public class PIDHeaterPowerPredictor implements HeaterPowerPredictor {
    private static final double P = 44;
    private static final double I = 0.045;
    private static final double D = 36;

    private double targetTemperature;
    private double lastTemperature;
    private boolean saturated;
    private double iTerm;

    @Override
    public synchronized void start(double targetTemperature) {
        this.targetTemperature = targetTemperature;
        iTerm = 0;
        lastTemperature = 0;
        saturated = false;
    }

    @Override
    public synchronized int predict(double currentTemperature) {
        long now = System.currentTimeMillis();
        double E = targetTemperature - currentTemperature;
        double deltaTemp = lastTemperature > 0 ? currentTemperature - lastTemperature : 0;
        if (!saturated) {
            // In order to prevent windup, only integrate if the process is not saturated
            iTerm = clamp(iTerm + I * E);
        }
        double p = P * E;
        double d = -(D * deltaTemp);
        double prediction = clamp(p + iTerm + d);
        saturated = prediction < 0 || prediction > 100;
        return (int) Math.round(prediction);
    }

    private double clamp(double x) {
        return Math.min(100, Math.max(x, 0));
    }
}
