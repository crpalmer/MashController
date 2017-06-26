package org.crpalmer.mashcontroller;

/**
 * Created by crpalmer on 6/23/17.
 */

public class TurnPumpOnStep implements BrewStep {
    @Override
    public boolean startStep(final BrewController brewController) throws BrewBossConnectionException {
        brewController.alarm();
        App.confirm("Connect the pump and open the valve", new Continuation() {
            public void go() {
                try {
                    brewController.setPumpOn(true);
                    brewController.stepStarted();
                } catch (BrewBossConnectionException e) {
                    App.toastException(e);
                    brewController.setAutomaticMode(false);
                }
            }
        });
        return false;
    }

    @Override
    public boolean finishStep(BrewController brewController) throws BrewBossConnectionException {
        return true;
    }

    @Override
    public boolean isStepReady(double currentTemperature) {
        return true;
    }

    @Override
    public String getDescription() {
        // TODO: Make this a string resource
        return "Turn pump on";
    }

    @Override
    public int getSeconds() {
        return 0;
    }

    public String toString() {
        return "TurnPumpOn";
    }
}
