package org.crpalmer.mashcontroller;

/**
 * HybridHeaterPowerPredictor
 * <p>
 * Power predictor that uses one predictor when it is close to the target and a
 * different predictor when trying to ramp up to the target temperature.
 */

public class HybridHeaterPowerPredictor implements HeaterPowerPredictor {
    private final HeaterPowerPredictor rampingPredictor;
    private final HeaterPowerPredictor maintainingPredictor;
    private double targetTemperature;
    private boolean maintaining;

    public HybridHeaterPowerPredictor(HeaterPowerPredictor rampingPredictor, HeaterPowerPredictor maintainingPredictor) {
        this.rampingPredictor = rampingPredictor;
        this.maintainingPredictor = maintainingPredictor;
    }

    @Override
    public synchronized void start(double currentTemperature, double targetTemperature) {
        maintaining = false;
        rampingPredictor.start(currentTemperature, targetTemperature);
        maintainingPredictor.start(currentTemperature, targetTemperature);
        this.targetTemperature = targetTemperature;
    }

    @Override
    public synchronized int predict(double currentTemperature) {
        if (currentTemperature >= targetTemperature) {
            maintaining = true;
        }
        if (maintaining) return maintainingPredictor.predict(currentTemperature);
        else return rampingPredictor.predict(currentTemperature);
    }
}
