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
package ddf.catalog.data.types.constants.core;

/**
 * These are the allowed values for the attribute Core#DATATYPE.
 * <p>
 * These generic type(s) of the resource include the Dublin Core Metadata Initiative DCMI Type
 * Vocabulary (http://dublincore.org/documents/dcmi-type-vocabulary/).
 * DCMI Type term labels are included here, as opposed to term names.
 *
 * The DDF extension types of "Document" and "Video" have been removed as of DDF-2.11.0
 */
public enum DataType {

    // DCMI type vocabulary labels
    COLLECTION("Collection"), //
    DATASET("Dataset"), //
    EVENT("Event"), //
    IMAGE("Image"), //
    INTERACTIVE_RESOURCE("Interactive Resource"), //
    MOVING_IMAGE("Moving Image"), //
    PHYSICAL_OBJECT("Physical Object"), //
    SERVICE("Service"), //
    SOFTWARE("Software"), //
    SOUND("Sound"), //
    STILL_IMAGE("Still Image"), //
    TEXT("Text");

    private final String value;

    DataType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

    public static DataType fromValue(String value) {
        for (DataType type: DataType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException(value);
    }

}
