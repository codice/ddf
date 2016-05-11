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
import java.io.Serializable;
import java.util.List;
import java.util.Map;

import ddf.catalog.data.Metacard;

/**
 * The {@code InputCollectionTransformer} is used to transform an {@link InputStream} into a list of {@code Metacard}s.
 */
public interface InputCollectionTransformer {

    /**
     * Transforms an {@link InputStream} into a list of {@link Metacard}s.
     *
     * @param inputStream - the {@link InputStream} to be transformed
     * @param arguments - the arguments that may be used to execute the transform
     * @return a list of {@link Metacard}s
     * @throws CatalogTransformerException if the response cannot be transformed
     */
    List<Metacard> transform(InputStream inputStream, Map<String, Serializable> arguments)
            throws CatalogTransformerException;
}

