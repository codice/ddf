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
package org.codice.ddf.rest.impl.action;

import static org.codice.ddf.rest.service.CatalogService.CONTEXT_ROOT;
import static org.codice.ddf.rest.service.CatalogService.SOURCES_PATH;

import ddf.action.Action;
import ddf.action.impl.ActionImpl;
import ddf.catalog.data.Metacard;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import org.apache.commons.lang.CharEncoding;
import org.codice.ddf.catalog.actions.AbstractMetacardActionProvider;
import org.codice.ddf.configuration.SystemBaseUrl;

public class ViewMetacardActionProvider extends AbstractMetacardActionProvider {

  public static final String TITLE = "Export Metacard XML";

  public static final String DESCRIPTION = "Provides a URL to the metacard";

  public ViewMetacardActionProvider(String id) {
    super(id, TITLE, DESCRIPTION);
  }

  @Override
  protected URL getMetacardActionUrl(String metacardSource, Metacard metacard)
      throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {
    String encodedMetacardId = URLEncoder.encode(metacard.getId(), CharEncoding.UTF_8);
    String encodedMetacardSource = URLEncoder.encode(metacardSource, CharEncoding.UTF_8);
    return getActionUrl(encodedMetacardSource, encodedMetacardId);
  }

  protected Action createMetacardAction(
      String actionProviderId, String title, String description, URL url) {
    return new ActionImpl(actionProviderId, title, description, url);
  }

  private URL getActionUrl(String metacardSource, String metacardId)
      throws MalformedURLException, URISyntaxException {
    return new URI(
            SystemBaseUrl.EXTERNAL.constructUrl(
                String.format("%s%s/%s/%s", CONTEXT_ROOT, SOURCES_PATH, metacardSource, metacardId),
                true))
        .toURL();
  }
}
