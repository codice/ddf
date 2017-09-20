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
package ddf.catalog.transformer.xml;

import ddf.catalog.data.Metacard;
import ddf.catalog.transformer.api.PrintWriter;
import ddf.catalog.transformer.api.PrintWriterProvider;
import java.io.StringWriter;

public class PrintWriterProviderImpl implements PrintWriterProvider {

  private static final int INITIAL_SIZE = 1024;

  @Override
  public <T> PrintWriter build(Class<T> klass) {

    if (Metacard.class.equals(klass)) {
      return new EscapingPrintWriter(new StringWriter(INITIAL_SIZE));
    } else {
      throw new IllegalArgumentException("No PrintWriter for " + klass.getCanonicalName());
    }
  }
}
