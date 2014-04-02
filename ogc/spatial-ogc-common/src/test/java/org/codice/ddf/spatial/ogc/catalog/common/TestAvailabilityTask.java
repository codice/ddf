/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package org.codice.ddf.spatial.ogc.catalog.common;



import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.atMost;

import org.junit.Before;
import org.junit.Test;

public class TestAvailabilityTask {

    private AvailabilityCommand mockCommand = mock(AvailabilityCommand.class);

    private AvailabilityTask task = new AvailabilityTask(1000, mockCommand, "Task");

    @Before
    public void setUp() {
        when(mockCommand.isAvailable()).thenReturn(true);
    }

    @Test
    public void testRunFirstTime() {
        task.run();
        verify(mockCommand, atMost(1)).isAvailable();
    }

    @Test
    public void testRunIntervalDidNotElapse() {
        task.updateLastAvailableTimestamp(System.currentTimeMillis());
        task.run();
        verify(mockCommand, never()).isAvailable();
    }

    @Test
    public void testRunIntervalElapsed() {
        task.updateLastAvailableTimestamp(System.currentTimeMillis() + 2000);
        task.run();
        verify(mockCommand, atMost(1)).isAvailable();
    }

}
