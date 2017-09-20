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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.Test;

public class GeoPdfParserTest {

  public static final String WKT_POLYGON =
      "POLYGON ((-75.15840498869015 43.93500189524018, -75.15840498868951 37.63917521983974, -80.50866091541451 37.639175219841135, -80.50866091541393 43.93500189523885, -75.15840498869015 43.93500189524018, -75.15840498869015 43.93500189524018))";

  public static final String WKT_MULTIPOLYGON =
      "MULTIPOLYGON (((-75.15840498869015 43.93500189524018, -75.15840498868951 37.63917521983974, -80.50866091541451 37.639175219841135, -80.50866091541393 43.93500189523885, -75.15840498869015 43.93500189524018, -75.15840498869015 43.93500189524018)),((-75.15840498869015 43.93500189524018, -75.15840498868951 37.63917521983974, -80.50866091541451 37.639175219841135, -80.50866091541393 43.93500189523885, -75.15840498869015 43.93500189524018, -75.15840498869015 43.93500189524018)))";

  public static final String WKT_POLYGON_NO_NEATLINE =
      "POLYGON ((-80.77532309213824 36.77844766314338, -78.57704723006924 36.77844766314343, -78.5770472300693 39.623275249350364, -80.7753230921383 39.623275249350314, -80.77532309213824 36.77844766314338))";

  private static final GeoPdfParserImpl GEO_PDF_PARSER = new GeoPdfParserImpl();

  @Test
  public void testEmptyPdf() throws Exception {
    PDDocument pdfDocument = GeoPdfDocumentGenerator.generateEmptyPdf();
    String wktString = GEO_PDF_PARSER.apply(pdfDocument);
    assertThat(wktString, nullValue());
  }

  @Test
  public void testSinglePageMultiFrameGeographicGeoPdf() throws Exception {
    PDDocument pdfDocument =
        GeoPdfDocumentGenerator.generateGeoPdf(1, 2, GeoPdfParserImpl.GEOGRAPHIC);
    String wktString = GEO_PDF_PARSER.apply(pdfDocument);
    assertThat(wktString, is(WKT_MULTIPOLYGON));
  }

  @Test
  public void testSinglePageSingleMultiFrameGeographicGeoPdf() throws Exception {
    PDDocument pdfDocument =
        GeoPdfDocumentGenerator.generateGeoPdf(1, 1, GeoPdfParserImpl.GEOGRAPHIC);
    String wktString = GEO_PDF_PARSER.apply(pdfDocument);
    assertThat(wktString, is(WKT_POLYGON));
  }

  @Test
  public void testSinglePageMultiFrameNonGeographicGeoPdf() throws Exception {
    PDDocument pdfDocument = GeoPdfDocumentGenerator.generateGeoPdf(1, 2, "UT");
    String wktString = GEO_PDF_PARSER.apply(pdfDocument);
    assertThat(wktString, nullValue());
  }

  @Test
  public void testSinglePageMultiFrameNoProjectionGeoPdf() throws Exception {
    PDDocument pdfDocument = GeoPdfDocumentGenerator.generateGeoPdf(1, 2, "");
    String wktString = GEO_PDF_PARSER.apply(pdfDocument);
    assertThat(wktString, nullValue());
  }

  @Test
  public void testSinglePageSingleFrameGeographicGeoPdf() throws Exception {
    PDDocument pdfDocument =
        GeoPdfDocumentGenerator.generateGeoPdf(1, GeoPdfParserImpl.GEOGRAPHIC, true);
    String wktString = GEO_PDF_PARSER.apply(pdfDocument);
    assertThat(wktString, is(WKT_POLYGON));
  }

  @Test
  public void testSinglePageSingleFrameNonGeographicGeoPdf() throws Exception {
    PDDocument pdfDocument = GeoPdfDocumentGenerator.generateGeoPdf(1, "UT", true);
    String wktString = GEO_PDF_PARSER.apply(pdfDocument);
    assertThat(wktString, nullValue());
  }

  @Test
  public void testSinglePageSingleFrameNoProjectionGeoPdf() throws Exception {
    PDDocument pdfDocument = GeoPdfDocumentGenerator.generateGeoPdf(1, "", true);
    String wktString = GEO_PDF_PARSER.apply(pdfDocument);
    assertThat(wktString, nullValue());
  }

  @Test
  public void testSinglePageSingleFrameNoNeatLineGeoPdf() throws Exception {
    PDDocument pdfDocument =
        GeoPdfDocumentGenerator.generateGeoPdf(1, GeoPdfParserImpl.GEOGRAPHIC, false);
    String wktString = GEO_PDF_PARSER.apply(pdfDocument);
    assertThat(wktString, is(WKT_POLYGON_NO_NEATLINE));
  }
}
