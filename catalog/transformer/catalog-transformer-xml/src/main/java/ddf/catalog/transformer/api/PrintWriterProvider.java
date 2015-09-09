/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package ddf.catalog.transformer.api;

import java.io.Writer;

/**
 * PrintWriterProvider is an abstraction layer that supports the export of PrintWriter
 * across bundle boundaries.
 * <p>
 * Client code needs to get a unique print writer on a per-method invocation basis.
 */
public interface PrintWriterProvider<T, U extends PrintWriter> {

    U build(Writer writer, Class<T> klass);

}
