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
package org.codice.ddf.configuration.persistence.felix;

/**
 * Class that converts a human-readable float value (e.g., 10.5) into the format required by Felix
 * configuration files (IEEE 754).
 */
class FloatValueConverter extends PropertyValueConverter {

    @Override
    public void convertSingleValue(String propertyValue, StringBuilder output) {
        try {
            float value = Float.parseFloat(propertyValue);
            Integer intBits = Float.floatToIntBits(value);
            output.append(intBits.toString());
            LOGGER.debug("Converted float value {} to {}", propertyValue, intBits);
        } catch (NumberFormatException e) {
            output.append(propertyValue);
            LOGGER.warn("Float value conversion failed for {}, leaving as-is", propertyValue, e);
        }
    }
}
