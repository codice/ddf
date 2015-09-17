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
package org.codice.ddf.security.policy.context.attributes;

import java.util.Arrays;

import ddf.security.permission.KeyValuePermission;

/**
 * Default implementation of ContextAttributeMapping
 */
public class DefaultContextAttributeMapping implements ContextAttributeMapping {

    private String attributeName;

    private String attributeValue;

    private String context;

    private KeyValuePermission keyValuePermission;

    public DefaultContextAttributeMapping(String context, String attributeName,
            String attributeValue) {
        this.context = context;
        this.attributeName = attributeName;
        this.attributeValue = attributeValue;
    }

    @Override
    public String getAttributeName() {
        return attributeName;
    }

    @Override
    public void setAttributeName(String name) {
        this.attributeName = name;
    }

    @Override
    public String getAttributeValue() {
        return attributeValue;
    }

    @Override
    public void setAttributeValue(String value) {
        this.attributeValue = value;
    }

    @Override
    public KeyValuePermission getAttributePermission() {
        if (keyValuePermission == null) {
            keyValuePermission = new KeyValuePermission(attributeName,
                    Arrays.asList(attributeValue));
        }
        return keyValuePermission;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
