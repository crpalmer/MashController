package org.crpalmer.mashcontroller;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Created by crpalmer on 6/27/17.
 */

public class HybridHeaterPowerPredictorTest {
    @Mock
    private HeaterPowerPredictor ramp;
    @Mock
    private HeaterPowerPredictor maintain;
    private HeaterPowerPredictor hybrid;

    private static double LOW_TEMP = 50;
    private static double HIGH_TEMP = 75;

    @Before
    public void before() {
        ramp = mock(HeaterPowerPredictor.class);
        maintain = mock(HeaterPowerPredictor.class);
        hybrid = new HybridHeaterPowerPredictor(ramp, maintain);
    }

    @Test
    public void callsBothStartMethods() {
        hybrid.start(LOW_TEMP, HIGH_TEMP);
        verify(ramp).start(LOW_TEMP, HIGH_TEMP);
        verify(maintain).start(LOW_TEMP, HIGH_TEMP);
    }

    @Test
    public void wayOffCallsRampingPredictor() {
        hybrid.start(LOW_TEMP, HIGH_TEMP);
        hybrid.predict(LOW_TEMP);
        verify(ramp).predict(LOW_TEMP);
        verify(maintain, never()).predict(any(Double.class));
    }

    @Test
    public void justBelowTargetCallsRampingPredictor() {
        hybrid.start(LOW_TEMP, HIGH_TEMP);
        hybrid.predict(HIGH_TEMP -1);
        verify(ramp).predict(HIGH_TEMP -1);
        verify(maintain, never()).predict(any(Double.class));
    }

    @Test
    public void atTargetCallsMaintainPredictor() {
        hybrid.start(LOW_TEMP, HIGH_TEMP);
        hybrid.predict(HIGH_TEMP );
        verify(ramp, never()).predict(any(Double.class));
        verify(maintain).predict(HIGH_TEMP);
    }

    @Test
    public void hittingTargetAndThenDroppingCallsMaintainPredictor() {
        hybrid.start(LOW_TEMP, HIGH_TEMP);
        hybrid.predict(HIGH_TEMP);
        hybrid.predict(LOW_TEMP);
        verify(ramp, never()).predict(any(Double.class));
        verify(maintain, times(2)).predict(any(Double.class));
    }
}
