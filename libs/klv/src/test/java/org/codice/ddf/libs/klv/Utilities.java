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

class Utilities {

    public static byte[] longToBytes(long value) {
        int size = 8;

        byte[] bytes = new byte[size];

        for (int i = 0; i < size; i++) {
            long mask = 0xFFL << (8 * i);
            bytes[size - 1 - i] = (byte) ((value & mask) >> (8 * i));
        }
        return bytes;
    }

    public static byte[] intToBytes(int value) {
        int size = 4;

        byte[] bytes = new byte[size];

        for (int i = 0; i < size; i++) {
            int mask = 0xFF << (8 * i);
            bytes[size - 1 - i] = (byte) ((value & mask) >> (8 * i));
        }
        return bytes;
    }

    public static byte[] shortToBytes(short value) {
        int size = 2;

        byte[] bytes = new byte[size];

        for (int i = 0; i < size; i++) {
            short mask = (short) (0xFF << (8 * i));
            bytes[size - 1 - i] = (byte) ((value & mask) >> (8 * i));
        }
        return bytes;
    }

}
