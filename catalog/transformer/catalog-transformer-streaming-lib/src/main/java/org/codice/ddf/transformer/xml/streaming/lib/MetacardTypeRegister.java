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
package org.codice.ddf.transformer.xml.streaming.lib;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.felix.utils.collections.MapToDictionary;
import org.codice.ddf.transformer.xml.streaming.SaxEventHandlerFactory;
import org.osgi.framework.ServiceRegistration;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.util.impl.SortedServiceList;

public class MetacardTypeRegister extends SortedServiceList {

    private Map<String, ServiceRegistration> metacardTypeServiceRegistrations = new HashMap<>();

    private List<SaxEventHandlerFactory> saxEventHandlerFactories;

    private List<String> saxEventHandlerConfiguration;

    private DynamicMetacardType metacardType;

    private String id = "DEFAULT_ID";

    public void bind(SaxEventHandlerFactory saxEventHandlerFactory) {
        setMetacardType();
    }

    public void unbind(SaxEventHandlerFactory saxEventHandlerFactory) {
        setMetacardType();
    }

    /**
     * Defines a {@link DynamicMetacardType} based on component Sax Event Handler Factories
     * and what attributes they populate
     *
     * @return a DynamicMetacardType that describes the type of metacard that is created in this transformer
     */
    public void setMetacardType() {
        Set<AttributeDescriptor> attributeDescriptors = new HashSet<>();

        if (saxEventHandlerConfiguration != null && saxEventHandlerFactories != null) {
            for (SaxEventHandlerFactory factory : saxEventHandlerFactories) {
                attributeDescriptors.addAll(factory.getSupportedAttributeDescriptors());
            }
        }
        attributeDescriptors.addAll(BasicTypes.BASIC_METACARD.getAttributeDescriptors());

        DynamicMetacardType dynamicMetacardType = new DynamicMetacardType(attributeDescriptors, id);
        registerMetacardType(dynamicMetacardType);
        metacardType = dynamicMetacardType;
    }

    public MetacardType getMetacardType() {
        if (metacardType == null) {
            setMetacardType();
        }

        return metacardType;
    }

    private synchronized void registerMetacardType(MetacardType metacardType) {
        unregisterMetacardType(metacardType);

        Map<String, Object> serviceProperties = new HashMap<>();
        serviceProperties.put("name", metacardType.getName());

        ServiceRegistration serviceRegistration =
                getContext().registerService(MetacardType.class.getName(),
                        metacardType,
                        new MapToDictionary(serviceProperties));

        this.metacardTypeServiceRegistrations.put(metacardType.getName(), serviceRegistration);
    }

    private synchronized void unregisterMetacardType(MetacardType metacardType) {
        if (metacardType == null) {
            return;
        }

        ServiceRegistration serviceRegistration =
                metacardTypeServiceRegistrations.get(metacardType.getName());
        if (serviceRegistration != null) {
            serviceRegistration.unregister();
            metacardTypeServiceRegistrations.remove(metacardType.getName());
        }
    }

    /**
     * Setter to set the list of all {@link SaxEventHandlerFactory}s. Usually called from a blueprint,
     * and the blueprint will keep the factories updated
     *
     * @param saxEventHandlerFactories a list of all SaxEventHandlerFactories (usually a list of all
     *                                 factories that are available as services)
     */
    public void setSaxEventHandlerFactories(List<SaxEventHandlerFactory> saxEventHandlerFactories) {
        this.saxEventHandlerFactories = saxEventHandlerFactories;
    }

    /**
     * Setter to set the configuration of SaxEventHandlers used to parse metacards
     *
     * @param saxEventHandlerConfiguration a list of SaxEventHandlerFactory ids
     */
    public void setSaxEventHandlerConfiguration(List<String> saxEventHandlerConfiguration) {
        this.saxEventHandlerConfiguration = saxEventHandlerConfiguration;
    }

    public void setId(String id) {
        this.id = id;
    }
}

class DynamicMetacardType implements MetacardType {

    Set<AttributeDescriptor> attributeDescriptors;

    String name;

    public DynamicMetacardType(Set<AttributeDescriptor> attDesc, String name) {
        this.attributeDescriptors = attDesc;
        this.name = name + ".metacard";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors() {
        return attributeDescriptors;
    }

    @Override
    public AttributeDescriptor getAttributeDescriptor(String attributeName) {
        return attributeDescriptors.stream()
                .filter(p -> p.getName()
                        .equals(attributeName))
                .collect(Collectors.toList())
                .get(0);
    }
}
