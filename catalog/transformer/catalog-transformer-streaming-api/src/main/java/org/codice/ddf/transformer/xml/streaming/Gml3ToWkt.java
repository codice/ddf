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
package org.codice.ddf.transformer.xml.streaming;

import ddf.catalog.validation.ValidationException;
import java.io.InputStream;

public interface Gml3ToWkt {

  /**
   * Convert a GML3 XML snippet String to a WKT String
   *
   * @param xml String of GML XML
   * @return a WKT String representation of the input
   */
  String convert(String xml) throws ValidationException;

  /**
   * Convert a GML3 XML snippet Input Stream to a WKT String
   *
   * @param xml an Input Stream of GML XML
   * @return a WKT String representation of the input
   */
  String convert(InputStream xml) throws ValidationException;

  /**
   * Parses XML through the Geotools parser
   *
   * @param xml InputStream of GML XML
   * @return Object parsed from the Geotools parser
   */
  Object parseXml(InputStream xml) throws ValidationException;
}
