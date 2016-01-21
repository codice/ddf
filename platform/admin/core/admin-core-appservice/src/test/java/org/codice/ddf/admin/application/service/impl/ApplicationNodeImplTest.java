/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.admin.application.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.karaf.features.Repository;
import org.apache.karaf.features.internal.service.RepositoryImpl;
import org.codice.ddf.admin.application.service.Application;
import org.codice.ddf.admin.application.service.ApplicationNode;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final String FEATURES_FILE_NAME = "test-features-with-main-feature.xml";

    private Logger logger = LoggerFactory.getLogger(ApplicationNodeImplTest.class);

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

    /**
     * Tests the constructor to make sure it does not accept null parameters
     */
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorNullParameters() {
        ApplicationNodeImpl testNode = new ApplicationNodeImpl(null, null);
    }

    /**
     * Tests the getStatus() method to make sure it returns the correct
     * value(s) after initialization, and after a new status has been
     * set via setStatus()
     */
    @Test
    public void testStatus() {
        Application testApp = mock(Application.class);
        when(testApp.getName()).thenReturn(APP_NAME);
        when(testApp.getVersion()).thenReturn(APP_VERSION);
        when(testApp.getDescription()).thenReturn(APP_DESCRIPTION);

        ApplicationStatusImpl testStatus = mock(ApplicationStatusImpl.class);
        ApplicationStatusImpl testStatus2 = mock(ApplicationStatusImpl.class);

        ApplicationNodeImpl testNode = new ApplicationNodeImpl(testApp, testStatus);
        assertEquals(testStatus, testNode.getStatus());

        testNode.setStatus(testStatus2);
        assertEquals(testStatus2, testNode.getStatus());

    }

    /**
     * Tests the {@link ApplicationNodeImpl#ApplicationNodeImpl(Application)} constructor
     * for the case where the application exists
     */
    @Test
    public void testApplicationNodeImplConstructorAppParam() {
        Application testApp = mock(Application.class);

        ApplicationNode testNode = new ApplicationNodeImpl(testApp);

        assertEquals(testApp, testNode.getApplication());
    }

    /**
     * Tests the {@link ApplicationNodeImpl#ApplicationNodeImpl(Application)} constructor
     * for the case where the application is null
     */
    @Test
    public void testApplicationNodeImplConstructorAppParamNull() {
        Application testApp = null;
        try {
            ApplicationNode testNode = new ApplicationNodeImpl(testApp);
        } catch (Exception e) {
            assertEquals(e.getMessage(), "Input application cannot be null.");
        }

    }

    /**
     * Tests the {@link ApplicationNodeImpl#hashCode()} method
     */
    @Test
    public void testHashCode() {
        try {
            Repository testRepo = new RepositoryImpl(ApplicationNodeImpl.class.getClassLoader()
                    .getResource(FEATURES_FILE_NAME).toURI());
            Application testApp = new ApplicationImpl(testRepo);

            ApplicationNode testNode = new ApplicationNodeImpl(testApp);

            assertEquals(testApp.hashCode(), testNode.hashCode());
        } catch (Exception e) {
            logger.info("Exception: ", e);
            fail();
        }
    }

    /**
     * Tests the {@link ApplicationNodeImpl#equals(Object)} method for the case where the
     * parameter is null, the parameter is the same object, the parameter is not an
     * ApplicationNodeImpl object, and where the parameter is a different ApplicationNodeImpl
     * which has the same application
     */
    @Test
    public void testEqualsObjParam() {
        Application testApp = mock(Application.class);
        ApplicationNode testNode = new ApplicationNodeImpl(testApp);
        ApplicationNode testNode2 = new ApplicationNodeImpl(testApp);

        //        Case 1:
        assertFalse(testNode.equals(null));

        //        Case 2:
        assertTrue(testNode.equals(testNode));

        //        Case 3:
        assertFalse(testNode.equals(testApp));

        //        Case 4:
        assertTrue(testNode.equals(testNode2));
    }

    /**
     * Tests the {@link ApplicationNodeImpl#compareTo(ApplicationNode)} method for the case
     * where the parameter node is null
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCompareToIAE() {
        Application testApp = mock(Application.class);
        ApplicationNodeImpl testNode = new ApplicationNodeImpl(testApp);

        testNode.compareTo(null);
    }

    /**
     * Tests the {@link ApplicationNodeImpl#compareTo(ApplicationNode)} method for the case
     * where the parameter node's application does not have the same name
     */
    @Test
    public void testCompareTo() {
        Application testApp = mock(Application.class);
        when(testApp.getName()).thenReturn(APP_NAME);
        Application testApp2 = mock(Application.class);
        when(testApp2.getName()).thenReturn(APP2_NAME);

        ApplicationNodeImpl testNode = new ApplicationNodeImpl(testApp);
        ApplicationNodeImpl testNode2 = new ApplicationNodeImpl(testApp2);

        assertEquals(-5, testNode.compareTo(testNode2));
    }
}
