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
package ddf.catalog.util;

import java.util.Comparator;
import java.util.Date;

import org.apache.log4j.Logger;
import org.opengis.filter.sort.SortOrder;

import ddf.catalog.data.Result;

/**
 * Comparator for the effective date of 2 {@link Result} objects.
 * 
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.util.impl.TemporalResultComparator
 */
@Deprecated
public class TemporalResultComparator implements Comparator<Result> {

    private static Logger logger = Logger.getLogger(TemporalResultComparator.class);

    private SortOrder sortOrder = SortOrder.DESCENDING;

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
     * Compares the effective date between the two results.
     * 
     * @return 1 if A is null and B is non-null -1 if A is non-null and B is null 0 if both A and B
     *         are null 1 if temporal ascending and A > B; -1 if temporal ascending and B > A -1 if
     *         temporal descending and A > B; 1 if temporal descending and B > A
     */
    @Override
    public int compare(Result contentA, Result contentB) {

        Date effectiveDateA = null;
        Date effectiveDateB = null;

        // Extract the effective date from the Result objects passed in.
        // If either the result object is null or its contained metacard is null, then
        // catch the NPE and set the Result's effectiveDate to null and proceed with
        // the comparison.
        try {
            effectiveDateA = contentA.getMetacard().getEffectiveDate();
        } catch (NullPointerException npe) {
            effectiveDateA = null;
        }
        try {
            effectiveDateB = contentB.getMetacard().getEffectiveDate();
        } catch (NullPointerException npe) {
            effectiveDateB = null;
        }

        if (effectiveDateA == null && effectiveDateB != null) {
            logger.debug("effectiveDateA is null and effectiveDateB is not null: " + effectiveDateB);
            return 1;
        } else if (effectiveDateA != null && effectiveDateB == null) {
            logger.debug("effectiveDateA is not null: " + effectiveDateA
                    + " and effectiveDateB is null");
            return -1;
        } else if (effectiveDateA == null && effectiveDateB == null) {
            logger.debug("both are null");
            return 0;
        }
        if (SortOrder.ASCENDING.equals(sortOrder)) {
            return effectiveDateA.compareTo(effectiveDateB);
        } else if (SortOrder.DESCENDING.equals(sortOrder)) {
            return effectiveDateB.compareTo(effectiveDateA);
        } else {
            logger.warn("Unknown order type. Returning 0.");
            return 0;
        }

    }
}
