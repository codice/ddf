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
package ddf.catalog.transformer.api;

import com.thoughtworks.xstream.io.ExtendedHierarchicalStreamWriter;

/** PrintWriter writes String values to a Stream for XML output. */
public interface PrintWriter extends ExtendedHierarchicalStreamWriter {

  /**
   * Write text without any escaping.
   *
   * @param text the string to this printwriter should write.
   */
  void setRawValue(String text);

  /**
   * Get string representation of PrintWriter.
   *
   * @return String the string representation of this PrintWriter.
   */
  String makeString();
}
