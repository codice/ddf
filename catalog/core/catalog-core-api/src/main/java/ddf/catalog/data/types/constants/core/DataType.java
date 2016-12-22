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
 *
 * Based on Dublin Core (http://dublincore.org/documents/2012/06/14/dcmi-terms/?v=elements#terms-type) and extended to include other DDF supported types.
 */
public enum DataType {

    COLLECTION("Collection"), //
    DATASET("Dataset"), //
    EVENT("Event"), //
    IMAGE("Image"), //
    INTERACTIVE_RESOURCE("Interactive Resource"), //
    SERVICE("Service"), //
    SOFTWARE("Software"), //
    SOUND("Sound"), //
    TEXT("Text"), //
    VIDEO("Video"), //
    DOCUMENT("Document");

    private final String value;

    DataType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }

}
