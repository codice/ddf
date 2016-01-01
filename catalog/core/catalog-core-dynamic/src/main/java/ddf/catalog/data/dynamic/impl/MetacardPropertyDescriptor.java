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

public class MetacardPropertyDescriptor extends DynaProperty {
    private boolean indexedBySource = true;
    private boolean tokenized = false;
    private boolean stored = true;

    public MetacardPropertyDescriptor(String name) {
        super(name);
    }
    public MetacardPropertyDescriptor(String name, Class<?> type) {
        super(name, type);
    }
    public MetacardPropertyDescriptor(String name, Class<?> type, Class<?> contentType) {
        super(name, type, contentType);
    }

    public MetacardPropertyDescriptor(String name, Class<?> type, boolean indexedBySource, boolean stored, boolean tokenized) {
        super(name, type);
        this.indexedBySource = indexedBySource;
        this.stored = stored;
        this.tokenized = tokenized;
    }

    public MetacardPropertyDescriptor(String name, Class<?> type, Class<?> contentType, boolean indexedBySource, boolean stored, boolean tokenized) {
        super(name, type, contentType);
        this.indexedBySource = indexedBySource;
        this.stored = stored;
        this.tokenized = tokenized;
    }

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
