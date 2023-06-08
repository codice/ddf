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
package ddf.catalog.plugin;

import ddf.catalog.operation.SourceInfoResponse;
import java.util.Set;

/**
 * Provides a plugin point for manipulating the framework source responses for getSourceIds and
 * getSourceInfo methods
 */
public interface SourcePlugin {

  /**
   * Plugin point for changing the list of source ids return from the catalog frameworks
   * getSourceIds method
   *
   * @param sourceIds The sources ids to process from the previous SourcePlugin
   * @return The processed set of source ids to pass to the next SourcePlugin
   */
  Set<String> processSourceIds(Set<String> sourceIds);

  /**
   * Plugin point for modifying the SourceInfoResponse return from the catalog frameworks
   * getSourceInfo method
   *
   * @param sourceInfoResponse The SourceInfoResponse from the previous SourcePlugin
   * @return The processed SourceInfoResponse to pass to the next SourcePlugin
   */
  SourceInfoResponse processSourceInfo(SourceInfoResponse sourceInfoResponse);
}
