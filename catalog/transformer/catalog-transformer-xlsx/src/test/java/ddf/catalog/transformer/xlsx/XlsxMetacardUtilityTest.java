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

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.Test;

public class XlsxMetacardUtilityTest {

  @Test
  public void testEmptyMetacardList() {
    BinaryContent binaryContent =
        XlsxMetacardUtility.buildSpreadSheet(
            Collections.emptyList(), Collections.emptyMap(), Collections.emptyList());

    assertThat(binaryContent, nullValue());
  }

  @Test
  public void testContentReturnType() {
    Metacard metacard = new MetacardImpl();

    BinaryContent binaryContent =
        XlsxMetacardUtility.buildSpreadSheet(
            Collections.singletonList(metacard), Collections.emptyMap(), Collections.emptyList());

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
        XlsxMetacardUtility.buildSpreadSheet(
            Collections.singletonList(metacard), Collections.emptyMap(), Collections.emptyList());

    assertThat(binaryContent, notNullValue());
  }

  @Test
  public void testNonNullMetacardAttribute() {
    Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Metacard.ID, UUID.randomUUID().toString()));

    BinaryContent binaryContent =
        XlsxMetacardUtility.buildSpreadSheet(
            Collections.singletonList(metacard), Collections.emptyMap(), Collections.emptyList());

    assertThat(binaryContent, notNullValue());
  }

  @Test
  public void testMultiValueMetacardAttribute() {
    Metacard metacard = new MetacardImpl();

    metacard.setAttribute(
        AttributeImpl.fromMultipleValues(Core.LANGUAGE, Arrays.asList("english", "spanish")));

    BinaryContent binaryContent =
        XlsxMetacardUtility.buildSpreadSheet(
            Collections.singletonList(metacard), Collections.emptyMap(), Collections.emptyList());

    assertThat(binaryContent, notNullValue());
  }

  @Test
  public void testAliasMapping() {
    Metacard metacard = new MetacardImpl();

    Workbook workbook = new XSSFWorkbook();
    Sheet sheet =
        XlsxMetacardUtility.writeHeaderRow(
            metacard.getMetacardType().getAttributeDescriptors().stream()
                .filter(desc -> "id".equalsIgnoreCase(desc.getName()))
                .collect(Collectors.toList()),
            Collections.singletonMap("id", "ALIAS"),
            workbook);

    Row header = sheet.getRow(0);
    assertThat(header, notNullValue());
    Cell id = header.getCell(0);
    assertThat(id.getStringCellValue(), is("ALIAS"));
  }

  @Test
  public void testColumnOrder() {
    Metacard metacard = new MetacardImpl();

    Workbook workbook = new XSSFWorkbook();
    List<AttributeDescriptor> orderedDescriptors =
        metacard.getMetacardType().getAttributeDescriptors().stream()
            .sorted((desc1, desc2) -> desc1.getName().compareTo(desc2.getName()))
            .collect(Collectors.toList());
    Sheet sheet =
        XlsxMetacardUtility.writeHeaderRow(orderedDescriptors, Collections.emptyMap(), workbook);

    Row header = sheet.getRow(0);
    assertThat(header, notNullValue());
    for (int i = 0; i < orderedDescriptors.size(); i++) {
      assertThat(header.getCell(i).getStringCellValue(), is(orderedDescriptors.get(i).getName()));
    }
  }
}
