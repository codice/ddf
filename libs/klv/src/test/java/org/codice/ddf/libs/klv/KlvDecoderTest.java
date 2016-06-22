/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.libs.klv;

import static java.lang.System.arraycopy;
import static org.codice.ddf.libs.klv.Utilities.intToBytes;
import static org.codice.ddf.libs.klv.Utilities.longToBytes;
import static org.codice.ddf.libs.klv.Utilities.shortToBytes;
import static org.codice.ddf.libs.klv.data.Klv.KeyLength;
import static org.codice.ddf.libs.klv.data.Klv.LengthEncoding;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.codice.ddf.libs.klv.data.numerical.KlvByte;
import org.codice.ddf.libs.klv.data.numerical.KlvDouble;
import org.codice.ddf.libs.klv.data.numerical.KlvFloat;
import org.codice.ddf.libs.klv.data.numerical.KlvInt;
import org.codice.ddf.libs.klv.data.numerical.KlvIntegerEncodedFloatingPoint;
import org.codice.ddf.libs.klv.data.numerical.KlvLong;
import org.codice.ddf.libs.klv.data.numerical.KlvShort;
import org.codice.ddf.libs.klv.data.numerical.KlvUnsignedByte;
import org.codice.ddf.libs.klv.data.numerical.KlvUnsignedShort;
import org.codice.ddf.libs.klv.data.raw.KlvBytes;
import org.codice.ddf.libs.klv.data.set.KlvLocalSet;
import org.codice.ddf.libs.klv.data.text.KlvString;
import org.junit.BeforeClass;
import org.junit.Test;

public class KlvDecoderTest {
    private static final String UAS_DATALINK_LOCAL_SET_UNIVERSAL_KEY =
            "UAS Datalink Local Set Universal Key";

    private static final String TIMESTAMP = "timestamp";

    private static final String UAS_LS_VERSION_NUMBER = "UAS LS Version Number";

    private static final String PLATFORM_HEADING_ANGLE = "platform heading angle";

    private static final String PLATFORM_PITCH_ANGLE = "platform pitch angle";

    private static final String PLATFORM_ROLL_ANGLE = "platform roll angle";

    private static final String IMAGE_SOURCE_SENSOR = "image source sensor";

    private static final String IMAGE_COORDINATE_SYSTEM = "image coordinate system";

    private static final String SENSOR_LATITUDE = "sensor latitude";

    private static final String SENSOR_LONGITUDE = "sensor longitude";

    private static final String SENSOR_TRUE_ALTITUDE = "sensor true altitude";

    private static final String SENSOR_HORIZONTAL_FOV = "sensor horizontal fov";

    private static final String SENSOR_VERTICAL_FOV = "sensor vertical fov";

    private static final String SENSOR_RELATIVE_AZIMUTH_ANGLE = "sensor relative azimuth angle";

    private static final String SENSOR_RELATIVE_ELEVATION_ANGLE = "sensor relative elevation angle";

    private static final String SENSOR_RELATIVE_ROLL_ANGLE = "sensor relative roll angle";

    private static final String SLANT_RANGE = "slant range";

    private static final String TARGET_WIDTH = "target width";

    private static final String FRAME_CENTER_LATITUDE = "frame center latitude";

    private static final String FRAME_CENTER_LONGITUDE = "frame center longitude";

    private static final String FRAME_CENTER_ELEVATION = "frame center elevation";

    private static final String TARGET_LOCATION_LATITUDE = "target location latitude";

    private static final String TARGET_LOCATION_LONGITUDE = "target location longitude";

    private static final String TARGET_LOCATION_ELEVATION = "target location elevation";

    private static final String PLATFORM_GROUND_SPEED = "platform ground speed";

    private static final String GROUND_RANGE = "ground range";

    private static final String CHECKSUM = "checksum";

    private static final Map<String, Object> EXPECTED_VALUES = new HashMap<>();

    private static final Set<KlvDataElement> DATA_ELEMENTS = new HashSet<>();

    @BeforeClass
    public static void setUpClass() {
        // The test KLV is a UAS Datalink Local Set (MISB ST 0601).
        EXPECTED_VALUES.put(TIMESTAMP, 1245257585099653L);
        EXPECTED_VALUES.put(UAS_LS_VERSION_NUMBER, (byte) 1);
        EXPECTED_VALUES.put(PLATFORM_HEADING_ANGLE, 15675);
        EXPECTED_VALUES.put(PLATFORM_PITCH_ANGLE, (short) 5504);
        EXPECTED_VALUES.put(PLATFORM_ROLL_ANGLE, (short) 338);
        EXPECTED_VALUES.put(IMAGE_SOURCE_SENSOR, "EON");
        EXPECTED_VALUES.put(IMAGE_COORDINATE_SYSTEM, "Geodetic WGS84");
        EXPECTED_VALUES.put(SENSOR_LATITUDE, 1304747195);
        EXPECTED_VALUES.put(SENSOR_LONGITUDE, -1314362114);
        EXPECTED_VALUES.put(SENSOR_TRUE_ALTITUDE, 8010);
        EXPECTED_VALUES.put(SENSOR_HORIZONTAL_FOV, 133);
        EXPECTED_VALUES.put(SENSOR_VERTICAL_FOV, 75);
        EXPECTED_VALUES.put(SENSOR_RELATIVE_AZIMUTH_ANGLE, 550031997L);
        EXPECTED_VALUES.put(SENSOR_RELATIVE_ELEVATION_ANGLE, -52624680);
        EXPECTED_VALUES.put(SENSOR_RELATIVE_ROLL_ANGLE, 4273523553L);
        EXPECTED_VALUES.put(SLANT_RANGE, 9387617L);
        EXPECTED_VALUES.put(TARGET_WIDTH, 457);
        EXPECTED_VALUES.put(FRAME_CENTER_LATITUDE, 1306364970);
        EXPECTED_VALUES.put(FRAME_CENTER_LONGITUDE, -1312907532);
        EXPECTED_VALUES.put(FRAME_CENTER_ELEVATION, 2949);
        EXPECTED_VALUES.put(TARGET_LOCATION_LATITUDE, 1306364970);
        EXPECTED_VALUES.put(TARGET_LOCATION_LONGITUDE, -1312907532);
        EXPECTED_VALUES.put(TARGET_LOCATION_ELEVATION, 2949);
        EXPECTED_VALUES.put(PLATFORM_GROUND_SPEED, (short) 46);
        EXPECTED_VALUES.put(GROUND_RANGE, 9294889L);
        EXPECTED_VALUES.put(CHECKSUM, 7263);

        DATA_ELEMENTS.add(new KlvLong(new byte[] {0x02}, TIMESTAMP));
        DATA_ELEMENTS.add(new KlvByte(new byte[] {0x41}, UAS_LS_VERSION_NUMBER));
        DATA_ELEMENTS.add(new KlvUnsignedShort(new byte[] {0x05}, PLATFORM_HEADING_ANGLE));
        DATA_ELEMENTS.add(new KlvShort(new byte[] {0x06}, PLATFORM_PITCH_ANGLE));
        DATA_ELEMENTS.add(new KlvShort(new byte[] {0x07}, PLATFORM_ROLL_ANGLE));
        DATA_ELEMENTS.add(new KlvString(new byte[] {0x0b}, IMAGE_SOURCE_SENSOR));
        DATA_ELEMENTS.add(new KlvString(new byte[] {0x0c}, IMAGE_COORDINATE_SYSTEM));
        DATA_ELEMENTS.add(new KlvInt(new byte[] {0x0d}, SENSOR_LATITUDE));
        DATA_ELEMENTS.add(new KlvInt(new byte[] {0x0e}, SENSOR_LONGITUDE));
        DATA_ELEMENTS.add(new KlvUnsignedShort(new byte[] {0x0f}, SENSOR_TRUE_ALTITUDE));
        DATA_ELEMENTS.add(new KlvUnsignedShort(new byte[] {0x10}, SENSOR_HORIZONTAL_FOV));
        DATA_ELEMENTS.add(new KlvUnsignedShort(new byte[] {0x11}, SENSOR_VERTICAL_FOV));
        DATA_ELEMENTS.add(new KlvLong(new byte[] {0x12}, SENSOR_RELATIVE_AZIMUTH_ANGLE));
        DATA_ELEMENTS.add(new KlvInt(new byte[] {0x13}, SENSOR_RELATIVE_ELEVATION_ANGLE));
        DATA_ELEMENTS.add(new KlvLong(new byte[] {0x14}, SENSOR_RELATIVE_ROLL_ANGLE));
        DATA_ELEMENTS.add(new KlvLong(new byte[] {0x15}, SLANT_RANGE));
        // Target width isn't actually a 32-bit int in the UAS Datalink Local Set; it's an unsigned
        // 16-bit int. However, this KLV encodes the target width using 4 bytes (for some reason).
        DATA_ELEMENTS.add(new KlvInt(new byte[] {0x16}, TARGET_WIDTH));
        DATA_ELEMENTS.add(new KlvInt(new byte[] {0x17}, FRAME_CENTER_LATITUDE));
        DATA_ELEMENTS.add(new KlvInt(new byte[] {0x18}, FRAME_CENTER_LONGITUDE));
        DATA_ELEMENTS.add(new KlvUnsignedShort(new byte[] {0x19}, FRAME_CENTER_ELEVATION));
        DATA_ELEMENTS.add(new KlvInt(new byte[] {0x28}, TARGET_LOCATION_LATITUDE));
        DATA_ELEMENTS.add(new KlvInt(new byte[] {0x29}, TARGET_LOCATION_LONGITUDE));
        DATA_ELEMENTS.add(new KlvUnsignedShort(new byte[] {0x2a}, TARGET_LOCATION_ELEVATION));
        DATA_ELEMENTS.add(new KlvUnsignedByte(new byte[] {0x38}, PLATFORM_GROUND_SPEED));
        DATA_ELEMENTS.add(new KlvLong(new byte[] {0x39}, GROUND_RANGE));
        DATA_ELEMENTS.add(new KlvUnsignedShort(new byte[] {0x01}, CHECKSUM));
    }

    private KlvContext getKLVContext(final Set<? extends KlvDataElement> dataElements) {
        final KlvContext localSetContext = new KlvContext(KeyLength.OneByte,
                LengthEncoding.OneByte,
                dataElements);

        final KlvLocalSet outerSet = new KlvLocalSet(new byte[] {0x06, 0x0E, 0x2B, 0x34, 0x02, 0x0B,
                0x01, 0x01, 0x0E, 0x01, 0x03, 0x01, 0x01, 0x00, 0x00, 0x00},
                UAS_DATALINK_LOCAL_SET_UNIVERSAL_KEY,
                localSetContext);

        final Set<KlvDataElement> outerSetContext = Collections.singleton(outerSet);

        return new KlvContext(KeyLength.SixteenBytes, LengthEncoding.BER, outerSetContext);
    }

    @Test
    public void testKLVSet() throws Exception {
        byte[] klvBytes;

        try (final InputStream inputStream = getClass().getClassLoader()
                .getResourceAsStream("testKLV.klv")) {
            klvBytes = IOUtils.toByteArray(inputStream);
        }

        final KlvContext klvContext = getKLVContext(DATA_ELEMENTS);

        final Map<String, KlvDataElement> decodedDataElements = new KlvDecoder(klvContext).decode(
                klvBytes)
                .getDataElements();

        assertThat(decodedDataElements.size(), is(1));
        assertThat(decodedDataElements, hasKey(UAS_DATALINK_LOCAL_SET_UNIVERSAL_KEY));

        final KlvContext localSet = ((KlvLocalSet) decodedDataElements.get(
                UAS_DATALINK_LOCAL_SET_UNIVERSAL_KEY)).getValue();

        final Map<String, KlvDataElement> localSetDataElements = localSet.getDataElements();

        assertThat(localSetDataElements.size(), is(DATA_ELEMENTS.size()));

        localSetDataElements.forEach((name, dataElement) -> {
            final Object expectedValue = EXPECTED_VALUES.get(name);
            assertThat(String.format("%s is not %s", name, expectedValue),
                    dataElement.getValue(),
                    is(expectedValue));
        });
    }

    private KlvContext decodeKLV(final KeyLength keyLength, final LengthEncoding lengthEncoding,
            final KlvDataElement dataElement, final byte[] encodedBytes)
            throws KlvDecodingException {
        final KlvContext klvContext = new KlvContext(keyLength, lengthEncoding);
        klvContext.addDataElement(dataElement);
        return new KlvDecoder(klvContext).decode(encodedBytes);
    }

    private byte[] getValueBytes(final KeyLength keyLength, final LengthEncoding lengthEncoding,
            final byte[] encodedBytes) throws KlvDecodingException {
        final byte[] key = Arrays.copyOf(encodedBytes, keyLength.value());
        final KlvBytes dataElement = new KlvBytes(key, "test");
        final KlvContext decodedKlvContext = decodeKLV(keyLength,
                lengthEncoding,
                dataElement,
                encodedBytes);
        return ((KlvBytes) decodedKlvContext.getDataElementByName("test")).getValue();
    }

    @Test
    public void testOneByteKey() throws KlvDecodingException {
        final byte[] klvBytes = {7, 3, 9, 8, 7};
        final byte[] value = getValueBytes(KeyLength.OneByte, LengthEncoding.OneByte, klvBytes);
        assertThat(value, is(new byte[] {9, 8, 7}));
    }

    @Test
    public void testTwoByteKey() throws KlvDecodingException {
        final byte[] klvBytes = {-14, 99, 3, -1, 0, 1};
        final byte[] value = getValueBytes(KeyLength.TwoBytes, LengthEncoding.OneByte, klvBytes);
        assertThat(value, is(new byte[] {-1, 0, 1}));
    }

    @Test
    public void testFourByteKey() throws KlvDecodingException {
        final byte[] klvBytes = {-14, 99, -55, 101, 3, -1, 0, 1};
        final byte[] value = getValueBytes(KeyLength.FourBytes, LengthEncoding.OneByte, klvBytes);
        assertThat(value, is(new byte[] {-1, 0, 1}));
    }

    @Test
    public void testSixteenByteKey() throws KlvDecodingException {
        final byte[] klvBytes =
                {-14, 99, -55, 101, 22, 0, -9, -45, -55, -1, 77, 89, 112, 17, 18, 19, 3, -1, 0, 1};
        final byte[] value = getValueBytes(KeyLength.SixteenBytes,
                LengthEncoding.OneByte,
                klvBytes);
        assertThat(value, is(new byte[] {-1, 0, 1}));
    }

    @Test
    // One-byte length encoding has already been tested in all the key length tests.
    public void testTwoByteLengthEncoding() throws KlvDecodingException {
        final byte[] expectedValueBytes = new byte[256];
        Arrays.fill(expectedValueBytes, (byte) 4);
        final byte[] klvBytes = ArrayUtils.addAll(new byte[] {5, 1, 0}, expectedValueBytes);
        final byte[] value = getValueBytes(KeyLength.OneByte, LengthEncoding.TwoBytes, klvBytes);
        assertThat(value, is(expectedValueBytes));
    }

    @Test
    public void testFourByteLengthEncoding() throws KlvDecodingException {
        final byte[] expectedValueBytes = new byte[256];
        Arrays.fill(expectedValueBytes, (byte) -2);
        final byte[] klvBytes = ArrayUtils.addAll(new byte[] {5, 0, 0, 1, 0}, expectedValueBytes);
        final byte[] value = getValueBytes(KeyLength.OneByte, LengthEncoding.FourBytes, klvBytes);
        assertThat(value, is(expectedValueBytes));
    }

    @Test
    public void testBERLengthEncodingSingleByte() throws KlvDecodingException {
        final byte length = 100;
        final byte[] expectedValueBytes = new byte[length];
        Arrays.fill(expectedValueBytes, (byte) 101);
        final byte[] klvBytes = ArrayUtils.addAll(new byte[] {5, length}, expectedValueBytes);
        final byte[] value = getValueBytes(KeyLength.OneByte, LengthEncoding.BER, klvBytes);
        assertThat(value, is(expectedValueBytes));
    }

    @Test
    public void testBERLengthEncodingMultipleBytes() throws KlvDecodingException {
        final byte length = 55;
        final byte[] expectedValueBytes = new byte[length];
        Arrays.fill(expectedValueBytes, (byte) -25);
        final byte[] klvBytes = ArrayUtils.addAll(new byte[] {5, (byte) 0b10000001, length},
                expectedValueBytes);
        final byte[] value = getValueBytes(KeyLength.OneByte, LengthEncoding.BER, klvBytes);
        assertThat(value, is(expectedValueBytes));
    }

    @Test
    public void testByteValue() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 1, -128};
        final KlvByte klvByte = new KlvByte(new byte[] {-8}, "test");
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                klvByte,
                klvBytes);
        final byte value = ((KlvByte) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is((byte) -128));
    }

    @Test
    public void testUnsignedByteValue() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 1, -127};
        final KlvUnsignedByte klvUnsignedByte = new KlvUnsignedByte(new byte[] {-8}, "test");
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                klvUnsignedByte,
                klvBytes);
        final short value =
                ((KlvUnsignedByte) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is((short) 129));
    }

    @Test
    public void testShortValue() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 2, (byte) 0xB6, 0x1f};
        final KlvShort klvShort = new KlvShort(new byte[] {-8}, "test");
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                klvShort,
                klvBytes);
        final short value = ((KlvShort) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is((short) -18913));
    }

    @Test
    public void testUnsignedShortValue() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 2, (byte) 0xB6, 0x1f};
        final KlvUnsignedShort klvUnsignedShort = new KlvUnsignedShort(new byte[] {-8}, "test");
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                klvUnsignedShort,
                klvBytes);
        final int value =
                ((KlvUnsignedShort) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is(46623));
    }

    @Test
    public void testIntValue() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 4, (byte) 0xAF, 0x69, 0x1E, 0x0F};
        final KlvInt klvInt = new KlvInt(new byte[] {-8}, "test");
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                klvInt,
                klvBytes);
        final int value = ((KlvInt) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is(-1352065521));
    }

    @Test
    public void testUnsignedIntValue() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 4, (byte) 0xAF, 0x69, 0x1E, 0x0F};
        final KlvLong klvUnsignedInt = new KlvLong(new byte[] {-8}, "test");
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                klvUnsignedInt,
                klvBytes);
        final long value = ((KlvLong) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is(2942901775L));
    }

    @Test
    public void testLongValue() throws KlvDecodingException {
        final byte[] klvBytes =
                {-8, 8, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x50, (byte) 0x96,
                        (byte) 0xE1, (byte) 0xF1};
        final KlvLong klvLong = new KlvLong(new byte[] {-8}, "test");
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                klvLong,
                klvBytes);
        final long value = ((KlvLong) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is(-2942901775L));
    }

    @Test
    public void testFloatValue() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 4, 0x46, (byte) 0xA8, 0x7E, 0x59};
        final KlvFloat klvFloat = new KlvFloat(new byte[] {-8}, "test");
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                klvFloat,
                klvBytes);
        final float value = ((KlvFloat) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is(21567.174f));
    }

    @Test
    public void testDoubleValue() throws KlvDecodingException {
        final byte[] klvBytes =
                {-8, 8, 0x40, (byte) 0xD5, 0x0F, (byte) 0xCB, 0x21, 0x07, (byte) 0xB7, (byte) 0x84};
        final KlvDouble klvDouble = new KlvDouble(new byte[] {-8}, "test");
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                klvDouble,
                klvBytes);
        final double value =
                ((KlvDouble) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is(21567.173891));
    }

    @Test
    public void testStringValue() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 3, 0x4b, 0x4c, 0x56};
        final KlvString klvString = new KlvString(new byte[] {-8},
                "test",
                StandardCharsets.UTF_8.name());
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                klvString,
                klvBytes);
        final String value =
                ((KlvString) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is("KLV"));
    }

    @Test
    // Example value taken from ST0601.8 Tag 36.
    public void testFloatingPointEncodedAsUnsignedByte() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 1, (byte) 0xB2};
        final KlvUnsignedByte klvUnsignedByte = new KlvUnsignedByte(new byte[] {-8}, "test");
        final KlvIntegerEncodedFloatingPoint windSpeed = new KlvIntegerEncodedFloatingPoint(
                klvUnsignedByte,
                0,
                (1 << 8) - 1,
                0,
                100);
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                windSpeed,
                klvBytes);
        final double value =
                ((KlvIntegerEncodedFloatingPoint) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is(closeTo(69.80392, 1e-5)));
    }

    @Test
    // Example value taken from ST0601.8 Tag 5.
    public void testFloatingPointEncodedAsUnsignedShort() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 2, 0x71, (byte) 0xC2};
        final KlvUnsignedShort klvUnsignedShort = new KlvUnsignedShort(new byte[] {-8}, "test");
        final KlvIntegerEncodedFloatingPoint platformHeadingAngle =
                new KlvIntegerEncodedFloatingPoint(klvUnsignedShort, 0, (1 << 16) - 1, 0, 360);
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                platformHeadingAngle,
                klvBytes);
        final double value =
                ((KlvIntegerEncodedFloatingPoint) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is(closeTo(159.9744, 1e-4)));
    }

    @Test
    // Example value taken from ST0601.8 Tag 7, but for some reason their example value is 3.405814,
    // which is wrong.
    public void testFloatingPointEncodedAsShort() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 2, (byte) 0x08, (byte) 0xB8};
        final KlvShort klvShort = new KlvShort(new byte[] {-8}, "test");
        final KlvIntegerEncodedFloatingPoint platformRollAngle =
                // Short.MIN_VALUE is an "out of range" indicator, so it is not included in the range.
                new KlvIntegerEncodedFloatingPoint(klvShort,
                        Short.MIN_VALUE + 1,
                        Short.MAX_VALUE,
                        -50,
                        50);
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                platformRollAngle,
                klvBytes);
        final double value =
                ((KlvIntegerEncodedFloatingPoint) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is(closeTo(3.405865, 1e-6)));
    }

    @Test
    // Example value taken from ST0601.8 Tag 18, but for some reason their example value is
    // 160.719211474396, which is wrong.
    public void testFloatingPointEncodedAsUnsignedInt() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 4, 0x72, 0x4A, 0x0A, 0x20};
        final KlvLong klvUnsignedInt = new KlvLong(new byte[] {-8}, "test");
        final KlvIntegerEncodedFloatingPoint sensorRelativeAzimuth =
                new KlvIntegerEncodedFloatingPoint(klvUnsignedInt, 0, (1L << 32) - 1, 0, 360);
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                sensorRelativeAzimuth,
                klvBytes);
        final double value =
                ((KlvIntegerEncodedFloatingPoint) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is(closeTo(160.719211436975, 1e-12)));
    }

    @Test
    // Example value taken from ST0601.8 Tag 19.
    public void testFloatingPointEncodedAsInt() throws KlvDecodingException {
        final byte[] klvBytes = {-8, 4, (byte) 0x87, (byte) 0xF8, 0x4B, (byte) 0x86};
        final KlvInt klvInt = new KlvInt(new byte[] {-8}, "test");
        final KlvIntegerEncodedFloatingPoint sensorRelativeElevationAngle =
                new KlvIntegerEncodedFloatingPoint(klvInt,
                        // Short.MIN_VALUE is an "out of range" indicator, so it is not included in the range.
                        Integer.MIN_VALUE + 1,
                        Integer.MAX_VALUE,
                        -180,
                        180);
        final KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                sensorRelativeElevationAngle,
                klvBytes);
        final double value =
                ((KlvIntegerEncodedFloatingPoint) decodedKlvContext.getDataElementByName("test")).getValue();
        assertThat(value, is(closeTo(-168.792324833941, 1e-12)));
    }

    @Test
    public void testMissingBytes() {
        final byte[] klvBytes = {-8, 4, (byte) 0x87, (byte) 0xF8, 0x4B};
        final KlvInt klvInt = new KlvInt(new byte[] {-8}, "test");
        try {
            decodeKLV(KeyLength.OneByte, LengthEncoding.OneByte, klvInt, klvBytes);
            fail("Should have thrown a KlvDecodingException.");
        } catch (KlvDecodingException e) {
            assertThat(e.getCause(), instanceOf(IndexOutOfBoundsException.class));
        }
    }

    private boolean isErrorIndicatedByte(byte value, Optional<Byte> errorValue)
            throws KlvDecodingException {
        KlvByte klvByte = new KlvByte(new byte[] {0}, "test", errorValue);

        KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                klvByte,
                new byte[] {0, 1, value});

        return decodedKlvContext.getDataElementByName("test")
                .isErrorIndicated();
    }

    @Test
    public void testIsErrorIndicatedByte() throws KlvDecodingException {
        assertThat(isErrorIndicatedByte((byte) 1, Optional.of((byte) 1)), is(true));
        assertThat(isErrorIndicatedByte((byte) 1, Optional.of((byte) 2)), is(false));
        assertThat(isErrorIndicatedByte((byte) 1, Optional.empty()), is(false));
    }

    @Test
    public void testLongToBytes() {
        long longValue = 0x0102030405060708L;
        byte[] bytes = {0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
        assertThat(longToBytes(longValue), is(bytes));
    }

    @Test
    public void testIntToBytes() {
        int intValue = 0x01020304;
        byte[] bytes = {0x01, 0x02, 0x03, 0x04};
        assertThat(intToBytes(intValue), is(bytes));
    }

    @Test
    public void testShortToBytes() {
        short shortValue = 0x0102;
        byte[] bytes = {0x01, 0x02};
        assertThat(shortToBytes(shortValue), is(bytes));
    }

    private boolean isErrorIndicatedDouble(double value, Optional<Double> errorValue)
            throws KlvDecodingException {

        KlvDouble klvDouble = new KlvDouble(new byte[] {0}, "test", errorValue);

        byte[] dataBytes = new byte[10];

        dataBytes[0] = 0;
        dataBytes[1] = 8;
        arraycopy(longToBytes(Double.doubleToLongBits(value)), 0, dataBytes, 2, 8);

        return isErrorIndicatedDecode(klvDouble, dataBytes);
    }

    @Test
    public void testIsErrorIndicatedDouble() throws KlvDecodingException {
        assertThat(isErrorIndicatedDouble(0, Optional.of(0D)), is(true));
        assertThat(isErrorIndicatedDouble(1, Optional.of(0D)), is(false));
        assertThat(isErrorIndicatedDouble(1, Optional.empty()), is(false));
    }

    private boolean isErrorIndicatedFloat(float value, Optional<Float> errorValue)
            throws KlvDecodingException {

        KlvFloat klvFloat = new KlvFloat(new byte[] {0}, "test", errorValue);

        byte[] dataBytes = new byte[6];

        dataBytes[0] = 0;
        dataBytes[1] = 4;
        arraycopy(intToBytes(Float.floatToIntBits(value)), 0, dataBytes, 2, 4);

        return isErrorIndicatedDecode(klvFloat, dataBytes);
    }

    private boolean isErrorIndicatedDecode(KlvDataElement klvDataElement, byte[] dataBytes)
            throws KlvDecodingException {
        KlvContext decodedKlvContext = decodeKLV(KeyLength.OneByte,
                LengthEncoding.OneByte,
                klvDataElement,
                dataBytes);

        return decodedKlvContext.getDataElementByName("test")
                .isErrorIndicated();
    }

    @Test
    public void testIsErrorIndicatedFloat() throws KlvDecodingException {
        assertThat(isErrorIndicatedFloat(0, Optional.of(0F)), is(true));
        assertThat(isErrorIndicatedFloat(1, Optional.of(0F)), is(false));
        assertThat(isErrorIndicatedFloat(1, Optional.empty()), is(false));
    }

    private boolean isErrorIndicatedInt(int value, Optional<Integer> errorValue)
            throws KlvDecodingException {

        KlvInt klvInt = new KlvInt(new byte[] {0}, "test", errorValue);

        byte[] dataBytes = new byte[6];

        dataBytes[0] = 0;
        dataBytes[1] = 4;
        arraycopy(intToBytes(value), 0, dataBytes, 2, 4);

        return isErrorIndicatedDecode(klvInt, dataBytes);
    }

    @Test
    public void testIsErrorIndicatedInt() throws KlvDecodingException {
        assertThat(isErrorIndicatedInt(0, Optional.of(0)), is(true));
        assertThat(isErrorIndicatedInt(1, Optional.of(0)), is(false));
        assertThat(isErrorIndicatedInt(1, Optional.empty()), is(false));
    }

    private boolean isErrorIndicatedIntegerIndicatedFloatingPoint(int value,
            Optional<Integer> errorValue) throws KlvDecodingException {

        KlvIntegerEncodedFloatingPoint klvIntegerEncodedFloatingPoint =
                new KlvIntegerEncodedFloatingPoint(new KlvInt(new byte[] {0}, "test", errorValue),
                        Integer.MIN_VALUE + 1,
                        Integer.MAX_VALUE,
                        -90,
                        90);

        byte[] dataBytes = new byte[6];

        dataBytes[0] = 0;
        dataBytes[1] = 4;
        arraycopy(intToBytes(value), 0, dataBytes, 2, 4);

        return isErrorIndicatedDecode(klvIntegerEncodedFloatingPoint, dataBytes);
    }

    @Test
    public void testIsErrorIndicatedIntegerEncodedFloatingPoint() throws KlvDecodingException {
        assertThat(isErrorIndicatedIntegerIndicatedFloatingPoint(Integer.MIN_VALUE,
                Optional.of(Integer.MIN_VALUE)), is(true));
        assertThat(isErrorIndicatedIntegerIndicatedFloatingPoint(Integer.MIN_VALUE + 1,
                Optional.of(Integer.MIN_VALUE)), is(false));

        assertThat(isErrorIndicatedIntegerIndicatedFloatingPoint(Integer.MIN_VALUE,
                Optional.empty()), is(false));
        assertThat(isErrorIndicatedIntegerIndicatedFloatingPoint(Integer.MIN_VALUE + 1,
                Optional.empty()), is(false));
    }

    private boolean isErrorIndicatedLong(long value, Optional<Long> errorValue)
            throws KlvDecodingException {

        KlvLong klvLong = new KlvLong(new byte[] {0}, "test", errorValue);

        byte[] dataBytes = new byte[10];

        dataBytes[0] = 0;
        dataBytes[1] = 8;
        arraycopy(longToBytes(value), 0, dataBytes, 2, 8);

        return isErrorIndicatedDecode(klvLong, dataBytes);
    }

    @Test
    public void testIsErrorIndicatedLong() throws KlvDecodingException {
        assertThat(isErrorIndicatedLong(0L, Optional.of(0L)), is(true));
        assertThat(isErrorIndicatedLong(1L, Optional.of(0L)), is(false));
        assertThat(isErrorIndicatedLong(1L, Optional.empty()), is(false));
    }

    private boolean isErrorIndicatedShort(short value, Optional<Short> errorValue)
            throws KlvDecodingException {

        KlvShort klvShort = new KlvShort(new byte[] {0}, "test", errorValue);

        byte[] dataBytes = new byte[4];

        dataBytes[0] = 0;
        dataBytes[1] = 2;
        arraycopy(shortToBytes(value), 0, dataBytes, 2, 2);

        return isErrorIndicatedDecode(klvShort, dataBytes);
    }

    @Test
    public void testIsErrorIndicatedShort() throws KlvDecodingException {
        assertThat(isErrorIndicatedShort((short) 0, Optional.of((short) 0)), is(true));
        assertThat(isErrorIndicatedShort((short) 1, Optional.of((short) 0)), is(false));
        assertThat(isErrorIndicatedShort((short) 1, Optional.empty()), is(false));
    }

    private boolean isErrorIndicatedUnsignedByte(short value, Optional<Short> errorValue)
            throws KlvDecodingException {

        KlvUnsignedByte klvUnsignedByte = new KlvUnsignedByte(new byte[] {0}, "test", errorValue);

        byte[] dataBytes = new byte[3];

        dataBytes[0] = 0;
        dataBytes[1] = 1;
        dataBytes[2] = (byte) (0xFF & value);

        return isErrorIndicatedDecode(klvUnsignedByte, dataBytes);
    }

    @Test
    public void testIsErrorIndicatedUnsignedByte() throws KlvDecodingException {
        assertThat(isErrorIndicatedUnsignedByte((short) 0, Optional.of((short) 0)), is(true));
        assertThat(isErrorIndicatedUnsignedByte((short) 1, Optional.of((short) 0)), is(false));
        assertThat(isErrorIndicatedUnsignedByte((short) 1, Optional.empty()), is(false));
    }

}
