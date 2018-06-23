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
package ddf.catalog.impl.action;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.catalog.source.Source;
import java.net.MalformedURLException;
import java.net.URL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Convert a JSON string to a map of source identifiers to action URLs. */
public class SourceActionProviderImpl implements ActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceActionProviderImpl.class);

  private static final String HTTP = "http://";

  private static final String HTTPS = "https://";

  private String actionProviderId;

  private String title;

  private String url;

  private String description;

  private String sourceId;

  public SourceActionProviderImpl(String actionProviderId) {
    this.actionProviderId = actionProviderId;
  }

  @Override
  public synchronized <T> Action getAction(T subject) {

    if (!(subject instanceof Source)) {
      return null;
    }

    Source source = (Source) subject;

    if (source.getId().equals(sourceId)) {
      return toAction();
    }

    return null;
  }

  @Override
  public String getId() {
    return actionProviderId;
  }

  public String getUrl() {
    return url;
  }

  public void setSourceId(String sourceId) {
    this.sourceId = sourceId;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public void setUrl(String url) {
    if (url.startsWith(HTTP) || url.startsWith(HTTPS)) {
      this.url = url;
    } else {
      this.url = HTTPS + url;
    }
  }

  public void setDescription(String description) {
    this.description = description != null ? description : "";
  }

  private Action toAction() {
    try {
      return new ActionImpl(getId(), title, description, new URL(constructUrl()));
    } catch (MalformedURLException e) {
      LOGGER.debug("Unable to parse the action url: url={}", url, e);
      return null;
    }
  }

  /**
   * Subclasses of {@link SourceActionProviderImpl} may override this method to alter the url. This
   * base class returns the original url.
   */
  protected String constructUrl() {
    return url;
  }
}
