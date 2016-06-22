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

import static org.codice.ddf.endpoints.rest.RESTService.CONTEXT_ROOT;
import static org.codice.ddf.endpoints.rest.RESTService.SOURCES_PATH;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;

import org.apache.commons.lang.CharEncoding;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import ddf.action.Action;
import ddf.catalog.data.Metacard;

@RunWith(MockitoJUnitRunner.class)
public class TestViewMetacardActionProvider extends AbstractActionProviderTest {

    private static final String ACTION_PROVIDER_ID = "actionID";

    private static final String METACARD_ID = "metacardID";

    private static final String REMOTE_SOURCE_ID = "remote";

    @Mock
    private Metacard metacard;

    private URL actionUrl;

    private ViewMetacardActionProvider actionProvider;

    @Before
    public void setup() throws Exception {
        System.setProperty(SystemBaseUrl.HOST, "localhost");

        when(metacard.getId()).thenReturn(METACARD_ID);
        when(metacard.getSourceId()).thenReturn(REMOTE_SOURCE_ID);

        actionUrl = getUrl(METACARD_ID);

        actionProvider = new ViewMetacardActionProvider(ACTION_PROVIDER_ID);
    }

    @Test
    public void canHandle() {
        assertThat(actionProvider.canHandle(metacard), is(true));
    }

    @Test
    public void createMetacardAction() throws MalformedURLException {
        List<Action> actions = actionProvider.getActions(metacard);

        assertThat(actions, hasSize(1));
        Action action = actions.get(0);
        assertThat(action.getId(), is(ACTION_PROVIDER_ID));
        assertThat(action.getTitle(), is("Export Metacard XML"));
        assertThat(action.getDescription(), is("Provides a URL to the metacard"));
        assertThat(action.getUrl(), is(actionUrl));
    }

    @Test
    public void getMetacardActionUrl() throws Exception {
        URL url = actionProvider.getMetacardActionUrl(REMOTE_SOURCE_ID, metacard);

        assertThat(url, is(actionUrl));
    }

    @Test
    public void getMetacardActionUrlEncodedAmpersand() throws Exception {
        String metacardId = "abc&def";
        when(metacard.getId()).thenReturn(metacardId);

        URL url = actionProvider.getMetacardActionUrl(REMOTE_SOURCE_ID, metacard);

        assertThat(url, is(getUrl(metacardId)));
    }

    @Test(expected = URISyntaxException.class)
    public void getMetacardActionUrlWhenUrlIsMalformed() throws Exception {
        String invalidHost = "23^&*#";
        System.setProperty(SystemBaseUrl.HOST, invalidHost);

        actionProvider.getMetacardActionUrl(REMOTE_SOURCE_ID, metacard);
    }

    private URL getUrl(String metacardId)
            throws MalformedURLException, UnsupportedEncodingException {
        String encodedMetacardId = URLEncoder.encode(metacardId, CharEncoding.UTF_8);
        String urlString = String.format("%s%s/%s/%s",
                CONTEXT_ROOT,
                SOURCES_PATH,
                REMOTE_SOURCE_ID,
                encodedMetacardId);
        return new URL(SystemBaseUrl.constructUrl(urlString, true));
    }
}
