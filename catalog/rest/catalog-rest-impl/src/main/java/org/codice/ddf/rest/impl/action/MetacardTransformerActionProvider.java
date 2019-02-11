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

import static org.codice.ddf.rest.impl.CatalogServiceImpl.CONTEXT_ROOT;
import static org.codice.ddf.rest.impl.CatalogServiceImpl.SOURCES_PATH;

import ddf.action.Action;
import ddf.action.impl.ActionImpl;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
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

public class MetacardTransformerActionProvider extends AbstractMetacardActionProvider {

  static final String DESCRIPTION_PREFIX =
      "Provides a URL to the metacard that transforms the return value via the ";

  static final String DESCRIPTION_SUFFIX = " transformer";

  static final String TITLE_PREFIX = "Export as ";

  private String metacardTransformerId;

  private String attributeName;

  private String baseUrl;

  private String context = null;

  /**
   * Constructor to instantiate this Metacard {@link ddf.action.ActionProvider}
   *
   * @param actionProviderId
   * @param metacardTransformerId
   */
  public MetacardTransformerActionProvider(String actionProviderId, String metacardTransformerId) {
    this(actionProviderId, metacardTransformerId, "");
  }

  /**
   * Constructor to instantiate this Metacard {@link ddf.action.ActionProvider}
   *
   * @param actionProviderId
   * @param metacardTransformerId
   * @param attributeName
   */
  public MetacardTransformerActionProvider(
      String actionProviderId, String metacardTransformerId, String attributeName) {
    super(
        actionProviderId,
        TITLE_PREFIX + metacardTransformerId,
        DESCRIPTION_PREFIX + metacardTransformerId + DESCRIPTION_SUFFIX);
    this.metacardTransformerId = metacardTransformerId;
    this.attributeName = attributeName;
    initBaseUrl(SystemBaseUrl.EXTERNAL.getBaseUrl());
    initContext(SystemBaseUrl.EXTERNAL.getRootContext(), SystemBaseUrl.INTERNAL.getRootContext());
  }

  @Override
  protected URL getMetacardActionUrl(String metacardSource, Metacard metacard)
      throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {
    String encodedMetacardId = URLEncoder.encode(metacard.getId(), CharEncoding.UTF_8);
    String encodedMetacardSource = URLEncoder.encode(metacardSource, CharEncoding.UTF_8);
    return getActionUrl(encodedMetacardSource, encodedMetacardId);
  }

  @Override
  protected boolean canHandleMetacard(Metacard metacard) {
    if (StringUtils.isNotBlank(attributeName)) {
      Attribute attr = metacard.getAttribute(attributeName);
      return (attr != null && attr.getValue() != null);
    }
    return true;
  }

  protected Action createMetacardAction(
      String actionProviderId, String title, String description, URL url) {
    return new ActionImpl(actionProviderId, title, description, url);
  }

  private void initBaseUrl(String externalBaseUrl) {
    if (externalBaseUrl.endsWith("/")) {
      externalBaseUrl = externalBaseUrl.substring(0, externalBaseUrl.length() - 2);
    }
    this.baseUrl = externalBaseUrl;
  }

  private void initContext(String externalContext, String internalContext) {
    StringBuilder context = new StringBuilder();
    if (StringUtils.isNotEmpty(externalContext) && !externalContext.startsWith("/")) {
      context.append("/");
    }
    context.append(externalContext);

    if (!internalContext.startsWith("/")) {
      context.append("/");
    }
    context.append(internalContext);
    this.context = context.toString();
  }

  private URL getActionUrl(String metacardSource, String metacardId)
      throws MalformedURLException, URISyntaxException {
    StringBuilder sb = new StringBuilder();
    sb.append(baseUrl);
    sb.append(context);
    sb.append("/");
    sb.append(
        String.format(
            "%s%s/%s/%s?transform=%s",
            CONTEXT_ROOT, SOURCES_PATH, metacardSource, metacardId, metacardTransformerId));
    return new URI(sb.toString()).toURL();
  }
}
