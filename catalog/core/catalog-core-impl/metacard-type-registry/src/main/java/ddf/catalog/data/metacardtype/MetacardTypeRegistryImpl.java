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
package ddf.catalog.data.metacardtype;

import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.data.QualifiedMetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.QualifiedMetacardTypeImpl;

/**
 * Default implementation of the {@link MetacardTypeRegistry} that automatically manages
 * {@link MetacardType}s that are registered as OSGi services.
 * <p>
 * For lookup operations ({@link #lookup(String)} and {@link #lookup(String, String)}), this
 * implementation doesn't return the exact {@link QualifiedMetacardType} objects that were
 * registered. Instead, it returns a new {@link QualifiedMetacardType} with the same name and
 * namespace as the original but with a set of {@link AttributeDescriptor}s containing those of the
 * original plus any that are marked as global in the {@link AttributeRegistry}.
 * <p>
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public final class MetacardTypeRegistryImpl implements MetacardTypeRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardTypeRegistryImpl.class);

    private final AttributeRegistry attributeRegistry;

    private final Set<QualifiedMetacardType> registeredMetacardTypes;

    public MetacardTypeRegistryImpl(AttributeRegistry attributeRegistry) {
        this.attributeRegistry = attributeRegistry;
        registeredMetacardTypes = new CopyOnWriteArraySet<>();
        // TODO (jrnorth) - should this be registered automatically like this?
        register(new QualifiedMetacardTypeImpl(BasicTypes.BASIC_METACARD));
    }

    private BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(getClass())
                .getBundleContext();
    }

    public void bind(ServiceReference<MetacardType> serviceReference) {
        LOGGER.info("New service registered [{}]", serviceReference);

        MetacardType metacardType = getBundleContext().getService(serviceReference);

        register(new QualifiedMetacardTypeImpl(metacardType));
    }

    public void unbind(ServiceReference<MetacardType> serviceReference) {
        LOGGER.info("Service deregistered [{}]", serviceReference);

        MetacardType metacardType = getBundleContext().getService(serviceReference);

        unregister(new QualifiedMetacardTypeImpl(metacardType));
    }

    @Override
    public void register(QualifiedMetacardType qualifiedMetacardType) {
        validateInput(qualifiedMetacardType);

        registeredMetacardTypes.add(qualifiedMetacardType);
    }

    @Override
    public Optional<QualifiedMetacardType> lookup(String namespace, String metacardTypeName) {
        validateInput(namespace, metacardTypeName);

        for (QualifiedMetacardType qmt : registeredMetacardTypes) {
            String currName = qmt.getName();
            String currNamespace = qmt.getNamespace();
            if (metacardTypeName.equals(currName) && namespace.equals(currNamespace)) {
                return Optional.of(addMixins(qmt));
            }
        }
        LOGGER.debug("No registered MetacardType with namespace: {} and name: {}",
                namespace,
                metacardTypeName);
        return Optional.empty();
    }

    @Override
    public Optional<QualifiedMetacardType> lookup(String metacardTypeName) {
        return lookup(QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, metacardTypeName);
    }

    @Override
    public void unregister(QualifiedMetacardType qualifiedMetacardType) {
        validateInput(qualifiedMetacardType);

        registeredMetacardTypes.remove(qualifiedMetacardType);
    }

    @Override
    public Set<QualifiedMetacardType> getRegisteredTypes() {
        return Collections.unmodifiableSet(registeredMetacardTypes);
    }

    private void validateInput(QualifiedMetacardType qmt) {
        notNull(qmt, "The qualified metacard type cannot be null.");
        validateInput(qmt.getNamespace(), qmt.getName());
    }

    private void validateInput(String namespace, String metacardTypeName) {
        notNull(namespace, "The namespace cannot be null.");
        notEmpty(metacardTypeName, "The metacard type name cannot be null or empty.");
    }

    private QualifiedMetacardType addMixins(QualifiedMetacardType metacardType) {
        Set<AttributeDescriptor> attributesWithMixins =
                new HashSet<>(metacardType.getAttributeDescriptors());

        attributesWithMixins.addAll(attributeRegistry.getGlobalAttributes());

        return new QualifiedMetacardTypeImpl(metacardType.getNamespace(),
                metacardType.getName(),
                attributesWithMixins);
    }
}
