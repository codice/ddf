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
package ddf.catalog.operation.faceting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class FacetedFieldResult {

    private String fieldName;

    private List<FacetValueCount> facetValues;

    /**
     * Instantiates a FacetedFieldResult with a representing a portion of the results of a faceted
     * query. A FacetedFieldResult is representative of a single attribute's faceting results,
     * and zero to many FacetedFieldResults may make up a complete faceted query result.
     * This constructor zips together the fieldValues and valueCounts provided, and these list
     * should correspond and be of the same length if sane results are desired.
     *
     * @param fieldName The field name for which faceting data is reported
     * @param fieldValues A list of the discovered facet values
     * @param valueCounts A list of the number of occurrences for each facet value
     */
    public FacetedFieldResult(String fieldName, List<String> fieldValues, List<Long> valueCounts) {
        this.fieldName = fieldName;
        facetValues = new ArrayList<>();

        Iterator<String> valueItr = fieldValues.iterator();
        Iterator<Long> countItr = valueCounts.iterator();

        while (valueItr.hasNext() && countItr.hasNext()) {
            facetValues.add(new FacetValueCount(valueItr.next(), countItr.next()));
        }

    }

    public String getFieldName() {
        return fieldName;
    }

    public List<FacetValueCount> getFacetValues() {
        return facetValues;
    }
}
