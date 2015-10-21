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
 **/
package ddf.common.test.config;

import java.util.Dictionary;

import org.codice.ddf.platform.util.ConfigurationPropertiesComparator;
import org.osgi.service.cm.Configuration;

/**
 * Configuration predicate class that checks to see if a {@link Configuration} object's properties
 * match the properties provided.
 */
public class ConfigurationPropertiesMatch implements ConfigurationPredicate {
    private static final ConfigurationPropertiesComparator CONFIGURATION_PROPERTIES_COMPARATOR = new ConfigurationPropertiesComparator();

    private final Dictionary<String, Object> expectedProperties;

    /**
     * Constructor.
     *
     * @param expectedProperties properties to match
     */
    public ConfigurationPropertiesMatch(Dictionary<String, Object> expectedProperties) {
        this.expectedProperties = expectedProperties;
    }

    @Override
    public boolean test(Configuration configuration) {
        if ((configuration == null) || (configuration.getProperties() == null)) {
            return false;
        }

        return CONFIGURATION_PROPERTIES_COMPARATOR
                .equal(expectedProperties, configuration.getProperties());
    }
}
