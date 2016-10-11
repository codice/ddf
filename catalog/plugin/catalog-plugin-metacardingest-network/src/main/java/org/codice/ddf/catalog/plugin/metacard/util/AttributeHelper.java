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

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.bind.DatatypeConverter;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.util.impl.SortedServiceReferenceList;

/**
 * Support class for working with {@link Attribute}s and {@link Metacard}s.
 */
public class AttributeHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeHelper.class);

    private final Map<String, AttributeDescriptor> systemDescriptors;

    /**
     * Default constructor for use when the basic attributes are sufficient.
     */
    public AttributeHelper() {
        this.systemDescriptors = new HashMap<>();
    }

    /**
     * Constructor given service references to available {@link MetacardType}s.
     *
     * @param serviceReferences A list of {@link ServiceReference}s representing exposed
     *                          {@link MetacardType}s.
     */
    public AttributeHelper(SortedServiceReferenceList serviceReferences) {
        this.systemDescriptors = new HashMap<>();

        BundleContext bundleContext = FrameworkUtil.getBundle(AttributeHelper.class)
                .getBundleContext();
        serviceReferences.stream()
                .map((Function<ServiceReference, Object>) bundleContext::getService)
                .filter(MetacardType.class::isInstance)
                .map(MetacardType.class::cast)
                .filter(Objects::nonNull)
                .map(MetacardType::getAttributeDescriptors)
                .forEach(descriptors -> loadUniqueDescriptors(descriptors, systemDescriptors));
    }

    /**
     * Iterates over each {@link Metacard} in the {@param metacards} collection and applies each
     * attribute in the {@param attributeMap} only if the attribute is not already set on the
     * {@link Metacard}. That is, the attribute must be {@code null} or not in the {@link Metacard}'s
     * map.
     *
     * @param metacards    The list of metacards to attempt to add the attributes to.
     * @param attributeMap The map of attributes to attempt to add. Attributes already set on any
     *                     given {@link Metacard} will not be changed.
     */
    public void applyNewAttributes(List<Metacard> metacards, Map<String, String> attributeMap) {
        if (metacards.isEmpty() || attributeMap.isEmpty()) {
            return;
        }

        Map<String, AttributeDescriptor> systemAndMetacardDescriptors = new HashMap<>();
        systemAndMetacardDescriptors.putAll(systemDescriptors);
        metacards.stream()
                .map(Metacard::getMetacardType)
                .map(MetacardType::getAttributeDescriptors)
                .forEach(descriptors -> loadUniqueDescriptors(descriptors,
                        systemAndMetacardDescriptors));

        metacards.forEach(metacard -> attributeMap.keySet()
                .stream()
                .filter(key -> metacard.getAttribute(key) == null)
                .map(systemAndMetacardDescriptors::get)
                .filter(Objects::nonNull)
                .map(ad -> createAttribute(ad, attributeMap.get(ad.getName())))
                .filter(Objects::nonNull)
                .forEach(metacard::setAttribute));
    }

    /**
     * Attempts to create an {@link Attribute} according to the provided {@link AttributeDescriptor}
     * whose value is represented by the given string {@param parsedValue}. Returns null if the string
     * could not be parsed based on the descriptor's underlying {@link ddf.catalog.data.AttributeType.AttributeFormat}.
     *
     * @param attributeDescriptor The descriptor holding the format to use for parsing.
     * @param parsedValue         The value to be parsed.
     * @return A new {@link Attribute} if parsing succeeded, or null otherwise.
     */
    public Attribute createAttribute(AttributeDescriptor attributeDescriptor, String parsedValue) {
        try {
            if (attributeDescriptor.isMultiValued()) {
                LOGGER.trace("Found multi-valued attribute descriptor: {}",
                        attributeDescriptor.getName());
                return createMultiValuedAttribute(attributeDescriptor.getName(), parsedValue);
            }

            Serializable newValue;
            switch (attributeDescriptor.getType()
                    .getAttributeFormat()) {

            case INTEGER:
                newValue = Integer.parseInt(parsedValue);
                break;

            case FLOAT:
                newValue = Float.parseFloat(parsedValue);
                break;

            case DOUBLE:
                newValue = Double.parseDouble(parsedValue);
                break;

            case SHORT:
                newValue = Short.parseShort(parsedValue);
                break;
            case LONG:
                newValue = Long.parseLong(parsedValue);
                break;

            case DATE:
                Calendar calendar = DatatypeConverter.parseDateTime(parsedValue);
                newValue = calendar.getTime();
                break;

            case BOOLEAN:
                newValue = Boolean.parseBoolean(parsedValue);
                break;

            case BINARY:
                newValue = parsedValue.getBytes(Charset.forName("UTF-8"));
                break;

            case OBJECT:
            case STRING:
            case GEOMETRY:
            case XML:
                newValue = parsedValue;
                break;

            default:
                return null;
            }

            return new AttributeImpl(attributeDescriptor.getName(), newValue);

        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void loadUniqueDescriptors(Set<AttributeDescriptor> descriptorsToLoad,
            Map<String, AttributeDescriptor> destination) {
        descriptorsToLoad.stream()
                .filter(descriptor -> !destination.containsKey(descriptor.getName()))
                .forEach(descriptor -> destination.put(descriptor.getName(), descriptor));
    }

    private Attribute createMultiValuedAttribute(String name, String commaSeparatedList) {
        final String splitSymbol = ",";
        String[] entities = commaSeparatedList.split(splitSymbol, 0);
        List<Serializable> values = Arrays.stream(entities)
                .map(String::trim)
                .filter(str -> !str.isEmpty())
                .collect(Collectors.toList());
        return new AttributeImpl(name, values);
    }
}
