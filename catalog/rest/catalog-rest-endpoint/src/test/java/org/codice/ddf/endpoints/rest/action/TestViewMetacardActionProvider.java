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
package org.codice.ddf.endpoints.rest.action;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.apache.log4j.BasicConfigurator;
import org.junit.Ignore;
import org.junit.Test;

import ddf.action.Action;
import ddf.catalog.data.MetacardImpl;

public class TestViewMetacardActionProvider extends AbstractActionProviderTest {

    static {
        BasicConfigurator.configure();
    }

    @Test
    public void testMetacardNull() {
        assertEquals(null, new ViewMetacardActionProvider(ACTION_PROVIDER_ID).getAction(null));
    }

    @Test
    public void testUriSyntaxException() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);

        actionProvider.configurationUpdateCallback(createMap(SAMPLE_PROTOCOL, "23^&*#", SAMPLE_PORT,
                SAMPLE_SERVICES_ROOT, SAMPLE_SOURCE_NAME));

        assertNull("A bad url should have been caught and a null action returned.",
                actionProvider.getAction(metacard));

    }

    @Test
    public void testMetacardIdUrlEncoded_Space() {

        // given
        MetacardImpl metacard = new MetacardImpl();

        metacard.setId("abd ef");

        AbstractMetacardActionProvider actionProvider = configureActionProvider(new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID));

        // when
        String url = actionProvider.getAction(metacard).getUrl().toString();

        // then
        assertThat(url, is(expectedDefaultAddressWith("abd+ef")));

    }

    @Test
    public void testMetacardIdUrlEncoded_Ampersand() {

        // given
        MetacardImpl metacard = new MetacardImpl();

        metacard.setId("abd&ef");

        AbstractMetacardActionProvider actionProvider = configureActionProvider(new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID));

        // when
        String url = actionProvider.getAction(metacard).getUrl().toString();

        // then
        assertThat(url, is(expectedDefaultAddressWith("abd%26ef")));

    }

    @Test
    public void testMetacardIdNull() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(null);

        AbstractMetacardActionProvider actionProvider = configureActionProvider(new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID));

        assertNull("An action should not have been created when no id is provided.",
                actionProvider.getAction(metacard));

    }

    @Test
    public void testNoConfigSettings() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);

        assertNull(actionProvider.getAction(metacard));

    }

    @Test
    public void testIpNull() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);

        actionProvider.configurationUpdateCallback(createMap(SAMPLE_PROTOCOL, null, SAMPLE_PORT,
                SAMPLE_SERVICES_ROOT, SAMPLE_SOURCE_NAME));

        assertNull("An action should not have been created when ip is null.",
                actionProvider.getAction(metacard));

    }

    @Test
    public void testIpUnknown() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);

        actionProvider.configurationUpdateCallback(createMap(SAMPLE_PROTOCOL, "0.0.0.0", SAMPLE_PORT,
                SAMPLE_SERVICES_ROOT, SAMPLE_SOURCE_NAME));

        assertNull("An action should not have been created when ip is unknown (0.0.0.0).",
                actionProvider.getAction(metacard));

    }

    @Test
    public void testPortNull() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);

        actionProvider.configurationUpdateCallback(createMap(SAMPLE_PROTOCOL, SAMPLE_IP, null,
                SAMPLE_SERVICES_ROOT, SAMPLE_SOURCE_NAME));

        assertNull("An action should not have been created when port is null.",
                actionProvider.getAction(metacard));

    }

    @Test
    public void testContextRootNull() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);

        actionProvider.configurationUpdateCallback(createMap(SAMPLE_PROTOCOL, SAMPLE_IP, SAMPLE_PORT,
                null, SAMPLE_SOURCE_NAME));

        assertNull("An action should not have been created when context root is null.",
                actionProvider.getAction(metacard));

    }

    @Test
    public void testNonMetacard() {

        AbstractMetacardActionProvider actionProvider = configureActionProvider(new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID));

        assertNull("An action when metacard was not provided.",
                actionProvider.getAction(new Date()));

    }

    @Test
    public void testMetacard() {

        // given
        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = configureActionProvider(new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID));

        // when
        Action action = actionProvider.getAction(metacard);

        // then
        assertEquals(ViewMetacardActionProvider.TITLE, action.getTitle());
        assertEquals(ViewMetacardActionProvider.DESCRIPTION, action.getDescription());
        assertThat(action.getUrl().toString(), is(expectedDefaultAddressWith(metacard.getId())));
        assertEquals(ACTION_PROVIDER_ID, actionProvider.getId());

    }

    @Test
    public void testFederatedMetacard() {

        // given
        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        String newSourceName = "newSource";
        metacard.setSourceId(newSourceName);

        AbstractMetacardActionProvider actionProvider = configureActionProvider(new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID));

        // when
        Action action = actionProvider.getAction(metacard);

        // then
        assertEquals(ViewMetacardActionProvider.TITLE, action.getTitle());
        assertEquals(ViewMetacardActionProvider.DESCRIPTION, action.getDescription());
        assertThat(action.getUrl().toString(),
                is(expectedDefaultAddressWith(metacard.getId(), newSourceName)));
        assertEquals(ACTION_PROVIDER_ID, actionProvider.getId());

    }

    @Test
    public void testSecureMetacard() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = configureSecureActionProvider();

        Action action = actionProvider.getAction(metacard);

        assertEquals(ViewMetacardActionProvider.TITLE, action.getTitle());
        assertEquals(ViewMetacardActionProvider.DESCRIPTION, action.getDescription());
        assertEquals(SAMPLE_SECURE_PROTOCOL + SAMPLE_IP + ":" + SAMPLE_SECURE_PORT
                + SAMPLE_SERVICES_ROOT + SAMPLE_PATH + SAMPLE_SOURCE_NAME + "/" + metacard.getId(),
                action.getUrl().toString());
        assertEquals(ACTION_PROVIDER_ID, actionProvider.getId());

    }

    @Test
    public void testNullProtocol() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);

        actionProvider.configurationUpdateCallback(createMap(null, SAMPLE_IP, SAMPLE_SECURE_PORT,
                SAMPLE_SERVICES_ROOT, SAMPLE_SOURCE_NAME));

        Action action = actionProvider.getAction(metacard);

        assertNull(action);
    }

    private String expectedDefaultAddressWith(String id, String sourceName) {
        return SAMPLE_PROTOCOL + SAMPLE_IP + ":" + SAMPLE_PORT + SAMPLE_SERVICES_ROOT + SAMPLE_PATH
                + sourceName + "/" + id;
    }

    private String expectedDefaultAddressWith(String id) {

        return expectedDefaultAddressWith(id, SAMPLE_SOURCE_NAME);
    }

}
