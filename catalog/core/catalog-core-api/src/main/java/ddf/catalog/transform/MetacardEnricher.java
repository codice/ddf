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
package ddf.catalog.transform;

import java.io.InputStream;

import ddf.catalog.data.Metacard;

/**
 * An MetacardEnricher extracts additional metadata from an {@code InputStream} and adds the
 * metadata to a {@link Metacard}.
 * <p>
 * <b>Implementations of this interface <em>must</em>:</b>
 * <ul>
 * <li/>Register with the OSGi Service Registry using the {@link MetacardEnricher} interface
 * <li/>Include a Service property with name "id" ({@link ddf.catalog.Constants#SERVICE_ID}) and a
 * {@code String} value uniquely identifying the particular implementation
 * </ul>
 */
public interface MetacardEnricher {
    /**
     * Enrich a {@link Metacard} by extracting additional metadata from {@code InputStream}.
     *
     * @param metacard the {@code Metacard} to add metadata to
     * @param input    the {@code InputStream} to extract metadata from
     * @throws CatalogTransformerException when any error occurs
     */
    void enrich(Metacard metacard, InputStream input) throws CatalogTransformerException;

}