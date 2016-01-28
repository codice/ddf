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
package ddf.catalog.data.dynamic.api;

import java.io.Serializable;

/**
 * The MetacardPropertyDescriptorImpl interface encompasses the methods used to describe a property
 * of a DynamicMetacard instance. The methods defined here provide the ability to interrogate the
 * property and discover operational characteristics (indexed or stored by the catalog, etc.)
 */
public interface MetacardPropertyDescriptor extends Serializable {
    boolean isIndexedBySource();

    void setIndexedBySource(boolean indexedBySource);

    boolean isTokenized();

    void setTokenized(boolean tokenized);

    boolean isStored();

    void setStored(boolean stored);

    // Add the methods from the DynaPropery class

    Class<?> getContentType();

    String getName();

    Class<?> getType();

    boolean isIndexed();

    boolean isMapped();
}
