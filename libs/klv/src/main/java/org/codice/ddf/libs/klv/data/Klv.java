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
package org.codice.ddf.libs.klv.data;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * A public domain class for working with Key-Length-Value (KLV)
 * byte-packing and unpacking. Supports 1-, 2-, 4-byte, and BER-encoded
 * length fields and 1-, 2-, 4-, and 16-byte key fields.
 * <p>
 * KLV has been used for years as a repeatable, no-guesswork technique
 * for byte-packing data, that is, sending data in a binary format
 * with two bytes for this integer, four bytes for that float,  and
 * so forth. KLV is used in broadcast television and is defined in
 * SMPTE 336M-2001, but it also greatly eases the burden of non-TV-related
 * applications for an easy, interchangeable binary format.
 * <p>
 * The underlying byte array is always king. If you change the key
 * length or the length encoding, you only change how the underlying
 * byte array is interpreted on subsequent calls.
 * <p>
 * Everything in KLV is Big Endian.
 * <p>
 * All <tt>getValue...</tt> methods will return up to the number
 * of bytes specified in the length fields unless there are fewer
 * bytes actually given. In that case, the number of bytes given
 * will be used. This is to make the code more robust for reading
 * corrupted data.
 *
 * @author Robert Harder
 * @author rharder # users.sourceforge.net
 * @version 0.3
 */
public class Klv {
    /**
     * The encoding style for the length field can be fixed at
     * one byte, two bytes, four bytes, or variable with
     * Basic Encoding Rules (BER).
     */
    public enum LengthEncoding {
        OneByte(1),
        TwoBytes(2),
        FourBytes(4),
        BER(5);    // Max bytes a BER field could take up

        private int value;

        LengthEncoding(int value) {
            this.value = value;
        }

        /**
         * Returns the number of bytes used to encode length,
         * or zero if encoding is <code>BER</code>
         */
        public int value() {
            return this.value;
        }
    }

    /**
     * The number of bytes in the key field can be
     * one byte, two bytes, four bytes, or sixteen bytes.
     */
    public enum KeyLength {
        OneByte(1),
        TwoBytes(2),
        FourBytes(4),
        SixteenBytes(16);

        private int value;

        KeyLength(int value) {
            this.value = value;
        }

        /**
         * Returns the number of bytes used in the key.
         */
        public int value() {
            return this.value;
        }
    }

    /**
     * Number of bytes in key.
     */
    private KeyLength keyLength;

    /**
     * The key if the key length is greater than four bytes.
     */
    private byte[] keyIfLong;

    /**
     * The key if the key length is four bytes or fewer.
     */
    private int keyIfShort;

    /**
     * The bytes from which the KLV set is made up.
     * May include irrelevant bytes so that byte arrays
     * with offset and length specified separately so arrays
     * can be passed around with a minimum of copying.
     */
    private byte[] value;

    /**
     * When instantiated by reading a byte array, this private
     * field will record the offset of the next byte in the array
     * where perhaps another KLV set begins. This is used by the
     * {@link #bytesToList} method to create a list of KLV sets
     * from a long byte array.
     */
    private int offsetAfterInstantiation;

    /**
     * Creates a KLV set from the given byte array, the given offset in that array,
     * the total length of the KLV set in the byte array, the specified key length,
     * and the specified length field encoding.
     * <p>
     * If there are not as many bytes in the array as the length field
     * suggests, as many bytes as possible will be stored as the value, and
     * the length field will reflect the actual length.
     *
     * @param theBytes       The bytes that make up the entire KLV set
     * @param offset         The offset from beginning of theBytes
     * @param keyLength      The number of bytes in the key.
     * @param lengthEncoding The length field encoding type.
     * @throws IndexOutOfBoundsException If offset is out of range of the byte array.
     */
    private Klv(final byte[] theBytes, final int offset, final KeyLength keyLength,
            final LengthEncoding lengthEncoding) {
        Preconditions.checkElementIndex(offset,
                theBytes.length,
                String.format("Offset %d is out of range (byte array length: %d).",
                        offset,
                        theBytes.length));

        // These methods will interpret the byte array
        // and set the appropriate key length and length encoding flags.
        // setLength returns the offset of where the length field ends
        // and the value portion begins. It also initializes an array in
        // this.value of the appropriate length.
        setKey(theBytes, offset, keyLength);

        // Set length and verify enough bytes exist
        // setLength(..) also establishes a this.value array.
        final int valueOffset = setLength(theBytes, offset + keyLength.value(), lengthEncoding);
        final int remaining = theBytes.length - valueOffset;
        checkEnoughBytesRemaining(remaining,
                this.value.length,
                String.format("Not enough bytes left in array (%d) for the declared length (%d).",
                        remaining,
                        this.value.length));

        System.arraycopy(theBytes, valueOffset, this.value, 0, this.value.length);

        // Private field used when creating a list of KLVs from a long array.
        this.offsetAfterInstantiation = valueOffset + this.value.length;
    }

    /**
     * Returns a byte array representing the key. This is a copy of the bytes
     * from the original byte set.
     *
     * @return the key
     */
    public byte[] getFullKey() {
        final int length = this.keyLength.value;
        final byte[] key = new byte[length];

        switch (this.keyLength) {
        case OneByte:
            key[0] = (byte) this.keyIfShort;
            break;

        case TwoBytes:
            key[0] = (byte) (this.keyIfShort >> 8);
            key[1] = (byte) this.keyIfShort;
            break;

        case FourBytes:
            key[0] = (byte) (this.keyIfShort >> 24);
            key[1] = (byte) (this.keyIfShort >> 16);
            key[2] = (byte) (this.keyIfShort >> 8);
            key[3] = (byte) this.keyIfShort;
            break;

        case SixteenBytes:
            System.arraycopy(this.keyIfLong, 0, key, 0, 16);
            break;
        }

        return key;
    }

    /**
     * Returns the value of this KLV set as a copy of the underlying byte array.
     *
     * @return the value
     */
    public byte[] getValue() {
        return Arrays.copyOf(this.value, this.value.length);
    }

    /**
     * Returns up to the first byte of the value as an 8-bit signed integer.
     *
     * @return the value as an 8-bit signed integer
     */
    public int getValueAs8bitSignedInt() {
        final byte[] bytes = getValue();
        byte value = 0;
        if (bytes.length > 0) {
            value = bytes[0];
        }
        return value;
    }

    /**
     * Returns up to the first byte of the value as an 8-bit unsigned integer.
     *
     * @return the value as an 8-bit unsigned integer
     */
    public int getValueAs8bitUnsignedInt() {
        final byte[] bytes = getValue();
        int value = 0;
        if (bytes.length > 0) {
            value = bytes[0] & 0xFF;
        }
        return value;
    }

    /**
     * Returns up to the first two bytes of the value as a 16-bit signed integer.
     *
     * @return the value as a 16-bit signed integer
     */
    public int getValueAs16bitSignedInt() {
        final byte[] bytes = getValue();
        final int length = bytes.length;
        final int shortLen = length < 2 ? length : 2;
        short value = 0;
        for (int i = 0; i < shortLen; i++) {
            value |= (bytes[i] & 0xFF) << (shortLen * 8 - i * 8 - 8);
        }
        return value;
    }

    /**
     * Returns up to the first two bytes of the value as a 16-bit unsigned integer.
     *
     * @return the value as a 16-bit unsigned integer
     */
    public int getValueAs16bitUnsignedInt() {
        final byte[] bytes = getValue();
        final int length = bytes.length;
        final int shortLen = length < 2 ? length : 2;
        int value = 0;
        for (int i = 0; i < shortLen; i++) {
            value |= (bytes[i] & 0xFF) << (shortLen * 8 - i * 8 - 8);
        }
        return value;
    }

    /**
     * Returns up to the first four bytes of the value as a 32-bit int.
     * Since all Java ints are signed, there is no signed/unsigned option.
     * If you need a 32-bit unsigned int, try {@link #getValueAs64bitLong}.
     *
     * @return the value as an int
     */
    public int getValueAs32bitInt() {
        final byte[] bytes = getValue();
        final int length = bytes.length;
        final int shortLen = length < 4 ? length : 4;
        int value = 0;
        for (int i = 0; i < shortLen; i++) {
            value |= (bytes[i] & 0xFF) << (shortLen * 8 - i * 8 - 8);
        }
        return value;
    }

    /**
     * Returns up to the first eight bytes of the value as a 64-bit signed long.
     * Note if you expect a 32-bit <b>unsigned</b> int, and since Java doesn't
     * have such a thing, you could return a long instead and get the proper effect.
     *
     * @return the value as a long
     */
    public long getValueAs64bitLong() {
        final byte[] bytes = getValue();
        final int length = bytes.length;
        final int shortLen = length < 8 ? length : 8;
        long value = 0;
        for (int i = 0; i < shortLen; i++) {
            value |= (long) (bytes[i] & 0xFF) << (shortLen * 8 - i * 8 - 8);
        }
        return value;
    }

    /**
     * Returns the first four bytes of the value as a float according
     * to IEEE 754 byte packing. See Java's Float class for details.
     * This method calls <code>Float.intBitsToFloat</code> with
     * {@link #getValueAs32bitInt} as the argument. However it does check
     * to see that the value has at least four bytes. If it does not,
     * then <tt>Float.NaN</tt> is returned.
     *
     * @return the value as a float
     */
    public float getValueAsFloat() {
        return this.getValue().length < 4 ? Float.NaN : Float.intBitsToFloat(getValueAs32bitInt());
    }

    /**
     * Returns the first eight bytes of the value as a double according
     * to IEEE 754 byte packing. See Java's Double class for details.
     * This method calls <code>Double.longBitsToDouble</code> with
     * {@link #getValueAs64bitLong} as the argument. However it does check
     * to see that the value has at least eight bytes. If it does not,
     * then <tt>Double.NaN</tt> is returned.
     *
     * @return the value as a double
     */
    public double getValueAsDouble() {
        return this.getValue().length < 8 ?
                Double.NaN :
                Double.longBitsToDouble(getValueAs64bitLong());
    }

    /**
     * Return the value as a String interpreted with the given encoding.
     *
     * @param charsetName the character encoding
     * @return value as String
     * @throws UnsupportedEncodingException if the String value cannot be interpreted using the
     *                                      given encoding
     */
    public String getValueAsString(final String charsetName) throws UnsupportedEncodingException {
        return new String(getValue(), charsetName);
    }

    /**
     * Sets the key according to the key found in the byte array
     * and of the given length. If <tt>keyLength</tt> is different
     * than what was previously set for this KLV, then this KLV's
     * key length parameter will be updated.
     *
     * @param inTheseBytes The byte array containing the key (and other stuff)
     * @param offset       The offset where to look for the key
     * @param keyLength    The length of the key
     * @return <tt>this</tt> to aid in stringing together commands
     * @throws IndexOutOfBoundsException If offset is invalid
     */
    private Klv setKey(final byte[] inTheseBytes, final int offset, final KeyLength keyLength) {
        Preconditions.checkElementIndex(offset,
                inTheseBytes.length,
                String.format("Offset %d is out of range (byte array length: %d).",
                        offset,
                        inTheseBytes.length));

        final int remaining = inTheseBytes.length - offset;
        checkEnoughBytesRemaining(remaining,
                keyLength.value(),
                String.format("Not enough bytes for %d-byte key.", keyLength.value()));

        // Set key according to length of key
        this.keyLength = keyLength;
        switch (keyLength) {
        case OneByte:
            this.keyIfShort = inTheseBytes[offset] & 0xFF;
            this.keyIfLong = null;
            break;

        case TwoBytes:
            this.keyIfShort = (inTheseBytes[offset] & 0xFF) << 8;
            this.keyIfShort |= inTheseBytes[offset + 1] & 0xFF;
            this.keyIfLong = null;
            break;

        case FourBytes:
            this.keyIfShort = (inTheseBytes[offset] & 0xFF) << 24;
            this.keyIfShort |= (inTheseBytes[offset + 1] & 0xFF) << 16;
            this.keyIfShort |= (inTheseBytes[offset + 2] & 0xFF) << 8;
            this.keyIfShort |= inTheseBytes[offset + 3] & 0xFF;
            this.keyIfLong = null;
            break;

        case SixteenBytes:
            this.keyIfLong = new byte[16];
            System.arraycopy(inTheseBytes, offset, this.keyIfLong, 0, 16);
            this.keyIfShort = 0;
            break;
        }
        return this;
    }

    /**
     * Sets the length according to the length found in the byte array
     * and of the given length encoding.
     * If <tt>lengthEncoding</tt> is different
     * than what was previously set for this KLV, then this KLV's
     * length encoding parameter will be updated.
     * An array of the appropriate length will be initialized.
     *
     * @param inTheseBytes   The byte array containing the key (and other stuff)
     * @param offset         The offset where to look for the key
     * @param lengthEncoding The length of the key
     * @return Offset where value field would begin after length
     * @throws IndexOutOfBoundsException If offset is invalid
     */
    private int setLength(final byte[] inTheseBytes, final int offset,
            final LengthEncoding lengthEncoding) {
        Preconditions.checkElementIndex(offset,
                inTheseBytes.length,
                String.format("Offset %d is out of range (byte array length: %d).",
                        offset,
                        inTheseBytes.length));

        int length = 0;
        int valueOffset = 0;
        final int remaining = inTheseBytes.length - offset;
        final String lengthEncodingErrorMessage = String.format(
                "Not enough bytes for %s length encoding.",
                lengthEncoding);

        switch (lengthEncoding) {
        case OneByte:
            checkEnoughBytesRemaining(remaining, 1, lengthEncodingErrorMessage);

            length = inTheseBytes[offset] & 0xFF;
            setLength(length, lengthEncoding);
            valueOffset = offset + 1;
            break;

        case TwoBytes:
            checkEnoughBytesRemaining(remaining, 2, lengthEncodingErrorMessage);

            length = (inTheseBytes[offset] & 0xFF) << 8;
            length |= inTheseBytes[offset + 1] & 0xFF;
            setLength(length, lengthEncoding);
            valueOffset = offset + 2;
            break;

        case FourBytes:
            checkEnoughBytesRemaining(remaining, 4, lengthEncodingErrorMessage);

            length = (inTheseBytes[offset] & 0xFF) << 24;
            length |= (inTheseBytes[offset + 1] & 0xFF) << 16;
            length |= (inTheseBytes[offset + 2] & 0xFF) << 8;
            length |= inTheseBytes[offset + 3] & 0xFF;
            setLength(length, lengthEncoding);
            valueOffset = offset + 4;
            break;

        case BER:
            // Short BER form: If high bit is not set, then
            // use the byte to determine length of payload.
            // Long BER form: If high bit is set (0x80),
            // then use low seven bits to determine how many
            // bytes that follow are themselves an unsigned
            // integer specifying the length of the payload.
            // Using more than four bytes to specify the length
            // is not supported in this code, though it's not
            // exactly illegal KLV notation either.
            checkEnoughBytesRemaining(remaining, 1, lengthEncodingErrorMessage);
            final int ber = inTheseBytes[offset] & 0xFF;

            // Easy case: low seven bits is length
            if ((ber & 0x80) == 0) {
                setLength(ber, lengthEncoding);
                valueOffset = offset + 1;
            } else {
                final int following = ber & 0x7F; // Low seven bits
                checkEnoughBytesRemaining(remaining, following + 1, lengthEncodingErrorMessage);

                for (int i = 0; i < following; i++) {
                    length |= (inTheseBytes[offset + 1 + i] & 0xFF) << (following - 1 - i) * 8;
                }
                setLength(length, lengthEncoding);
                valueOffset = offset + 1 + following;
            }
            break;
        }

        return valueOffset;
    }

    /**
     * Sets the length of the value, copying or truncating the old value
     * as appropriate for the new length.
     *
     * @param length         The new number of bytes in the Value
     * @param lengthEncoding The length encoding to use
     * @return <tt>this</tt> to aid in stringing commands together
     */
    private Klv setLength(final int length, final LengthEncoding lengthEncoding) {
        // Copy old value
        final byte[] bytes = new byte[length];
        if (this.value != null) {
            System.arraycopy(value, 0, bytes, 0, Math.min(length, this.value.length));
        }
        this.value = bytes;

        return this;
    }

    /**
     * Returns a list of KLV sets in the supplied byte array
     * assuming the provided key length and length field encoding.
     *
     * @param bytes          The byte array to parse
     * @param offset         Where to start parsing
     * @param length         How many bytes to parse
     * @param keyLength      Length of keys assumed in the KLV sets
     * @param lengthEncoding Flag indicating encoding type
     * @return List of KLVs
     */
    public static List<Klv> bytesToList(final byte[] bytes, final int offset, final int length,
            final KeyLength keyLength, LengthEncoding lengthEncoding) {
        final List<Klv> list = new LinkedList<>();

        int currentPos = offset;
        while (currentPos < offset + length) {
            final Klv klv = new Klv(bytes, currentPos, keyLength, lengthEncoding);
            currentPos = klv.offsetAfterInstantiation;
            list.add(klv);
        }

        return list;
    }

    private void checkEnoughBytesRemaining(final int actualNumberOfBytesRemaining,
            final int minimumExpectedNumberOfBytesRemaining, final String message) {
        if (actualNumberOfBytesRemaining < minimumExpectedNumberOfBytesRemaining) {
            throw new IndexOutOfBoundsException(message);
        }
    }
}
