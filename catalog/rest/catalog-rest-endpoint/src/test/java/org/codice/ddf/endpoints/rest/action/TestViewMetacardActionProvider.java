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
package org.codice.ddf.endpoints.rest.action;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Test;

import ddf.action.Action;
import ddf.catalog.data.impl.MetacardImpl;

public class TestViewMetacardActionProvider extends AbstractActionProviderTest {

    @Test
    public void testMetacardNull() {
        assertEquals(null,
                new ViewMetacardActionProvider(ACTION_PROVIDER_ID).getAction(null));
    }

    @Test
    public void testUriSyntaxException() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);
        this.configureActionProvider(SAMPLE_PROTOCOL, "23^&*#", SAMPLE_PORT, SAMPLE_SERVICES_ROOT,
                SAMPLE_SOURCE_NAME);

        assertNull("A bad url should have been caught and a null action returned.",
                actionProvider.getAction(metacard));

    }

    @Test
    public void testMetacardIdUrlEncodedSpace() {

        // given
        MetacardImpl metacard = new MetacardImpl();

        metacard.setId("abd ef");

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);
        this.configureActionProvider();

        // when
        String url = actionProvider.getAction(metacard)
                .getUrl()
                .toString();

        // then
        assertThat(url, is(expectedDefaultAddressWith("abd+ef")));

    }

    @Test
    public void testMetacardIdUrlEncodedAmpersand() {

        // given
        MetacardImpl metacard = new MetacardImpl();

        metacard.setId("abd&ef");

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);
        this.configureActionProvider();

        // when
        String url = actionProvider.getAction(metacard)
                .getUrl()
                .toString();

        // then
        assertThat(url, is(expectedDefaultAddressWith("abd%26ef")));

    }

    @Test
    public void testMetacardIdNull() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(null);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);
        this.configureActionProvider();

        assertNull("An action should not have been created when no id is provided.",
                actionProvider.getAction(metacard));

    }

    @Test
    public void testIpNull() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);

        this.configureActionProvider(SAMPLE_PROTOCOL, null, SAMPLE_PORT, SAMPLE_SERVICES_ROOT,
                SAMPLE_SOURCE_NAME);

        assertThat(actionProvider.getAction(metacard)
                .getUrl()
                .toString(), containsString("localhost"));

    }

    @Test
    public void testIpUnknown() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);

        this.configureActionProvider(SAMPLE_PROTOCOL, "0.0.0.0", SAMPLE_PORT, SAMPLE_SERVICES_ROOT,
                SAMPLE_SOURCE_NAME);

        assertNull("An action should not have been created when ip is unknown (0.0.0.0).",
                actionProvider.getAction(metacard));
    }

    @Test
    public void testPortNull() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);

        this.configureActionProvider(SAMPLE_PROTOCOL, SAMPLE_IP, null, SAMPLE_SERVICES_ROOT,
                SAMPLE_SOURCE_NAME);

        assertThat(actionProvider.getAction(metacard)
                .getUrl()
                .toString(), containsString("8181"));
    }

    @Test
    public void testContextRootNull() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);

        this.configureActionProvider(SAMPLE_PROTOCOL, SAMPLE_IP, SAMPLE_PORT, null,
                SAMPLE_SOURCE_NAME);

        assertThat(actionProvider.getAction(metacard)
                .getUrl()
                .toString(), not(containsString("/services")));
    }

    @Test
    public void testNonMetacard() {

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);
        this.configureActionProvider();

        assertNull("An action when metacard was not provided.",
                actionProvider.getAction(new Date()));

    }

    @Test
    public void testMetacard() {

        // given
        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);
        this.configureActionProvider();

        // when
        Action action = actionProvider.getAction(metacard);

        // then
        assertEquals(ViewMetacardActionProvider.TITLE, action.getTitle());
        assertEquals(ViewMetacardActionProvider.DESCRIPTION, action.getDescription());
        assertThat(action.getUrl()
                .toString(), is(expectedDefaultAddressWith(metacard.getId())));
        assertEquals(ACTION_PROVIDER_ID, actionProvider.getId());

    }

    @Test
    public void testFederatedMetacard() {

        // given
        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        String newSourceName = "newSource";
        metacard.setSourceId(newSourceName);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);
        this.configureActionProvider();

        // when
        Action action = actionProvider.getAction(metacard);

        // then
        assertEquals(ViewMetacardActionProvider.TITLE, action.getTitle());
        assertEquals(ViewMetacardActionProvider.DESCRIPTION, action.getDescription());
        assertThat(action.getUrl()
                .toString(), is(expectedDefaultAddressWith(metacard.getId(), newSourceName)));
        assertEquals(ACTION_PROVIDER_ID, actionProvider.getId());

    }

    @Test
    public void testSecureMetacard() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);
        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);
        this.configureSecureActionProvider();

        Action action = actionProvider.getAction(metacard);

        assertEquals(ViewMetacardActionProvider.TITLE, action.getTitle());
        assertEquals(ViewMetacardActionProvider.DESCRIPTION, action.getDescription());
        assertEquals(
                SAMPLE_SECURE_PROTOCOL + SAMPLE_IP + ":" + SAMPLE_SECURE_PORT + SAMPLE_SERVICES_ROOT
                        + SAMPLE_PATH + SAMPLE_SOURCE_NAME + "/" + metacard.getId(), action.getUrl()
                        .toString());
        assertEquals(ACTION_PROVIDER_ID, actionProvider.getId());

    }

    @Test
    public void testNullProtocol() {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        AbstractMetacardActionProvider actionProvider = new ViewMetacardActionProvider(
                ACTION_PROVIDER_ID);

        this.configureActionProvider(null, SAMPLE_IP, SAMPLE_SECURE_PORT, SAMPLE_SERVICES_ROOT,
                SAMPLE_SOURCE_NAME);

        Action action = actionProvider.getAction(metacard);

        //when null protocal should default to https
        assertThat(action.getUrl()
                .toString(), containsString("https"));

    }

    private String expectedDefaultAddressWith(String id, String sourceName) {
        return SAMPLE_PROTOCOL + SAMPLE_IP + ":" + SAMPLE_PORT + SAMPLE_SERVICES_ROOT + SAMPLE_PATH
                + sourceName + "/" + id;
    }

    private String expectedDefaultAddressWith(String id) {

        return expectedDefaultAddressWith(id, SAMPLE_SOURCE_NAME);
    }

}
