/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/

package ddf.catalog.data;

import java.util.Set;

import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;

/**
 * Registry to maintain {@link MetacardType}s that are supported by the system's Endpoints and
 * Sources. MetacardTypes describe the attributes in a specific Metacard. This registry allows other
 * DDF components to lookup MetacardTypes based on their qualified name. MetacardTypes should
 * typically be registered by Sources that support that data set. Lookups will typically be
 * performed by InputTransformers to discover how the incoming metadata should be parsed.
 * 
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 * 
 * @author Ian Barnett
 * @author ddf.isgs@lmco.com
 * 
 */
public interface MetacardTypeRegistry {

    public static final String METACARD_TYPE_NAMESPACE_KEY = "metacardtype.namespace";

    public static final String METACARD_TYPE_NAME_KEY = "metacardtype.name";

    /**
     * Registers a {@link QualifiedMetacardType} in the system so that it is accessible to
     * {@link InputTransformer}s, {@link MetacardTransformer}s, {@link QueryResponseTransformer}s,
     * and other components. This allows those components to know how to properly interpret a
     * {@link Metacard} and its {@link Attribute}s.
     * 
     * If the QualifiedMetacardType does not contain a namespace, the default namespace will be
     * assumed.
     * 
     * @param qualifiedMetacardType
     *            the {@link QualifiedMetacardType} to make available to the catalog framework.
     * 
     * @throws IllegalArgumentException
     *             An IllegalArgumentException will be thrown if qualifiedMetacardType is null. An
     *             IllegalArgumentException will also be thrown if the {@link QualifiedMetacardType}
     *             's name is null or empty.
     */
    public void register(QualifiedMetacardType qualifiedMetacardType)
        throws IllegalArgumentException;

    /**
     * Removes from the registry the given QualifiedMetacardType.
     * 
     * @param qualifiedMetacardType
     *            The MetacardType to remove from the registry. Cannot be null or empty.
     * 
     * @throws IllegalArgumentException
     * @throws MetacardTypeUnregistrationException
     */
    public void unregister(QualifiedMetacardType qualifiedMetacardType)
        throws IllegalArgumentException, MetacardTypeUnregistrationException;

    /**
     * Gets the {@link MetacardType} identified by the namespace and MetacardType name.
     * 
     * @param namespace
     *            prefix qualifier in which the {@link MetacardType} name is unique.
     * @param metacardTypeName
     *            unique name identifying {@link MetacardType}. Cannot be null or empty.
     * @return {@link QualifiedMetacardType} matching provided namespace and metacardTypeName or
     *         null if no matching MetacardType can be found.
     * @throws IllegalArgumentException
     *             if the namespace is null or if the metacardTypeName is null or empty.
     */
    public QualifiedMetacardType lookup(String namespace, String metacardTypeName)
        throws IllegalArgumentException;

    /**
     * Gets the MetacardType identified using the default namespace and the specified MetacardType
     * name.
     * 
     * @param metacardTypeName
     *            unique name identifying {@link MetacardType}. Cannot be null or empty.
     * @return {@link QualifiedMetacardType} matching provided namespace and metacardTypeName or
     *         null if no matching MetacardType can be found.
     * @throws IllegalArgumentException
     *             if the metacardTypeName is null or empty.
     */
    public QualifiedMetacardType lookup(String metacardTypeName) throws IllegalArgumentException;

    /**
     * This must be invoked each time an updated view of the registered types is desired.
     * 
     * @return Unmodifiable copy of the set of registered MetacardTypes in the system.
     */
    public Set<QualifiedMetacardType> getRegisteredTypes();
}
