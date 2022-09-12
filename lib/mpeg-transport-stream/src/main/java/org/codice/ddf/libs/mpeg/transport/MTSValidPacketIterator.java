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

import static org.apache.commons.lang.Validate.notNull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.taktik.mpegts.MTSPacket;
import org.taktik.mpegts.sources.ResettableMTSSource;

/**
 * Support class for working with a stream of packets from a source where the underlying data may
 * cause exceptions. In this case, a greedy approach is taken; every bad packet is ignored without
 * limit in hopes good data will be encountered.
 *
 * <p>Caution: Be careful when modifying this class. Methods here mutate its state. While a
 * recursive solution would have been preferrable, ingested products with sufficiently large
 * segments of bad data would cause a stack overflow.
 */
public class MTSValidPacketIterator {
  private static final Logger LOGGER = LoggerFactory.getLogger(MTSValidPacketIterator.class);

  private final ResettableMTSSource source;

  private MTSPacket currentPacket;

  private boolean currentPacketIsValid;

  private long packetsProcessed;

  private long packetsFailed;

  public long getPacketsProcessed() {
    return packetsProcessed;
  }

  public long getPacketsFailed() {
    return packetsFailed;
  }

  public MTSValidPacketIterator(ResettableMTSSource source) {
    notNull(source);
    this.source = source;

    currentPacket = null;
    currentPacketIsValid = false;

    packetsProcessed = 0;
    packetsFailed = 0;
  }

  /**
   * Iterate through any number of exception-causing packets to reach either a valid one or the end
   * of the packet stream.
   *
   * @return The next packet in the provided source that <b>does not</b> throw an exception. Or
   *     {@code null} if no more packets are available.
   */
  public MTSPacket getNextValidPacket() {
    do {
      scanNextPacket();
    } while (!currentPacketIsValid);
    return currentPacket;
  }

  private void scanNextPacket() {
    try {
      currentPacket = source.nextPacket();
      handleExceptionNotThrown();
    } catch (Exception e) {
      LOGGER.trace("Skipping invalid MTS packet, caused by: ", e);
      currentPacketIsValid = false;
      packetsProcessed++;
      packetsFailed++;
    }
  }

  private void handleExceptionNotThrown() {
    currentPacketIsValid = true;
    if (currentPacket != null) {
      packetsProcessed++;
    }
  }
}
