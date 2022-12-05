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
package ddf.catalog.filter;

import org.opengis.filter.Filter;

/**
 * Builds XPath {@link Filter}s
 *
 * @author Michael Menousek
 */
@Deprecated
public interface XPathBuilder extends XPathBasicBuilder {

  /**
   * Builds a {@link Filter} that matches {@link ddf.catalog.data.Metacard}s where the XML node
   * indicated by the XPath exists. Searches across all {@link ddf.catalog.data.Metacard} {@link
   * ddf.catalog.data.Attribute}s of type {@link
   * ddf.catalog.data.AttributeType.AttributeFormat#XML}.
   *
   * @return {@link Filter} for indicated XPath
   */
  public Filter exists();

  /**
   * Continue building the {@link Filter} with an implied EQUALS
   *
   * @return {@link XPathBasicBuilder}, to continue building the {@link Filter}
   */
  public XPathBasicBuilder is();
}
