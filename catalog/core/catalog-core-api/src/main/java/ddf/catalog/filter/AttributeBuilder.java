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

/**
 * Starts the fluent API to create {@link org.geotools.api.filter.Filter} based on a particular
 * {@link ddf.catalog.data.Attribute}
 *
 * @author Michael Menousek
 */
public interface AttributeBuilder extends ExpressionBuilder {

  /**
   * Continue building the {@link org.geotools.api.filter.Filter} with an implied equality operator.
   * Also used for syntactic completeness (readability).
   *
   * @return ExpressionBuilder to continue building this {@link org.geotools.api.filter.Filter}
   */
  public abstract ExpressionBuilder is();
}
