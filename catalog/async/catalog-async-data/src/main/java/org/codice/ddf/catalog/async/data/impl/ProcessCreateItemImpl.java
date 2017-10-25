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

import ddf.catalog.data.Metacard;
import javax.annotation.Nullable;
import org.codice.ddf.catalog.async.data.api.internal.ProcessCreateItem;
import org.codice.ddf.catalog.async.data.api.internal.ProcessResource;

public class ProcessCreateItemImpl extends ProcessItemImpl implements ProcessCreateItem {

  private ProcessResource processResource;

  private boolean isMetacardModified;

  /**
   * Creates a {@link ProcessCreateItem} with {@link #isMetacardModified} set to {@code true}.
   *
   * @param processResource {@link ProcessResource} of this {@code ProcessCreateItemImpl}, can be
   *     null
   * @param metacard non null {@link Metacard} of this {@code ProcessCreateItem}
   */
  public ProcessCreateItemImpl(@Nullable ProcessResource processResource, Metacard metacard) {
    this(processResource, metacard, true);
  }

  /**
   * Creates a {@link ProcessCreateItem}.
   *
   * @param processResource {@link ProcessResource} of this {@code ProcessCreateItemImpl}, can be
   *     null
   * @param metacard non null {@link Metacard} of this {@code ProcessCreateItem}
   * @param isMetacardModified {@code true} if updates are required to be sent back to the Catalog
   *     {@code false} otherwise.
   */
  public ProcessCreateItemImpl(
      @Nullable ProcessResource processResource, Metacard metacard, boolean isMetacardModified) {
    super(metacard);
    this.processResource = processResource;
    this.isMetacardModified = isMetacardModified;
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

  @Override
  public void markMetacardAsModified() {
    isMetacardModified = true;
  }
}
