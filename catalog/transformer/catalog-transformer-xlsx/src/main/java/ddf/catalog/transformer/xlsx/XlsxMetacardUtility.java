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

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transformer.csv.common.CsvTransformer;
import ddf.catalog.transformer.csv.common.MetacardIterator;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.collections.CollectionUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class XlsxMetacardUtility {

  private static final Logger LOGGER = LoggerFactory.getLogger(XlsxMetacardUtility.class);

  public static final String COLUMN_ORDER_KEY = "columnOrder";

  public static final String COLUMN_ALIAS_KEY = "aliases";

  private static MimeType mimeType = new MimeType();

  static {
    try {
      mimeType.setPrimaryType(OOXML_SHEET.type());
      mimeType.setSubType(OOXML_SHEET.subtype());
    } catch (MimeTypeParseException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private XlsxMetacardUtility() {}

  public static BinaryContent buildSpreadSheet(
      List<Metacard> metacards, Map<String, String> aliases, List<String> attributeOrder) {
    if (metacards.isEmpty()) {
      return null;
    }

    Set<AttributeDescriptor> attributeDescriptors =
        CsvTransformer.getNonEmptyValueAttributes(metacards);

    Set<AttributeDescriptor> descriptors =
        CollectionUtils.isEmpty(attributeOrder)
            ? attributeDescriptors
            : attributeDescriptors.stream()
                .filter(attr -> attributeOrder.contains(attr.getName()))
                .collect(Collectors.toSet());

    Workbook workbook = new XSSFWorkbook();

    List<AttributeDescriptor> sortedAttributeDescriptors =
        CsvTransformer.sortAttributes(descriptors, attributeOrder);

    Sheet sheet = writeHeaderRow(sortedAttributeDescriptors, aliases, workbook);

    writeMetacardValues(metacards, sortedAttributeDescriptors, sheet);

    return writeWorkbook(workbook);
  }

  static Sheet writeHeaderRow(
      List<AttributeDescriptor> orderedAttributeDescriptors,
      Map<String, String> columnAliasMap,
      Workbook workbook) {
    CellStyle style = workbook.createCellStyle();
    Font font = workbook.createFont();
    font.setBold(true);
    style.setFont(font);
    style.setAlignment(HorizontalAlignment.CENTER);
    Sheet sheet = workbook.createSheet();
    Row row = sheet.createRow(0);
    int cellIndex = 0;
    for (AttributeDescriptor attributeDescriptor : orderedAttributeDescriptors) {
      String attributeName = attributeDescriptor.getName();
      Cell cell = row.createCell(cellIndex++);
      cell.setCellValue(
          Optional.ofNullable(columnAliasMap.get(attributeName)).orElse(attributeName));
      cell.setCellStyle(style);
    }
    return sheet;
  }

  private static void writeMetacardValues(
      List<Metacard> metacards,
      List<AttributeDescriptor> orderedAttributeDescriptors,
      Sheet sheet) {
    int rowIndex = 1; // start at 1 since the header is row 0
    for (Metacard metacard : metacards) {
      Row row = sheet.createRow(rowIndex++);
      int cellIndex = 0;
      Iterator<Serializable> metacardIterator =
          new MetacardIterator(metacard, orderedAttributeDescriptors);
      while (metacardIterator.hasNext()) {
        row.createCell(cellIndex++).setCellValue(String.valueOf(metacardIterator.next()));
      }
    }
    for (int i = 0; i < sheet.getRow(0).getPhysicalNumberOfCells(); i++) {
      sheet.autoSizeColumn(i);
    }
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
}
