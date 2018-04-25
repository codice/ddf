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
package ddf.catalog.source.impl;

import ddf.action.Action;
import ddf.catalog.data.ContentType;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.util.impl.DescribableImpl;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.codice.ddf.platform.util.DateUtils;

/** The Class SourceDescriptorImpl is the default representation of a {@link SourceDescriptor}. */
public class SourceDescriptorImpl extends DescribableImpl implements SourceDescriptor {

  protected String sourceId = null;

  /** The content types contained in this source */
  protected Set<ContentType> catalogedTypes = null;

  protected boolean isAvailable = false;

  protected Date lastAvailableDate = null;

  private List<Action> actions;

  /**
   * Instantiates a new SourceDescriptorImpl.
   *
   * @param sourceId the source's id
   * @param catalogedTypes the cataloged types
   * @param actions list of actions
   */
  public SourceDescriptorImpl(
      String sourceId, Set<ContentType> catalogedTypes, List<Action> actions) {
    this.sourceId = sourceId;
    this.catalogedTypes = catalogedTypes;
    this.actions = actions;
  }

  @Override
  public String getSourceId() {
    return sourceId;
  }

  /**
   * Sets the source id.
   *
   * @param siteName the sourceId
   */
  public void setSiteName(String siteName) {
    this.sourceId = siteName;
  }

  @Override
  public Set<ContentType> getContentTypes() {
    return catalogedTypes;
  }

  /**
   * Sets the content types.
   *
   * @param catalogedTypes the new content types
   */
  public void setContentTypes(Set<ContentType> catalogedTypes) {
    this.catalogedTypes = catalogedTypes;
  }

  @Override
  public boolean isAvailable() {
    return isAvailable;
  }

  /**
   * Sets the available.
   *
   * @param isAvailable the new available
   */
  public void setAvailable(boolean isAvailable) {
    this.isAvailable = isAvailable;
  }

  @Override
  public Date getLastAvailabilityDate() {
    return DateUtils.copy(lastAvailableDate);
  }

  @Override
  public List<Action> getActions() {
    return actions;
  }

  /**
   * Sets the last availability date.
   *
   * @param lastAvailableDate the new last availability date
   */
  public void setLastAvailabilityDate(Date lastAvailableDate) {
    this.lastAvailableDate = DateUtils.copy(lastAvailableDate);
  }
}
