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
package org.codice.ddf.libs.mpeg.transport;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.io.ByteSource;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class MpegTransportStreamMetadataExtractorTest {
  private MpegTransportStreamMetadataExtractor getExtractor() throws IOException {
    final ByteSource byteSource =
        ByteSource.wrap(
            IOUtils.toByteArray(getClass().getClassLoader().getResourceAsStream("dayflight.mpg")));

    return new MpegTransportStreamMetadataExtractor(byteSource);
  }

  @Test
  public void testExtractCallback() throws Exception {
    final MpegTransportStreamMetadataExtractor extractor = getExtractor();

    // Mockito cannot spy anonymous classes.
    final BiConsumer<Integer, byte[]> callback =
        new BiConsumer<Integer, byte[]>() {
          @Override
          public void accept(Integer integer, byte[] bytes) {}
        };
    final BiConsumer<Integer, byte[]> callbackSpy = spy(callback);
    extractor.getMetadata(callbackSpy);

    // The packet ID of the metadata stream in this file is 497.
    final ArgumentCaptor<byte[]> metadataCaptor = ArgumentCaptor.forClass(byte[].class);
    verify(callbackSpy, times(12)).accept(eq(497), metadataCaptor.capture());

    verifyExtractedBytes(metadataCaptor.getAllValues());
  }

  @Test
  public void testExtractAll() throws Exception {
    final MpegTransportStreamMetadataExtractor extractor = getExtractor();

    final Map<Integer, List<byte[]>> metadataStreams = extractor.getMetadata();

    // The packet ID of the metadata stream in this file is 497.
    assertThat(metadataStreams, hasKey(497));

    verifyExtractedBytes(metadataStreams.get(497));
  }

  private void verifyExtractedBytes(final List<byte[]> metadataPackets) {
    assertThat(metadataPackets.size(), is(12));

    final byte[] expectedMetadataBytesNoPayload =
        new byte[] {0x00, 0x00, 0x01, (byte) 0xBD, 0x00, 0x03, (byte) 0x84, 0x00, 0x00};

    final byte[] expectedHeaderMetadataBytesWithPayload =
        new byte[] {0x00, 0x00, 0x01, (byte) 0xBD, 0x00, (byte) 0xA6};

    for (int packetNum = 0; packetNum < metadataPackets.size(); ++packetNum) {
      final byte[] metadataPacketBytes = metadataPackets.get(packetNum);
      // The test file has 12 metadata packets that belong to the metadata stream. The 11th
      // packet is the only one with a payload.
      if (packetNum == 10) {
        // The last byte in the header (0xA6) is the length of the payload.
        assertThat(
            metadataPacketBytes.length, is(0xA6 + expectedHeaderMetadataBytesWithPayload.length));
        assertThat(
            Arrays.copyOf(metadataPacketBytes, 6), is(expectedHeaderMetadataBytesWithPayload));
      } else {
        assertThat(metadataPacketBytes, is(expectedMetadataBytesNoPayload));
      }
    }
  }
}
