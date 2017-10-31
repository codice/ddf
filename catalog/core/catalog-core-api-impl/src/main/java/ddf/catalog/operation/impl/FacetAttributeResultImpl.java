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
package ddf.catalog.operation.impl;

import ddf.catalog.operation.FacetAttributeResult;
import ddf.catalog.operation.FacetValueCount;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacetAttributeResultImpl implements FacetAttributeResult {

  private static final Logger LOGGER = LoggerFactory.getLogger(FacetAttributeResultImpl.class);

  private String attributeName;

  private List<FacetValueCount> facetValues;

  /**
   * Instantiates a FacetAttributeResultImpl representing a portion of the results of a faceted
   * query. A FacetAttributeResultImpl is representative of a single attribute's faceting results,
   * and zero to many {@link FacetAttributeResult}s may make up a complete faceted query result.
   * This constructor zips together the attributeValues and valueCounts provided, and these list
   * should correspond and be of the same length if sane results are desired.
   *
   * @param attributeName The attribute name for which faceting data is reported
   * @param attributeValues A list of the discovered facet values
   * @param valueCounts A list of the number of occurrences for each facet value
   */
  public FacetAttributeResultImpl(
      String attributeName, List<String> attributeValues, List<Long> valueCounts) {
    this.attributeName = attributeName;
    facetValues = new ArrayList<>();

    if (attributeValues.size() != valueCounts.size()) {
      LOGGER.debug(
          "Creating result with unmatched field values or counts. Values: {}, Counts: {}",
          attributeValues.size(),
          valueCounts.size());
    }

    Iterator<String> valueItr = attributeValues.iterator();
    Iterator<Long> countItr = valueCounts.iterator();

    while (valueItr.hasNext() && countItr.hasNext()) {
      facetValues.add(new FacetValueCountImpl(valueItr.next(), countItr.next()));
    }
  }

  /**
   * Instantiates a FacetAttributeResultImpl representing a portion of the results of a faceted
   * query. This constructor takes a zipped list of value to count pairings.
   *
   * @param attributeName The field name for which faceting data is reported
   * @param valueCountPairs A list of value-count pairs for the faceted field
   */
  public FacetAttributeResultImpl(String attributeName, List<Pair<String, Long>> valueCountPairs) {
    this.attributeName = attributeName;
    facetValues = new ArrayList<>();
    for (Pair<String, Long> valueCountPair : valueCountPairs) {
      facetValues.add(new FacetValueCountImpl(valueCountPair.getLeft(), valueCountPair.getRight()));
    }
  }

  @Override
  public String getAttributeName() {
    return attributeName;
  }

  @Override
  public List<FacetValueCount> getFacetValues() {
    return new ArrayList<>(facetValues);
  }
}
