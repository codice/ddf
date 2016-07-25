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
package org.codice.ddf.catalog.content.plugin.uri;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.StopProcessingException;

public class TestContentUriAccessPlugin {

    public static final String ID_OR_URI = "ID_OR_URI";

    private UpdateRequest input;

    private Metacard updateCard;

    private HashMap<String, Metacard> existingMetacards;

    private Metacard existingMetacard;

    private ContentUriAccessPlugin contentUriAccessPlugin;

    @Before
    public void setup() {
        input = mock(UpdateRequest.class);
        updateCard = mock(Metacard.class);
        when(input.getUpdates()).thenReturn(Collections.singletonList(new HashMap.SimpleEntry<>(
                ID_OR_URI,
                updateCard)));
        existingMetacards = new HashMap<>();
        existingMetacard = mock(Metacard.class);
        existingMetacards.put(ID_OR_URI, existingMetacard);
        contentUriAccessPlugin = new ContentUriAccessPlugin();
    }

    @Test
    public void bothUrisNull() throws StopProcessingException {
        when(updateCard.getResourceURI()).thenReturn(null);
        when(existingMetacard.getResourceURI()).thenReturn(null);
        contentUriAccessPlugin.processPreUpdate(input, existingMetacards);
        assertNoChanges();
    }

    @Test
    public void updateUriNull() throws StopProcessingException, URISyntaxException {
        when(updateCard.getResourceURI()).thenReturn(null);
        when(existingMetacard.getResourceURI()).thenReturn(new URI(""));
        contentUriAccessPlugin.processPreUpdate(input, existingMetacards);
        assertNoChanges();
    }

    @Test
    public void existingUriNull() throws StopProcessingException, URISyntaxException {
        when(updateCard.getResourceURI()).thenReturn(new URI(""));
        when(existingMetacard.getResourceURI()).thenReturn(null);
        contentUriAccessPlugin.processPreUpdate(input, existingMetacards);
        assertNoChanges();
    }

    @Test
    public void existingUriNonContent() throws StopProcessingException, URISyntaxException {
        when(updateCard.getResourceURI()).thenReturn(new URI(""));
        when(existingMetacard.getResourceURI()).thenReturn(new URI(""));
        contentUriAccessPlugin.processPreUpdate(input, existingMetacards);
        assertNoChanges();
    }

    @Test(expected = StopProcessingException.class)
    public void existingUriContent() throws StopProcessingException, URISyntaxException {
        when(updateCard.getResourceURI()).thenReturn(new URI(""));
        when(existingMetacard.getResourceURI()).thenReturn(new URI(ContentItem.CONTENT_SCHEME,
                UUID.randomUUID()
                        .toString(),
                null));
        contentUriAccessPlugin.processPreUpdate(input, existingMetacards);
    }

    private void assertNoChanges() {
        assertThat("Input should not have been modified", input, is(equalTo(input)));
        assertThat("Existing metacards should not have been modified",
                existingMetacards,
                is(equalTo(existingMetacards)));
    }
}
