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
package ddf.video.thumbnail;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import javax.activation.MimeType;

public interface VideoThumbnail {

  /**
   * Extract a thumnail from a video file. If mimeType is not video ({@link #isVideo(MimeType)}),
   * then returns Optional.empty(). If thumbnail extraction fails, then throws an exception. The
   * returned byte array is intended for {@link Metacard#getThumbnail}.
   */
  Optional<byte[]> videoThumbnail(File file, MimeType mimeType)
      throws IOException, InterruptedException;

  /**
   * Test if a specific mime-type is supported for thumbnail extraction. This allows clients to skip
   * expensive setup requirements before calling {@link #videoThumbnail(File, MimeType)}.
   *
   * <p>TODO should this method be changed to isSupported and define it to include other processing
   * requirements such as file size limits?
   */
  boolean isVideo(MimeType mimeType);
}
