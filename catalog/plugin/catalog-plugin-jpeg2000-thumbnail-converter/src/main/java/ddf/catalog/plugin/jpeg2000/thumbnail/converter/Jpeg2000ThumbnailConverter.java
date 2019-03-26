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
package ddf.catalog.plugin.jpeg2000.thumbnail.converter;

import com.github.jaiimageio.jpeg2000.impl.IISRandomAccessIO;
import com.github.jaiimageio.jpeg2000.impl.J2KImageReaderSpi;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;

/**
 * check for Jpeg 2000 thumbnails in the result set, and convert them to standard Jpeg so the
 * browser can render them.
 */
public class Jpeg2000ThumbnailConverter implements PostQueryPlugin {

  public static final int OTHER_JP2_SIGNATURE = 0x0000000c;

  public static final int JP2_SIGNATURE_BOX = 0x6a502020;

  public static final int OFFICIAL_JP2_SIGNATURE = 0x0d0a870a;

  public static final short START_OF_CODESTREAM_MARKER = (short) 0xff4f;

  public Jpeg2000ThumbnailConverter() {
    IIORegistry.getDefaultInstance().registerServiceProvider(new J2KImageReaderSpi());
  }

  @Override
  public QueryResponse process(QueryResponse input)
      throws PluginExecutionException, StopProcessingException {
    for (Result result : input.getResults()) {
      Metacard metacard = result.getMetacard();
      byte[] thumbnailBytes = metacard.getThumbnail();
      if (thumbnailBytes == null) {
        continue;
      }

      try (ByteArrayInputStream original = new ByteArrayInputStream(thumbnailBytes);
          ByteArrayOutputStream converted = new ByteArrayOutputStream()) {
        IISRandomAccessIO in = new IISRandomAccessIO(ImageIO.createImageInputStream(original));

        if (in.length() == 0) {
          continue;
        }

        // extracted from jj2000.j2k.fileformat.reader.FileFormatReader
        if (in.readInt() != OTHER_JP2_SIGNATURE
            || in.readInt() != JP2_SIGNATURE_BOX
            || in.readInt() != OFFICIAL_JP2_SIGNATURE) { // Not a JP2 file
          in.seek(0);

          if (in.readShort() != START_OF_CODESTREAM_MARKER) { // Standard syntax marker found
            continue;
          }
        }

        // convert j2k thumbnail to jpeg thumbnail
        original.reset();
        BufferedImage thumbnail = ImageIO.read(original);
        if (thumbnail == null) {
          continue;
        }
        ImageIO.write(thumbnail, "jpeg", converted);
        metacard.setAttribute(new AttributeImpl(Core.THUMBNAIL, converted.toByteArray()));
      } catch (IOException e) {
        throw new PluginExecutionException(e);
      }
    }
    return input;
  }
}
