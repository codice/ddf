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
package org.codice.ddf.spatial.kml.transformer;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;

import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * Created by kcwire on 1/28/15.
 */
public class HandlebarsMetacard extends MetacardImpl {

    public HandlebarsMetacard(Metacard metacard) {
        super(metacard);
    }

    public Set<AttributeEntry> getAttributes() {
        MetacardType metacardType = this.getMetacardType();
        Set<AttributeEntry> attributes = new TreeSet<>();
        for (AttributeDescriptor descriptor : metacardType.getAttributeDescriptors()) {
            Attribute attr = this.getAttribute(descriptor.getName());
            if (attr != null) {
                attributes.add(new AttributeEntry(attr, descriptor.getType().getAttributeFormat()));
            }
        }
        return attributes;
    }

    private class AttributeEntry
            implements Entry<Attribute, AttributeFormat>, Comparable<AttributeEntry> {

        private Attribute key;

        private AttributeFormat value;

        public AttributeEntry(Attribute key, AttributeFormat value) {
            this.key = key;
            this.value = value;
        }

        @Override
        public Attribute getKey() {
            return key;
        }

        @Override
        public AttributeFormat getValue() {
            return value;
        }

        @Override
        public AttributeFormat setValue(AttributeFormat value) {
            this.value = value;
            return this.value;
        }

        @Override
        public int compareTo(AttributeEntry other) {
            return this.getKey().getName().compareTo(other.getKey().getName());
        }
    }

}
