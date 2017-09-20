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

import org.osgi.service.blueprint.container.Converter;
import org.osgi.service.blueprint.container.ReifiedType;

/**
 * This converter is used to allow {@link ServiceSelector} objects to pass through for {@link
 * java.util.List} implementations. This was originally intended to allow plugins to be
 * automatically sorted in the list. Without this converter, blueprint will copy the list and lose
 * the reference.
 */
public class ServiceSelectorConverter implements Converter {

  /**
   * @parameter sourceObject object considering to be converted
   * @parameter targetType
   * @return true if sourceObject is an instance of ServiceSelector; false otherwise
   */
  @Override
  public boolean canConvert(Object sourceObject, ReifiedType targetType) {
    return (sourceObject instanceof ServiceSelector);
  }

  /**
   * Converts (casts) the sourceObject to a ServiceSelector.
   *
   * @parameter sourceObject object being converted
   * @parameter targetType
   * @return sourceObject cast to a ServiceSelector
   */
  @Override
  public Object convert(Object sourceObject, ReifiedType targetType) throws Exception {
    return sourceObject;
  }
}
