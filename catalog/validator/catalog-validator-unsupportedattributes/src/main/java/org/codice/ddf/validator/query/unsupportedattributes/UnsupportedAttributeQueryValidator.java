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
package org.codice.ddf.validator.query.unsupportedattributes;

import com.google.common.collect.ImmutableMap;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.source.SourceAttributeRestriction;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.validation.QueryValidator;
import ddf.catalog.validation.impl.violation.QueryValidationViolationImpl;
import ddf.catalog.validation.violation.QueryValidationViolation;
import ddf.catalog.validation.violation.QueryValidationViolation.Severity;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnsupportedAttributeQueryValidator implements QueryValidator {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(UnsupportedAttributeQueryValidator.class);

  private AttributeExtractor attributeExtractor;

  private Map<String, Set<String>> sourceIdToSupportedAttributesMap = new ConcurrentHashMap<>();

  public UnsupportedAttributeQueryValidator(AttributeExtractor attributeExtractor) {
    this.attributeExtractor = attributeExtractor;
  }

  @Override
  public String getValidatorId() {
    return "unsupportedAttribute";
  }

  @Override
  public Set<QueryValidationViolation> validate(QueryRequest request) {
    Set<String> queryAttributes = getAttributes(request);
    Set<String> sourceIds =
        request.getSourceIds() != null ? request.getSourceIds() : Collections.emptySet();

    return queryAttributes
        .stream()
        .map(attr -> getViolation(attr, sourceIds))
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.toSet());
  }

  private Set<String> getAttributes(QueryRequest request) {
    try {
      return attributeExtractor.extractAttributes(request.getQuery());
    } catch (UnsupportedQueryException e) {
      LOGGER.debug(
          "Skipping validation: Failed to aggregate all attributes from query {}",
          request.getQuery(),
          e);
      return Collections.emptySet();
    }
  }

  private Optional<QueryValidationViolation> getViolation(String attribute, Set<String> sourceIds) {
    Set<String> sourcesThatDontSupportAttribute = new HashSet<>();
    for (String sourceId : sourceIds) {
      if (!sourceIdToSupportedAttributesMap.containsKey(sourceId)) {
        LOGGER.debug("Source {} supports querying by all attributes", sourceId);
        continue;
      }
      Set<String> supportedAttributes = sourceIdToSupportedAttributesMap.get(sourceId);
      if (!supportedAttributes.contains(attribute)) {
        LOGGER.debug(
            "Source \"{}\" does not support querying by attribute {}", sourceId, attribute);
        sourcesThatDontSupportAttribute.add(sourceId);
      } else {
        LOGGER.debug("Source \"{}\" supports querying by attribute {}", sourceId, attribute);
      }
    }

    if (sourcesThatDontSupportAttribute.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(
        new QueryValidationViolationImpl(
            Severity.ERROR,
            createMessage(attribute, sourcesThatDontSupportAttribute),
            createExtras(attribute, sourcesThatDontSupportAttribute)));
  }

  private Map<String, Object> createExtras(String invalidAttribute, Set<String> sourceIds) {
    return ImmutableMap.of("attribute", invalidAttribute, "sources", sourceIds);
  }

  private String createMessage(String invalidAttribute, Set<String> sourceIds) {
    return String.format(
        "The field \"%s\" is not supported by the %s Content Store(s)",
        invalidAttribute, prettyPrintSources(sourceIds));
  }

  private String prettyPrintSources(Set<String> sourceIds) {
    List<String> sourceIdsList = sourceIds.stream().sorted().collect(Collectors.toList());
    if (sourceIds.size() == 1) {
      return sourceIdsList.get(0);
    } else if (sourceIds.size() == 2) {
      return String.format("%s and %s", sourceIdsList.get(0), sourceIdsList.get(1));
    } else {
      String allSourcesExceptLast =
          String.join(", ", sourceIdsList.subList(0, sourceIdsList.size() - 1));
      String lastSource = sourceIdsList.get(sourceIdsList.size() - 1);
      return String.format("%s, and %s", allSourcesExceptLast, lastSource);
    }
  }

  /**
   * Adds a new {@code SourceAttributeRestriction} to the {@code sourceIdToSupportedAttributesMap}
   * map. Called by blueprint when a new {@code SourceAttributeRestriction} is registered as a
   * service.
   *
   * @param sourceAttributeRestriction the new {@code SourceAttributeRestriction} to be registered.
   */
  public void bind(SourceAttributeRestriction sourceAttributeRestriction) {
    if (sourceAttributeRestriction != null) {
      String sourceId = sourceAttributeRestriction.getSource().getId();
      LOGGER.trace("Binding new SourceAttributeRestriction instance with id {}", sourceId);
      sourceIdToSupportedAttributesMap.put(
          sourceId, sourceAttributeRestriction.getSupportedAttributes());
    }
  }

  /**
   * Removes an existing {@code SourceAttributeRestriction} from the {@code
   * sourceIdToSupportedAttributesMap} map. Called by blueprint when an existing {@code
   * SourceAttributeRestriction} service is removed.
   *
   * @param sourceAttributeRestriction the {@code SourceAttributeRestriction} to be removed from the
   *     collection.
   */
  public void unbind(SourceAttributeRestriction sourceAttributeRestriction) {
    if (sourceAttributeRestriction != null) {
      String sourceId = sourceAttributeRestriction.getSource().getId();
      LOGGER.trace("Unbinding SourceAttributeRestriction instance with id {}", sourceId);
      sourceIdToSupportedAttributesMap.remove(sourceId);
    }
  }
}
