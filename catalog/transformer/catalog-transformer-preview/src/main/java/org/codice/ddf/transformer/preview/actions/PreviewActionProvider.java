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

import ddf.action.Action;
import ddf.action.impl.ActionImpl;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.experimental.Extracted;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import org.apache.commons.lang.CharEncoding;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.catalog.actions.AbstractMetacardActionProvider;
import org.codice.ddf.configuration.SystemBaseUrl;

public class PreviewActionProvider extends AbstractMetacardActionProvider {

  private static final String TITLE = "Text Preview";

  private static final String DESCRIPTION = "Provides a text preview of the resource";

  /**
   * Constructor that accepts the values to be used when a new {@link Action} is created by this
   * {@link ddf.action.ActionProvider}.
   *
   * @param actionProviderId ID that will be assigned to the {@link Action} that will be created.
   *     Cannot be empty or blank.
   * @param title title that will be used when this {@link ddf.action.ActionProvider} creates a new
   *     {@link Action}
   * @param description description that will be used when this {@link ddf.action.ActionProvider}
   *     creates a new {@link Action}
   */
  protected PreviewActionProvider(String actionProviderId, String title, String description) {
    super(actionProviderId, title, description);
  }

  public PreviewActionProvider(String actionProviderId) {
    super(actionProviderId, TITLE, DESCRIPTION);
  }

  @Override
  protected boolean canHandleMetacard(Metacard metacard) {
    Attribute bodyText = metacard.getAttribute(Extracted.EXTRACTED_TEXT);
    return bodyText != null && StringUtils.isNotBlank((String) bodyText.getValue());
  }

  @Override
  protected Action createMetacardAction(
      String actionProviderId, String title, String description, URL url) {
    return new ActionImpl(actionProviderId, title, description, url);
  }

  @Override
  protected URL getMetacardActionUrl(String metacardSource, Metacard metacard)
      throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {
    String encodedMetacardId = URLEncoder.encode(metacard.getId(), CharEncoding.UTF_8);
    String encodedMetacardSource = URLEncoder.encode(metacardSource, CharEncoding.UTF_8);
    return getActionUrl(encodedMetacardSource, encodedMetacardId);
  }

  private URL getActionUrl(String metacardSource, String metacardId)
      throws URISyntaxException, MalformedURLException {
    return new URI(
            SystemBaseUrl.EXTERNAL.constructUrl(
                String.format(
                    "%s%s/%s/%s?transform=preview",
                    CONTEXT_ROOT, SOURCES_PATH, metacardSource, metacardId),
                true))
        .toURL();
  }
}
