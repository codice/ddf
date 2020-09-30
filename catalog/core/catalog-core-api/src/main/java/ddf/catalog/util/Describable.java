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
package ddf.catalog.util;

/**
 * Describable is used to capture a basic description. For an example of a how the Describable
 * interface is used view the {@link ddf.catalog.source.Source} interface and the {@link
 * DescribableImpl} class.
 *
 * @deprecated see {@link org.codice.ddf.platform.services.common.Describable}
 */
@Deprecated
@SuppressWarnings(
    "squid:S2176" /* Intentionally shadowing superclass name as deprecation strategy */)
public interface Describable extends org.codice.ddf.platform.services.common.Describable {

  /**
   * Retrieve the version.
   *
   * @return the version of the item being described (example: 1.0)
   */
  @Override
  public String getVersion();

  /**
   * Returns the name, aka ID, of the describable item. The name should be unique for each
   * instance. <br/>
   * Example:
   * <code>html<code> for a {@link ddf.catalog.transform.MetacardTransformer} that transforms {@link ddf.catalog.data.Metacard}s to HTML
   *
   * @return ID of the item
   */
  @Override
  public String getId();

  /**
   * Returns the title of the describable item. It is generally more verbose than the name (aka ID).
   *
   * @return title of the item (example: Dummy Catalog Provider)
   */
  @Override
  public String getTitle();

  /**
   * Returns a description of the describable item.
   *
   * @return description of the item (example: Provider that returns back static results)
   */
  @Override
  public String getDescription();

  /**
   * Returns the organization associated with the describable item.
   *
   * @return organizational name or acronym (example: USAF)
   */
  @Override
  public String getOrganization();
}
