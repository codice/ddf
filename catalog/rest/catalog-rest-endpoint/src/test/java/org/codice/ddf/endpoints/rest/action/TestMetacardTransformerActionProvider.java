/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.endpoints.rest.action;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(
            TestMetacardTransformerActionProvider.class);

    @Test
    public void testMalformedUrlException() {

        String noSource = null;
        Metacard metacard = givenMetacard(SAMPLE_ID, noSource);

        AbstractMetacardActionProvider actionProvider = new MetacardTransformerActionProvider(
                ACTION_PROVIDER_ID,
                SAMPLE_TRANSFORMER_ID);

        this.configureActionProvider("badProtocol",
                SAMPLE_IP,
                SAMPLE_PORT,
                SAMPLE_SERVICES_ROOT,
                SAMPLE_SOURCE_NAME);

        assertThat("A bad url should have been caught and an empty list returned.",
                actionProvider.getActions(metacard),
                hasSize(0));

    }

    @Test
    public void testUriSyntaxException() {

        String noSource = null;
        Metacard metacard = givenMetacard(SAMPLE_ID, noSource);

        AbstractMetacardActionProvider actionProvider = new MetacardTransformerActionProvider(
                ACTION_PROVIDER_ID,
                SAMPLE_TRANSFORMER_ID);

        this.configureActionProvider(SAMPLE_PROTOCOL,
                "23^&*#",
                SAMPLE_PORT,
                SAMPLE_SERVICES_ROOT,
                SAMPLE_SOURCE_NAME);

        assertThat("A bad url should have been caught and an empty list returned.",
                actionProvider.getActions(metacard),
                hasSize(0));

    }

    @Test
    public void testMetacard() {

        // given

        String noSource = null;
        Metacard metacard = givenMetacard(SAMPLE_ID, noSource);

        AbstractMetacardActionProvider actionProvider = new MetacardTransformerActionProvider(
                ACTION_PROVIDER_ID,
                SAMPLE_TRANSFORMER_ID);
        this.configureActionProvider();

        // when
        Action action = actionProvider.getActions(metacard)
                .get(0);

        // then
        assertEquals(MetacardTransformerActionProvider.TITLE_PREFIX + SAMPLE_TRANSFORMER_ID,
                action.getTitle());

        assertEquals(MetacardTransformerActionProvider.DESCRIPTION_PREFIX + SAMPLE_TRANSFORMER_ID +
                MetacardTransformerActionProvider.DESCRIPTION_SUFFIX, action.getDescription());

        assertThat(action.getUrl()
                        .toString(),
                is(expectedDefaultAddressWith(metacard.getId(),
                        SAMPLE_SOURCE_NAME,
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

        AbstractMetacardActionProvider actionProvider = new MetacardTransformerActionProvider(
                ACTION_PROVIDER_ID,
                SAMPLE_TRANSFORMER_ID);
        this.configureActionProvider();

        // when
        Action action = actionProvider.getActions(metacard)
                .get(0);

        // then
        assertThat(action.getUrl()
                        .toString(),
                is(expectedDefaultAddressWith(metacard.getId(),
                        newSourceName,
                        SAMPLE_TRANSFORMER_ID)));
        assertEquals(ACTION_PROVIDER_ID, actionProvider.getId());

    }

    @Test
    public void testEmptyAttributeName() {
        AbstractMetacardActionProvider provider = new MetacardTransformerActionProvider(
                ACTION_PROVIDER_ID,
                SAMPLE_TRANSFORMER_ID,
                "");
        assertThat(provider.canHandle(new MetacardImpl()), is(true));
        assertThat(provider.canHandle(new String()), is(false));
    }

    @Test
    public void testSpecificAttributeName() {
        AbstractMetacardActionProvider provider = new MetacardTransformerActionProvider(
                ACTION_PROVIDER_ID,
                SAMPLE_TRANSFORMER_ID,
                "thumbnail");
        MetacardImpl metacard = new MetacardImpl();
        assertThat(provider.canHandle(metacard), is(false));
        metacard.setThumbnail(new byte[] {000});
        assertThat(provider.canHandle(metacard), is(true));
    }

    private String expectedDefaultAddressWith(String id, String sourceName,
            String transformerName) {
        return SAMPLE_PROTOCOL + SAMPLE_IP + ":" + SAMPLE_PORT + SAMPLE_SERVICES_ROOT + SAMPLE_PATH
                + sourceName + "/" + id + "?transform=" + transformerName;
    }

}
