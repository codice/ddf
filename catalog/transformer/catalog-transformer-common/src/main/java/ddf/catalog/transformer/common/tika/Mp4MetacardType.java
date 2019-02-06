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
package ddf.catalog.transformer.common.tika;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import java.util.Set;

/** Base MetacardType for Mp4 Metacards. */
public class Mp4MetacardType extends MetacardTypeImpl {

  private static final String MP4_METACARD_TYPE_NAME = "mp4";

  private static final String EXT_PREFIX = "ext.mp4.";

  public static final String AUDIO_SAMPLE_RATE = EXT_PREFIX + "audio-sample-rate";

  public Mp4MetacardType() {
    this(MP4_METACARD_TYPE_NAME, null);
  }

  public Mp4MetacardType(String name, Set<AttributeDescriptor> descriptors) {
    super(name, descriptors);
    addMp4Attributes();
  }

  private void addMp4Attributes() {
    add(
        new AttributeDescriptorImpl(
            AUDIO_SAMPLE_RATE,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.INTEGER_TYPE));
  }
}
