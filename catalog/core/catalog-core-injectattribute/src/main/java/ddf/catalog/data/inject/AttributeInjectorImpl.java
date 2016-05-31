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
package ddf.catalog.data.inject;

import static org.apache.commons.lang.Validate.notNull;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeInjector;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class AttributeInjectorImpl implements AttributeInjector {
    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeInjectorImpl.class);

    private final AttributeRegistry attributeRegistry;

    private final Set<String> globalInjections = new CopyOnWriteArraySet<>();

    private final Map<String, Set<String>> metacardTypeInjections = new ConcurrentHashMap<>();

    public AttributeInjectorImpl(AttributeRegistry attributeRegistry) {
        this.attributeRegistry = attributeRegistry;
    }

    private BundleContext getBundleContext() {
        return FrameworkUtil.getBundle(getClass())
                .getBundleContext();
    }

    public void bind(ServiceReference<InjectableAttribute> reference) {
        LOGGER.debug("New service registered [{}]", reference);

        InjectableAttribute injectableAttribute = getBundleContext().getService(reference);
        String attributeName = injectableAttribute.attribute();
        Set<String> metacardTypeNames = injectableAttribute.metacardTypes();

        if (metacardTypeNames.isEmpty()) {
            globalInjections.add(attributeName);
        } else {
            metacardTypeNames.forEach(metacardTypeName -> {
                metacardTypeInjections.computeIfAbsent(metacardTypeName,
                        k -> new CopyOnWriteArraySet<>())
                        .add(attributeName);
            });
        }
    }

    public void unbind(ServiceReference<InjectableAttribute> reference) {
        LOGGER.debug("Service deregistered [{}]", reference);

        InjectableAttribute injectableAttribute = getBundleContext().getService(reference);
        String attributeName = injectableAttribute.attribute();
        Set<String> metacardTypeNames = injectableAttribute.metacardTypes();

        if (metacardTypeNames.isEmpty()) {
            globalInjections.remove(attributeName);
        } else {
            metacardTypeNames.forEach(metcardTypeName -> {
                Set<String> injectIntoMetacardType = metacardTypeInjections.get(metcardTypeName);
                if (injectIntoMetacardType != null) {
                    injectIntoMetacardType.remove(attributeName);
                }
            });
        }
    }

    private Set<String> injectableAttributes(String metacardTypeName) {
        final Set<String> injections = new HashSet<>(globalInjections);
        injections.addAll(metacardTypeInjections.getOrDefault(metacardTypeName,
                Collections.emptySet()));
        return injections;
    }

    @Override
    public MetacardType injectAttributes(MetacardType original) {
        notNull(original, "The metacard type cannot be null.");

        final String metacardTypeName = original.getName();

        final Set<AttributeDescriptor> injectAttributes =
                injectableAttributes(metacardTypeName).stream()
                        .map(attributeRegistry::lookup)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toSet());

        if (injectAttributes.isEmpty()) {
            return original;
        } else {
            return new MetacardTypeImpl(original.getName(), original, injectAttributes);
        }
    }

    @Override
    public Metacard injectAttributes(Metacard original) {
        notNull(original, "The metacard cannot be null.");

        final MetacardType newMetacardType = injectAttributes(original.getMetacardType());

        if (newMetacardType == original.getMetacardType()) {
            return original;
        } else {
            return changeMetacardType(original, newMetacardType);
        }
    }

    private Metacard changeMetacardType(Metacard original, MetacardType newMetacardType) {
        MetacardImpl newMetacard = new MetacardImpl(original);
        newMetacard.setType(newMetacardType);
        newMetacard.setSourceId(original.getSourceId());
        return newMetacard;
    }
}
