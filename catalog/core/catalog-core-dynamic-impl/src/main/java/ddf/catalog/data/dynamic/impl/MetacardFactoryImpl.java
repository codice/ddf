/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.data.dynamic.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.beanutils.DynaProperty;
import org.apache.commons.beanutils.LazyDynaBean;
import org.apache.commons.beanutils.LazyDynaClass;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.dynamic.api.DynamicMetacard;
import ddf.catalog.data.dynamic.api.MetacardPropertyDescriptor;

/**
 * MetacardFactoryImpl is used to manage registered metacard types and to generate instances of
 * requested types. This class includes the definition for the basic DynamicMetacardImpl and ensures
 * that all generated metacards include the DynamicMetacardImpl properties.
 */
public class MetacardFactoryImpl implements ddf.catalog.data.dynamic.api.MetacardFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardFactoryImpl.class);

    private static Map<String, LazyDynaClass> typeClasses = new HashMap<>();
    private static Map<String, MetacardPropertyDescriptor[]> typeProperties = new HashMap<>();

    private static MetacardPropertyDescriptorImpl[] metacardPropertyDescriptors;

    public MetacardFactoryImpl() {

        // register the base ddf type
        LOGGER.debug("Adding basic dynamic property descriptors");
        metacardPropertyDescriptors = new MetacardPropertyDescriptorImpl[]{
                new MetacardPropertyDescriptorImpl(Metacard.CREATED, Date.class),
                new MetacardPropertyDescriptorImpl(Metacard.MODIFIED, Date.class),
                new MetacardPropertyDescriptorImpl(Metacard.EXPIRATION, Date.class),
                new MetacardPropertyDescriptorImpl(Metacard.EFFECTIVE, Date.class),
                new MetacardPropertyDescriptorImpl(Metacard.ID, String.class),
                new MetacardPropertyDescriptorImpl(Metacard.GEOGRAPHY, String.class), // ("location", String.class),
                new MetacardPropertyDescriptorImpl(DynamicMetacard.SOURCE_ID, String.class), // ("sourceId", String.class),
                new MetacardPropertyDescriptorImpl(Metacard.THUMBNAIL, byte[].class),
                new MetacardPropertyDescriptorImpl(Metacard.TITLE, String.class),
                new MetacardPropertyDescriptorImpl(Metacard.METADATA, String.class),
                //new MetacardPropertyDescriptorImpl(DynamicMetacardImpl.METACARD_TYPE, MetacardType.class), // ("metacardType", MetacardType.class),
                new MetacardPropertyDescriptorImpl(Metacard.TARGET_NAMESPACE, String.class), // ("contentTypeNamespace", URI.class),
                new MetacardPropertyDescriptorImpl(Metacard.CONTENT_TYPE, String.class), // ("contentTypeName", String.class),
                new MetacardPropertyDescriptorImpl(Metacard.POINT_OF_CONTACT, String.class),
                new MetacardPropertyDescriptorImpl(Metacard.DESCRIPTION, String.class),
                new MetacardPropertyDescriptorImpl(Metacard.CONTENT_TYPE_VERSION, String.class),
                new MetacardPropertyDescriptorImpl(Metacard.RESOURCE_URI, String.class),
                new MetacardPropertyDescriptorImpl(Metacard.RESOURCE_SIZE, String.class),
                new MetacardPropertyDescriptorImpl(Metacard.SECURITY, Map.class)
        };
        LOGGER.debug("Registering dynamic metacard type with name: {}", DynamicMetacard.DYNAMIC);
        registerDynamicMetacardType(DynamicMetacard.DYNAMIC, Arrays.asList(metacardPropertyDescriptors));

        LOGGER.debug("Created instance of MetacardFactoryImpl");
    }

    /**
     * Generates an instance of the default DynamicMetacardImpl.
     * @return an instance of the default DynamicMetacardImpl
     */
    @Override
    public DynamicMetacard newInstance() {
        return newInstance(DynamicMetacard.DYNAMIC);
    }

    /**
     * Creates a new metacard instance corresponding the the name provided. Any registered metacard type
     * can be generated. Returns null if the requested metacard type has not been registered.
     * @param name the name of the type of metacard to create
     * @return an instance of the specified type of metacard, or null if the specified type has not been registered
     */
    @Override
    public DynamicMetacard newInstance(String name)  { //throws InstantiationException, IllegalAccessException {
        LOGGER.debug("Creating a new metacard of type {}", name);
        DynamicMetacard dynamicMetacard = null;
        LazyDynaBean lazyDynaBean = null;
        LazyDynaClass dynaClass = typeClasses.get(name);
        if (dynaClass != null) {
            lazyDynaBean = new LazyDynaBean(dynaClass);
            dynamicMetacard = new DynamicMetacardImpl(lazyDynaBean);
        }
        return dynamicMetacard;
    }

    @Override
    public DynamicMetacard newInstance(List<String> names) {
        LOGGER.debug("Creating a new metacard with the following types: {}", names);
        if (names == null || names.size() == 0) {
            return newInstance();
        }

        DynamicMetacard metacard = null;
        for (String name : names) {
            if (metacard == null) {
                LOGGER.debug("Creating metacard for type {}", name);
                metacard = newInstance(name);
            } else {
                LOGGER.debug("Adding attributes to metacard for type {}", name);
                metacard = addAttributesForType(metacard, name);
            }
        }
        return metacard;
    }

    @Override
    public DynamicMetacard addAttributesForType(DynamicMetacard metacard, String name) {
        String newName = metacard.getName() + '.' + name;
        // If this combination hasn't been registered, combine attributes and register
        LazyDynaClass dynaClass = typeClasses.get(newName);
        if (dynaClass == null) {
            ArrayList<MetacardPropertyDescriptor> combinedDescriptors = new ArrayList<>();
            MetacardPropertyDescriptor[] newDescriptors = typeProperties.get(name);
            if (newDescriptors != null) {
                combinedDescriptors.addAll(Arrays.asList(typeProperties.get(metacard.getName())));
                combinedDescriptors.addAll(Arrays.asList(newDescriptors));
                registerDynamicMetacardType(newName, combinedDescriptors);
            }
        }

        return newInstance(newName);
    }

    /**
     * Returns an array of {@link MetacardPropertyDescriptorImpl}s representing the core attributes used in the basic
     * {@link DynamicMetacardImpl} - useful for combining with other property descriptors to generate a new type of metacard.
     * @return array of {@link MetacardAttributeDescriptor}s
     */
    @Override
    public MetacardPropertyDescriptorImpl[] getBaseMetacardPropertyDescriptors() {
        return Arrays.copyOf(metacardPropertyDescriptors, metacardPropertyDescriptors.length);
    }

    @Override
    public MetacardPropertyDescriptor[] getMetacardPropertyDescriptors(String name) {
        MetacardPropertyDescriptor[] result = null;
        MetacardPropertyDescriptor[] descriptors = typeProperties.get(name);
        if (descriptors != null && descriptors.length > 0) {
            result = Arrays.copyOf(descriptors, descriptors.length);
        }
        return result;
    }

    @Override
    public void registerDynamicMetacardType(String name,
            List<MetacardPropertyDescriptor> descriptors) {
        if (StringUtils.isNotEmpty(name) && descriptors != null && descriptors.size() > 0) {
            DynaProperty[] properties = new DynaProperty[descriptors.size()];
            int i = 0;
            for (MetacardPropertyDescriptor descriptor : descriptors) {
                properties[i++] = getDynaProperty(descriptor);
            }
            LazyDynaClass dynaClass = new LazyDynaClass(name, properties);
            // set to return null when getter is called with non-existent property
            dynaClass.setReturnNull(true);
            LOGGER.info("Registering new dynamic metacard - name {}", dynaClass.getName());
            typeClasses.put(dynaClass.getName(), dynaClass);
            typeProperties.put(name, descriptors.toArray(new MetacardPropertyDescriptor[0]));
        } else {
            LOGGER.warn("Called to register dynamic metacard with invalid data - name must be non-null and one or more descriptors - name: {}  descriptor size: {}",
                    name, descriptors == null ? "null" : descriptors.size());
        }
    }
    
    private DynaProperty getDynaProperty(MetacardPropertyDescriptor property) {
        MetacardPropertyDescriptorImpl prop = null;
        if (property instanceof MetacardPropertyDescriptorImpl) {
            prop = (MetacardPropertyDescriptorImpl) property;
        } else {
            prop = new MetacardPropertyDescriptorImpl(property.getName(),
                    property.getType(), property.getContentType(), property.isIndexedBySource(),
                    property.isStored(), property.isTokenized());
        }
        return prop;
    }

    private static void registerNewType(String name, List<MetacardPropertyDescriptorImpl> descriptors) {
        // save the list of MetacardPropertyDescriptor
        if (typeProperties.get(name) != null) {
            LOGGER.warn("Attempt to overwrite dynamic metacard type with name {} - only one instance allowed.", name);
            return;
        }

        MetacardPropertyDescriptorImpl[] descriptorArray = new MetacardPropertyDescriptorImpl[descriptors.size()];
        int i = 0;
        for (MetacardPropertyDescriptorImpl descriptor : descriptors) {
            descriptorArray[i++] = descriptor;
        }
        typeProperties.put(name, descriptorArray);
        LazyDynaClass dynaClass = new LazyDynaClass(name, descriptorArray);
        dynaClass.setReturnNull(true);
        LOGGER.info("Registering new dynamic metacard - name {}", dynaClass.getName());
        typeClasses.put(dynaClass.getName(), dynaClass);


    }
}
