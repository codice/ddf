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
package org.codice.ddf.catalog.actions;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.catalog.data.Metacard;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.configuration.SystemInfo;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class for {@link ActionProvider}s that can handle {@link Metacard} source objects.
 */
public abstract class AbstractMetacardActionProvider implements ActionProvider {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(AbstractMetacardActionProvider.class);

  private static final String UNKNOWN_TARGET = "0.0.0.0";

  private final String actionProviderId;

  private final String title;

  private final String description;

  /**
   * Constructor that accepts the values to be used when a new {@link Action} is created by this
   * {@link ActionProvider}.
   *
   * @param actionProviderId ID that will be assigned to the {@link Action} that will be created.
   *     Cannot be empty or blank.
   * @param title title that will be used when this {@link ActionProvider} creates a new {@link
   *     Action}
   * @param description description that will be used when this {@link ActionProvider} creates a new
   *     {@link Action}
   */
  protected AbstractMetacardActionProvider(
      String actionProviderId, String title, String description) {
    Validate.notBlank(actionProviderId.trim(), "Action provider ID cannot be null, empty or blank");
    Validate.notNull(title, "Title cannot be null");
    Validate.notNull(description, "Description cannot be null");

    this.actionProviderId = actionProviderId;
    this.title = title;
    this.description = description;
  }

  /**
   * Default implementation that ensures the {@code subject} provided is a {@link Metacard} with a
   * valid ID before delegating to {@link #getMetacardAction(String, Metacard)}.
   *
   * <p>{@inheritDoc}
   */
  @Override
  public <T> Action getAction(T subject) {

    if (!canHandle(subject)) {
      return null;
    }

    Metacard metacard = (Metacard) subject;

    if (StringUtils.isBlank(metacard.getId())) {
      LOGGER.debug("Cannot create Action: No metacard ID.");
      return null;
    }

    if (isHostUnset(SystemBaseUrl.EXTERNAL.getHost())) {
      LOGGER.debug(
          "Cannot create Action URL for metacard {}: Host name/IP not set.",
          LogSanitizer.sanitize(metacard.getId()));
      return null;
    }

    try {
      return getMetacardAction(getSource(metacard), metacard);
    } catch (Exception e) {
      LOGGER.debug(
          "Cannot create Action URL for metacard {}.", LogSanitizer.sanitize(metacard.getId()), e);
      return null;
    }
  }

  @Override
  public String getId() {
    return this.actionProviderId;
  }

  /**
   * Default implementation that ensures the {@code subject} provided is a {@link Metacard} before
   * delegating to {@link #canHandleMetacard(Metacard)}.
   *
   * <p>{@inheritDoc}
   */
  public <T> boolean canHandle(T subject) {
    return (subject instanceof Metacard)
        && isResourceMetacard((Metacard) subject)
        && canHandleMetacard((Metacard) subject);
  }

  /**
   * Determines if this {@link ActionProvider} can handle this {@link Metacard}. Returns {@code
   * true} by default. Should be overwritten if more complex verification is needed.
   *
   * @param metacard metacard to which this {@link ActionProvider} could apply
   * @return {@code true} if this {@link ActionProvider} applies to the {@link Metacard}
   */
  protected boolean canHandleMetacard(Metacard metacard) {
    return true;
  }

  @Override
  public String toString() {
    return String.format(
        "%s [%s], Impl=%s", ActionProvider.class.getName(), getId(), getClass().getName());
  }

  /**
   * Creates a new {@link Action} given a {@link Metacard}. This method delegates to {@link
   * #getMetacardActionUrl(String, Metacard)} and {@link #createMetacardAction(String, String,
   * String, URL)}.
   *
   * @param metacardSource source ID of the {@link Metacard}
   * @param metacard {@link Metacard} to create an {@link Action} from
   * @return new {@link Action} object. Cannot be {@code null}.
   * @throws Exception thrown if an error occurred while creating the {@link Action}
   */
  protected Action getMetacardAction(String metacardSource, Metacard metacard)
      throws MalformedURLException, URISyntaxException, UnsupportedEncodingException {
    URL url = getMetacardActionUrl(metacardSource, metacard);
    return createMetacardAction(actionProviderId, title, description, url);
  }

  /**
   * Factory method that creates the proper {@link Action} object from the information provided.
   * Must be implemented by sub-classes.
   *
   * @param actionProviderId {@link Action} ID
   * @param title {@link Action} title
   * @param description {@link Action} description
   * @param url {@link Action} url
   * @return new {@link Action} object. Cannot be {@code null}.
   */
  protected abstract Action createMetacardAction(
      String actionProviderId, String title, String description, URL url);

  /**
   * Gets the {@link URL} that will be used when the {@link Action} is created.
   *
   * @param metacardSource source ID of the {@link Metacard}
   * @param metacard {@link Metacard} for which a {@link URL} needs to be created
   * @return {@link URL} that will be used to create the {@link Action}
   * @throws Exception thrown if the {@link URL} couldn't be created
   */
  protected abstract URL getMetacardActionUrl(String metacardSource, Metacard metacard)
      throws MalformedURLException, URISyntaxException, UnsupportedEncodingException;

  private boolean isHostUnset(String host) {
    return (host == null || host.trim().equals(UNKNOWN_TARGET));
  }

  private String getSource(Metacard metacard) {

    if (StringUtils.isNotBlank(metacard.getSourceId())) {
      return metacard.getSourceId();
    }

    return SystemInfo.getSiteName();
  }

  private boolean isResourceMetacard(Metacard metacard) {
    return metacard.getTags() == null
        || metacard.getTags().isEmpty()
        || metacard.getTags().contains(Metacard.DEFAULT_TAG);
  }
}
