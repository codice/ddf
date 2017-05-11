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
package ddf.catalog.history;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.Validate;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;

public class DynamicMultiMetacardType implements MetacardType {
    private final String name;

    private final List<MetacardType> metacardTypes;

    private final List<MetacardType> extraTypes;

    private static final Set<String> REGISTERED_DMMTS =
            Collections.synchronizedSet(new HashSet<>());

    public DynamicMultiMetacardType(String name, List<MetacardType> dynamicMetacardTypes) {
        this(name, dynamicMetacardTypes, new MetacardType[0]);
    }

    public DynamicMultiMetacardType(String name, List<MetacardType> dynamicMetacardTypes,
            MetacardType... extraTypes) {
        Validate.notNull(dynamicMetacardTypes,
                "The list of Dynamic Metacard Types Cannot be null.");
        this.name = name;
        this.metacardTypes = dynamicMetacardTypes;
        this.extraTypes = Arrays.asList(extraTypes);

        REGISTERED_DMMTS.add(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors() {
        return streamAttributeDescriptors().collect(Collectors.toSet());
    }

    @Override
    public AttributeDescriptor getAttributeDescriptor(String attributeName) {
        return streamAttributeDescriptors().filter(ad -> ad.getName()
                .equals(attributeName))
                .findFirst()
                .orElse(null);
    }

    /*
     * WARNING: Attribute descriptors should always be fetched through this method to prevent
     * any infinite recursion.
     */
    private Stream<AttributeDescriptor> streamAttributeDescriptors() {
        Stream<MetacardType> filteredTypes = metacardTypes.stream()
                .filter(mt -> !REGISTERED_DMMTS.contains(mt.getName()));

        return Stream.concat(filteredTypes, extraTypes.stream())
                .map(MetacardType::getAttributeDescriptors)
                .flatMap(Collection::stream);
    }
}
