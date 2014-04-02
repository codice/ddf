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

package org.codice.ddf.spatial.ogc.wfs.catalog.common;

import java.text.Collator;
import java.util.Comparator;

import ddf.catalog.data.AttributeDescriptor;

public class AttributeDescriptorComparator implements Comparator<AttributeDescriptor> {

    @Override
    public int compare(AttributeDescriptor ad1, AttributeDescriptor ad2) {
        int compared = 0;
        compared = Collator.getInstance().compare(ad1.getName(), ad2.getName());
        if (0 != compared) {
            return compared;
        }
        compared = ad1.getName().compareTo(ad2.getName());
        if (0 != compared) {
            return compared;
        }
        compared = ad1.getType().getAttributeFormat().compareTo(ad2.getType().getAttributeFormat());
        if (0 != compared) {
            return compared;
        }

        compared = ad1.getType().getBinding().toString()
                .compareTo(ad2.getType().getBinding().toString());
        if (0 != compared) {
            return compared;
        }

        compared = new Boolean(ad1.isIndexed()).compareTo(new Boolean(ad2.isIndexed()));
        if (0 != compared) {
            return compared;
        }

        compared = new Boolean(ad1.isStored()).compareTo(new Boolean(ad2.isStored()));
        if (0 != compared) {
            return compared;
        }

        compared = new Boolean(ad1.isTokenized()).compareTo(new Boolean(ad2.isTokenized()));
        if (0 != compared) {
            return compared;
        }

        compared = new Boolean(ad1.isMultiValued()).compareTo(new Boolean(ad2.isMultiValued()));
        if (0 != compared) {
            return compared;
        }

        return compared;
    }
}
