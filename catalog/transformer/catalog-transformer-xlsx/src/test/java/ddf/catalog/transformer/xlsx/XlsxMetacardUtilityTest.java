/*
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
package ddf.catalog.transformer.xlsx;

import static com.google.common.net.MediaType.OOXML_SHEET;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.util.Collections;
import java.util.UUID;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.junit.Test;

public class XlsxMetacardUtilityTest {

  @Test
  public void testEmptyMetacardList() {
    BinaryContent binaryContent = XlsxMetacardUtility.buildSpreadSheet(Collections.emptyList());

    assertThat(binaryContent, nullValue());
  }

  @Test
  public void testContentReturnType() {
    Metacard metacard = new MetacardImpl();

    BinaryContent binaryContent =
        XlsxMetacardUtility.buildSpreadSheet(Collections.singletonList(metacard));

    MimeType mimeType = new MimeType();
    try {
      mimeType.setPrimaryType(OOXML_SHEET.type());
      mimeType.setSubType(OOXML_SHEET.subtype());
    } catch (MimeTypeParseException e) {
      throw new ExceptionInInitializerError(e);
    }

    MimeType returnedType = binaryContent.getMimeType();

    assertThat(returnedType.toString(), is(mimeType.toString()));
  }

  @Test
  public void testNullMetacardAttribute() {
    Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Metacard.CONTENT_TYPE, (Serializable) null));

    BinaryContent binaryContent =
        XlsxMetacardUtility.buildSpreadSheet(Collections.singletonList(metacard));

    assertThat(binaryContent, notNullValue());
  }

  @Test
  public void testNonNullMetacardAttribute() {
    Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Metacard.ID, UUID.randomUUID()));

    BinaryContent binaryContent =
        XlsxMetacardUtility.buildSpreadSheet(Collections.singletonList(metacard));

    assertThat(binaryContent, notNullValue());
  }

  @Test
  public void testMultiValueMetacardAttribute() {
    Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Core.LANGUAGE, new String[] {"english", "spanish"}));

    BinaryContent binaryContent =
        XlsxMetacardUtility.buildSpreadSheet(Collections.singletonList(metacard));

    assertThat(binaryContent, notNullValue());
  }
}
