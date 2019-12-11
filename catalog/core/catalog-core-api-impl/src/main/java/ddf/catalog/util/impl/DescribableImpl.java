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
package ddf.catalog.util.impl;

import ddf.catalog.util.Describable;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of the Describable interface, providing basic setter/getter methods for a
 * describable item's ID, title, version, organization, and description.
 */
public abstract class DescribableImpl implements Describable {

  private static final Logger LOGGER = LoggerFactory.getLogger(DescribableImpl.class);

  private String version = null;

  private String id = null;

  private String title = null;

  private String description = null;

  private String organization = null;

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.util.Describable#getVersion()
   */
  @Override
  public String getVersion() {
    return version;
  }

  /**
   * Sets the version of the describable item.
   *
   * @param version
   */
  public void setVersion(String version) {
    LOGGER.debug("Setting version = {}", LogSanitizer.sanitize(version));
    this.version = version;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.util.Describable#getId()
   */
  @Override
  public String getId() {
    return id;
  }

  /**
   * Sets the ID of the describable item.
   *
   * @param id
   */
  public void setId(String id) {
    LOGGER.debug("ENTERING: setId - id = {}", LogSanitizer.sanitize(id));
    this.id = id;
    LOGGER.debug("EXITING: setId");
  }

  /**
   * @deprecated
   * @param shortname
   */
  public void setShortname(String shortname) {
    this.id = shortname;
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.util.Describable#getTitle()
   */
  @Override
  public String getTitle() {
    return title;
  }

  /**
   * Sets the title of the describable item.
   *
   * @param title
   */
  public void setTitle(String title) {
    LOGGER.debug("ENTERING: setTitle");
    this.title = title;
    LOGGER.debug("EXITING: setTitle");
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.util.Describable#getDescription()
   */
  @Override
  public String getDescription() {
    return description;
  }

  /**
   * Sets the description of the describable item.
   *
   * @param description
   */
  public void setDescription(String description) {
    LOGGER.debug("ENTERING: setDescription");
    this.description = description;
    LOGGER.debug("EXITING: setDescription");
  }

  /*
   * (non-Javadoc)
   *
   * @see ddf.catalog.util.Describable#getOrganization()
   */
  @Override
  public String getOrganization() {
    return organization;
  }

  /**
   * Sets the organization of the describable item.
   *
   * @param organization
   */
  public void setOrganization(String organization) {
    LOGGER.debug("ENTERING: setOrganization");
    LOGGER.debug("Setting organization = {}", LogSanitizer.sanitize(organization));
    this.organization = organization;
    LOGGER.debug("EXITING: setOrganization");
  }
}
