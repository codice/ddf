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
package ddf.catalog.transformer;

import static ddf.catalog.transformer.GeometryUtils.canHandleGeometry;
import static ddf.catalog.transformer.GeometryUtils.parseGeometry;

import com.jhlabs.image.PerspectiveFilter;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.imageio.ImageIO;
import org.apache.commons.lang.Validate;
import org.la4j.Vector;
import org.la4j.vector.dense.BasicVector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

public class OverlayMetacardTransformer implements MetacardTransformer {
  private static final String PNG = "png";

  private static final MimeType MIME_TYPE;

  static {
    try {
      MIME_TYPE = new MimeType("image", PNG);
    } catch (MimeTypeParseException e) {
      throw new ExceptionInInitializerError(e);
    }
  }

  private final BiFunction<Metacard, Map<String, Serializable>, Optional<BufferedImage>>
      imageSupplier;

  public OverlayMetacardTransformer(
      BiFunction<Metacard, Map<String, Serializable>, Optional<BufferedImage>> imageSupplier) {
    Validate.notNull(imageSupplier, "The image supplier cannot be null.");
    this.imageSupplier = imageSupplier;
  }

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    Validate.notNull(metacard, "The metacard cannot be null.");
    return overlay(metacard, arguments);
  }

  private BinaryContent overlay(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    final Optional<BufferedImage> bufferedImageOptional = imageSupplier.apply(metacard, arguments);
    final BufferedImage image =
        bufferedImageOptional.orElseThrow(
            () ->
                new CatalogTransformerException(
                    "Did not receive an image from the image supplier."));

    List<Vector> boundary = parseBoundary(metacard.getLocation());
    BufferedImage tile = createTileFromImageAndBoundary(image, boundary);
    return createBinaryContent(tile);
  }

  private List<Vector> parseBoundary(String location) throws CatalogTransformerException {
    final Geometry geometry = parseGeometry(location);
    if (!canHandleGeometry(geometry)) {
      throw new CatalogTransformerException("The Image boundary is not a rectangle");
    }

    final Coordinate[] coordinates = geometry.getCoordinates();

    List<Vector> boundary = new ArrayList<>();

    // Using indices rather than for-each because the first coordinate is duplicated.
    for (int i = 0; i < 4; i++) {
      boundary.add(new BasicVector(new double[] {coordinates[i].x, coordinates[i].y}));
    }
    return boundary;
  }

  private BufferedImage createTileFromImageAndBoundary(BufferedImage image, List<Vector> boundary) {
    /*
     * We transform the image by moving the corners and applying
     * transparency so that it looks right when laid down as a north-up
     * rectangular tile.
     */

    // Scaling by latitude so our x and y axes have roughly equal units.
    double lat = boundary.get(0).get(1);
    boundary.replaceAll(v -> scaleByLatitude(v, lat));

    // We are putting the image into a north-up rectangle, so we need
    // to get the minimum rectangle surrounding the boundary.
    List<Vector> boundingBox = calculateBoundingBox(boundary);
    Vector origin = boundingBox.get(0).copy();
    boundary.replaceAll(v -> v.subtract(origin));
    boundingBox.replaceAll(v -> v.subtract(origin));

    // The image may be stretched in weird ways, but we do our best to preserve
    // the resolution by scaling by the width of the image when going from lon/lat
    // to pixel space.
    double scaleFactor = calculateScaleFactor(boundary, image.getWidth());
    boundary.replaceAll(v -> v.multiply(scaleFactor));
    boundingBox.replaceAll(v -> v.multiply(scaleFactor));

    return createImage(
        image, boundary, (int) boundingBox.get(1).get(0), (int) -boundingBox.get(1).get(1));
  }

  private BinaryContent createBinaryContent(BufferedImage image)
      throws CatalogTransformerException {
    try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
      ImageIO.write(image, PNG, baos);
      return new BinaryContentImpl(new ByteArrayInputStream(baos.toByteArray()), MIME_TYPE);
    } catch (IOException e) {
      throw new CatalogTransformerException(e);
    }
  }

  private static BufferedImage createImage(
      BufferedImage original, List<Vector> boundary, int width, int height) {
    BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

    int[] xs = new int[4];
    int[] ys = new int[4];
    for (int i = 0; i < boundary.size(); i++) {
      xs[i] = (int) boundary.get(i).get(0);
      ys[i] = (int) -boundary.get(i).get(1);
    }

    // We have a rectangular image and we have boundary points that the image should
    // be in. This transform moves the corners of the image to be at those boundary points.
    PerspectiveFilter perspectiveFilter = new PerspectiveFilter();
    perspectiveFilter.setCorners(xs[0], ys[0], xs[1], ys[1], xs[2], ys[2], xs[3], ys[3]);
    img = perspectiveFilter.filter(original, img);

    return img;
  }

  private static double calculateScaleFactor(List<Vector> boundary, int width) {
    return width / boundary.get(1).subtract(boundary.get(0)).euclideanNorm();
  }

  private static Vector scaleByLatitude(Vector v, double lat) {
    double lon = v.get(0) * Math.cos(Math.toRadians(lat));
    return new BasicVector(new double[] {lon, v.get(1)});
  }

  public static List<Vector> calculateBoundingBox(List<Vector> boundary) {
    double maxLon = Collections.max(boundary, Comparator.comparing(v -> v.get(0))).get(0);
    double minLon = Collections.min(boundary, Comparator.comparing(v -> v.get(0))).get(0);
    double maxLat = Collections.max(boundary, Comparator.comparing(v -> v.get(1))).get(1);
    double minLat = Collections.min(boundary, Comparator.comparing(v -> v.get(1))).get(1);

    List<Vector> boundingBox = new ArrayList<>();
    boundingBox.add(new BasicVector(new double[] {minLon, maxLat}));
    boundingBox.add(new BasicVector(new double[] {maxLon, minLat}));

    return boundingBox;
  }
}
