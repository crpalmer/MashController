package org.crpalmer.mashcontroller;

/**
 * Created by crpalmer on 6/17/17.
 */

public class HybridHeaterPowerPredictor implements HeaterPowerPredictor {
    private boolean maintaining;
    private double targetTemperature;
    private final HeaterPowerPredictor rampingPredictor = new VendorHeaterPowerPredictor();
    private final HeaterPowerPredictor maintainingPredictor = new PIDHeaterPowerPredictor();

    @Override
    public synchronized void start(double targetTemperature) {
        this.targetTemperature = targetTemperature;
        maintaining = false;
        rampingPredictor.start(targetTemperature);
        maintainingPredictor.start(targetTemperature);
    }

    @Override
    public synchronized int predict(double currentTemperature) {
        if (maintaining) return maintainingPredictor.predict(currentTemperature);
        else return rampingPredictor.predict(currentTemperature);
    }
}
