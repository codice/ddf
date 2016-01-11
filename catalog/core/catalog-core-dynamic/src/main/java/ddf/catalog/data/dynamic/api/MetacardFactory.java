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
package ddf.catalog.data.dynamic.api;

import org.apache.commons.beanutils.LazyDynaClass;

import ddf.catalog.data.dynamic.impl.DynamicMetacard;
import ddf.catalog.data.dynamic.impl.MetacardPropertyDescriptor;

public interface MetacardFactory {
    /**
     * Creates an instance of a basic dynamic metacard with the default attributes.
     * @return new instance of a basic dynamic metacard
     */
    DynamicMetacard newInstance();

    /**
     * Creates an instance of a dynamic metacard corresponding to the given name. It is
     * expected that a metacard of the given name has been registered with the factory
     * beforehand.
     * @param name the name of the metacard type to create
     * @return an instance of a dynamic metacard of the type whose name was provided
     */
    DynamicMetacard newInstance(String name);

    /**
     * Returns an array of {@link MetacardPropertyDescriptor} for the base metacard type
     * @return an array of MetacardPropertyDescriptor objects representing the base metacard
     */
    MetacardPropertyDescriptor[] getBaseMetacardPropertyDescriptors();

    /**
     * Registers a new metacard type info with the factory. The provided {@link LazyDynaClass}
     * is registered under it's name in the factory allow new instances to be created.
     * @param dynaClass the metacard class type to be registered
     */
    void addDynaClass(LazyDynaClass dynaClass);
}
