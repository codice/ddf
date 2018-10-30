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
package org.codice.ddf.attachment;

import java.io.InputStream;
import javax.annotation.Nullable;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface AttachmentParser {

  /**
   * Generate attachment details, which includes a generated filename based on the submitted
   * filename (optional) and content-type (optional). Also checks that the stream is readable.
   *
   * @param stream
   * @param contentType
   * @param submittedFilename
   * @return attachment details
   */
  AttachmentInfo generateAttachmentInfo(
      InputStream stream, @Nullable String contentType, @Nullable String submittedFilename);
}
