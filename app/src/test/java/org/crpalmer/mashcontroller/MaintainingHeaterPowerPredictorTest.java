package org.crpalmer.mashcontroller;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class MaintainingHeaterPowerPredictorTest {
    private MaintainingHeaterPowerPredictor p;
    private long startMs;

    @Mock
    private BrewController brewController;

    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();

    @Before
    public void before() {
        p = new MaintainingHeaterPowerPredictor(brewController);
        startMs = System.currentTimeMillis();
    }

    @Test
    public void predictsZeroWithNoData() {
        p.start(10, 20);
        assertEquals(0, p.predict(50));
    }

    @Test
    public void tracksConstantChange() {
        p.start(70, 150);
        p.onHeaterChanged(true, 100);
        p.onTemperatureChanged(71, startMs + 10000);
        p.onTemperatureChanged(72, startMs + 20000);
        assertEquals(10000, p.getMsPerDegree(), 1);
    }

    @Test
    public void accountsForHeaterPowerInEstimate() {
        p.start(70, 150);
        p.onHeaterChanged(true, 50);
        p.onTemperatureChanged(71, startMs + 20000);
        p.onTemperatureChanged(72, startMs + 40000);
        assertEquals(10000, p.getMsPerDegree(), 1);
    }

    @Test
    public void ignoresFirstDegreeChange() {
        p.start(70, 150);
        p.onHeaterChanged(true, 100);
        p.onTemperatureChanged(71, startMs + 10000);
        assertEquals(0, p.getMsPerDegree(), 1);
    }

    @Test
    public void ignoresFirstDegreeChangeAfterHeaterRestarts() {
        p.start(70, 150);
        p.onHeaterChanged(true, 100);
        p.onTemperatureChanged(71, startMs + 10000);
        p.onTemperatureChanged(72, startMs + 20000);
        p.onHeaterChanged(false, 100);
        p.onHeaterChanged(true, 100);
        p.onTemperatureChanged(73, startMs + 20001);
        assertEquals(10000, p.getMsPerDegree(), 1);
    }

    @Test
    public void decaysOldEstimates() {
        p.start(70, 150);
        p.onHeaterChanged(true, 100);
        // Predicting 1 degree a second ramp rate
        for (int i = 1; i <= 10; i++) {
            p.onTemperatureChanged(70 + i, startMs + 1000 * i);
        }
        // Should now be close to a 1 degree every 10 seconds ramp rate
        for (int i = 1; i <= 9; i++) {
            p.onTemperatureChanged(80 + i, startMs + 10000 + i * 10000);
        }
        assertEquals(10000, p.getMsPerDegree(), 1000);
    }

    @Test
    public void oneWonkyEstimateDoesntChangeEverything() {
        p.start(70, 150);
        p.onHeaterChanged(true, 100);
        // Predicting 1 degree a second ramp rate
        for (int i = 0; i < 20; i++) {
            p.onTemperatureChanged(70 + i, startMs + 1000 * i);
        }
        assertEquals(1000, p.getMsPerDegree(), 1);
        p.onTemperatureChanged(90, startMs + 20000 + 10000);
        assertEquals(1000, p.getMsPerDegree(), 1000);
    }

    @Test
    public void predictsRampingPower() {
        p.start(140, 152);
        p.onHeaterChanged(true, 100);
        for (int i = 1; i <= 10; i++) {
            p.onTemperatureChanged(140+i, startMs+30000 * i);
        }
        assertEquals(50, p.predict(151));
    }

    @Test
    public void predictsRampingPowerAccountingForHeaterPower() {
        p.start(140, 152);
        p.onHeaterChanged(true, 50);
        for (int i = 1; i <= 10; i++) {
            p.onTemperatureChanged(140+i, startMs+60000 * i);
        }
        assertEquals(50, p.predict(151));
    }

    @Test
    public void predictsMaintainingPower() {
        p.start(140, 152);
        p.onHeaterChanged(true, 100);
        for (int i = 1; i <= 10; i++) {
            p.onTemperatureChanged(140+i, startMs+30000 * i);
        }
        assertEquals(50/15, p.predict(152));
    }

    @Test
    public void predictsSlightOvershootPower() {
        p.start(140, 152);
        p.onHeaterChanged(true, 100);
        for (int i = 1; i <= 10; i++) {
            p.onTemperatureChanged(140+i, startMs+30000 * i);
        }
        assertEquals(Math.round(50/60.0), p.predict(153));
    }

    @Test
    public void noHeaterPowerWhenOvershotTooMuch() {
        p.start(140, 152);
        p.onHeaterChanged(true, 100);
        for (int i = 1; i <= 10; i++) {
            p.onTemperatureChanged(140+i, startMs+30000 * i);
        }
        assertEquals(0, p.predict(154));
    }
}