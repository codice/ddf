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
package ddf.catalog.data.impl;

import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

import ddf.catalog.data.DefaultAttributeValue;

/**
 * Manages default attribute values that apply to all metacard types and default attribute values
 * that apply to specific metacard types.
 * <p>
 * Default attribute values are separated into two categories:
 * <ol>
 * <li>'Global' default values (default values that are tied to an attribute but not to a
 * specific metacard type)</li>
 * <li>Default values that apply to specific metacard types</li>
 * </ol>
 * In the event that an attribute has a 'global' default value as well as a default value for a
 * specific metacard type, the default value for the specific metacard type will be returned (i.e.,
 * the more specific default value wins).
 *
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * 
 */
public class DefaultAttributeValueImpl implements DefaultAttributeValue {
    private final Set<String> metacardTypeNames;

    private final String attributeName;

    private final Serializable defaultValue;

    /**
     * Registers the given value as the default value for the given attribute across all metacard
     * types (i.e., it is a 'global' default value). Overwrites the current default value for the
     * attribute if one exists.
     *
     * @param attributeName the name of the attribute, cannot be null
     * @param defaultValue  the default value, cannot be null
     * @throws IllegalArgumentException if either argument is null
     */
    public DefaultAttributeValueImpl(String attributeName, Serializable defaultValue) {
        notEmpty(attributeName, "The attribute name cannot be empty.");
        notNull(defaultValue, "The default value cannot be null.");
        metacardTypeNames = Collections.emptySet();
        this.attributeName = attributeName;
        this.defaultValue = defaultValue;
    }

    /**
     * Registers the given value as the default value for the given attribute of the given metacard
     * type. Overwrites the current default value for the given metacard type-attribute pair if one
     * exists.
     *
     * @param metacardTypeName the name of the metacard type, cannot be null
     * @param attributeName    the name of the attribute belonging to the metacard type, cannot be null
     * @param defaultValue     the default value, cannot be null
     * @throws IllegalArgumentException if any arguments are null
     */
    public DefaultAttributeValueImpl(String metacardTypeName, String attributeName,
            Serializable defaultValue) {
        notNull(metacardTypeName, "The metacard type name cannot be null.");
        notEmpty(attributeName, "The attribute name cannot be empty.");
        notNull(defaultValue, "The default value cannot be null.");
        this.metacardTypeNames =
                Collections.unmodifiableSet(Collections.singleton(metacardTypeName));
        this.attributeName = attributeName;
        this.defaultValue = defaultValue;
    }

    /**
     * Registers the given value as the default value for the given attribute of the given metacard
     * type. Overwrites the current default value for the given metacard type-attribute pair if one
     * exists.
     *
     * @param metacardTypeNames the name of the metacard type, cannot be null
     * @param attributeName     the name of the attribute belonging to the metacard type, cannot be null
     * @param defaultValue      the default value, cannot be null
     * @throws IllegalArgumentException if any arguments are null
     */
    public DefaultAttributeValueImpl(Set<String> metacardTypeNames, String attributeName,
            Serializable defaultValue) {
        notNull(metacardTypeNames, "The metacard type name cannot be null.");
        notEmpty(metacardTypeNames, "The metacard type name cannot be empty.");
        notEmpty(attributeName, "The attribute name cannot be empty.");
        notNull(defaultValue, "The default value cannot be null.");
        this.metacardTypeNames = Collections.unmodifiableSet(metacardTypeNames);
        this.attributeName = attributeName;
        this.defaultValue = defaultValue;
    }

    public Set<String> getMetacardTypeNames() {
        return metacardTypeNames;
    }

    public String getAttributeName() {
        return attributeName;
    }

    public Serializable getDefaultValue() {
        return defaultValue;
    }
}
