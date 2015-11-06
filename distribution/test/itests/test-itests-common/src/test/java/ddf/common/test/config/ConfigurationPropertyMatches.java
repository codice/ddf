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

import org.osgi.service.cm.Configuration;

/**
 * Configuration predicate class that checks to see if a {@link Configuration} property exists and
 * matches a specific regular expression.
 */
public class ConfigurationPropertyMatches implements ConfigurationPredicate {
    private String propertyName;

    private String valueRegex;

    /**
     * Constructor.
     *
     * @param propertyName name of the property this predicate will use
     * @param valueRegex   regular expression the property value needs to match for this predicate
     *                     to return {@code true}.
     */
    public ConfigurationPropertyMatches(String propertyName, String valueRegex) {
        this.propertyName = propertyName;
        this.valueRegex = valueRegex;
    }

    @Override
    public boolean test(Configuration configuration) {
        if ((configuration == null) || (configuration.getProperties() == null) || (
                configuration.getProperties().get(propertyName) == null)) {
            return false;
        }

        return ((String) configuration.getProperties().get(propertyName)).matches(valueRegex);
    }

    public String toString() {
        return String
                .format("property [%s] matches regular expression [%s]", propertyName, valueRegex);
    }
}
