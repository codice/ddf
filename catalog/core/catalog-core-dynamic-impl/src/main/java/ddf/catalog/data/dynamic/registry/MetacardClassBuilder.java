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
package ddf.catalog.data.dynamic.registry;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.dynamic.api.MetacardPropertyDescriptor;
import ddf.catalog.data.dynamic.impl.MetacardPropertyDescriptorImpl;

/**
 * MetacardClassBuilder facilitates reading in external definitions of metacard types
 * from XML descriptors. The definition of a metacard is built up as the name and each attribute
 * are added dynamically. Once the complete definition has been read in and added, the
 * name and MetacardPropertyDescriptors can be retrieved to create a new DynamicMetacard type.
 */
public class MetacardClassBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardClassBuilder.class);

    private String name;
    private ArrayList<String> types = new ArrayList<>();
    private ArrayList<MetacardAttribute> attributes = new ArrayList<>();
    private static final MetacardPropertyDescriptorImpl[] EMPTY_IMPL_ARRAY = new MetacardPropertyDescriptorImpl[] {};
    private static final MetacardPropertyDescriptor[] EMPTY_ARRAY = new MetacardPropertyDescriptor[] {};
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addType(String typeName) {
        types.add(typeName);
    }

    public void addAttribute(MetacardAttribute attribute) {
        attributes.add(attribute);
    }

    /**
     * Takes the values for this metacard type that have been set and generates a new metacard
     * type class to be used in creating dynamic metacards.
     * @return instance of a dynamic class containing the name and attributes collected
     */
/*
    public LazyDynaClass getDynamicMetacardClass() {
        LazyDynaClass dynaClass = null;

        DynaProperty[] descriptors = getDescriptors();
        if (descriptors != null) {
            LOGGER.debug("Creating an instance of LazyDynaClass with name {} and {} attributes.",
                    name,
                    descriptors.length);
            dynaClass = new LazyDynaClass(name, null, descriptors);
        }

        return dynaClass;
    }
*/
    /**
     * Returns a list of type names that this metacard builds upon. The intent is for each type
     * in the list to be included in the metacard definition before attributes specific to the
     * type currently being defined.
     */
    public List<String> getTypes() {
        return types;
    }

    /**
     * Returns an array of the {@link MetacardPropertyDescriptor}s that have been prepared for the
     * current Metacard class definition.
     * @return array of metacard property descriptors used to describe
     */
    public MetacardPropertyDescriptor[] getDescriptorsAsArray() {
        List<MetacardPropertyDescriptor> descriptors = getDescriptors();
        return descriptors.toArray(EMPTY_ARRAY);
    }

    public List<MetacardPropertyDescriptorImpl> getDescriptorImpls() {
        ArrayList<MetacardPropertyDescriptorImpl> descriptors = new ArrayList<>();
        for (MetacardAttribute attr : attributes) {
            descriptors.add(attr.getMetacardPropertyDescriptor());
        }
        return descriptors;
    }

    public List<MetacardPropertyDescriptor> getDescriptors() {
        ArrayList<MetacardPropertyDescriptor> descriptors = new ArrayList<>();
        for (MetacardAttribute attr : attributes) {
            descriptors.add(attr.getMetacardPropertyDescriptor());
        }
        return descriptors;
    }
}