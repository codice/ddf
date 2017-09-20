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
package org.codice.ddf.registry.rest.endpoint;

import com.github.jknack.handlebars.Options;
import com.google.common.base.Objects;
import java.io.IOException;

public class HandlebarsHelper {

  public CharSequence ifeq(final Object value1, final Options options) throws IOException {
    Object value2 = options.param(0);
    return Objects.equal(value1, value2) ? options.fn() : options.inverse();
  }
}
