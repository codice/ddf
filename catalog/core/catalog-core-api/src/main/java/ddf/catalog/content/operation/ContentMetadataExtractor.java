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
 **/
package ddf.catalog.content.operation;

import java.io.InputStream;
import java.util.Set;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;

/**
 * Parses content, extracting metadata and adding it to the provided card.
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public interface ContentMetadataExtractor {
    /**
     * Parses the input string, extracting metadata from it to add to the metacard.
     *
     * <strong>This method can have large memory effects. If you already have data in
     * a datasource that can be streamed, consider the {@link #process(InputStream, Metacard)}
     * overloaded method instead.</strong>
     *
     * @param input    the content to process
     * @param metacard the incoming metacard
     */
    void process(String input, Metacard metacard);

    /**
     * Parses the input stream, extracting metadata from it to add to the metacard.
     *
     * @param input    the content to process
     * @param metacard the incoming metacard
     */
    void process(InputStream input, Metacard metacard);

    /**
     * Returns the valid set of Metacard attributes that are populated by this extractor.
     *
     * @return set of attributes populated by this extractor
     */
    Set<AttributeDescriptor> getMetacardAttributes();
}
