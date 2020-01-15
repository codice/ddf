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

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSBoolean;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSDocument;
import org.apache.pdfbox.cos.COSFloat;
import org.apache.pdfbox.cos.COSInteger;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.cos.COSNull;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.cos.COSString;
import org.apache.pdfbox.cos.ICOSVisitor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoPdfParserImpl implements GeoPdfParser {

  public static final String GEOGRAPHIC = "GEOGRAPHIC";

  public static final String LGIDICT = "LGIDict";

  public static final String PROJECTION = "Projection";

  public static final String PROJECTION_TYPE = "ProjectionType";

  public static final String NEATLINE = "Neatline";

  public static final String CTM = "CTM";

  private static final String POLYGON = "POLYGON ((";

  private static final String MULTIPOLYGON = "MULTIPOLYGON (";

  private static final Logger LOGGER = LoggerFactory.getLogger(GeoPdfParserImpl.class);

  private static final int CTM_SIZE = 6;

  /**
   * Generates a WKT compliant String from a PDF Document if it contains GeoPDF information.
   * Currently, only WGS84 Projections are supported (GEOGRAPHIC GeoPDF ProjectionType).
   *
   * @param pdfDocument - The PDF document
   * @return the WKT String
   * @throws IOException
   */
  @Override
  public String apply(PDDocument pdfDocument) throws IOException {
    ToDoubleVisitor toDoubleVisitor = new ToDoubleVisitor();
    LinkedList<String> polygons = new LinkedList<>();

    for (PDPage pdPage : pdfDocument.getPages()) {
      COSDictionary cosObject = pdPage.getCOSObject();

      COSBase lgiDictObject = cosObject.getObjectFromPath(LGIDICT);

      // Handle Multiple Map Frames
      if (lgiDictObject instanceof COSArray) {
        for (int i = 0; i < ((COSArray) lgiDictObject).size(); i++) {
          COSDictionary lgidict =
              (COSDictionary) cosObject.getObjectFromPath(LGIDICT + "/[" + i + "]");

          COSDictionary projectionArray = (COSDictionary) lgidict.getDictionaryObject(PROJECTION);
          if (projectionArray != null) {
            String projectionType =
                ((COSString) projectionArray.getItem(PROJECTION_TYPE)).getString();
            if (GEOGRAPHIC.equals(projectionType)) {
              COSArray neatlineArray =
                  (COSArray) cosObject.getObjectFromPath(LGIDICT + "/[" + i + "]/" + NEATLINE);
              getWktFromNeatLine(lgidict, neatlineArray, toDoubleVisitor).ifPresent(polygons::add);
            } else {
              LOGGER.debug(
                  "Unsupported projection type {}.  Map Frame will be skipped.", projectionType);
            }
          } else {
            LOGGER.debug("No projection array found on the map frame.  Map Frame will be skipped.");
          }
        }
        // Handle One Map Frame
      } else if (lgiDictObject instanceof COSDictionary) {
        COSDictionary lgidict = (COSDictionary) lgiDictObject;
        COSDictionary projectionArray = (COSDictionary) lgidict.getDictionaryObject(PROJECTION);
        if (projectionArray != null) {
          String projectionType =
              ((COSString) projectionArray.getItem(PROJECTION_TYPE)).getString();
          if (GEOGRAPHIC.equals(projectionType)) {
            COSArray neatlineArray =
                (COSArray) cosObject.getObjectFromPath(LGIDICT + "/" + NEATLINE);
            if (neatlineArray == null) {
              neatlineArray = generateNeatLineFromPDFDimensions(pdPage);
            }

            getWktFromNeatLine(lgidict, neatlineArray, toDoubleVisitor).ifPresent(polygons::add);
          } else {
            LOGGER.debug(
                "Unsupported projection type {}.  Map Frame will be skipped.", projectionType);
          }
        } else {
          LOGGER.debug("No projection array found on the map frame.  Map Frame will be skipped.");
        }
      }
    }

    if (polygons.isEmpty()) {
      LOGGER.debug(
          "No GeoPDF information found on PDF during transformation.  Metacard location will not be set.");
      return null;
    }

    if (polygons.size() == 1) {
      return POLYGON + polygons.get(0) + "))";
    } else {
      return polygons
          .stream()
          .map(polygon -> "((" + polygon + "))")
          .collect(Collectors.joining(",", MULTIPOLYGON, ")"));
    }
  }

  /**
   * A PDF Neatline defines the area of a PDF image with relation to the PDF page. If no neatline is
   * given it is assumed that the image encompasses the entire page. This functiong generates a
   * NeatLine in this fashion.
   *
   * @param pdPage the page to generate the NeatLine
   * @return an array of points representing the NeatLine
   */
  private COSArray generateNeatLineFromPDFDimensions(PDPage pdPage) {
    COSArray neatLineArray = new COSArray();

    String width = String.valueOf(pdPage.getMediaBox().getWidth());
    String height = String.valueOf(pdPage.getMediaBox().getHeight());

    neatLineArray.add(new COSString("0"));
    neatLineArray.add(new COSString("0"));

    neatLineArray.add(new COSString(width));
    neatLineArray.add(new COSString("0"));

    neatLineArray.add(new COSString(width));
    neatLineArray.add(new COSString(height));

    neatLineArray.add(new COSString("0"));
    neatLineArray.add(new COSString(height));

    return neatLineArray;
  }

  /**
   * Convert a Point2d into WKT Lat/Lon
   *
   * @param point2D
   * @return a String representation of a WKT Lat/Lon pair
   */
  private String point2dToWkt(Point2D point2D) {
    return point2D.getX() + " " + point2D.getY();
  }

  /**
   * Parses a given NeatLine and Transformation matrix into a WKT String
   *
   * @param lgidict - The PDF's LGIDict object
   * @param neatLineArray - The NeatLine array of points for the PDF
   * @param toDoubleVisitor - A visitor that converts PDF Strings / Ints / Longs into doubles.
   * @return the generated WKT Lat/Lon set
   * @throws IOException
   */
  private Optional<String> getWktFromNeatLine(
      COSDictionary lgidict, COSArray neatLineArray, ICOSVisitor toDoubleVisitor)
      throws IOException {
    List<Double> neatline = new LinkedList<>();
    List<String> coordinateList = new LinkedList<>();
    String firstCoordinate = null;

    double[] points = new double[CTM_SIZE];
    for (int i = 0; i < CTM_SIZE; i++) {
      Object obj = lgidict.getObjectFromPath(CTM + "/[" + i + "]").accept(toDoubleVisitor);
      if (obj != null) {
        points[i] = (Double) obj;
      } else {
        return Optional.empty();
      }
    }
    AffineTransform affineTransform = new AffineTransform(points);

    for (int i = 0; i < neatLineArray.size(); i++) {
      neatline.add((Double) neatLineArray.get(i).accept(toDoubleVisitor));
    }

    for (int i = 0; i < neatline.size(); i += 2) {
      double x = neatline.get(i);
      double y = neatline.get(i + 1);

      Point2D p = new Point2D.Double(x, y);

      Point2D pprime = affineTransform.transform(p, null);

      String xySet = point2dToWkt(pprime);

      if (firstCoordinate == null) {
        firstCoordinate = xySet;
      }
      coordinateList.add(xySet);
    }
    coordinateList.add(firstCoordinate);
    String wktString = StringUtils.join(coordinateList, ", ");
    LOGGER.debug("{}", wktString);
    return Optional.of(wktString);
  }

  /** This visitor class converts parsable COS Objects into {@link Double}s */
  private static class ToDoubleVisitor implements ICOSVisitor {

    @Override
    public Object visitFromArray(COSArray cosArray) throws IOException {
      return null;
    }

    @Override
    public Object visitFromBoolean(COSBoolean cosBoolean) throws IOException {
      return null;
    }

    @Override
    public Object visitFromDictionary(COSDictionary cosDictionary) throws IOException {
      return null;
    }

    @Override
    public Object visitFromDocument(COSDocument cosDocument) throws IOException {
      return null;
    }

    @Override
    public Object visitFromFloat(COSFloat cosFloat) throws IOException {
      return cosFloat.doubleValue();
    }

    @Override
    public Object visitFromInt(COSInteger cosInteger) throws IOException {
      return (double) cosInteger.longValue();
    }

    @Override
    public Object visitFromName(COSName cosName) throws IOException {
      return null;
    }

    @Override
    public Object visitFromNull(COSNull cosNull) throws IOException {
      return null;
    }

    @Override
    public Object visitFromStream(COSStream cosStream) throws IOException {
      return null;
    }

    @Override
    public Object visitFromString(COSString cosString) throws IOException {
      return Double.valueOf(cosString.getString());
    }
  }
}
