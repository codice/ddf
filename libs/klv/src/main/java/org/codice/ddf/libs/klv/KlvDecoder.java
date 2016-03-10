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
package org.codice.ddf.libs.klv;

import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.codice.ddf.libs.klv.data.Klv;

import com.google.common.base.Preconditions;

/**
 * Decodes bytes that contain KLV-encoded data.
 */
public class KlvDecoder {
    private final KlvContext klvContext;

    /**
     * Creates a {@code KlvDecoder} with the given {@link KlvContext}.
     *
     * @param klvContext the {@code KlvContext} containing the properties of the KLV data to be
     *                   decoded by this {@code KlvDecoder}
     */
    public KlvDecoder(final KlvContext klvContext) {
        this.klvContext = klvContext;
    }

    /**
     * Decodes the KLV data inside {@code klvBytes} according to the properties in the
     * {@link KlvContext} that was provided in the constructor.
     * <p>
     * It will recursively decode any {@link org.codice.ddf.libs.klv.data.set.KlvLocalSet}s it finds,
     * provided that they are given in the {@code KlvContext}.
     * <p>
     * The decoded KLV values are returned in a new {@code KlvContext} object that has the same
     * structure as the {@code KlvContext} that was provided in the constructor but contains only
     * the {@link KlvDataElement}s that were found in the given bytes.
     *
     * @param klvBytes bytes encoding data in KLV format
     * @return a new {@code KlvContext} containing the decoded KLV data elements
     * @throws IllegalArgumentException if {@code klvBytes} is null
     * @throws KlvDecodingException     if the KLV cannot be decoded using the given context information
     */
    public KlvContext decode(final byte[] klvBytes) throws KlvDecodingException {
        Preconditions.checkArgument(klvBytes != null,
                "The array of bytes to decode cannot be null.");

        List<Klv> klvDataElements;

        try {
            klvDataElements = Klv.bytesToList(klvBytes,
                    0,
                    klvBytes.length,
                    klvContext.getKeyLength(),
                    klvContext.getLengthEncoding());
        } catch (RuntimeException e) {
            throw new KlvDecodingException(String.format(
                    "Could not decode KLV using the given key length %s and length encoding %s",
                    klvContext.getKeyLength(),
                    klvContext.getLengthEncoding()), e);
        }

        final KlvContext decodedContext = new KlvContext(klvContext.getKeyLength(),
                klvContext.getLengthEncoding());
        final Map<String, KlvDataElement> keyToDataElementMap = klvContext.getKeyToDataElementMap();

        klvDataElements.forEach(klv -> {
            final String key = DatatypeConverter.printHexBinary(klv.getFullKey());

            if (keyToDataElementMap.containsKey(key)) {
                final KlvDataElement dataElementCopy = keyToDataElementMap.get(key)
                        .copy();
                dataElementCopy.decodeValue(klv);
                decodedContext.addDataElement(dataElementCopy);
            }
        });

        return decodedContext;
    }
}
