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
package ddf.action;

import java.util.List;

/**
 * This class provides an {@link Action} for a given subject. Objects that the {@link
 * ActionProvider} can handle are not restricted to a particular class and can be whatever the
 * {@link ActionProvider} is able to handle. <br>
 *
 * @see Action
 * @see ActionRegistry
 *     <p><b> This code is experimental. While this interface is functional and tested, it may
 *     change or be removed in a future version of the library. </b>
 */
@Deprecated
public interface MultiActionProvider {

  /**
   * Assumes that {@link #canHandle(Object)} for the {@param subject} has already been checked.
   *
   * @param subject object for which the {@link ActionProvider} is requested to provide an {@link
   *     Action}
   * @return an {@link Action} object. If no action can be taken on the input, then <code>
   *     Collections.emptyList()</code> shall be returned
   */
  <T> List<Action> getActions(T subject);

  /**
   * @return a unique identifier to distinguish the type of service this {@link ActionProvider}
   *     provides
   */
  String getId();

  /**
   * Checks if an {@link ActionProvider} supports a given subject.
   *
   * @param subject the input to check
   * @return true if is supported, false otherwise.
   */
  <T> boolean canHandle(T subject);
}
