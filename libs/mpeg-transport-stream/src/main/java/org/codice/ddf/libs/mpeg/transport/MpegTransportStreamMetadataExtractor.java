/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.libs.mpeg.transport;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.jcodec.api.JCodecException;
import org.jcodec.containers.mps.MTSUtils.StreamType;
import org.jcodec.containers.mps.psi.PMTSection;
import org.jcodec.containers.mps.psi.PMTSection.PMTStream;
import org.taktik.mpegts.MTSPacket;
import org.taktik.mpegts.PATSection;
import org.taktik.mpegts.sources.MTSSource;
import org.taktik.mpegts.sources.MTSSources;
import org.taktik.mpegts.sources.ResettableMTSSource;

import com.google.common.io.ByteSource;

/**
 * This class is for extracting arbitrary metadata (as raw bytes) from an MPEG transport stream.
 */
public class MpegTransportStreamMetadataExtractor {
    private final ByteSource byteSource;

    private final Set<Integer> programMapTablePacketIdDirectory = new HashSet<>();

    private final Map<Integer, PMTSection> programMapTables = new HashMap<>();

    private final Map<Integer, PMTStream> programElementaryStreams = new HashMap<>();

    private final Map<Integer, byte[]> currentMetadataPacketBytesByStream = new HashMap<>();

    /**
     * Constructs an {@code MpegTransportStreamMetadataExtractor} with the given {@link ByteSource}
     * as the provider of the transport stream bytes.
     *
     * @param byteSource the {@code ByteSource} providing the transport stream bytes
     */
    public MpegTransportStreamMetadataExtractor(final ByteSource byteSource) {
        this.byteSource = byteSource;
    }

    /**
     * Parses the transport stream and calls the given callback for each metadata packet in each
     * metadata stream found in the transport stream. The callback is called immediately upon
     * finding a complete metadata packet.
     *
     * @param callback a callback that will be called for each metadata packet in each metadata
     *                 stream found in the transport stream, where the first parameter is the packet
     *                 ID of the metadata stream and the second parameter is the metadata packet's
     *                 payload
     * @throws Exception if an error occurs while parsing the transport stream
     */
    public void getMetadata(final BiConsumer<Integer, byte[]> callback) throws Exception {
        extractTransportStreamMetadata(callback);
    }

    /**
     * Parses the transport stream and returns all the metadata packet payloads (in the order in
     * which they were encountered) that belong to each metadata stream.
     *
     * @return a {@link Map} whose keys are the packet IDs of the metadata streams and whose values
     * are the packet payloads belonging to that stream
     * @throws Exception if an error occurs while parsing the transport stream
     */
    public Map<Integer, List<byte[]>> getMetadata() throws Exception {
        final Map<Integer, List<byte[]>> metadataPacketsByStream = new HashMap<>();

        getMetadata((streamId, metadataPacketBytes) -> {
            if (!metadataPacketsByStream.containsKey(streamId)) {
                metadataPacketsByStream.put(streamId, new ArrayList<>());
            }

            metadataPacketsByStream.get(streamId)
                    .add(metadataPacketBytes);
        });

        return metadataPacketsByStream;
    }

    private void extractTransportStreamMetadata(final BiConsumer<Integer, byte[]> callback)
            throws Exception {
        final ResettableMTSSource source = MTSSources.from(byteSource);

        getProgramSpecificInformation(source);

        source.reset();

        MTSPacket transportStreamPacket;

        while ((transportStreamPacket = source.nextPacket()) != null) {
            final int packetId = transportStreamPacket.getPid();

            if (isElementaryStreamPacket(packetId)) {
                handleElementaryStreamPacket(transportStreamPacket, packetId, callback);
            }
        }

        handleLastPacketOfEachStream(callback);
    }

    private void getProgramSpecificInformation(final MTSSource source) throws Exception {
        MTSPacket packet;

        while ((packet = source.nextPacket()) != null) {
            if (isProgramAssociationTable(packet) && !seenProgramAssociationTable()) {
                getProgramAssociationTable(packet);
            } else if (isProgramMapTable(packet) && !seenProgramMapTable(packet)) {
                getProgramMapTable(packet);

                if (foundAllProgramMapTables()) {
                    break;
                }
            }
        }
    }

    private boolean seenProgramAssociationTable() {
        return !programMapTablePacketIdDirectory.isEmpty();
    }

    private boolean seenProgramMapTable(final MTSPacket packet) {
        return programMapTables.containsKey(packet.getPid());
    }

    private boolean isProgramAssociationTable(final MTSPacket packet) {
        return packet.getPid() == 0 && packet.isPayloadUnitStartIndicator();
    }

    private void getProgramAssociationTable(final MTSPacket packet) throws JCodecException {
        final ByteBuffer payload = packet.getPayload();

        final int pointer = payload.get() & 0xff;
        payload.position(payload.position() + pointer);
        final PATSection programAssociationTable = PATSection.parse(payload);
        programMapTablePacketIdDirectory.addAll(programAssociationTable.getPrograms()
                .values());

        if (programMapTablePacketIdDirectory.isEmpty()) {
            throw new JCodecException("No programs found in transport stream.");
        }
    }

    private boolean isProgramMapTable(final MTSPacket packet) {
        return programMapTablePacketIdDirectory.contains(packet.getPid())
                && packet.isPayloadUnitStartIndicator();
    }

    private void getProgramMapTable(final MTSPacket packet) {
        final ByteBuffer payload = packet.getPayload();

        final int pointer = payload.get() & 0xff;
        payload.position(payload.position() + pointer);

        final int packetId = packet.getPid();
        final PMTSection pmt = PMTSection.parsePMT(payload);
        programMapTables.put(packetId, pmt);

        for (final PMTStream stream : pmt.getStreams()) {
            programElementaryStreams.put(stream.getPid(), stream);
        }
    }

    private boolean foundAllProgramMapTables() {
        final Set<Integer> packetIdsOfProgramMapTablesSeen = programMapTables.keySet();
        return CollectionUtils.isEqualCollection(packetIdsOfProgramMapTablesSeen,
                programMapTablePacketIdDirectory);
    }

    private boolean isElementaryStreamPacket(final int packetId) {
        return packetId != 0 && !programMapTablePacketIdDirectory.contains(packetId);
    }

    private byte[] getByteBufferAsBytes(final ByteBuffer buffer) {
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return bytes;
    }

    private void handleElementaryStreamPacket(final MTSPacket packet, final int packetId,
            final BiConsumer<Integer, byte[]> callback) {
        if (programElementaryStreams.containsKey(packetId)) {
            final PMTStream stream = programElementaryStreams.get(packetId);

            if (isMetadataStream(stream)) {
                final byte[] currentMetadataPacketBytes = currentMetadataPacketBytesByStream.get(
                        packetId);

                final boolean startingNewMetadataPacket = packet.isPayloadUnitStartIndicator();
                final boolean currentMetadataPacketToHandle = currentMetadataPacketBytes != null;
                final boolean reachedEndOfCurrentMetadataPacket =
                        startingNewMetadataPacket && currentMetadataPacketToHandle;

                final byte[] payloadBytes = getByteBufferAsBytes(packet.getPayload());

                if (reachedEndOfCurrentMetadataPacket) {
                    callback.accept(packetId, currentMetadataPacketBytes);
                    startNewMetadataPacketBytes(packetId, payloadBytes);
                } else if (startingNewMetadataPacket) {
                    startNewMetadataPacketBytes(packetId, payloadBytes);
                } else if (currentMetadataPacketToHandle) {
                    final byte[] concatenatedMetadataPacket = ArrayUtils.addAll(
                            currentMetadataPacketBytes,
                            payloadBytes);
                    currentMetadataPacketBytesByStream.put(packetId, concatenatedMetadataPacket);
                }
            }
        }
    }

    private boolean isPrivateDataStream(final PMTStream stream) {
        return stream.getStreamType() == StreamType.PRIVATE_DATA;
    }

    private boolean isMetadataPesStream(final PMTStream stream) {
        return stream.getStreamType() == StreamType.META_PES;
    }

    private boolean isMetadataStream(final PMTStream stream) {
        return isPrivateDataStream(stream) || isMetadataPesStream(stream);
    }

    private void startNewMetadataPacketBytes(final int packetId, final byte[] newMetadataBytes) {
        currentMetadataPacketBytesByStream.put(packetId, newMetadataBytes);
    }

    /*
     * In a transport stream, any elementary stream packet can be large enough to require multiple
     * transport stream packets to hold it. Therefore, when analyzing the transport stream packets,
     * knowing that you've seen a complete metadata packet for a given stream is possible only if
     * you encounter a new metadata packet for that stream (meaning the previous packet has ended).
     * This means that the last metadata packet for each stream won't be handled during the pass
     * over the transport stream and they will need to be handled separately.
     */
    private void handleLastPacketOfEachStream(final BiConsumer<Integer, byte[]> callback) {
        currentMetadataPacketBytesByStream.forEach(callback);
    }
}
