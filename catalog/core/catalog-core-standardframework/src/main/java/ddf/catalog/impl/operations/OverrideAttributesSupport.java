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
 **/
package ddf.catalog.impl.operations;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;

public class OverrideAttributesSupport {
    private OverrideAttributesSupport() {

    }

    public static void overrideAttributes(List<ContentItem> contentItems,
            Map<String, Metacard> metacardMap) {
        for (ContentItem contentItem : contentItems) {
            if (contentItem.getMetacard() == null) {
                continue;
            }

            Metacard currentMetacard = metacardMap.get(contentItem.getId());
            Metacard overrideMetacard = contentItem.getMetacard();

            Metacard updatedMetacard = overrideMetacard(currentMetacard, overrideMetacard, false,
                    false);

            metacardMap.put(contentItem.getId(), updatedMetacard);
        }
    }

    public static Metacard overrideMetacard(Metacard currentMetacard, Metacard overrideMetacard,
            boolean ignoreType, boolean onlyFillNull) {
        MetacardType updatedMetacardType = currentMetacard.getMetacardType();
        if (!ignoreType && !BasicTypes.BASIC_METACARD.equals(overrideMetacard.getMetacardType())) {
            updatedMetacardType = overrideMetacard.getMetacardType();
        }

        Metacard updatedMetacard = new MetacardImpl(currentMetacard, updatedMetacardType);

        addAttributes(updatedMetacard, overrideMetacard, onlyFillNull);
        return updatedMetacard;
    }

    private static void addAttributes(Metacard metacard, Metacard otherMetacard,
            boolean onlyFillNull) {
        otherMetacard.getMetacardType()
                .getAttributeDescriptors()
                .stream()
                .map(attributeDescriptor -> otherMetacard.getAttribute(
                        attributeDescriptor.getName()))
                .filter(Objects::nonNull)
                .filter(attribute -> !attribute.getName()
                        .equals(Metacard.ID))
                .filter(attribute -> !attribute.getName()
                        .equals(Metacard.RESOURCE_URI))
                .filter(attribute -> !onlyFillNull
                        || metacard.getAttribute(attribute.getName()) == null)
                .forEach(metacard::setAttribute);
    }
}
