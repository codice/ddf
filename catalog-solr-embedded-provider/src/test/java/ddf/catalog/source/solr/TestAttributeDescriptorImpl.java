/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.source.solr;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;

public class TestAttributeDescriptorImpl extends AttributeDescriptorImpl {
    private static final long serialVersionUID = 1L;

    private String propertyName;

    public TestAttributeDescriptorImpl(String name, String propertyName, boolean indexed,
            boolean stored, boolean tokenized, boolean multivalued, AttributeType<?> type) {
        super(name, indexed, stored, tokenized, multivalued, type);

        this.propertyName = propertyName;
    }

    public TestAttributeDescriptorImpl(AttributeDescriptor ad) {
        super(ad.getName(), ad.isIndexed(), ad.isStored(), ad.isTokenized(), ad.isMultiValued(), ad
                .getType());

    }

    public String getPropertyName() {
        return propertyName;
    }

}