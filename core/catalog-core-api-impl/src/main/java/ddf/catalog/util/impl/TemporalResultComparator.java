/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.util.impl;

import java.util.Comparator;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;

/**
 * Comparator for the temporal attribute of 2 {@link Result} objects.
 * 
 */
public class TemporalResultComparator implements Comparator<Result> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TemporalResultComparator.class);

    private SortOrder sortOrder = SortOrder.DESCENDING;

    private String temporalAttribute = Metacard.EFFECTIVE;

    /**
     * Constructs the comparator with the specified sort order, either temporal ascending or
     * temporal descending.
     * 
     * @param sortOrder
     *            the temporal sort order, if null is passed in, then the default SortOrder applies,
     *            which is SortOrder.DESCENDING.
     */
    public TemporalResultComparator(SortOrder sortOrder) {
        if (sortOrder != null) {
            this.sortOrder = sortOrder;
        }
    }

    /**
     * Constructs the comparator with the specified sort order, either temporal ascending or
     * temporal descending and the Attribute Name to sort on.
     * 
     * @param sortOrder
     *            the temporal sort order, if null is passed in, then the default SortOrder applies,
     *            which is SortOrder.DESCENDING.
     * @param temporalAttribute
     *            the name of the attribute to sort on. Default is Metacard.EFFECTIVE
     */
    public TemporalResultComparator(SortOrder sortOrder, String temporalAttribute) {
        this(sortOrder);
        if (StringUtils.isNotBlank(temporalAttribute)) {
            this.temporalAttribute = temporalAttribute;
        }
        LOGGER.debug("Comparing on Temporal Attribute: '{}' with Sort Order: '{}'",
                this.temporalAttribute, this.sortOrder);
    }

    /**
     * Compares the effective date between the two results.
     * 
     * @return 1 if A is null and B is non-null -1 if A is non-null and B is null 0 if both A and B
     *         are null 1 if temporal ascending and A > B; -1 if temporal ascending and B > A -1 if
     *         temporal descending and A > B; 1 if temporal descending and B > A
     */
    @Override
    public int compare(Result contentA, Result contentB) {

        Date dateA = null;
        Date dateB = null;

        // Extract the temporal attribute from the Result objects passed in.
        // If either the result object is null or its contained metacard is null or is not a Date,
        // then catch the NPE and set the Result's temporal attribute to null and proceed with
        // the comparison.
        try {
            dateA = (Date) contentA.getMetacard().getAttribute(temporalAttribute).getValue();
        } catch (NullPointerException npe) {
            dateA = null;
        } catch (ClassCastException e) {
            dateA = null;
        }
        try {
            dateB = (Date) contentB.getMetacard().getAttribute(temporalAttribute).getValue();
        } catch (NullPointerException npe) {
            dateB = null;
        } catch (ClassCastException e) {
            dateB = null;
        }

        if (dateA == null && dateB != null) {
            LOGGER.debug("dateA is null and dateB is not null: " + dateB);
            return 1;
        } else if (dateA != null && dateB == null) {
            LOGGER.debug("dateA is not null: " + dateA + " and dateB is null");
            return -1;
        } else if (dateA == null && dateB == null) {
            LOGGER.debug("both are null");
            return 0;
        }
        if (SortOrder.ASCENDING.equals(sortOrder)) {
            return dateA.compareTo(dateB);
        } else if (SortOrder.DESCENDING.equals(sortOrder)) {
            return dateB.compareTo(dateA);
        } else {
            LOGGER.warn("Unknown order type. Returning 0.");
            return 0;
        }

    }
}
