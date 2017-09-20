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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;

public class PdfThumbnailGeneratorImpl implements PdfThumbnailGenerator {

  private static final int RESOLUTION_DPI = 44;

  private static final float IMAGE_QUALITY = 1.0f;

  private static final float IMAGE_HEIGHTWIDTH = 128;

  private static final String FORMAT_NAME = "jpg";

  @Override
  public Optional<byte[]> apply(PDDocument pdfDocument) throws IOException {
    PDFRenderer pdfRenderer = new PDFRenderer(pdfDocument);

    if (pdfDocument.getNumberOfPages() < 1) {
      return Optional.empty();
    }

    BufferedImage image = pdfRenderer.renderImageWithDPI(0, RESOLUTION_DPI, ImageType.RGB);

    int largestDimension = Math.max(image.getHeight(), image.getWidth());
    float scalingFactor = IMAGE_HEIGHTWIDTH / largestDimension;
    int scaledHeight = (int) (image.getHeight() * scalingFactor);
    int scaledWidth = (int) (image.getWidth() * scalingFactor);

    BufferedImage scaledImage =
        new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = scaledImage.createGraphics();
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    graphics.drawImage(image, 0, 0, scaledWidth, scaledHeight, null);
    graphics.dispose();

    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
      ImageIOUtil.writeImage(scaledImage, FORMAT_NAME, outputStream, RESOLUTION_DPI, IMAGE_QUALITY);
      return Optional.of(outputStream.toByteArray());
    }
  }
}
