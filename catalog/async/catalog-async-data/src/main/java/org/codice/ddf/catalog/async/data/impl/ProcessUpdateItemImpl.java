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
package org.codice.ddf.catalog.async.data.impl;

import static org.apache.commons.lang.Validate.notNull;

import ddf.catalog.data.Metacard;
import javax.annotation.Nullable;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResource;
import org.codice.ddf.catalog.async.data.api.internal.ProcessUpdateItem;

public class ProcessUpdateItemImpl extends ProcessItemImpl implements ProcessUpdateItem {

  private ProcessResource processResource;

  private Metacard oldMetacard;

  private boolean isMetacardModified;

  /**
   * Creates a {@link ProcessUpdateItem} with {@link #isMetacardModified} defaulted to {@code true}.
   *
   * @param processResource {@link ProcessResource} associated with this {@link ProcessUpdateItem},
   *     can be null
   * @param newMetacard non null {@link Metacard} that represents the metacard after updates
   * @param oldMetacard non null {@link Metacard} that represents the original metacard before
   *     updates
   */
  public ProcessUpdateItemImpl(
      @Nullable ProcessResource processResource, Metacard newMetacard, Metacard oldMetacard) {
    this(processResource, newMetacard, oldMetacard, true);
  }

  /**
   * Creates a {@link ProcessUpdateItem} with {@link #isMetacardModified} defaulted to {@code true}.
   *
   * @param processResource {@link ProcessResource} associated with this {@link ProcessUpdateItem},
   *     can be null
   * @param newMetacard non null {@link Metacard} that represents the metacard after updates
   * @param oldMetacard non null {@link Metacard} that represents the original metacard before
   *     updates
   * @param isMetacardModified {@code true} if updates are required to be sent back to the Catalog
   *     {@code false} otherwise.
   */
  public ProcessUpdateItemImpl(
      @Nullable ProcessResource processResource,
      Metacard newMetacard,
      Metacard oldMetacard,
      boolean isMetacardModified) {
    super(newMetacard);

    notNull(oldMetacard, "ProcessUpdateItemImpl argument newMetacard may not be null");

    this.processResource = processResource;
    this.oldMetacard = oldMetacard;
    this.isMetacardModified = isMetacardModified;
  }

  @Override
  public Metacard getOldMetacard() {
    return oldMetacard;
  }

  @Override
  @Nullable
  public ProcessResource getProcessResource() {
    return processResource;
  }

  @Override
  public boolean isMetacardModified() {
    return isMetacardModified;
  }

  public void markMetacardAsModified() {
    isMetacardModified = true;
  }
}
