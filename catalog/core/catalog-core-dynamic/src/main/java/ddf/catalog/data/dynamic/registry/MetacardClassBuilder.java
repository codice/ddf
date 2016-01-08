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

import org.apache.commons.beanutils.LazyDynaClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.dynamic.impl.MetacardPropertyDescriptor;

/**
 * MetacardClassBuilder facilitates reading in external definitions of metacard types
 * from XML descriptors. The definition of a metacard is built up as the name and each attribute
 * are added dynamically. Once the complete definition has been read in and added, the
 * {@link #getDynamicMetacardClass()} can be called to generate a dynamic class representing this
 * metacard type.
 */
public class MetacardClassBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardClassBuilder.class);

    private String name;
    private ArrayList<MetacardAttribute> attributes = new ArrayList<>();
    private static final MetacardPropertyDescriptor[] EMPTY_ARRAY = new MetacardPropertyDescriptor[] {};

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addAttribute(MetacardAttribute attribute) {
        attributes.add(attribute);
    }

    /**
     * Takes the values for this metacard type that have been set and generates a new metacard
     * type class to be used in creating dynamic metacards.
     * @return instance of a dynamic class containing the name and attributes collected
     */
    public LazyDynaClass getDynamicMetacardClass() {
        LazyDynaClass dynaClass = null;

        ArrayList<MetacardPropertyDescriptor> descriptors = new ArrayList<>();
        for (MetacardAttribute attr : attributes) {
            descriptors.add(attr.getMetacardPropertyDescriptor());
        }

        LOGGER.debug("Creating an instance of LazyDynaClass with name {} and {} attributes.", name, descriptors.size());
        dynaClass = new LazyDynaClass(name, null, descriptors.toArray(EMPTY_ARRAY));

        return dynaClass;
    }
}