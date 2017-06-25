package org.crpalmer.mashcontroller;

/**
 * Interface to the different brew steps
 */

public interface BrewStep {
    boolean startStep(BrewBoss brewBoss) throws BrewBossConnectionException;
    boolean finishStep(BrewBoss brewBoss) throws BrewBossConnectionException;
    boolean isStepReady(double currentTemperature);
    int getSeconds();
}
