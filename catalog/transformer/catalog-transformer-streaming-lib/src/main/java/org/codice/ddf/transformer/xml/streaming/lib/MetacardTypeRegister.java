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

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.codice.ddf.transformer.xml.streaming.SaxEventHandlerFactory;
import org.osgi.framework.ServiceRegistration;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.util.impl.SortedServiceList;

public class MetacardTypeRegister extends SortedServiceList {

    private Map<String, ServiceRegistration> metacardTypeServiceRegistrations = new HashMap<>();

    private List<SaxEventHandlerFactory> saxEventHandlerFactories = new ArrayList<>();

    private DynamicMetacardType metacardType;

    private String id = "DEFAULT_ID";

    public synchronized void bind(SaxEventHandlerFactory saxEventHandlerFactory) {
        saxEventHandlerFactories.add(saxEventHandlerFactory);
        setMetacardType();
    }

    public synchronized void unbind(SaxEventHandlerFactory saxEventHandlerFactory) {
        saxEventHandlerFactories.remove(saxEventHandlerFactory);
        setMetacardType();
    }

    /**
     * Defines a {@link DynamicMetacardType} based on component Sax Event Handler Factories
     * and what attributes they populate
     *
     * @return a DynamicMetacardType that describes the type of metacard that is created in this transformer
     */
    private synchronized void setMetacardType() {
        Set<AttributeDescriptor> attributeDescriptors = new HashSet<>();

        if (saxEventHandlerFactories != null) {
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

        Dictionary serviceProperties = new Hashtable();
        serviceProperties.put("name", metacardType.getName());

        ServiceRegistration serviceRegistration =
                getContext().registerService(MetacardType.class.getName(),
                        metacardType,
                        serviceProperties);

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

    public synchronized void setId(String id) {
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
                .filter(p -> p.getName().equals(attributeName))
                .findFirst()
                .orElse(null);
    }
}
