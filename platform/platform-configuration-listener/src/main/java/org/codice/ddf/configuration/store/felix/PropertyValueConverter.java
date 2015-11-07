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

import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class used to convert a Felix property value into a different format.
 */
abstract class PropertyValueConverter {

    static final Logger LOGGER = LoggerFactory.getLogger(PropertyValueConverter.class);

    private static final String SINGLE_VALUE_REGEX = "\".*\"";

    private static final String EMPTY_ARRAY_REGEX = "\\[\\]";

    private static final String EMPTY_VECTOR_REGEX = "\\(\\)";

    private static final String ARRAY_REGEX = "\\[(\".*\")(,\".*\")*\\]";

    private static final String VECTOR_REGEX = "\\((\".*\")(,\".*\")*\\)";

    private static final Pattern PROPERTY_VALUE_PATTERN = Pattern.compile(
            String.format("%s|%s|%s|%s|%s", SINGLE_VALUE_REGEX, EMPTY_ARRAY_REGEX,
                    EMPTY_VECTOR_REGEX, ARRAY_REGEX, VECTOR_REGEX));

    public void convert(String propertyValue, StringBuilder output) {

        if (!PROPERTY_VALUE_PATTERN.matcher(propertyValue).matches()) {
            LOGGER.debug("Property value {} doesn't seem valid, skipping", propertyValue);
            output.append(propertyValue);
            return;
        }

        if (propertyValue.startsWith("\"")) {
            LOGGER.debug("Converting single value {}", propertyValue);
            output.append('"');
            convertSingleValue(propertyValue.trim().replaceAll("\"", ""), output);
            output.append('"');
        } else if (propertyValue.startsWith("[")) {
            LOGGER.debug("Converting values in array {}", propertyValue);
            processMultipleValues("[", propertyValue, "]", output);
        } else if (propertyValue.startsWith("(")) {
            LOGGER.debug("Converting values in vector {}", propertyValue);
            processMultipleValues("(", propertyValue, ")", output);
        } else {
            LOGGER.warn("Property value {} matched valid pattern but couldn't be processed. "
                    + "Leaving as-is.");
            output.append(propertyValue);
        }

        LOGGER.debug("Configuration property after value conversion: {}", output.toString());
    }

    protected abstract void convertSingleValue(String value, StringBuilder output);

    private void processMultipleValues(String openingBracket, String propertyValues,
            String closingBracket, StringBuilder output) {
        String propertyValuesWithoutBrackets = StringUtils
                .removeEnd(StringUtils.removeStart(propertyValues, openingBracket), closingBracket);
        output.append(openingBracket);

        for (String value : propertyValuesWithoutBrackets.split(",")) {
            output.append('"');
            convertSingleValue(value.trim().replaceAll("\"", ""), output);
            output.append("\",");
        }

        output.deleteCharAt(output.length() - 1);
        output.append(closingBracket);
    }
}
