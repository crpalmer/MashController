package org.crpalmer.mashcontroller;

import java.text.NumberFormat;

/**
 * Created by crpalmer on 6/23/17.
 */

public class MashInStep implements BrewStep {
    private final Double volume;
    private final int temperature;

    public MashInStep(Double volume, int temperature) {
        this.volume = volume;
        this.temperature = temperature;
    }

    @Override
    public boolean startStep(final BrewController brewController) throws BrewBossConnectionException {
        brewController.alarm();
        App.confirm(getWaterVolumeText(), new Continuation() {
            @Override
            public void go() {
                brewController.setTargetTemperature(temperature);
                brewController.stepStarted();
            }
        });
        return false;
    }

    @Override
    public boolean finishStep(final BrewController brewController) throws BrewBossConnectionException {
        brewController.setTargetTemperature(0);
        brewController.alarm();
        App.confirm("Add the grains to the pot", new Continuation() {
            @Override
            public void go() {
                brewController.stepFinished();
            }
        });
        return false;
    }

    @Override
    public boolean isStepReady(double currentTemperature) {
        return false;
    }

    @Override
    public int getSeconds() {
        return 0;
    }

    private String getWaterVolumeText() {
        if (volume != null) {
            double mm = volume < 2 ? 62 / volume : 66 + (volume - 2) * 32;
            return String.format("Add %.2f gallons (%.0f mm) of water to pot.", volume, mm);
        } else {
            return "Add water to the pot";
        }
    }

    public String toString() {
        return "MashIn[volume=" + volume + ",temperature=" + temperature + "]";
    }
}
