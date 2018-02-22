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
package org.codice.ddf.admin.application.plugin;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** @deprecated going away in a future release. Defines an application configuration plugin. */
@Deprecated
public interface ApplicationPlugin {
  /** key to mark that a plugin should be used for ALL applications. */
  public static final String ALL_ASSOCATION_KEY = "ALL";
  /** key for the display name. Used for creating json. */
  public static final String DISPLAY_NAME_KEY = "displayName";
  /** key for the application name. Used for creating json. */
  public static final String APPLICATION_ASSOCIATION_KEY = "applicationAssociation";
  /** key for the iframe location. Used for creating json. */
  public static final String IFRAME_LOCATION_KEY = "iframeLocation";
  /** key for the id location. Used for creating json. */
  public static final String ID_KEY = "id";

  /**
   * Returns a list of applications that this plugin should be associated with.
   *
   * @return a list of applications that this plugin should be associated with.
   */
  @Deprecated
  public List<String> getAssocations();

  /**
   * Returns the display name. This is the value that will be display to the user.
   *
   * @return the display name.
   */
  @Deprecated
  public String getDisplayName();

  /**
   * Returns the id of this plugin. This is an unique identifier for the front end javascript.
   *
   * @return a unique identifier for this plugin as a uuid.
   */
  @Deprecated
  public UUID getID();

  /**
   * Returns the iframe location. Can be null.
   *
   * @return the iframe location.
   */
  @Deprecated
  public String getIframeLocation();

  /**
   * Utility method that will handle the conversion of this object to something jolokia can convert
   * to json.
   *
   * @return a constructed map that jolokia can convert to json.
   */
  @Deprecated
  public Map<String, Object> toJSON();

  /**
   * Handles figuring out if this plugin is matching to the app name sent in. This will handle the
   * case where a plugin should be used for all.
   *
   * @param appName - the name of the application we are going to test.
   * @return yes if the application matches, or should be applied to all applications, false if it
   *     doesn't.
   */
  @Deprecated
  public boolean matchesAssocationName(String assocationName);

  /**
   * Sets the application assocations to the inputted values. This will overwrite all previous
   * values.
   *
   * @param appName - the string name of an application.
   */
  @Deprecated
  public void setAssociations(List<String> assocations);

  /**
   * Adds an application assocation list to the existing list. This does not overwrite the previous
   * values, and if there is an existing value it wont add it.
   *
   * @param applicationAssociations
   */
  @Deprecated
  public void addAssocations(List<String> assocations);

  /**
   * Adds a single application association to this plugin. If the application is already there, then
   * nothing will happen.
   *
   * @param applicationAssocation - the string name of the application.
   */
  @Deprecated
  public void addAssociations(String assocations);
}
