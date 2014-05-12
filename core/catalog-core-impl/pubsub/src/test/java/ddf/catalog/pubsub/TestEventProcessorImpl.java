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
package ddf.catalog.pubsub;

import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.service.event.EventAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.impl.MetacardImpl;

public class TestEventProcessorImpl {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestEventProcessorImpl.class);

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void testNullMetacard() {
        EventAdmin eventAdmin = new MockEventAdmin();
        try {
            EventProcessorImpl.processEntry(null, "Operation", eventAdmin);
        } catch (Exception e) {
            fail();
        }

    }

    @Test
    public void testNullEventAdmin() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setContentTypeName("Nitf");
        metacard.setContentTypeVersion("2.0");
        metacard.setMetadata("<xml/>");
        try {
            EventProcessorImpl.processEntry(metacard, "Operation", null);
        } catch (Exception e) {
            LOGGER.error("Unexpected exception.", e);
            fail();
        }

    }

    @Test
    public void testNullOperation() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setContentTypeName("Nitf");
        metacard.setContentTypeVersion("2.0");
        metacard.setMetadata("<xml/>");
        EventAdmin eventAdmin = new MockEventAdmin();

        try {
            EventProcessorImpl.processEntry(metacard, null, eventAdmin);
        } catch (Exception e) {
            LOGGER.error("Unexpected exception.", e);
            fail();
        }

    }

}
