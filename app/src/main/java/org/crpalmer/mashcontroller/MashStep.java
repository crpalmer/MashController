package org.crpalmer.mashcontroller;

/**
 * Created by crpalmer on 6/23/17.
 */

public class MashStep {
    private final double temperature;
    private final int seconds;
    private final String description;

    public MashStep(double temperature, int seconds, String description) {
        this.temperature = temperature;
        this.seconds = seconds;
        this.description = description;
    }

    public double getTemperature() {
        return temperature;
    }

    public int getSeconds() {
        return seconds;
    }

    public String getDescription() {
        return description;
    }
}
