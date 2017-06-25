package org.crpalmer.mashcontroller;

/**
 * Created by crpalmer on 6/23/17.
 */

public class TurnPumpOnStep implements BrewStep {
    @Override
    public boolean startStep(final BrewBoss brewBoss) throws BrewBossConnectionException {
        brewBoss.alarm();
        App.confirm("Connect the pump and open the valve", new Continuation() {
            public void go() {
                try {
                    brewBoss.setPumpOn(true);
                    brewBoss.stepStarted();
                } catch (BrewBossConnectionException e) {
                    App.toastException(e);
                    brewBoss.setAutomaticMode(false);
                }
            }
        });
        return false;
    }

    @Override
    public boolean finishStep(BrewBoss brewBoss) throws BrewBossConnectionException {
        return true;
    }

    @Override
    public boolean isStepReady(double currentTemperature) {
        return true;
    }

    @Override
    public int getSeconds() {
        return 0;
    }

    public String toString() {
        return "TurnPumpOn";
    }
}
