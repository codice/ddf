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
 **/
package ddf.catalog.transformer.xml;

import java.io.IOException;
import java.io.Writer;
import java.util.BitSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.core.util.QuickWriter;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;

import ddf.catalog.transformer.api.PrintWriter;

/**
 * EscapingPrintWriter supports the examination of individual 16 bit characters in a stream writer.
 * <p>
 * In the default mode, EscapingPrintWriter escapes XML/HTML metacharacters,
 * supplementary UNICODE characters, most control characters and any undefined char values.
 * <p>
 * EscapingPrintWriter is optimized for speed by caching well-known characters locally and by
 * calling write() on the underlying writer only once, after examining every character in the
 * stream.
 * <p>
 * In raw mode, EscapingPrintWriter does no escaping of characters.
 * <p>
 * EscapingPrintWriter is not thread-safe; instances should not be shared.
 * <p>
 * There are existing XML escaping libraries, such as
 * https://commons.apache.org/proper/commons-lang/javadocs/api-2.6/org/apache/commons/lang/StringEscapeUtils.html
 * <p>
 * But the public static String escapeXml(String str) method supports only the five basic
 * XML entities (gt, lt, quot, amp, apos).
 * </p>
 */
public class EscapingPrintWriter extends PrettyPrintWriter implements PrintWriter {

    private static final Logger LOGGER = LoggerFactory.getLogger(EscapingPrintWriter.class);

    private static final char[] AMP = "&amp;".toCharArray();

    private static final char[] LT = "&lt;".toCharArray();

    private static final char[] GT = "&gt;".toCharArray();

    private static final char[] CR = "&#xd;".toCharArray();

    private static final char[] APOS = "&apos;".toCharArray();

    private static final char[] QUOT = "&quot;".toCharArray();

    private boolean isRawText = false;

    private static final BitSet WELL_KNOWN_CHARACTERS;

    static {
        /*
            - BitSet is vector of bits that grows as needed.
            - Each component of the bit set has a boolean value.
            - The bits of a BitSet are indexed by nonnegative integers.
            - Individual indexed bits can be examined, set, or cleared.
            - By default, all bits in the set initially have the value false.
            - A BitSet is not safe for multithreaded use without external synchronization, but
              this bitset does not need synchronization b/c it is:
                - private to this class.
                - statically initialized; java guarantees to be single-threaded.
                - only read, and never written, by instances.
            - using a bitset to cache the lookup of characters trades away space efficiency
              to gain time efficiency.
            - if space efficiency becomes a concern, this cache could be implemented
              differently, perhaps as a collection of "ranges".
         */
        WELL_KNOWN_CHARACTERS = new BitSet();
        for (int i = 0; i < Character.MAX_CODE_POINT + 1; i++) {
            //  Basic Multilingual Plane (BMP) *can* be represented using a single char.
            if (Character.isBmpCodePoint(i) && isWellKnownCharacter((char) i)) {
                WELL_KNOWN_CHARACTERS.set(i);
            }
        }
    }

    private final Writer writer;

    public EscapingPrintWriter(Writer writer) {
        super(writer);
        this.writer = writer;
    }

    private static boolean isWellKnownCharacter(char c) {
        return Character.isDefined(c) && !Character.isISOControl(c) && !Character.isSurrogate(c);
    }

    @Override
    public void setRawValue(String text) {
        try {
            isRawText = true;
            setValue(text);
        } finally {
            isRawText = false;
        }
    }

    @Override
    public String makeString() {
        try {
            writer.flush();
        } catch (IOException e) {
            LOGGER.error("Error flushing.", e);
        }
        return writer.toString();
    }

    /*
        - String#length is equal to the number of "Unicode code units" in the string.
        - Java "Unicode code unit" means a 16-bit char value in the UTF-16 encoding.
        - Java uses the UTF-16 representation in char arrays, String and StringBuffer classes.
        - original char data type (Character object) defines "characters" as fixed-width 16-bit entities.
        - Unicode Standard has since been changed to allow for "characters" whose representation requires more than 16 bits.
        - Characters whose code points are greater than max char value (U+FFFF) are called "supplementary characters."
        - "supplementary characters" are represented by a pair of char values, the first from
           the high-surrogates range, (\uD800-\uDBFF), the second from the low-surrogates range
           (\uDC00-\uDFFF).

        - Character#isDefined(char ch) cannot handle supplementary characters.
        -
        - A char value
            - *can* always represent a valid Basic Multilingual Plane (BMP) code point
            - but, the valid code point may map to "undefined" character
            - but, the valid code point may map to "surrogate" (partial) character
     */
    @Override
    protected void writeText(QuickWriter writer, String text) {
        if (text == null) {
            return;
        }

        if (isRawText) {
            writer.write(text);
        } else {
            int length = text.length();
            StringBuilder sb = new StringBuilder();
            char c;
            for (int i = 0; i < length; i++) {
                c = text.charAt(i);
                switch (c) {
                case '&':
                    sb.append(AMP);
                    break;
                case '<':
                    sb.append(LT);
                    break;
                case '>':
                    sb.append(GT);
                    break;
                case '\'':
                    sb.append(APOS);
                    break;
                case '\"':
                    sb.append(QUOT);
                    break;
                case '\r':
                    sb.append(CR);
                    break;
                case '\t':
                case '\n':
                    sb.append(c); // allow these control chars as is.
                    break;
                default:
                    if (WELL_KNOWN_CHARACTERS.get((int) c)) {
                        sb.append(c);
                    } else {
                        sb.append("&#x").append(Integer.toHexString(c)).append(';');
                    }
                }
            } // for loop
            writer.write(sb.toString());
        }
    } // end writeText()

}
