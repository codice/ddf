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
 **/
package org.codice.ddf.registry.common.metacard;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.registry.common.RegistryConstants;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;

public class RegistryUtility {

    /**
     * Checks that the metacard passed in has the Registry metacard tag and a valid RegistryId.
     *
     * @param metacard - will have tags and RegistryId evaluated
     * @return true if registryId is present in the metacard with the Registry_Tag, false otherwise
     */
    public static boolean isRegistryMetacard(Metacard metacard) {
        if (!metacard.getTags()
                .contains(RegistryConstants.REGISTRY_TAG)) {
            return false;
        }

        Attribute registryAttr = metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID);
        if (registryAttr == null || registryAttr.getValue() == null) {
            return false;
        }

        String registryId = registryAttr.getValue()
                .toString();

        return !StringUtils.isEmpty(registryId);
    }

    /**
     * Returns the registry id of the metacard passed in. In most cases this method expects that
     * the user has already evaluated the metacard with the isRegistryMetacard method from this
     * class.
     *
     * @param metacard - the metacard for which the registry Id will be returned
     * @return the String representation of the metacard's registry id attribute, null if not
     * present
     */
    public static String getRegistryId(Metacard metacard) {
        return getStringAttribute(metacard, RegistryObjectMetacardType.REGISTRY_ID, null);
    }

    /**
     * Identifies whether the metacard is a registry identity node
     *
     * @param metacard - metacard to be evaluated for registry identity status
     * @return true if the metacard is an identity node, false otherwise
     */
    public static boolean isIdentityNode(Metacard metacard) {
        return getBooleanAttribute(metacard, RegistryObjectMetacardType.REGISTRY_IDENTITY_NODE);
    }

    /**
     * Identifies whether the metacard is a local registry node
     *
     * @param metacard - metacard to be evaluated for local registry status
     * @return true if the metacard is an local node, false otherwise
     */
    public static boolean isLocalNode(Metacard metacard) {
        return getBooleanAttribute(metacard, RegistryObjectMetacardType.REGISTRY_LOCAL_NODE);
    }

    /**
     * Identifies whether the metacard has the attribute and its boolean value
     *
     * @param metacard         - metacard to be evaluated for boolean attribute
     * @param attributeToCheck - attribute to be evaluted, expected to be boolean in value
     * @return true if the attribute is present and set to true, false if the attribute is not present
     * set to null, or not a boolean valued attribute
     */
    public static boolean getBooleanAttribute(Metacard metacard, String attributeToCheck) {
        Attribute attribute = metacard.getAttribute(attributeToCheck);
        try {
            return attribute != null && attribute.getValue() != null
                    && (boolean) attribute.getValue();
        } catch (ClassCastException e) {
            return false;
        }
    }

    /**
     * Attempts to return a String attribute's value. If the attribute is null the default value
     * will be returned instead.
     *
     * @param metacard         - metacard to be evaluated for a String type attribute
     * @param attributeToCheck - attribute to be checked for, should have a String type
     * @param defaultValue     - the value expected to be returned if the attribute is not present
     * @return the defaultValue if attribute is not present, otherwise a String representing the
     * value of the attribute
     */
    public static String getStringAttribute(Metacard metacard, String attributeToCheck,
            String defaultValue) {
        Attribute attribute = metacard.getAttribute(attributeToCheck);
        if (attribute == null || attribute.getValue() == null) {
            return defaultValue;
        }
        return attribute.getValue()
                .toString();
    }

    /**
     * Attempts to return a List of Strings from an attribute. If the attribute is null the default
     * value will be returned instead.
     *
     * @param metacard         - metacard to be evaluated for a List of Strings type attribute
     * @param attributeToCheck - attribute to be checked for, should be a List of Strings type
     * @return the defaultValue if attribute is not present, otherwise a List of Strings
     * representing the values of the attribute
     */
    public static List<String> getListOfStringAttribute(Metacard metacard,
            String attributeToCheck) {
        Attribute attribute = metacard.getAttribute(attributeToCheck);
        if (attribute == null || attribute.getValue() == null) {
            return new ArrayList<>();
        }
        return attribute.getValues()
                .stream()
                .map(Object::toString)
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
