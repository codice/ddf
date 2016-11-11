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
package org.codice.ddf.catalog.plugin.metacard.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;

/**
 * Support and bulk operations for {@link Metacard}s. Contains state relevant to the provided
 * services.
 */
public class MetacardServices {

    private final List<MetacardType> systemMetacardTypes;

    /**
     * Initializes the services with an empty list of system {@link MetacardType}s.
     */
    public MetacardServices() {
        this.systemMetacardTypes = new ArrayList<>();
    }

    /**
     * Initializes the services with the provided list of {@link MetacardType}s.
     *
     * @param systemMetacardTypes The list of metacard types to use.
     */
    public MetacardServices(List<MetacardType> systemMetacardTypes) {
        this.systemMetacardTypes = systemMetacardTypes;
    }

    /**
     * Iterates over each {@link Metacard} in the {@param metacards} collection and applies each
     * attribute in the {@param attributeMap} only if the attribute is not already set on the
     * {@link Metacard}. That is, the attribute must be {@code null} or not in the {@link Metacard}'s
     * map.
     *
     * @param metacards        The list of metacards to attempt to add the attributes to.
     * @param attributeMap     The map of attributes to attempt to add. Attributes already set on any
     *                         given {@link Metacard} will not be changed. For multi-valued attributes,
     *                         the value string should separate different entities with commas.
     * @param attributeFactory The factory to use to create attributes.
     */
    public void setAttributesIfAbsent(List<Metacard> metacards, Map<String, String> attributeMap,
            AttributeFactory attributeFactory) {

        if (metacards.isEmpty() || attributeMap.isEmpty()) {
            return;
        }

        List<MetacardType> systemMetacardTypesCopy = ImmutableList.copyOf(systemMetacardTypes);
        Map<String, AttributeDescriptor> systemAndMetacardDescriptors = new HashMap<>();

        systemMetacardTypesCopy.stream()
                .map(MetacardType::getAttributeDescriptors)
                // ** Does our API allow null descriptors? **
                .filter(Objects::nonNull)
                .forEach(descriptors -> loadUniqueDescriptors(descriptors,
                        systemAndMetacardDescriptors));

        metacards.stream()
                .map(Metacard::getMetacardType)
                .map(MetacardType::getAttributeDescriptors)
                // ** Does our API allow null descriptors? **
                .filter(Objects::nonNull)
                .forEach(descriptors -> loadUniqueDescriptors(descriptors,
                        systemAndMetacardDescriptors));

        metacards.forEach(metacard -> attributeMap.keySet()
                .stream()
                .filter(key -> metacard.getAttribute(key) == null)
                .map(systemAndMetacardDescriptors::get)
                // ** Filter during unique loading instead of here **
                .filter(Objects::nonNull)
                .map(descriptor -> attributeFactory.createAttribute(descriptor,
                        attributeMap.get(descriptor.getName())))
                .filter(Objects::nonNull)
                .forEach(metacard::setAttribute));
    }

    /**
     * Helper method for loading only unique descriptors into a specified map.
     */
    private void loadUniqueDescriptors(Set<AttributeDescriptor> descriptorsToLoad,
            /*mutable*/ Map<String, AttributeDescriptor> destination) {
        descriptorsToLoad.forEach(descriptor -> destination.putIfAbsent(descriptor.getName(),
                descriptor));
    }
}
