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
 */
package org.codice.ddf.configuration.persistence.felix;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

/**
 * Class called by {@link FelixPersistenceStrategy} to convert certain values (e.g., floats,
 * doubles) from a human-readable form to a Felix compatible version.
 */
class PropertyConverter implements Consumer<String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyConverter.class);

    private static final String PROPERTY_NAME_REGEX = "(\\s*[\\w-]+(?:\\.[\\w-]+)*\\s*)";

    private static final String PROPERTY_TYPE_REGEX = "([A-Za-z])*";

    private static final String PROPERTY_VALUE_REGEX = "([\"\\[\\(]+.*)";

    private static final Pattern PROPERTY_PATTERN = Pattern.compile(String.format("^%s=%s%s",
            PROPERTY_NAME_REGEX,
            PROPERTY_TYPE_REGEX,
            PROPERTY_VALUE_REGEX));

    private static final String NEW_LINE = "\r\n";

    private static Map<String, PropertyValueConverter> valueConverters = ImmutableMap.of("f",
            new FloatValueConverter(),
            "d",
            new DoubleValueConverter());

    private final StringBuilder filteredOutput;

    private final Set<String> propertyNames = new HashSet<>();

    public PropertyConverter(StringBuilder filteredOutput) {
        this.filteredOutput = filteredOutput;
    }

    /**
     * Converts the line provided if necessary and appends the line back to the
     * {@code filteredOutput} provided in the constructor.
     * <p/>
     * This method will delegate the property value conversion to the proper
     * {@link PropertyValueConverter} based on the type of property and whether a property converter
     * can be found for that type.
     * <p/>
     * Before calling the property converter, this method will remove all leading and trailing
     * white spaces and make sure that the value starts with either a quote ("), square bracket ([)
     * or opening parenthesis and is not {@code null}.
     *
     * @param line line to convert
     */
    @Override
    public void accept(String line) {
        Matcher matcher = PROPERTY_PATTERN.matcher(line);

        if (matcher.matches()) {
            LOGGER.debug("Line {} matched regex", line);

            StringBuilder newLine = new StringBuilder();

            String propertyName = matcher.group(1);
            String propertyType = matcher.group(2);
            String propertyValue = matcher.group(3);

            propertyNames.add(propertyName);

            newLine.append(propertyName)
                    .append('=');

            if (propertyType != null) {
                newLine.append(propertyType);
            }

            if (propertyType != null && !propertyValue.isEmpty() && valueConverters.containsKey(
                    propertyType.toLowerCase())) {
                valueConverters.get(propertyType.toLowerCase())
                        .convert(propertyValue, newLine);
            } else {
                LOGGER.debug("Property value {} for line {} doesn't need conversion",
                        propertyValue,
                        line);
                newLine.append(propertyValue);
            }

            filteredOutput.append(newLine)
                    .append(NEW_LINE);
        } else {
            filteredOutput.append(line)
                    .append(NEW_LINE);
        }
    }

    public Set<String> getPropertyNames() {
        return propertyNames;
    }

    // For unit testing purposes
    void setValueConverters(Map<String, PropertyValueConverter> valueConverters) {
        PropertyConverter.valueConverters = valueConverters;
    }
}
