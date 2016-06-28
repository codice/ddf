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
package ddf.catalog.cache.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;

public class MetacardComparator {

    private MetacardComparator() {
    }

    static List<Function<Metacard, ?>> metacardMethods = new ArrayList<>();

    static {
        metacardMethods.add(Metacard::getModifiedDate);
        metacardMethods.add(Metacard::getEffectiveDate);
        metacardMethods.add(Metacard::getCreatedDate);
        metacardMethods.add(Metacard::getExpirationDate);
        metacardMethods.add(Metacard::getResourceURI);
        metacardMethods.add(Metacard::getContentTypeName);
        metacardMethods.add(Metacard::getResourceSize);
        metacardMethods.add(Metacard::getLocation);
    }

    private static boolean validChecksum(Attribute cachedMetacardAttr,
            Attribute updatedMetacardAttr) {

        return cachedMetacardAttr.toString()
                .equals(updatedMetacardAttr.toString());
    }

    private static boolean sameAttributes(Metacard cachedMetacard, Metacard updatedMetacard) {
        boolean result = false;

        Set<AttributeDescriptor> cachedDescriptors = cachedMetacard.getMetacardType()
                .getAttributeDescriptors();
        Set<AttributeDescriptor> updatedDescriptors = updatedMetacard.getMetacardType()
                .getAttributeDescriptors();

        if (cachedDescriptors.size() == updatedDescriptors.size()) {

            String attrName;
            Attribute cachedAttr;
            Attribute updatedAttr;

            for (AttributeDescriptor descriptor : cachedDescriptors) {

                attrName = descriptor.getName();
                cachedAttr = cachedMetacard.getAttribute(attrName);
                updatedAttr = updatedMetacard.getAttribute(attrName);

                // check if both are null or if not null, check if the values are the same
                if ((cachedAttr == updatedAttr) || (cachedAttr.toString()
                        .equals(updatedAttr.toString()))) {
                    result = true;
                } else {
                    result = false;
                    break;
                }

            }
        }

        return result;
    }

    public static boolean isSame(Metacard cachedMetacard, Metacard updatedMetacard) {

        boolean result = false;

        if (cachedMetacard.getId()
                .equals(updatedMetacard.getId())) {

            Attribute cachedMetacardAttr = cachedMetacard.getAttribute(Metacard.CHECKSUM);

            if (cachedMetacardAttr != null) {

                Attribute updatedMetacardAttr = updatedMetacard.getAttribute(Metacard.CHECKSUM);

                if (updatedMetacardAttr != null) {

                    if (validChecksum(cachedMetacardAttr, updatedMetacardAttr)) {

                        result = metacardMethods.stream()
                                .map(m -> m.apply(cachedMetacard)
                                        .equals(m.apply(updatedMetacard)))
                                .reduce(true, (acc, val) -> acc && val) && sameAttributes(
                                cachedMetacard,
                                updatedMetacard);

                    }
                }
            }

        }

        return result;

    }
}
