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

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static org.codice.ddf.platform.util.JSONUtils.isJSONValid;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

public class JSONUtilsTest {

  private static final String VALID_JSON =
      "{ \"Key1\": value1,\"Key2\": {\"InsideKey\": \"Inside Value\"}}";

  private static final String INVALID_JSON_WITH_QUOTES =
      "{ \"\"Key1\"\": value1,\"Key2\": {\"InsideKey\": \"Inside Value\"}}";

  private static final String INVALID_JSON_MALFORMED =
      "{ \"Key1\": ,\"Key2\": {\"InsideKey\": \"Inside Value\"}}";

  @Test
  public void testValidJSON() {
    assertThat(isJSONValid(VALID_JSON), is(TRUE));
  }

  @Test
  public void testInvalidMalformedJSON() {
    assertThat(isJSONValid(INVALID_JSON_MALFORMED), is(FALSE));
  }

  @Test
  public void testInvalidQuotesJSON() {
    assertThat(isJSONValid(INVALID_JSON_WITH_QUOTES), is(FALSE));
  }
}
