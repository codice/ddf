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
package ddf.catalog.operation;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;

/**
 * Interface representing a request to update a Metacard.  Requests may be batched, but each
 * Metacard/Attribute pair will only update a single Metacard.
 *
 * @see ddf.catalog.source.CatalogProvider
 */
public interface UpdateRequest extends Request {

  /**
   * Shortcut for the {@link ddf.catalog.data.Attribute} name used for updating {@link Metacard}s by
   * ID.
   *
   * @see Core#ID
   */
  public static final String UPDATE_BY_ID = Core.ID;

  /**
   * Shortcut for the {@link ddf.catalog.data.Attribute} name used for updating {@link Metacard}s by
   * the {@link String} value of the {@link Metacard}'s {@link Core#RESOURCE_URI Resource URI}.
   *
   * @see Core#RESOURCE_URI
   */
  public static final String UPDATE_BY_PRODUCT_URI = Core.RESOURCE_URI;

  /**
   * The attribute name tells the {@link ddf.catalog.source.CatalogProvider} what type of attribute
   * values are the {@link Entry} keys in the update list. For instance, if the attribute name was
   * "id," then the {@link ddf.catalog.source.CatalogProvider} would know that the {@link Entry}
   * keys in the update list ( {@code List<Entry<Serializable, Metacard>>}) were id values (such as
   * {@code 575aa9625fa24b338bd3c439f2613709}) and could create the appropriate backend search.
   *
   * @return the name of the attribute, the attribute name must correspond to an {@link
   *     ddf.catalog.data.Attribute} that has an ddf.catalog.data.AttributeType.AttributeFormat of
   *     {@link ddf.catalog.data.AttributeType.AttributeFormat#STRING}
   */
  public String getAttributeName();

  /**
   * Get the updates to be made. Any {@link Metacard}s that have an {@link
   * ddf.catalog.data.Attribute#getName()} that matches the name returned by {@link
   * #getAttributeName()} and a {@link ddf.catalog.data.Attribute#getValue()} that matches the
   * {@link Serializable} value in an {@link Entry} of this {@link List} will be updated with the
   * value of the associated {@link Metacard} in the {@link List}. An entry's key in the {@code
   * List} must match zero metacards or one metacard in the Source.
   *
   * @return List - pairs of {@link ddf.catalog.data.Attribute} values and associated new {@link
   *     Metacard}s to update if a match is found.
   */
  public List<Entry<Serializable, Metacard>> getUpdates();
}
