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
package org.codice.ddf.platform.util;

import com.google.gson.JsonParser;
import  com.google.gson.JsonSyntaxException;

public class JSONUtils {

  /**
   * Verifies the Syntax of a JSON
   *
   * @param jsonInString JSON String to check for validation
   * @return Boolean results of the validation check
   */
  public static boolean isJSONValid(String jsonInString) {

    JsonParser parser = new JsonParser();
    try {
      parser.parse(jsonInString);
      return true;
    } catch (JsonSyntaxException ex) {
      return false;
    }
  }
}
