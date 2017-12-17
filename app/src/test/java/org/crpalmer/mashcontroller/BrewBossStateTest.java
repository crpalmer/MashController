package org.crpalmer.mashcontroller;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.mock;

/**
 * Created by crpalmer on 6/27/17.
 */

public class BrewBossStateTest {
    @Mock
    private BrewBossConnection connection;
    private BrewBossState state;

    @Before
    public void before() {
        connection = mock(BrewBossConnection.class);
        state = new BrewBossState(connection);
    }

    @Test
    public void stateChangeListenersCalledWhenStateChanges() {
//        when(connection.readLine()).thenReturn(new String("*,10,0,"))
    }
}
