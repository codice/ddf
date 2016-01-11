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
package ddf.catalog.data.dynamic.impl;

import org.apache.commons.beanutils.DynaProperty;

/**
 * Extends the DynaProperty to include fields used by the catalog: indexedBySource, tokenized,
 * and stored. This class mirrors the DynaProperty constructors with the addition of constructors taking
 * values for the added fields. Attributes of the dynamic metacards are described by instances
 * of this class.
 */
public class MetacardPropertyDescriptor extends DynaProperty {
    private boolean indexedBySource = true;
    private boolean tokenized = false;
    private boolean stored = true;

    /**
     * Creates a property descriptor for the given name using default values for indexedBySource,
     * tokenized, and stored.
     * @param name name of this attribute
     */
    public MetacardPropertyDescriptor(String name) {
        super(name);
    }

    /**
     * Creates a property descriptor for the given name and type using default values for indexedBySource,
     * tokenized, and stored.
     * @param name name of this attribute
     * @param type the base data type for this attribute
     */
    public MetacardPropertyDescriptor(String name, Class<?> type) {
        super(name, type);
    }

    /**
     * Creates a property descriptor for the given name, index type, and content type
     * using default values for indexedBySource, tokenized, and stored.
     * @param name name of this attribute
     * @param type the type of collection for this attribute
     * @param contentType the base data type for this attribute
     */
    public MetacardPropertyDescriptor(String name, Class<?> type, Class<?> contentType) {
        super(name, type, contentType);
    }

    /**
     * Creates a property descriptor for the given name, type, and specified values for
     * indexedBySource, tokenized, and stored.
     * @param name name of this attribute
     * @param type the base data type for this attribute
     * @param indexedBySource indicates whether the source should index this attribute for search
     * @param stored indicates whether the source should store this value
     * @param tokenized indicates whether the value for this attribute should be parsed
     */
    public MetacardPropertyDescriptor(String name, Class<?> type, boolean indexedBySource, boolean stored, boolean tokenized) {
        super(name, type);
        this.indexedBySource = indexedBySource;
        this.stored = stored;
        this.tokenized = tokenized;
    }

    /**
     * Creates a property descriptor for the given name, type, and specified values for
     * indexedBySource, tokenized, and stored.
     * @param name name of this attribute
     * @param type the type of collection for this attribute
     * @param contentType the base data type for this attribute
     * @param indexedBySource indicates whether the source should index this attribute for search
     * @param stored indicates whether the source should store this value
     * @param tokenized indicates whether the value for this attribute should be parsed
     */
    public MetacardPropertyDescriptor(String name, Class<?> type, Class<?> contentType, boolean indexedBySource, boolean stored, boolean tokenized) {
        super(name, type, contentType);
        this.indexedBySource = indexedBySource;
        this.stored = stored;
        this.tokenized = tokenized;
    }

    /**
     * Creates a property desriptor based on the given DynaProperty object and the specified values for
     * indexedBySource, tokenized, and stored.
     * @param dynaProperty descriptor of the attribute (name, value, etc.)
     * @param indexedBySource indicates whether the source should index this attribute for search
     * @param stored indicates whether the source should store this value
     * @param tokenized indicates whether the value for this attribute should be parsed
     */
    public MetacardPropertyDescriptor(DynaProperty dynaProperty, boolean indexedBySource, boolean stored, boolean tokenized) {
        super(dynaProperty.getName(), dynaProperty.getType(), dynaProperty.getContentType());
        this.indexedBySource = indexedBySource;
        this.stored = stored;
        this.tokenized = tokenized;
    }

    public boolean isIndexedBySource() {
        return indexedBySource;
    }

    public void setIndexedBySource(boolean indexedBySource) {
        this.indexedBySource = indexedBySource;
    }

    public boolean isTokenized() {
        return tokenized;
    }

    public void setTokenized(boolean tokenized) {
        this.tokenized = tokenized;
    }

    public boolean isStored() {
        return stored;
    }

    public void setStored(boolean stored) {
        this.stored = stored;
    }
}
