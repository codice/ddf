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
package ddf.catalog.source;

import java.util.List;

/**
 * This class provides capabilities for a given source.
 *
 * @see SourceCapabilityRegistry
 */
public interface SourceCapabilityProvider {

  /**
   * @param source - the {@link Source} for which the {@link SourceCapabilityProvider} is requested
   *     to provide a list of strings representing capabilities
   * @return a {@link List<String>} of capabilities. If there are no capabilities associated with
   *     the source, then an empty List shall be returned
   */
  public List<String> getSourceCapabilities(Source source);

  /**
   * @return a unique identifier to distinguish the type of service this {@link
   *     SourceCapabilityProvider} provides
   */
  public String getId();
}
