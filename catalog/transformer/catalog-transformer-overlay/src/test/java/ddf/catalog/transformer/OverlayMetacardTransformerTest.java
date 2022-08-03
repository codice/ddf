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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiFunction;
import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.locationtech.jts.io.ParseException;

public class OverlayMetacardTransformerTest {
  private OverlayMetacardTransformer transformer;

  @Rule public ExpectedException expectedException = ExpectedException.none();

  @Before
  public void setUp() {
    final BiFunction<Metacard, Map<String, Serializable>, Optional<BufferedImage>> supplier =
        (metacard, arguments) -> {
          try (final InputStream inputStream =
              getClass().getClassLoader().getResourceAsStream("flower.jpg")) {
            return Optional.ofNullable(ImageIO.read(inputStream))
                .map(
                    image -> {
                      // Add a 5x5 red square to the upper-left corner so we can check whether the
                      // image is rotated correctly
                      for (int x = 0; x < 5; ++x) {
                        for (int y = 0; y < 5; ++y) {
                          image.setRGB(x, y, Color.RED.getRGB());
                        }
                      }
                      return image;
                    });
          } catch (IOException e) {
            return Optional.empty();
          }
        };

    transformer = new OverlayMetacardTransformer(supplier);
  }

  private MetacardImpl getMetacard() {
    final MetacardImpl metacard = new MetacardImpl();
    metacard.setLocation("POLYGON ((0 0, 1 0, 1 -0.1, 0 -0.1, 0 0))");
    return metacard;
  }

  @Test
  public void testOverlaySquishedHeight() throws Exception {
    final MetacardImpl metacard = getMetacard();
    metacard.setLocation("POLYGON ((0 0, 1 0, 1 -0.1, 0 -0.1, 0 0))");
    final BinaryContent content = transform(metacard, null);

    final BufferedImage originalImage = getImage(getImageBytes());
    final BufferedImage overlayImage = getImage(content.getByteArray());

    assertThat(overlayImage.getWidth(), equalTo(originalImage.getWidth()));
    assertThat(overlayImage.getHeight(), lessThan(originalImage.getHeight()));
  }

  @Test
  public void testOverlayRotated() throws Exception {
    final MetacardImpl metacard = getMetacard();
    metacard.setLocation("POLYGON ((0 1, 1 0, 0 -1, -1 0, 0 1))");
    final BinaryContent content = transform(metacard, null);

    final BufferedImage originalImage = getImage(getImageBytes());
    final BufferedImage overlayImage = getImage(content.getByteArray());

    // We can only make these assertions because we are rotating a square image
    assertThat(overlayImage.getWidth(), greaterThan(originalImage.getHeight()));
    assertThat(overlayImage.getHeight(), greaterThan(originalImage.getHeight()));
  }

  @Test
  public void testOverlayCrossesDateline() throws Exception {
    final MetacardImpl metacard = getMetacard();
    metacard.setLocation("POLYGON ((179 1, -179 1, -179 -1, 179 -1, 179 1))");
    final BinaryContent content = transform(metacard, null);

    final BufferedImage originalImage = getImage(getImageBytes());
    final BufferedImage overlayImage = getImage(content.getByteArray());

    // Verifies the image was not stretched by going the wrong way around the globe
    assertThat(overlayImage.getWidth(), is(originalImage.getHeight()));
    assertThat(overlayImage.getHeight(), is(originalImage.getHeight()));
  }

  @Test
  public void testNormalizeOrientation() throws Exception {
    final MetacardImpl metacard = getMetacard();
    metacard.setLocation("POLYGON ((1 0, 0 0, 0 1, 1 1, 1 0))");
    final BinaryContent content = transform(metacard, null);

    final BufferedImage overlay = getImage(content.getByteArray());
    final Color lowerRightColor =
        new Color(overlay.getRGB(overlay.getWidth() - 1, overlay.getHeight() - 1));
    assertThat(lowerRightColor, is(Color.RED));

    transformer.setNormalizeOrientation(true);
    final BinaryContent normalizedContent = transform(metacard, null);

    final BufferedImage normalizedOverlay = getImage(normalizedContent.getByteArray());
    final Color upperLeftColor = new Color(normalizedOverlay.getRGB(0, 0));
    assertThat(upperLeftColor, is(Color.RED));
  }

  @Test
  public void testNormalizeOrientationOverDatelineClockwiseCoordinates() throws Exception {
    final MetacardImpl metacard = getMetacard();
    metacard.setLocation("POLYGON ((179 -1, 179 1, -179 1, -179 -1, 179 -1))");
    final BinaryContent content = transform(metacard, null);

    final BufferedImage overlay = getImage(content.getByteArray());
    final Color lowerLeftColor = new Color(overlay.getRGB(0, overlay.getHeight() - 1));
    assertThat(lowerLeftColor, is(Color.RED));

    transformer.setNormalizeOrientation(true);
    final BinaryContent normalizedContent = transform(metacard, null);

    final BufferedImage normalizedOverlay = getImage(normalizedContent.getByteArray());
    final Color upperLeftColor = new Color(normalizedOverlay.getRGB(0, 0));
    assertThat(upperLeftColor, is(Color.RED));
  }

  @Test
  public void testNormalizeOrientationOverDatelineCounterclockwiseCoordinates() throws Exception {
    final MetacardImpl metacard = getMetacard();
    metacard.setLocation("POLYGON ((-179 -1, -179 1, 179 1, 179 -1, -179 -1))");
    final BinaryContent content = transform(metacard, null);

    final BufferedImage overlay = getImage(content.getByteArray());
    final Color lowerRightColor =
        new Color(overlay.getRGB(overlay.getWidth() - 1, overlay.getHeight() - 1));
    assertThat(lowerRightColor, is(Color.RED));

    transformer.setNormalizeOrientation(true);
    final BinaryContent normalizedContent = transform(metacard, null);

    final BufferedImage normalizedOverlay = getImage(normalizedContent.getByteArray());
    final Color upperLeftColor = new Color(normalizedOverlay.getRGB(0, 0));
    assertThat(upperLeftColor, is(Color.RED));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNoImageFromSupplier() throws Exception {
    final BiFunction<Metacard, Map<String, Serializable>, Optional<BufferedImage>> imageSupplier =
        (metacard, arguments) -> Optional.empty();
    transformer = new OverlayMetacardTransformer(imageSupplier);
    transform(getMetacard(), null);
  }

  private BufferedImage getImage(byte[] imageBytes) throws IOException {
    try (final ByteArrayInputStream inputStream = new ByteArrayInputStream(imageBytes)) {
      final ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream);
      return ImageIO.read(imageInputStream);
    }
  }

  private byte[] getImageBytes() throws IOException {
    try (final InputStream inputStream =
        getClass().getClassLoader().getResourceAsStream("flower.jpg")) {
      return IOUtils.toByteArray(inputStream);
    }
  }

  private BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException, IOException {
    final BinaryContent content = transformer.transform(metacard, arguments);
    verifyImageIsPng(content.getByteArray());
    return content;
  }

  private void verifyImageIsPng(byte[] imageBytes) {
    // Check the PNG header bytes.
    assertThat(imageBytes[0], is((byte) 0x89));
    assertThat(imageBytes[1], is((byte) 0x50));
    assertThat(imageBytes[2], is((byte) 0x4E));
    assertThat(imageBytes[3], is((byte) 0x47));
    assertThat(imageBytes[4], is((byte) 0x0D));
    assertThat(imageBytes[5], is((byte) 0x0A));
    assertThat(imageBytes[6], is((byte) 0x1A));
    assertThat(imageBytes[7], is((byte) 0x0A));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testNoLocation() throws Exception {
    final MetacardImpl metacard = getMetacard();
    metacard.setLocation(null);
    transform(metacard, null);
  }

  @Test
  public void testInvalidWkt() throws Exception {
    final MetacardImpl metacard = getMetacard();
    metacard.setLocation("INVALID WKT");

    expectedException.expect(CatalogTransformerException.class);
    expectedException.expectCause(isA(ParseException.class));
    transform(metacard, null);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testCannotHandleGeometry() throws Exception {
    final MetacardImpl metacard = getMetacard();
    metacard.setLocation("LINESTRING (30 10, 10 30, 40 40)");
    transform(metacard, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullImageSupplier() {
    new OverlayMetacardTransformer(null);
  }
}
