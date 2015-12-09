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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base class used to convert a Felix property value into a different format.
 */
abstract class PropertyValueConverter {

    static final Logger LOGGER = LoggerFactory.getLogger(PropertyValueConverter.class);

    private static final String SINGLE_VALUE_REGEX = "(\"[^\"]*\")";

    private static final String ARRAY_REGEX = "\\[\\s*(\"[^\"]*\")(\\s*,\\s*\"[^\"]*\")*\\s*\\]";

    private static final String VECTOR_REGEX = "\\(\\s*(\"[^\"]*\")(\\s*,\\s*\"[^\"]*\")*\\s*\\)";
    
    private static final Pattern PROPERTY_VALUE_PATTERN = Pattern
            .compile(String.format("%s|%s|%s", SINGLE_VALUE_REGEX, ARRAY_REGEX, VECTOR_REGEX));

    /**
     * Converts a property value (which can be a single value or an array or list of values) to a
     * different format. Sub-classes are responsible for the conversion of individual values by
     * implementing the {@link #convertSingleValue(String, StringBuilder)}.
     * <p/>
     * Note that values will be converted only if their format follows the Felix format for values
     * which can be "value", ["value1", "value2", ...] or ("value1", "value2", ...). Any value
     * that doesn't match that format will just be output as-is and not converted.
     *
     * @param propertyValue property value to convert. Assumes that the value provided has been
     *                      trimmed, starts with a quote ("), square bracket ([) or parenthesis
     *                      and is not {@code null}.
     * @param output        {@link StringBuilder} to append the value to. Assumes that this is
     *                      never {@code null}.
     */
    public void convert(String propertyValue, StringBuilder output) {

        Matcher matcher = PROPERTY_VALUE_PATTERN.matcher(propertyValue);

        if (!matcher.matches()) {
            LOGGER.debug("Property value {} doesn't seem valid, skipping", propertyValue);
            output.append(propertyValue);
            return;
        }

        if (propertyValue.startsWith("\"")) {
            LOGGER.debug("Converting single value {}", propertyValue);
            output.append('"');
            convertSingleValue(propertyValue.trim().replaceAll("\"", ""), output);
            output.append('"');
        } else if (propertyValue.startsWith("[") && propertyValue.endsWith("]")) {
            LOGGER.debug("Converting values in array {}", propertyValue);
            processMultipleValues("[", propertyValue, "]", output);
        } else if (propertyValue.startsWith("(") && propertyValue.endsWith(")")) {
            LOGGER.debug("Converting values in vector {}", propertyValue);
            processMultipleValues("(", propertyValue, ")", output);
        }

        LOGGER.debug("Configuration property after value conversion: {}", output.toString());
    }

    /**
     * Converts a single property value to a new format if needed.  If the conversion
     * fails or the value cannot or does not need to be converted, the original value
     * should be appended back. Unless part of the conversion logic, the returned
     * value should not be quoted.
     *
     * @param value  value to convert. The value will never have any leading or trailing white
     *               spaces or quotes (") and will never be {@code null}.
     * @param output {@link StringBuilder} to use to output the converted value. Will never be
     *               {@code null}.
     */
    protected abstract void convertSingleValue(String value, StringBuilder output);

    private void processMultipleValues(String openingBracket, String propertyValues,
            String closingBracket, StringBuilder output) {
        output.append(openingBracket);

        String propertyValuesWithoutBrackets = removeSurroundingCharacters(propertyValues,
                openingBracket, closingBracket);

        for (String value : propertyValuesWithoutBrackets.split("\"\\s*,\\s*\"")) {
            String trimmedValue = value.trim();
            String unquotedValue = removeSurroundingCharacters(trimmedValue, "\"", "\"");

            output.append('"');
            convertSingleValue(unquotedValue, output);
            output.append("\",");
        }

        output.deleteCharAt(output.length() - 1);
        output.append(closingBracket);
    }

    private String removeSurroundingCharacters(String value, String left, String right) {
        String cleanedUpValue = value.trim();

        if (cleanedUpValue.startsWith(left)) {
            cleanedUpValue = cleanedUpValue.substring(1);
        }

        if (cleanedUpValue.endsWith(right)) {
            cleanedUpValue = cleanedUpValue.substring(0, cleanedUpValue.length() - 1);
        }

        return cleanedUpValue;
    }
}
