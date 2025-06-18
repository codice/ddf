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

import org.geotools.api.filter.Filter;

/**
 * Completes building a {@link Filter} based on Contextual relevance
 *
 * @author Michael Menousek
 */
public interface ContextualExpressionBuilder {

  /**
   * Creates a case-insensitive contextual {@link Filter}
   *
   * <p>Special Characters:
   *
   * <ul>
   *   <li>Wildcard (match one to many characters): "*"
   *   <li>Single Character (match exactly one character): "_"
   *   <li>Escape Character (to include a special character in a filter): "'"
   * </ul>
   *
   * @param text - contextual query
   * @return {@link Filter} - case-insensitive {@link Filter}
   */
  public Filter text(String text);

  /**
   * Creates a case-insensitive contextual {@link Filter} that will also include relevant matches
   * that do not precisely match
   *
   * <p>Special Characters:
   *
   * <ul>
   *   <li>Wildcard (match one to many characters): "*"
   *   <li>Single Character (match exactly one character): "_"
   *   <li>Escape Character (to include a special character in a filter): "'"
   * </ul>
   *
   * @param text - contextual query
   * @return {@link Filter} - case-insensitive {@link Filter}
   */
  public Filter fuzzyText(String text);

  /**
   * Creates a case-sensitive contextual {@link Filter}
   *
   * <p>Special Characters:
   *
   * <ul>
   *   <li>Wildcard (match one to many characters): "*"
   *   <li>Single Character (match exactly one character): "_"
   *   <li>Escape Character (to include a special character in a filter): "'"
   * </ul>
   *
   * @param text - contextual query
   * @return {@link Filter} - case-sensitive {@link Filter}
   */
  public Filter caseSensitiveText(String text);
}
