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

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class XlsxMetacardUtility {

  private static final Logger LOGGER = LoggerFactory.getLogger(XlsxMetacardUtility.class);

  private static MimeType mimeType = new MimeType();

  static {
    try {
      mimeType.setPrimaryType(OOXML_SHEET.type());
      mimeType.setSubType(OOXML_SHEET.subtype());
    } catch (MimeTypeParseException e) {
      LOGGER.warn("Failure creating MIME type", e);
      throw new ExceptionInInitializerError(e);
    }
  }

  private XlsxMetacardUtility() {}

  public static BinaryContent buildSpreadSheet(List<Metacard> metacards) {
    if (metacards.isEmpty()) {
      return null;
    }

    Set<AttributeDescriptor> attributeDescriptors =
        metacards.get(0).getMetacardType().getAttributeDescriptors();

    int rowIndex = 0;
    int cellIndex = 0;

    Workbook workbook = new XSSFWorkbook();
    Sheet sheet = workbook.createSheet();
    Row row = sheet.createRow(rowIndex++);

    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);

    // Write header row.
    for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
      String attributeName = attributeDescriptor.getName();
      Cell cell = row.createCell(cellIndex++);
      cell.setCellValue(attributeName);
      cell.setCellStyle(style);
    }

    for (Metacard metacard : metacards) {
      List<String> values = getMetacardValues(metacard);
      row = sheet.createRow(rowIndex++);

      cellIndex = 0;
      for (String value : values) {
        row.createCell(cellIndex++).setCellValue(value);
      }
    }

    return writeWorkbook(workbook);
  }

  private static BinaryContent writeWorkbook(Workbook workbook) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

    try {
      workbook.write(byteArrayOutputStream);
    } catch (IOException e) {
      LOGGER.debug("There was a problem writing the XLSX file.", e);
    }

    return new BinaryContentImpl(
        new ByteArrayInputStream(byteArrayOutputStream.toByteArray()), mimeType);
  }

  private static List<String> getMetacardValues(Metacard metacard) {
    List<String> values = new ArrayList<>();

    Set<AttributeDescriptor> attributeDescriptors =
        metacard.getMetacardType().getAttributeDescriptors();

    for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
      String attributeName = attributeDescriptor.getName();
      Attribute attribute = metacard.getAttribute(attributeName);

      if (attribute != null) {
        if (attributeDescriptor.isMultiValued()) {
          String value = StringUtils.join(attribute.getValues(), ", ");
          values.add(value);
        } else {
          values.add(attribute.getValue().toString());
        }
      } else {
        values.add("");
      }
    }

    return values;
  }
}
