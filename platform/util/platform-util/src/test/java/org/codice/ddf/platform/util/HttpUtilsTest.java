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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import org.junit.Test;

public class HttpUtilsTest {

  @Test
  public void testStripQueryStringValidUrl() {
    assertThat(HttpUtils.stripQueryString("http://localhost"), is("http://localhost"));
    assertThat(HttpUtils.stripQueryString("http://localhost?wsdl"), is("http://localhost"));
    assertThat(
        HttpUtils.stripQueryString("http://localhost/path?wsdl"), is("http://localhost/path"));
    assertThat(HttpUtils.stripQueryString("http://localhost:3000"), is("http://localhost:3000"));
    assertThat(
        HttpUtils.stripQueryString("http://localhost:3000?wsdl"), is("http://localhost:3000"));
    assertThat(
        HttpUtils.stripQueryString("http://localhost:3000/path?wsdl"),
        is("http://localhost:3000/path"));

    assertThat(HttpUtils.stripQueryString("http://localhost?wsdl?wsdl"), is("http://localhost"));
    assertThat(
        HttpUtils.stripQueryString("http://localhost:3000?wsdl?wsdl"), is("http://localhost:3000"));
  }

  @Test
  public void testStripQueryStringInvalidUrl() {
    assertThat(HttpUtils.stripQueryString("abc"), is("abc"));
    assertThat(HttpUtils.stripQueryString("abc?123"), is("abc?123"));
  }
}
