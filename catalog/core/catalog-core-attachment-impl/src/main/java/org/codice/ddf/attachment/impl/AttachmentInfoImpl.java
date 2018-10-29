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
package org.codice.ddf.attachment.impl;

import java.io.InputStream;
import org.codice.ddf.attachment.AttachmentInfo;

public class AttachmentInfoImpl implements AttachmentInfo {

  private final InputStream stream;

  private final String filename;

  private final String contentType;
  private String name;

  public AttachmentInfoImpl(InputStream stream, String filename, String contentType, String name) {
    this.stream = stream;
    this.filename = filename;
    this.contentType = contentType;
    this.name = name;
  }

  public InputStream getStream() {
    return stream;
  }

  public String getFilename() {
    return filename;
  }

  @Override
  public String getName() {
    return name;
  }

  public String getContentType() {
    return contentType;
  }
}
