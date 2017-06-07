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

package ddf.catalog.content.operation;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;

/**
 * Enrich metacard using provided input.
 * <b>This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</b>
 * </p>
 */
public interface MetadataExtractor {

    /**
     * Parses the input string, extracting metadata from it to add to the metacard.
     *
     * @param metadata the metadata to process
     * @param metacard the metacard to enrich
     */
    void process(String metadata, Metacard metacard);

    MetacardType getMetacardType(String contentType);

    boolean canProcess(String contentType);

}
