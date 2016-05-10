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
package ddf.catalog.data;

import java.io.Serializable;
import java.util.Optional;

/**
 * Manages default attribute values that apply to all metacard types and default attribute values
 * that apply to specific metacard types.
 * <p>
 * Default attribute values are separated into two categories:
 * <ol>
 *     <li>'Global' default values (default values that are tied to an attribute but not to a
 *     specific metacard type)</li>
 *     <li>Default values that apply to specific metacard types</li>
 * </ol>
 * In the event that an attribute has a 'global' default value as well as a default value for a
 * specific metacard type, the default value for the specific metacard type will be returned (i.e.,
 * the more specific default value wins).
 */
public interface DefaultAttributeValueRegistry {
    /**
     * Registers the given value as the default value for the given attribute across all metacard
     * types (i.e., it is a 'global' default value). Overwrites the current default value for the
     * attribute if one exists.
     *
     * @param attributeName the name of the attribute, cannot be null
     * @param defaultValue  the default value, cannot be null
     * @throws IllegalArgumentException if either argument is null
     */
    void setDefaultValue(String attributeName, Serializable defaultValue);

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
    void setDefaultValue(String metacardTypeName, String attributeName, Serializable defaultValue);

    /**
     * Retrieves the default value registered for the given attribute of the given metacard type.
     * <p>
     * In the event that an attribute has a 'global' default value as well as a default value for
     * the given metacard type, the default value for the given metacard type will be returned
     * (i.e., the more specific default value wins). If the attribute has a 'global' default value
     * but no default value for the given metacard type, then the 'global' default value will be
     * returned.
     *
     * @param metacardTypeName the name of the metacard type, cannot be null
     * @param attributeName    the name of the attribute belonging to the metacard type, cannot be null
     * @return an {@link Optional} containing the default value registered for the given attribute
     * of the given metacard type, or {@link Optional#empty()} if the attribute does not have a
     * default value registered
     * @throws IllegalArgumentException if either argument is null
     */
    Optional<Serializable> getDefaultValue(String metacardTypeName, String attributeName);

    /**
     * Removes the default value for the given attribute.
     *
     * @param attributeName the name of the attribute whose default value will be removed, cannot
     *                      be null
     * @throws IllegalArgumentException if {@code attributeName} is null
     */
    void removeDefaultValue(String attributeName);

    /**
     * Removes the default value for the given attribute of the given metacard type.
     *
     * @param metacardTypeName the name of the metacard type, cannot be null
     * @param attributeName    the name of the attribute whose default value will be removed, cannot
     *                         be null
     * @throws IllegalArgumentException if either argument is null
     */
    void removeDefaultValue(String metacardTypeName, String attributeName);

    /**
     * Removes all 'global' default values (i.e., the default values for the attributes that are not
     * tied to specific metacard types).
     */
    void removeDefaultValues();

    /**
     * Removes all the default attribute values for the given metacard type.
     *
     * @param metacardTypeName the name of the metacard type whose default attribute values will be
     *                         removed, cannot be null
     * @throws IllegalArgumentException if {@code metacardTypeName} is null
     */
    void removeDefaultValues(String metacardTypeName);
}
