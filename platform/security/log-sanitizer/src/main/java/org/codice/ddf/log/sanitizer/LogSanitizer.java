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
package org.codice.ddf.log.sanitizer;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

public class LogSanitizer {

  private final Object object;

  public LogSanitizer(Object object) {
    this.object = object;
  }

  public static LogSanitizer sanitize(Object object) {
    return new LogSanitizer(object);
  }

  @Override
  public String toString() {
    if (object == null) {
      return "";
    }
    final String objStr = object.toString();
    if (StringUtils.isEmpty(objStr)) {
      // Explicitly log the empty string for null or empty values coming out of toString
      return "";
    }
    return StringEscapeUtils.escapeHtml(objStr.replace('\n', '_').replace('\r', '_'));
  }
}
