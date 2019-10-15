/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.transformer.preview.actions;

import static org.codice.ddf.endpoints.rest.RESTService.CONTEXT_ROOT;
import static org.codice.ddf.endpoints.rest.RESTService.SOURCES_PATH;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import ddf.action.Action;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.experimental.Extracted;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import org.apache.commons.lang.CharEncoding;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.junit.Test;

public class PreviewActionProviderTest {
  public static final String ACTION_PROVIDER_ID = "actionProviderId";

  private PreviewActionProvider previewActionProvider =
      new PreviewActionProvider(ACTION_PROVIDER_ID);

  private Metacard metacard = mock(Metacard.class);

  @Test
  public void metacardNotHandled() throws Exception {
    boolean canHandle = previewActionProvider.canHandle(metacard);
    assertThat(canHandle, is(equalTo(false)));
  }

  @Test
  public void metacardHandled() throws Exception {
    Attribute extractedTextAttribute =
        new AttributeImpl(Extracted.EXTRACTED_TEXT, "some extracted text");

    doReturn(extractedTextAttribute).when(metacard).getAttribute(Extracted.EXTRACTED_TEXT);

    boolean canHandle = previewActionProvider.canHandle(metacard);
    assertThat(canHandle, is(equalTo(true)));
  }

  @Test
  public void createMetacardAction() throws Exception {
    String title = "title";
    String description = "description";
    URL url = new URL("https://localhost/url");

    Action action =
        previewActionProvider.createMetacardAction(ACTION_PROVIDER_ID, title, description, url);
    assertThat(action.getId(), is(equalTo(ACTION_PROVIDER_ID)));
    assertThat(action.getTitle(), is(equalTo(title)));
    assertThat(action.getDescription(), is(equalTo(description)));
    assertThat(url, is(equalTo(url)));
  }

  @Test
  public void getMetacardActionUrl() throws Exception {
    String metacardId = "metacardId";
    String metacardSource = "metacardSource";
    doReturn(metacardId).when(metacard).getId();

    URL url = previewActionProvider.getMetacardActionUrl(metacardSource, metacard);

    assertThat(url, notNullValue());

    URL anotherOne = getUrl(metacardId, metacardSource);

    assertThat(url, is(anotherOne));
  }

  private URL getUrl(String metacardId, String metacardSource)
      throws MalformedURLException, UnsupportedEncodingException {
    String encodedMetacardId = URLEncoder.encode(metacardId, CharEncoding.UTF_8);
    String urlString =
        String.format(
            "%s%s/%s/%s?transform=preview",
            CONTEXT_ROOT, SOURCES_PATH, metacardSource, encodedMetacardId);
    return new URL(SystemBaseUrl.EXTERNAL.constructUrl(urlString, true));
  }


}
