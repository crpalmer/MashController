package org.crpalmer.mashcontroller;

/**
 * Created by crpalmer on 6/21/17.
 */

public interface BrewBossStateChangeListener {
    public void onHeaterChanged(boolean on, int power);
    public void onPumpChanged(boolean on);
    public void onTemperatureChanged(double temperature);
}
