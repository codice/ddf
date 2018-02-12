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
package org.codice.ddf.spatial.geocoder.endpoint;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JsonpValidatorTest {
  private String inputJsonp;

  private Boolean expectedResult;

  public JsonpValidatorTest(String inputJsonp, Boolean expectedResult) {
    this.inputJsonp = inputJsonp;
    this.expectedResult = expectedResult;
  }

  @Parameterized.Parameters
  public static Collection testValues() {
    return Arrays.asList(
        new Object[][] {
          {"", false},
          {".", false},
          {"...", false},
          {"foo..bar", false},
          {"foo", true},
          {"foo.bar", true},
          {"123", false},
          {"foo123", true},
          {"foo.bar.123", false},
          {"test()", false},
          {"a-b", false},
          {"$", true},
          {"$123", true},
          {"foo$bar", true},
          {"_foo", true},
          {"foo_bar", true},
          {"$.callback", true},
          {"_.callback", true},
          {" foobar", false},
          {"function", false},
          {"(function xss(x){evil()})", false},
          {"foo.function.bar", false},
          {"$.ajaxHandler", true},
          {"$.123", false},
          {"array_of_functions[42]", true},
          {"array_of_functions[42][54]", true},
          {"array_of_functions[]", false},
          {"array_of_functions[\"key\"]", true},
          {"$.ajaxHandler[42][54].foo", true},
          {":badFunction", false}
        });
  }

  @Test
  public void testIsValidJSONP() {
    assertEquals(expectedResult, JsonpValidator.isValidJsonp(inputJsonp));
  }
}
