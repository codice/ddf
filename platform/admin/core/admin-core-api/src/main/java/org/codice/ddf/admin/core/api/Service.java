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
package org.codice.ddf.admin.core.api;

import java.util.List;

/**
 * Represents a managed service or managed service factory instance
 * <p>
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 * </p>
 */
public interface Service extends Metatype {

    String FACTORY = "factory";

    String CONFIGURATIONS = "configurations";

    String DISABLED_CONFIGURATIONS = "disabledConfigurations";

    default boolean isFactory() {
        return (boolean) get(FACTORY);
    }

    default void setFactory(boolean factory) {
        put(FACTORY, factory);
    }

    default List<ConfigurationDetails> getConfigurations() {
        return (List<ConfigurationDetails>) get(CONFIGURATIONS);
    }

    default void setConfigurations(List<ConfigurationDetails> configurations) {
        put(CONFIGURATIONS, configurations);
    }

    default List<ConfigurationDetails> getDisabledConfigurations() {
        return (List<ConfigurationDetails>) get(DISABLED_CONFIGURATIONS);
    }

    default void setDisabledConfigurations(List<ConfigurationDetails> disabledConfigurations) {
        put(DISABLED_CONFIGURATIONS, disabledConfigurations);
    }
}
