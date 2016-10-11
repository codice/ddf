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
package org.codice.ddf.catalog.plugin.metacard;

import java.io.Serializable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a rule-based check on a metacard given a map of possible criteria to check against.
 */
public class MetacardCondition {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardCondition.class);

    private String criteriaKey;

    private String expectedValue;

    public String getCriteriaKey() {
        return criteriaKey;
    }

    public void setCriteriaKey(String criteriaKey) {
        this.criteriaKey = criteriaKey.trim();
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public void setExpectedValue(String expectedValue) {
        this.expectedValue = expectedValue.trim();
    }

    public boolean applies(Map<String, Serializable> criteria) {
        if (!criteria.containsKey(criteriaKey)) {
            LOGGER.debug("No such criteria exists for key = {}", criteriaKey);
            return false;
        }

        if (!expectedValue.equals(criteria.get(criteriaKey))) {
            LOGGER.debug("Adjustment condition failed; expected {} of {} but was {}",
                    criteriaKey,
                    expectedValue,
                    criteria.get(criteriaKey));
            return false;
        }

        return true;
    }
}
