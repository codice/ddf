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

import ddf.catalog.util.Maskable;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A utility used to mask the source ID for all maskable items that have their own name (ID). Masker
 * enables DDF to specify a single name, the DDF's site name as specified in the DDF System
 * Settings, for the site name in all responses from connected sources and catalog providers, which
 * are both maskable items.
 */
public class Masker {

  private static final Logger LOGGER = LoggerFactory.getLogger(Masker.class);

  protected List<Maskable> maskees;

  private String mask;

  private boolean maskSet = false;

  /** Creates an empty list of Maskable services that this Masker will internally maintain. */
  public Masker() {
    maskees = new LinkedList<Maskable>();
  }

  /**
   * Set the id of all Maskable sources to be the specified input ID. Once this masked ID is set it
   * cannot be changed (except by changing the value of DDF's site name).
   *
   * @param id
   */
  public void setId(String id) {
    synchronized (this) {
      this.mask = id;
      this.maskSet = true;

      for (Maskable masked : maskees) {
        masked.maskId(id);
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Updating id for {} from {} to {}",
              masked.getClass().getName(),
              masked.getId(),
              this.mask);
        }
      }
    }
  }

  /**
   * Called by blueprint when a service is bound, this method sets the ID of the newly bound
   * Maskable service to the masked ID if the mask ID has been previously set, and adds the new
   * service to the internally maintained list of Maskable services.
   *
   * @param masked
   */
  public void bind(Maskable masked) {
    synchronized (this) {
      if (maskSet) {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug(
              "Updating id for {} from {} to {}",
              masked.getClass().getName(),
              masked.getId(),
              this.mask);
        }
        masked.maskId(this.mask);
      }
      maskees.add(masked);
    }
  }

  /**
   * Called by blueprint when a service is unbound, this method removes the unbound service from the
   * list of Maskable sources maintained internally by this Masker.
   *
   * @param masked
   */
  public void unbind(Maskable masked) {
    synchronized (this) {
      if (maskees.contains(masked)) {
        maskees.remove(masked);
      }
    }
  }
}
