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
package org.codice.ddf.configuration.store.felix;

import java.util.Map;
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

    private static final String PROPERTY_TYPE_REGEX = "\\s*([A-Za-z])?\\s*";

    private static final String PROPERTY_VALUE_REGEX = "\\s*(.*)";

    private static final Pattern PROPERTY_PATTERN = Pattern.compile(
            String.format("^%s=%s%s", PROPERTY_NAME_REGEX, PROPERTY_TYPE_REGEX,
                    PROPERTY_VALUE_REGEX));

    private static final Map<String, PropertyValueConverter> VALUE_CONVERTERS = ImmutableMap
            .of("f", new FloatValueConverter(), "d", new DoubleValueConverter());

    private final StringBuilder filteredOutput;

    public PropertyConverter(StringBuilder filteredOutput) {
        this.filteredOutput = filteredOutput;
    }

    @Override
    public void accept(String line) {
        Matcher matcher = PROPERTY_PATTERN.matcher(line);

        if (matcher.matches()) {
            LOGGER.debug("Line {} matched regex", line);

            StringBuilder newLine = new StringBuilder();

            String propertyName = matcher.group(1);
            String propertyType = matcher.group(2);
            String propertyValue = matcher.group(3);

            newLine.append(propertyName).append('=');

            if (propertyType != null) {
                newLine.append(propertyType);
            }

            if (propertyType != null && VALUE_CONVERTERS.containsKey(propertyType.toLowerCase())) {
                VALUE_CONVERTERS.get(propertyType.toLowerCase()).convert(propertyValue, newLine);
            } else {
                LOGGER.debug("Property value {} for line {} doesn't need conversion", propertyValue,
                        line);
                newLine.append(propertyValue);
            }

            filteredOutput.append(newLine).append('\n');
        } else {
            filteredOutput.append(line).append('\n');
        }
    }
}
