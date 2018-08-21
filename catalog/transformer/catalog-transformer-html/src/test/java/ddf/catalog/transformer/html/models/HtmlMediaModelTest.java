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
package ddf.catalog.transformer.html.models;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Test;

public class HtmlMediaModelTest {

  @Test
  public void testNullRawData() {
    HtmlMediaModel mediaModel = new HtmlMediaModel(null);
    assertNull(mediaModel.getValue());
  }

  @Test
  public void testValidRawData() {
    String data = "Hello World";
    byte[] rawData = data.getBytes(StandardCharsets.UTF_8);
    byte[] encoded = Base64.getEncoder().encode(rawData);

    HtmlMediaModel mediaModel = new HtmlMediaModel(rawData);
    assertThat(mediaModel.getValue(), is(new String(encoded)));
  }
}
