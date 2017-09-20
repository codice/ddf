/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.plugin.metacard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.codice.ddf.catalog.plugin.metacard.util.KeyValueParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents an immutable, rule-based check on a metacard given a map of possible criteria to check
 * against. The class must remain immutable to account for the case where an admin submits
 * configuration changes during live ingest traffic; there must exist a deterministic boundary where
 * the rule (and all its fields) synchronously swap to the new configuration without corrupting the
 * ingest traffic (i.e. attempts to test a scheme criteria for an IP address value, which will
 * produce undefined behavior, resulting with incorrect attribute markings).
 */
public class MetacardCondition {
  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardCondition.class);

  private String criteriaKey;

  private String expectedValue;

  private List<String> newAttributes;

  private Map<String, String> parsedAttributes;

  public MetacardCondition(final String criteriaKey, final String expectedValue) {
    this(criteriaKey, expectedValue, new ArrayList<>(), new KeyValueParser());
  }

  public MetacardCondition(
      final String criteriaKey,
      final String expectedValue,
      final List<String> newAttributes,
      final KeyValueParser parser) {
    this.criteriaKey = criteriaKey.trim();
    this.expectedValue = expectedValue.trim();
    this.newAttributes = newAttributes;
    this.parsedAttributes = parser.parsePairsToMap(this.newAttributes);
  }

  public String getCriteriaKey() {
    return criteriaKey;
  }

  public String getExpectedValue() {
    return expectedValue;
  }

  public List<String> getNewAttributes() {
    return newAttributes;
  }

  public Map<String, String> getParsedAttributes() {
    return parsedAttributes;
  }

  public boolean applies(Map<String, Serializable> criteria) {
    if (!criteria.containsKey(criteriaKey)) {
      LOGGER.debug("No such criteria exists for key = {}", criteriaKey);
      return false;
    }

    if (!expectedValue.equals(criteria.get(criteriaKey))) {
      LOGGER.debug(
          "Adjustment condition failed; expected {} of {} but was {}",
          criteriaKey,
          expectedValue,
          criteria.get(criteriaKey));
      return false;
    }

    return true;
  }
}
