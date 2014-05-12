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

package ddf.catalog.data.metacardtype;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.data.MetacardTypeUnregistrationException;
import ddf.catalog.data.QualifiedMetacardType;

/**
 * Default implementation of the MetacardTypeRegistry.
 * 
 * <p>
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 * 
 */
public final class MetacardTypeRegistryImpl implements MetacardTypeRegistry {

    private Set<QualifiedMetacardType> registeredMetacardTypes;

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardTypeRegistryImpl.class);

    private MetacardTypeRegistryImpl() {
        this.registeredMetacardTypes = new CopyOnWriteArraySet<QualifiedMetacardType>();
    }

    public static MetacardTypeRegistry getInstance() {
        return new MetacardTypeRegistryImpl();
    }

    @Override
    public void register(QualifiedMetacardType qualifiedMetacardType)
        throws IllegalArgumentException {
        validateInput(qualifiedMetacardType);

        registeredMetacardTypes.add(qualifiedMetacardType);
    }

    @Override
    public QualifiedMetacardType lookup(String namespace, String metacardTypeName)
        throws IllegalArgumentException {

        validateInput(namespace, metacardTypeName);

        for (QualifiedMetacardType qmt : registeredMetacardTypes) {
            String currName = qmt.getName();
            String currNamespace = qmt.getNamespace();
            if (metacardTypeName.equals(currName) && namespace.equals(currNamespace)) {
                return qmt;
            }
        }
        LOGGER.debug("No registered MetacardType with namespace: {} and name: {}", namespace, metacardTypeName);
        return null;
    }

    @Override
    public QualifiedMetacardType lookup(String metacardTypeName) throws IllegalArgumentException {
        return lookup(QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, metacardTypeName);
    }

    @Override
    public void unregister(QualifiedMetacardType qualifiedMetacardType)
        throws IllegalArgumentException, MetacardTypeUnregistrationException {

        validateInput(qualifiedMetacardType);

        boolean removedSuccessfully = registeredMetacardTypes.remove(qualifiedMetacardType);

        if (!removedSuccessfully) {
            String message = "Unable to unregister specified MetacardType.";
            throw new MetacardTypeUnregistrationException(message);
        }
        LOGGER.debug("Successfully unregistered MetacardType.");
    }

    @Override
    public Set<QualifiedMetacardType> getRegisteredTypes() {
        return Collections.unmodifiableSet(new HashSet<QualifiedMetacardType>(
                registeredMetacardTypes));
    }

    private void validateInput(QualifiedMetacardType qmt) {
        if (qmt == null) {
            String message = "QualifiedMetacardType passed in cannot be null.";
            throw new IllegalArgumentException(message);
        }

        validateInput(qmt.getNamespace(), qmt.getName());
    }

    private void validateInput(String namespace, String metacardTypeName) {
        if (namespace == null) {
            String message = "Namespace parameter cannot be null.";
            throw new IllegalArgumentException(message);
        }

        if (metacardTypeName == null || metacardTypeName.isEmpty()) {
            String message = "MetacardTypeName parameter cannot be null or empty.";
            throw new IllegalArgumentException(message);
        }
    }
}
