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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class TestMetacardTransformerActionProvider extends AbstractActionProviderTest {

    private static final String SAMPLE_TRANSFORMER_ID = "XML";

    private static final Logger LOGGER = LoggerFactory.getLogger(TestMetacardTransformerActionProvider.class);

    @Test
    public void testMalformedUrlException() {

        String noSource = null;
        Metacard metacard = givenMetacard(SAMPLE_ID, noSource);

        AbstractMetacardActionProvider actionProvider = new MetacardTransformerActionProvider(
                ACTION_PROVIDER_ID, SAMPLE_TRANSFORMER_ID);

        actionProvider.configurationUpdateCallback(createMap("badProtocol", SAMPLE_IP, SAMPLE_PORT,
                SAMPLE_SERVICES_ROOT, SAMPLE_SOURCE_NAME));

        assertNull("A bad url should have been caught and a null action returned.",
                actionProvider.getAction(metacard));

    }

    @Test
    public void testUriSyntaxException() {

        String noSource = null;
        Metacard metacard = givenMetacard(SAMPLE_ID, noSource);

        AbstractMetacardActionProvider actionProvider = new MetacardTransformerActionProvider(
                ACTION_PROVIDER_ID, SAMPLE_TRANSFORMER_ID);

        actionProvider.configurationUpdateCallback(createMap(SAMPLE_PROTOCOL, "23^&*#", SAMPLE_PORT,
                SAMPLE_SERVICES_ROOT, SAMPLE_SOURCE_NAME));

        assertNull("A bad url should have been caught and a null action returned.",
                actionProvider.getAction(metacard));

    }

    @Test
    public void testMetacard() {

        // given

        String noSource = null;
        Metacard metacard = givenMetacard(SAMPLE_ID, noSource);

        AbstractMetacardActionProvider actionProvider = configureActionProvider(new MetacardTransformerActionProvider(
                ACTION_PROVIDER_ID, SAMPLE_TRANSFORMER_ID));

        // when
        Action action = actionProvider.getAction(metacard);

        // then
        assertEquals(MetacardTransformerActionProvider.TITLE_PREFIX + SAMPLE_TRANSFORMER_ID,
                action.getTitle());

        assertEquals(MetacardTransformerActionProvider.DESCRIPTION_PREFIX + SAMPLE_TRANSFORMER_ID +
                MetacardTransformerActionProvider.DESCRIPTION_SUFFIX,
                action.getDescription());

        assertThat(
                action.getUrl().toString(),
                is(expectedDefaultAddressWith(metacard.getId(), SAMPLE_SOURCE_NAME,
                        SAMPLE_TRANSFORMER_ID)));
        assertEquals(ACTION_PROVIDER_ID, actionProvider.getId());

    }

    @Test
    public void testToString() {
        String toString = new MetacardTransformerActionProvider(ACTION_PROVIDER_ID,
                SAMPLE_TRANSFORMER_ID).toString();

        LOGGER.info(toString);

        assertThat(toString, containsString(ActionProvider.class.getName()));
        assertThat(toString, containsString(ACTION_PROVIDER_ID));

    }

    private Metacard givenMetacard(String sampleId, String sourceName) {

        Metacard metacard = mock(Metacard.class);

        when(metacard.getId()).thenReturn(sampleId);
        when(metacard.getSourceId()).thenReturn(sourceName);

        return metacard;
    }

    @Test
    public void testFederatedMetacard() {

        // given
        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(SAMPLE_ID);

        String newSourceName = "newSource";

        metacard.setSourceId(newSourceName);

        AbstractMetacardActionProvider actionProvider = configureActionProvider(new MetacardTransformerActionProvider(
                ACTION_PROVIDER_ID, SAMPLE_TRANSFORMER_ID));

        // when
        Action action = actionProvider.getAction(metacard);

        // then
        assertThat(
                action.getUrl().toString(),
                is(expectedDefaultAddressWith(metacard.getId(), newSourceName,
                        SAMPLE_TRANSFORMER_ID)));
        assertEquals(ACTION_PROVIDER_ID, actionProvider.getId());

    }

    private String expectedDefaultAddressWith(String id, String sourceName, String transformerName) {
        return SAMPLE_PROTOCOL + SAMPLE_IP + ":" + SAMPLE_PORT + SAMPLE_SERVICES_ROOT + SAMPLE_PATH
                + sourceName + "/" + id + "?transform=" + transformerName;
    }

}
