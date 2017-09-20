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
package ddf.catalog.transformer.input.pdf;

import org.apache.commons.lang.StringUtils;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

public class GeoPdfDocumentGenerator {

  public static PDDocument generateEmptyPdf() {
    PDDocument pdDocument = new PDDocument();
    pdDocument.addPage(new PDPage());
    return pdDocument;
  }

  public static PDDocument generateGeoPdf(
      int numberOfPages, int numberOfFramesPerPage, String projectionType) {
    PDDocument pdDocument = new PDDocument();
    for (int i = 0; i < numberOfPages; i++) {
      pdDocument.addPage(generateGeoPdfPage(numberOfFramesPerPage, projectionType));
    }
    return pdDocument;
  }

  public static PDDocument generateGeoPdf(
      int numberOfPages, String projectionType, boolean generateNeatLine) {
    PDDocument pdDocument = new PDDocument();
    for (int i = 0; i < numberOfPages; i++) {
      pdDocument.addPage(generateGeoPdfPage(projectionType, generateNeatLine));
    }
    return pdDocument;
  }

  private static PDPage generateGeoPdfPage(int numberOfFramesPerPage, String projectionType) {
    PDPage pdPage = new PDPage();
    COSDictionary cosDictionary = pdPage.getCOSObject();
    cosDictionary.setItem(
        GeoPdfParserImpl.LGIDICT, generateLGIDictArray(numberOfFramesPerPage, projectionType));
    return pdPage;
  }

  private static PDPage generateGeoPdfPage(String projectionType, boolean generateNeatLine) {
    PDPage pdPage = new PDPage();
    COSDictionary cosDictionary = pdPage.getCOSObject();
    cosDictionary.setItem(
        GeoPdfParserImpl.LGIDICT, generateMapFrameDictionary(projectionType, generateNeatLine));
    return pdPage;
  }

  private static COSArray generateLGIDictArray(int numberOfFrames, String projectionType) {
    COSArray cosArray = new COSArray();
    for (int i = 0; i < numberOfFrames; i++) {
      cosArray.add(generateMapFrameDictionary(projectionType));
    }
    return cosArray;
  }

  private static COSDictionary generateMapFrameDictionary(String projectionType) {
    COSDictionary cosDictionary = new COSDictionary();
    if (StringUtils.isNotBlank(projectionType)) {
      cosDictionary.setItem(
          GeoPdfParserImpl.PROJECTION, generateProjectionDictionary(projectionType));
    }

    cosDictionary.setItem(GeoPdfParserImpl.NEATLINE, generateNeatLineArray());
    cosDictionary.setItem(GeoPdfParserImpl.CTM, generateCTMArray());
    return cosDictionary;
  }

  private static COSDictionary generateMapFrameDictionary(
      String projectionType, boolean generateNeatLine) {
    COSDictionary cosDictionary = new COSDictionary();
    if (StringUtils.isNotBlank(projectionType)) {
      cosDictionary.setItem(
          GeoPdfParserImpl.PROJECTION, generateProjectionDictionary(projectionType));
    }

    if (generateNeatLine) {
      cosDictionary.setItem(GeoPdfParserImpl.NEATLINE, generateNeatLineArray());
    }

    cosDictionary.setItem(GeoPdfParserImpl.CTM, generateCTMArray());
    return cosDictionary;
  }

  private static COSArray generateNeatLineArray() {
    COSArray neatLineArray = new COSArray();
    neatLineArray.add(new COSString("1563.749999999969"));
    neatLineArray.add(new COSString("1992.384698215686"));
    neatLineArray.add(new COSString("1563.75000000011"));
    neatLineArray.add(new COSString("239.6265517842302"));
    neatLineArray.add(new COSString("74.23874999988999"));
    neatLineArray.add(new COSString("239.6265517846492"));
    neatLineArray.add(new COSString("74.23875000008906"));
    neatLineArray.add(new COSString("1992.38469821535"));
    neatLineArray.add(new COSString("1563.749999999969"));
    neatLineArray.add(new COSString("1992.384698215686"));
    return neatLineArray;
  }

  private static COSArray generateCTMArray() {
    COSArray ctmArray = new COSArray();
    ctmArray.add((new COSString("0.003591954022988553")));
    ctmArray.add((new COSString("7.868785750012534e-017")));
    ctmArray.add((new COSString("-7.868785750012534e-017")));
    ctmArray.add((new COSString("0.003591954022988553")));
    ctmArray.add((new COSString("-80.77532309213824")));
    ctmArray.add((new COSString("36.77844766314338")));
    return ctmArray;
  }

  private static COSDictionary generateProjectionDictionary(String projectionType) {
    COSDictionary projectionDictionary = new COSDictionary();
    projectionDictionary.setString(GeoPdfParserImpl.PROJECTION_TYPE, projectionType);
    return projectionDictionary;
  }
}
