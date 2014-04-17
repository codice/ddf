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
package org.codice.ddf.admin.application.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.codice.ddf.admin.application.service.Application;
import org.junit.Test;

/**
 * Tests out the ApplicationNodeImpl code to make sure it is following the
 * interface specification.
 * 
 */
public class ApplicationNodeImplTest {

    private static final String APP_NAME = "test-app";

    private static final String APP_VERSION = "1.2.3";

    private static final String APP_DESCRIPTION = "Test Description";

    private static final String APP2_NAME = "test2-app";

    private static final String APP2_VERSION = "2.3.4";

    private static final String APP2_DESCRIPTION = "Test Description 2";

    /**
     * Tests the 'getters' to make sure that they return the correct values
     * after initialization and after setting the child and parent.
     */
    @Test
    public void testGetters() {
        Application testApp = mock(Application.class);
        when(testApp.getName()).thenReturn(APP_NAME);
        when(testApp.getVersion()).thenReturn(APP_VERSION);
        when(testApp.getDescription()).thenReturn(APP_DESCRIPTION);

        ApplicationNodeImpl testNode = new ApplicationNodeImpl(testApp);
        // initialization test
        assertTrue(testNode.getChildren().isEmpty());
        assertNull(testNode.getParent());
        assertEquals(testApp, testNode.getApplication());

        // test after setting child and parent
        Application testChildApp = mock(Application.class);
        when(testChildApp.getName()).thenReturn(APP2_NAME);
        when(testChildApp.getVersion()).thenReturn(APP2_VERSION);
        when(testChildApp.getDescription()).thenReturn(APP2_DESCRIPTION);
        ApplicationNodeImpl testChildNode = new ApplicationNodeImpl(testChildApp);
        testChildNode.setParent(testNode);
        testNode.getChildren().add(testChildNode);
        assertEquals(1, testNode.getChildren().size());
        assertEquals(testChildNode, testNode.getChildren().iterator().next());

        assertNotNull(testChildNode.getParent());
        assertEquals(testNode, testChildNode.getParent());

    }

}
