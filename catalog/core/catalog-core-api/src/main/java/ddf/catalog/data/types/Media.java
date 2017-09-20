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
package ddf.catalog.data.types;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface Media {
  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the format of a {@link
   * ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String FORMAT = "media.format";
  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the format version of a {@link
   * ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String FORMAT_VERSION = "media.format-version";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the bit rate of a {@link
   * ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String BITS_PER_SECOND = "media.bit-rate";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the number of bits per image component of
   * a {@link ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String BITS_PER_SAMPLE = "media.bits-per-sample";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the type of compression of a {@link
   * ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String COMPRESSION = "media.compression";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the encoding scheme of a {@link
   * ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String ENCODING = "media.encoding";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the center of the video frame of a {@link
   * ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String FRAME_CENTER = "media.frame-center";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the frame rate of a {@link
   * ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String FRAMES_PER_SECOND = "media.frame-rate";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the height in pixels of a {@link
   * ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String HEIGHT = "media.height-pixels";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the number of spectral bands of a {@link
   * ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String NUMBER_OF_BANDS = "media.number-of-bands";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing whether progressive or interlaced scans
   * are being applied to a {@link ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String SCANNING_MODE = "media.scanning-mode";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the type of a {@link
   * ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String TYPE = "media.type";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the width in pixels of a {@link
   * ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String WIDTH = "media.width-pixels";

  /**
   * {@link ddf.catalog.data.Attribute} name for accessing the duration in seconds of a {@link
   * ddf.catalog.resource.Resource} of a {@link Metacard}. <br>
   */
  String DURATION = "media.duration";
}
