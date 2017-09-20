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
package ddf.security.samlp.impl;

import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

/**
 * Example:
 *
 * <pre>
 * {@Code
 *
 *    <type-converters>
 *      <bean id="relayStatesConverter" class="ddf.security.samlp.impl.RelayStatesConverter"/>
 *    </type-converters>
 *
 *    <bean id="relayStates" class="ddf.security.samlp.impl.RelayStates"/>
 *
 * }
 * </pre>
 */
public class RelayStatesConverter implements Converter {
  /**
   * Return if this converter is able to convert the specified object to the specified type.
   *
   * @param sourceObject The source object {@code s} to convert.
   * @param targetType The target type {@code T}.
   * @return {@code true} if the conversion is possible, {@code false} otherwise.
   */
  @Override
  public boolean canConvert(Object sourceObject, ReifiedType targetType) {
    return (sourceObject instanceof RelayStates);
  }

  /**
   * Convert the specified object to an instance of the specified type.
   *
   * @param sourceObject The source object {@code s} to convert.
   * @param targetType The target type {@code T}.
   * @return An instance with a type that is assignable from targetType's raw class
   * @throws Exception If the conversion cannot succeed. This exception should not be thrown when
   *     the {@link #canConvert(Object, ReifiedType) canConvert} method has returned {@code true}.
   */
  @Override
  public Object convert(Object sourceObject, ReifiedType targetType) throws Exception {
    return sourceObject;
  }
}
