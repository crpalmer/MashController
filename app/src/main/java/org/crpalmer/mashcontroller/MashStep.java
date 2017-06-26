package org.crpalmer.mashcontroller;

/**
 * Created by crpalmer on 6/23/17.
 */

public class MashStep  implements BrewStep {
    private final double temperature;
    private final int seconds;
    private final String description;

    public MashStep(double temperature, int seconds, String description) {
        this.temperature = temperature;
        this.seconds = seconds;
        this.description = description;
    }

    @Override
    public boolean startStep(BrewController brewController) {
        brewController.setTargetTemperature(temperature);
        return true;
    }

    @Override
    public boolean finishStep(BrewController brewController) throws BrewBossConnectionException {
        return true;
    }

    @Override
    public boolean isStepReady(double currentTemperature) {
        return currentTemperature >= temperature;
    }

    @Override
    public String getDescription() {
        // TODO: Make this a string resource
        return "Mash at " + temperature;
    }

    @Override
    public int getSeconds() {
        return seconds;
    }

    public String toString() {
        return "MashStep[temperature=" + temperature + ",seconds=" + seconds + ",description=" + description + "]";
    }
}
