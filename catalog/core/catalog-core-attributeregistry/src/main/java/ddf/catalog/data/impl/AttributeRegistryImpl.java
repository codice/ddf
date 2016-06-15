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
package ddf.catalog.data.impl;

import static java.util.AbstractMap.SimpleEntry;
import static java.util.AbstractMap.SimpleImmutableEntry;
import static java.util.Map.Entry;
import static org.apache.commons.lang.Validate.notNull;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;

public class AttributeRegistryImpl implements AttributeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeRegistryImpl.class);

    private static final Entry<AttributeDescriptor, Boolean> DEFAULT_ENTRY =
            new SimpleImmutableEntry<>(null, null);

    private final ConcurrentHashMap<String, Entry<AttributeDescriptor, Boolean>> attributeMap =
            new ConcurrentHashMap<>();

    public AttributeRegistryImpl() {
        BasicTypes.BASIC_METACARD.getAttributeDescriptors()
                .stream()
                .forEach(this::register);
    }

    @Override
    public boolean register(final AttributeDescriptor attributeDescriptor) {
        notNull(attributeDescriptor, "The attribute descriptor cannot be null.");
        notNull(attributeDescriptor.getName(), "The attribute name cannot be null.");

        final String name = attributeDescriptor.getName();

        if (attributeMap.putIfAbsent(name, new SimpleEntry<>(attributeDescriptor, false)) == null) {
            return true;
        }

        LOGGER.debug(
                "Attempted to register an attribute with name '{}', but an attribute with that name already exists.",
                name);
        return false;
    }

    @Override
    public void deregister(final String name) {
        notNull(name, "The attribute name cannot be null.");

        attributeMap.remove(name);
    }

    @Override
    public boolean globalize(String name) {
        notNull(name, "The attribute name cannot be null.");

        return attributeMap.computeIfPresent(name, (k, attributeEntry) -> {
            attributeEntry.setValue(true);
            return attributeEntry;
        }) != null;
    }

    @Override
    public void deglobalize(String name) {
        notNull(name, "The attribute name cannot be null.");

        attributeMap.computeIfPresent(name, (k, attributeEntry) -> {
            attributeEntry.setValue(false);
            return attributeEntry;
        });
    }

    @Override
    public Optional<AttributeDescriptor> lookup(final String name) {
        notNull(name, "The attribute name cannot be null.");

        return Optional.ofNullable(attributeMap.getOrDefault(name, DEFAULT_ENTRY)
                .getKey());
    }

    @Override
    public Set<AttributeDescriptor> getGlobalAttributes() {
        Set<AttributeDescriptor> globalAttributes = new HashSet<>();
        final long noParallelism = Long.MAX_VALUE;
        attributeMap.forEachValue(noParallelism,
                attributeEntry -> attributeEntry.getValue() ? attributeEntry.getKey() : null,
                globalAttributes::add);
        return globalAttributes;
    }
}
