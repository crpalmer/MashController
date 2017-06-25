package org.crpalmer.mashcontroller;

/**
 * Interface to the different brew steps
 */

public interface BrewStep {
    boolean startStep(BrewController brewController) throws BrewBossConnectionException;
    boolean finishStep(BrewController brewController) throws BrewBossConnectionException;
    boolean isStepReady(double currentTemperature);
    int getSeconds();
}
