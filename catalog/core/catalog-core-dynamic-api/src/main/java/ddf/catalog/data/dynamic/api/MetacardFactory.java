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

import java.util.List;

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
     * Creates an instance of a dynamic metacard containing attributes corresponding to each
     * of the given names. It is expected that a metacard definition for each of the given
     * name has been registered with teh factory beforehand.
     * @param names the list of names describing the attributes to be included
     * @return an instances of a dynamic metacard containing attributes from all the named types
     */
    DynamicMetacard newInstance(List<String> names);

    /**
     * Adds the attributes for the named type to the given {@link DynamicMetacard} and return
     * the updated metacard.
     * @param metacard the existing metacard that to be extended with new attributes
     * @param name the metacard type whose attributes are to be added to the given dynamic metacard
     * @return the updated dynamic metacard with the new attributes
     */
    DynamicMetacard addAttributesForType(DynamicMetacard metacard, String name);

    /**
     * Returns an array of {@link MetacardPropertyDescriptor} for the base metacard type
     * @return an array of MetacardPropertyDescriptor objects representing the base metacard
     */
    MetacardPropertyDescriptor[] getBaseMetacardPropertyDescriptors();

    /**
     * Returns an array of {@link MetacardPropertyDescriptor} objects for the named metacard type.
     * If the name is null or empty, the base metacard attributes are returned (common to all
     * dynamic metacard types).
     * Returns an array of {@link MetacardPropertyDescriptor} for the the named metacard type
     * @param name the metacard type name whose attributes are to be returned (null = 'basic')
     * @return MetacardPropertyDescriptor objects for the named metacard type, empty array if not found
     */
    MetacardPropertyDescriptor[] getMetacardPropertyDescriptors(String name);

    /**
     * Registers a new metacard type info with the factory. The caller provides the name to be
     * registered under and the list of {@link MetacardPropertyDescriptor}s to be associated with
     * the name.
     * @param name the name the metacard type should be registered under
     * @param descriptors the list of {@link MetacardPropertyDescriptor}s to be associated with the name
     */
    void registerDynamicMetacardType(String name, List<MetacardPropertyDescriptor> descriptors);
}
