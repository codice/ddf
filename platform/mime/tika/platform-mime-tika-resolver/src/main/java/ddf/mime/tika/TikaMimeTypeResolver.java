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
package ddf.mime.tika;

import ddf.mime.MimeTypeResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.tika.Tika;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.mime.MimeType;
import org.apache.tika.mime.MimeTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Apache Tika mime type resolution packaged as a {@link MimeTypeResolver} OSGi service that can map
 * a list of file extensions to their corresponding mime types, and vice versa.
 *
 * @author Hugh Rodgers
 * @since 2.1.0
 */
public class TikaMimeTypeResolver implements MimeTypeResolver {
  private static final Logger LOGGER = LoggerFactory.getLogger(TikaMimeTypeResolver.class);

  private TikaConfig config;

  private Tika tika;

  private int priority;

  /** Constructs the Tika instance that will be used for mime type resolution. */
  public TikaMimeTypeResolver() {
    try {
      config = new TikaConfig(this.getClass().getClassLoader());
      tika = new Tika(config);
    } catch (Exception e) {
      LOGGER.debug("Error creating TikaConfig with ClassLoader", e);
    }
  }

  public void init() {
    LOGGER.trace("INSIDE: init");
  }

  public void destroy() {
    LOGGER.trace("INSIDE: destroy");
  }

  @Override
  public String getName() {
    return this.getClass().getName();
  }

  @Override
  public int getPriority() {
    return priority;
  }

  /**
   * Sets the priority of thie {@link MimeTypeResolver}. For the TikaMimeTypeResolver this priority
   * should usually be set (via blueprint) to a negative value to insure that this {@link
   * MimeTypeResolver} is invoked last amongst all registered {@link MimeTypeResolver}s. This is
   * desired so that any custom {@link MimeTypeResolver}s that may override Tika's handling of a
   * mime type will be processed first.
   *
   * @param priority the priority
   */
  public void setPriority(int priority) {
    LOGGER.debug("Setting priority = {}", priority);
    this.priority = priority;
  }

  @Override
  public boolean hasSchema() {
    return false;
  }

  @Override
  public String getSchema() {
    return null;
  }

  @Override
  public String getFileExtensionForMimeType(String contentType) {
    LOGGER.trace("ENTERING: getFileExtensionForMimeType()");

    MimeTypes mimeTypes = config.getMimeRepository();
    String extension = null;
    if (StringUtils.isNotEmpty(contentType)) {
      try {
        MimeType mimeType = mimeTypes.forName(contentType);
        extension = mimeType.getExtension();
      } catch (Exception e) {
        LOGGER.debug("Exception caught getting file extension for mime type {}", contentType, e);
      }
    }
    LOGGER.debug("mimeType = {},   file extension = [{}]", contentType, extension);

    LOGGER.trace("EXITING: getFileExtensionForMimeType()");

    return extension;
  }

  @Override
  public String getMimeTypeForFileExtension(String fileExtension) {
    LOGGER.trace("ENTERING: getMimeTypeForFileExtension()");

    String mimeType = null;
    if (StringUtils.isNotEmpty(fileExtension)) {
      try {
        String filename = "dummy." + fileExtension;
        mimeType = tika.detect(filename);
      } catch (Exception e) {
        LOGGER.debug("Exception caught getting mime type for file extension {}", fileExtension, e);
      }
    }

    LOGGER.debug("mimeType = {},   file extension = [{}]", mimeType, fileExtension);

    LOGGER.trace("EXITING: getMimeTypeForFileExtension()");

    return mimeType;
  }
}
