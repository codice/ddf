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
package ddf.catalog.transformer.input.pptx;

import static org.apache.commons.lang3.Validate.notNull;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.sl.usermodel.SlideShow;
import org.apache.poi.sl.usermodel.SlideShowFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a decorator class that adds a thumbnail for PPTX files. It relies on an injected {@link
 * InputTransformer} to extract the metadata, and then it generates a thumbnail image with Apache
 * POI.
 */
public class PptxInputTransformer implements InputTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(PptxInputTransformer.class);

  private static final int RESOLUTION_DPI = 44;

  private static final float IMAGE_QUALITY = 1.0f;

  private static final float IMAGE_HEIGHTWIDTH = 128;

  private static final String FORMAT_NAME = "jpg";

  private final InputTransformer inputTransformer;

  /**
   * The inputTransformer parameter will be used to generate the basic metadata. If the parameter is
   * null, then a {@link NullPointerException} will be thrown.
   *
   * @param inputTransformer must be non-null
   * @throws NullPointerException
   */
  public PptxInputTransformer(InputTransformer inputTransformer) {

    notNull(inputTransformer, "The inputTransformer parameter must be non-null");

    this.inputTransformer = inputTransformer;
  }

  @Override
  public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
    return transform(input, null);
  }

  @Override
  public Metacard transform(InputStream input, String id)
      throws IOException, CatalogTransformerException {

    if (input == null) {
      throw new CatalogTransformerException("Cannot transform null input.");
    }

    return transformLogic(input);
  }

  /**
   * This is a three step process. First, create a FileBackedOutputStream because we need to consume
   * the stream twice. Once for the injected inputTransformer and once for Apache POI. Next, extract
   * the metadata with the injected input transformer. And last, use Apache POI to create the
   * thumbnail.
   *
   * @param input
   * @return
   * @throws IOException
   * @throws CatalogTransformerException
   */
  private Metacard transformLogic(InputStream input)
      throws IOException, CatalogTransformerException {

    try (TemporaryFileBackedOutputStream fileBackedOutputStream =
        new TemporaryFileBackedOutputStream()) {
      try {
        int c = IOUtils.copy(input, fileBackedOutputStream);
        LOGGER.debug("copied {} bytes from input stream to file backed output stream", c);
      } catch (IOException e) {
        throw new CatalogTransformerException("Could not copy bytes of content message.", e);
      }

      Metacard metacard =
          extractInitialMetadata(fileBackedOutputStream.asByteSource().openStream());

      try {
        extractThumbnail(metacard, fileBackedOutputStream.asByteSource().openStream());
      } catch (EncryptedDocumentException e) {
        LOGGER.debug("Unable to generate thumbnail", e);
      }
      return metacard;
    }
  }

  /**
   * Extract the initial metadata using the injected input transformer.
   *
   * @param input
   * @return
   * @throws IOException
   * @throws CatalogTransformerException
   */
  private Metacard extractInitialMetadata(InputStream input)
      throws IOException, CatalogTransformerException {
    return inputTransformer.transform(input);
  }

  /**
   * SlideShowFactory.create() will perform the tests for password protected files.
   *
   * <p>Because Apache POI dynamically loads the classes needed to handle a PPTX file, the default
   * class loader is unable to find the dependencies during runtime. Therefore, the original class
   * loader is saved, then current class loader is set to this class's class loader, and finally the
   * original class loader is restored.
   *
   * @param metacard
   * @param input
   * @throws IOException
   * @throws EncryptedDocumentException
   */
  private void extractThumbnail(Metacard metacard, InputStream input) throws IOException {

    ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(getClass().getClassLoader());
    try (SlideShow<?, ?> genericSlideShow = SlideShowFactory.create(input)) {

      if (genericSlideShow instanceof XMLSlideShow) {
        XMLSlideShow xmlSlideShow = (XMLSlideShow) genericSlideShow;

        byte[] thumbnail = generatePptxThumbnail(xmlSlideShow);
        if (thumbnail != null) {
          metacard.setAttribute(new AttributeImpl(Core.THUMBNAIL, thumbnail));
        }

      } else {
        LOGGER.debug("Cannot transform old style (OLE2) ppt : id = {}", metacard.getId());
      }

    } finally {
      Thread.currentThread().setContextClassLoader(originalContextClassLoader);
    }
  }

  /**
   * If the slide show does not contain any slides, then return null. Otherwise, return jpeg image
   * data of the first slide in the deck. If there is an exceptions, log the fact and return null.
   *
   * @param slideShow
   * @return jpeg thumbnail or null if thumbnail can't be created
   * @throws IOException
   */
  private byte[] generatePptxThumbnail(XMLSlideShow slideShow) {

    if (slideShow.getSlides().isEmpty()) {
      LOGGER.debug("Powerpoint file does not contain any slides, skipping thumbnail generation");
      return null;
    }
    Graphics2D graphics = null;
    try {
      Dimension pgsize = slideShow.getPageSize();
      int largestDimension = (int) Math.max(pgsize.getHeight(), pgsize.getWidth());
      float scalingFactor = IMAGE_HEIGHTWIDTH / largestDimension;
      int scaledHeight = (int) (pgsize.getHeight() * scalingFactor);
      int scaledWidth = (int) (pgsize.getWidth() * scalingFactor);
      BufferedImage img = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
      graphics = img.createGraphics();
      graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
      graphics.setRenderingHint(
          RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
      graphics.scale(scalingFactor, scalingFactor);
      slideShow.getSlides().get(0).draw(graphics);

      try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
        ImageIOUtil.writeImage(img, FORMAT_NAME, outputStream, RESOLUTION_DPI, IMAGE_QUALITY);
        return outputStream.toByteArray();
      }
    } catch (IOException | RuntimeException e) {
      LOGGER.debug("Unable to generate thumbnail for PPTX file", e);
    } finally {
      if (Objects.nonNull(graphics)) {
        graphics.dispose();
      }
    }

    return null;
  }
}
