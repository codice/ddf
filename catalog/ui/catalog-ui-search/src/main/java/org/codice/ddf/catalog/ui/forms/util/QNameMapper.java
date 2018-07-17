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
package org.codice.ddf.catalog.ui.forms.util;

import javax.xml.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility for mapping an object type to a QName and vice-versa. */
public class QNameMapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(QNameMapper.class);

  private static final QName BOOLEAN = new QName("http://www.w3.org/2001/XMLSchema", "boolean");

  private static final QName STRING = new QName("http://www.w3.org/2001/XMLSchema", "string");

  private static final QName INTEGER = new QName("http://www.w3.org/2001/XMLSchema", "integer");

  public static Object convert(Object object, QName qName) {
    if (INTEGER.equals(qName)) {
      return Integer.valueOf(object.toString());
    } else if (BOOLEAN.equals(qName)) {
      return Boolean.valueOf(object.toString());
    } else if (STRING.equals(qName)) {
      return object.toString();
    }

    LOGGER.debug(
        "Unrecognized QName. Falling back to returning the object as a string. qName={}", qName);
    return object.toString();
  }

  public static QName convert(Object object) {
    if (object instanceof Integer) {
      return INTEGER;
    } else if (object instanceof Boolean) {
      return BOOLEAN;
    } else if (object instanceof String) {
      return STRING;
    }

    LOGGER.debug(
        "Unrecognized object type. Falling back to returning a string QName. type={}",
        object.getClass().getName());
    return STRING;
  }

  private QNameMapper() {}
}
