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
package org.codice.ddf.catalog.actions;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ddf.action.Action;
import ddf.catalog.data.Metacard;

@RunWith(MockitoJUnitRunner.class)
public class AbstractMetacardActionProviderTest {

    private static final String ACTION_ID = "id";

    private static final String TITLE = "title";

    private static final String DESCRIPTION = "description";

    private static final String METACARD_ID = "metacard_id";

    private static final String SOURCE_ID = "source_id";

    private static URL url;

    @Mock
    private Metacard metacard;

    @Mock
    private Action action;

    @BeforeClass
    public static void setupClass() throws MalformedURLException {
        url = new URL("https://localhost/action");
    }

    @Before
    public void setup() {
        when(metacard.getId()).thenReturn(METACARD_ID);
        when(metacard.getSourceId()).thenReturn(SOURCE_ID);
    }

    private class MetacardActionProvider extends AbstractMetacardActionProvider {

        MetacardActionProvider(String actionProviderId, String title, String description) {
            super(actionProviderId, title, description);
        }

        @Override
        protected boolean canHandleMetacard(Metacard metacard) {
            return super.canHandleMetacard(metacard);
        }

        @Override
        protected Action createMetacardAction(String actionProviderId, String title,
                String description, URL url) {
            return null;
        }

        @Override
        protected URL getMetacardActionUrl(String metacardSource, Metacard metacard)
                throws Exception {
            return new URL("https://localhost/action");
        }
    }

    @Test(expected = NullPointerException.class)
    public void constructorWithNullActionProviderId() {
        new MetacardActionProvider(null, TITLE, DESCRIPTION);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorWithBlankActionProviderId() {
        new MetacardActionProvider("  ", TITLE, DESCRIPTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructorWithNullTitle() {
        new MetacardActionProvider(ACTION_ID, null, DESCRIPTION);
    }

    @Test(expected = NullPointerException.class)
    public void constructorWithNullDescription() {
        new MetacardActionProvider(ACTION_ID, TITLE, null);
    }

    @Test
    public void canHandleWithNull() {
        MetacardActionProvider actionProvider = createMetacardActionProvider();
        assertThat(actionProvider.canHandle(null), is(false));
        verify(actionProvider, never()).canHandleMetacard(any());
    }

    @Test
    public void canHandleWithNonMetacard() {
        MetacardActionProvider actionProvider = createMetacardActionProvider();
        assertThat(actionProvider.canHandle("blah"), is(false));
        verify(actionProvider, never()).canHandleMetacard(any());
    }

    @Test
    public void canHandleDelegatesToCanHandleMetacard() {
        MetacardActionProvider actionProvider = createMetacardActionProvider();
        when(actionProvider.canHandleMetacard(metacard)).thenReturn(true);

        assertThat(actionProvider.canHandle(metacard), is(true));
        verify(actionProvider).canHandleMetacard(metacard);
    }

    @Test
    public void getActionsWithNull() throws Exception {
        MetacardActionProvider actionProvider = createMetacardActionProvider();

        List<Action> actions = actionProvider.getActions(null);

        assertThat(actions, is(empty()));
        verify(actionProvider, never()).getMetacardAction(any(), any());
    }

    @Test
    public void getActionsWithNonMetacard() throws Exception {
        MetacardActionProvider actionProvider = createMetacardActionProvider();

        List<Action> actions = actionProvider.getActions("blah");

        assertThat(actions, is(empty()));
        verify(actionProvider, never()).getMetacardAction(any(), any());
    }

    @Test
    public void getActionsWithMetacardThatHasNullId() throws Exception {
        MetacardActionProvider actionProvider = createMetacardActionProvider();
        when(metacard.getId()).thenReturn(null);

        List<Action> actions = actionProvider.getActions(metacard);

        assertThat(actions, is(empty()));
        verify(actionProvider, never()).getMetacardAction(any(), any());
    }

    @Test
    public void getActionsWithMetacardThatHasBlankId() throws Exception {
        MetacardActionProvider actionProvider = createMetacardActionProvider();
        when(metacard.getId()).thenReturn(" ");

        List<Action> actions = actionProvider.getActions(metacard);

        assertThat(actions, is(empty()));
        verify(actionProvider, never()).getMetacardAction(any(), any());
    }

    @Test
    public void getActionsWhenHostNotSet() throws Exception {
        MetacardActionProvider actionProvider = createMetacardActionProvider();
        when(actionProvider.canHandleMetacard(metacard)).thenReturn(true);
        when(actionProvider.createMetacardAction(eq(ACTION_ID),
                eq(TITLE),
                eq(DESCRIPTION),
                any())).thenReturn(action);
        when(actionProvider.getMetacardActionUrl(SOURCE_ID, metacard)).thenReturn(url);
        System.clearProperty(SystemBaseUrl.HOST);

        List<Action> actions = actionProvider.getActions(metacard);

        assertThat(actions, hasItem(action));
        verify(actionProvider).createMetacardAction(ACTION_ID, TITLE, DESCRIPTION, url);
    }

    @Test
    public void getActionsWhenHostUnknown() throws Exception {
        MetacardActionProvider actionProvider = createMetacardActionProvider();
        when(actionProvider.canHandleMetacard(metacard)).thenReturn(true);
        when(actionProvider.createMetacardAction(eq(ACTION_ID),
                eq(TITLE),
                eq(DESCRIPTION),
                any())).thenReturn(action);
        when(actionProvider.getMetacardActionUrl(SOURCE_ID, metacard)).thenReturn(url);
        System.setProperty(SystemBaseUrl.HOST, "0.0.0.0");

        List<Action> actions = actionProvider.getActions(metacard);

        assertThat(actions, is(empty()));
        verify(actionProvider, never()).getMetacardAction(any(), any());
    }

    @Test
    public void getActionsWhenSubclassCannotHandleMetacard() throws Exception {
        MetacardActionProvider actionProvider = createMetacardActionProvider();
        when(actionProvider.canHandleMetacard(metacard)).thenReturn(false);

        List<Action> actions = actionProvider.getActions(metacard);

        assertThat(actions, is(empty()));
        verify(actionProvider, never()).getMetacardAction(any(), any());
    }

    @Test
    public void getActions() {
        MetacardActionProvider actionProvider = createMetacardActionProvider();
        when(actionProvider.createMetacardAction(eq(ACTION_ID),
                eq(TITLE),
                eq(DESCRIPTION),
                any())).thenReturn(action);
        System.setProperty(SystemBaseUrl.HOST, "codice.org");

        List<Action> actions = actionProvider.getActions(metacard);

        assertThat(actions, hasItem(action));
    }

    @Test
    public void getActionsWhenGetMetacardActionFails() {
        MetacardActionProvider actionProvider = new MetacardActionProvider(ACTION_ID,
                TITLE,
                DESCRIPTION) {

            @Override
            protected URL getMetacardActionUrl(String metacardSource, Metacard metacard)
                    throws IOException {
                throw new IOException();
            }
        };

        System.setProperty(SystemBaseUrl.HOST, "codice.org");
        List<Action> actions = actionProvider.getActions(metacard);
        assertThat(actions, is(empty()));
    }

    @Test
    public void getActionsWhenMetacardSourceIdIsNull() throws Exception {
        MetacardActionProvider actionProvider = createMetacardActionProvider();
        when(metacard.getSourceId()).thenReturn(null);
        System.setProperty(SystemBaseUrl.HOST, "codice.org");
        System.setProperty(SystemInfo.SITE_NAME, "ddf");
        when(actionProvider.createMetacardAction(eq(ACTION_ID),
                eq(TITLE),
                eq(DESCRIPTION),
                any())).thenReturn(action);

        List<Action> actions = actionProvider.getActions(metacard);

        assertThat(actions, hasItem(action));
        verify(actionProvider).getMetacardAction("ddf", metacard);
    }

    private MetacardActionProvider createMetacardActionProvider() {
        return spy(new MetacardActionProvider(ACTION_ID, TITLE, DESCRIPTION));
    }
}
