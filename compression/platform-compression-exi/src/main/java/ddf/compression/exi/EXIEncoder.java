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
package ddf.compression.exi;

import org.openexi.proc.common.AlignmentType;
import org.openexi.proc.common.EXIOptionsException;
import org.openexi.proc.common.GrammarOptions;
import org.openexi.proc.grammars.GrammarCache;
import org.openexi.sax.Transmogrifier;
import org.openexi.sax.TransmogrifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Performs encoding and decoding xml compression using EXI.
 * <br/>
 * <br/>
 * More information is available at <a href="http://www.w3.org/XML/EXI/">http://www.w3.org/XML/EXI/</a>
 */
public final class EXIEncoder {

    private static final Logger LOGGER = LoggerFactory.getLogger(EXIEncoder.class);

    private EXIEncoder() {

    }

    /**
     * Takes the incoming xmlStream and performs EXI-encoding on it into the exiStream.
     *
     * @param xmlStream Input of xml data
     * @param exiStream Output of exi-encoded data
     * @throws EXIOptionsException
     * @throws TransmogrifierException
     * @throws java.io.IOException
     */
    public static void encode(InputStream xmlStream, OutputStream exiStream)
            throws EXIOptionsException, TransmogrifierException, IOException {
        Transmogrifier trans = new Transmogrifier();
        trans.setAlignmentType(AlignmentType.bitPacked);
        GrammarCache grammarCache = new GrammarCache(null, GrammarOptions.DEFAULT_OPTIONS);
        trans.setGrammarCache(grammarCache);
        trans.setOutputStream(exiStream);
        LOGGER.debug("Starting EXI encoding process.");
        trans.encode(new InputSource(xmlStream));
        LOGGER.debug("EXI encoding complete.");
    }

    /**
     * Takes the incoming exiStream and performs EXI-decoding on it into the xmlStream.
     *
     * @param exiStream Input of exi-encoded data
     * @param xmlStream Output of xml data
     */
    public static void decode(InputStream exiStream, OutputStream xmlStream) {
        throw new IllegalArgumentException("Decode operation not supported.");
    }
}
