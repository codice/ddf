/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.security.filter.saml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class DeflateEncoderDecoder {
    public InputStream inflateToken(byte[] deflatedToken) throws DataFormatException {
        Inflater inflater = new Inflater(true);
        inflater.setInput(deflatedToken);

        byte[] input = new byte[deflatedToken.length * 2];
        int inflatedLen = 0;
        int inputLen = 0;
        byte[] inflatedToken = input;
        while (!inflater.finished()) {
            inputLen = inflater.inflate(input);
            if (!inflater.finished()) {
                inflatedToken = new byte[input.length + inflatedLen];
                System.arraycopy(input, 0, inflatedToken, inflatedLen, inputLen);
                inflatedLen += inputLen;
            }
        }
        InputStream is = new ByteArrayInputStream(input, 0, inputLen);
        if (inflatedToken != input) {
            is = new SequenceInputStream(new ByteArrayInputStream(inflatedToken, 0, inflatedLen), is);
        }
        return is;
    }

    public byte[] deflateToken(byte[] tokenBytes) {
        Deflater compresser = new Deflater(Deflater.DEFLATED, true);

        compresser.setInput(tokenBytes);
        compresser.finish();

        byte[] output = new byte[tokenBytes.length];

        int compressedDataLength = compresser.deflate(output);
        byte[] result = new byte[compressedDataLength];
        System.arraycopy(output, 0, result, 0, compressedDataLength);
        return result;
    }
}