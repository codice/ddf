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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.taktik.mpegts.MTSPacket;
import org.taktik.mpegts.sources.ResettableMTSSource;

/** Validate the behavior of {@link MTSValidPacketIterator}. */
@RunWith(MockitoJUnitRunner.class)
public class MTSValidPacketIteratorTest {

  @Mock private ResettableMTSSource mockSource;

  private List<MTSPacket> mockedValidPackets;

  private Iterator<MTSPacket> validPackets;

  private MTSValidPacketIterator packetIterator;

  @Before
  public void setup() throws Exception {
    packetIterator = new MTSValidPacketIterator(mockSource);
    mockedValidPackets = new ArrayList<>();

    mockedValidPackets.add(mock(MTSPacket.class));
    mockedValidPackets.add(mock(MTSPacket.class));
    mockedValidPackets.add(mock(MTSPacket.class));

    validPackets = mockedValidPackets.iterator();
  }

  @Test
  public void testBadPacketsAtBeginning() throws Exception {
    when(mockSource.nextPacket())
        .thenThrow(Exception.class)
        .thenThrow(Exception.class)
        .thenReturn(validPackets.next())
        .thenReturn(validPackets.next())
        .thenReturn(validPackets.next())
        .thenReturn(null);
    validatePacketsFromIterator();
    assertThat(packetIterator.getPacketsProcessed(), is(5L));
    assertThat(packetIterator.getPacketsFailed(), is(2L));
  }

  @Test
  public void testBadPacketsAtEnd() throws Exception {
    when(mockSource.nextPacket())
        .thenReturn(validPackets.next())
        .thenReturn(validPackets.next())
        .thenReturn(validPackets.next())
        .thenThrow(Exception.class)
        .thenThrow(Exception.class)
        .thenReturn(null);
    validatePacketsFromIterator();
    assertThat(packetIterator.getPacketsProcessed(), is(5L));
    assertThat(packetIterator.getPacketsFailed(), is(2L));
  }

  @Test
  public void testBadPacketsInbetween() throws Exception {
    when(mockSource.nextPacket())
        .thenReturn(validPackets.next())
        .thenThrow(Exception.class)
        .thenReturn(validPackets.next())
        .thenThrow(Exception.class)
        .thenThrow(Exception.class)
        .thenThrow(Exception.class)
        .thenReturn(validPackets.next())
        .thenReturn(null);
    validatePacketsFromIterator();
    assertThat(packetIterator.getPacketsProcessed(), is(7L));
    assertThat(packetIterator.getPacketsFailed(), is(4L));
  }

  private void validatePacketsFromIterator() throws Exception {
    mockedValidPackets.forEach(
        mtsPacket -> assertThat(mtsPacket, is(packetIterator.getNextValidPacket())));
    assertThat(null, is(packetIterator.getNextValidPacket()));
  }
}
