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
package org.codice.ddf.transformer.xml.streaming.lib;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

public class InputTransformerErrorHandlerTest {

  private InputTransformerErrorHandler inputTransformerErrorHandler;

  private SAXParseException saxParseException;

  @Test
  public void testNormal() throws SAXException {
    inputTransformerErrorHandler =
        new InputTransformerErrorHandler().configure(new StringBuilder());
    saxParseException = new SAXParseException("Test", "publicId", "systemId", 0, 0);

    inputTransformerErrorHandler.warning(saxParseException);
    assertThat(
        inputTransformerErrorHandler.getParseWarningsErrors(),
        is("Warning: URI=systemId Line=0: Test"));

    inputTransformerErrorHandler.error(saxParseException);
    assertThat(
        inputTransformerErrorHandler.getParseWarningsErrors(),
        is("Error: URI=systemId Line=0: Test"));

    inputTransformerErrorHandler.warning(saxParseException);
    inputTransformerErrorHandler.error(saxParseException);
    assertThat(
        inputTransformerErrorHandler.getParseWarningsErrors(),
        is("Warning: URI=systemId Line=0: Test\nError: URI=systemId Line=0: Test"));
  }
}
