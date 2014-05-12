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

import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Result;

/**
 * Comparator for the distance (in meters) of 2 {@link Result} objects.
 * 
 * @deprecated As of release 2.3.0, replaced by
 *             ddf.catalog.util.impl.DistanceResultComparator
 */
@Deprecated
public class DistanceResultComparator implements Comparator<Result> {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistanceResultComparator.class);

    private SortOrder distanceOrder;

    /**
     * Constructs the comparator with the specified sort order, either distance ascending or
     * distance descending.
     * 
     * @param distanceOrder
     *            the distance sort order
     */
    public DistanceResultComparator(SortOrder distanceOrder) {
        this.distanceOrder = distanceOrder;
    }

    /**
     * Compares the distance (in meters) between the two results.
     * 
     * @return 1 if A is null and B is non-null -1 if A is non-null and B is null 0 if both A and B
     *         are null 1 if ascending sort order and A > B; -1 if ascending sort order and B > A -1
     *         if descending sort order and A > B; 1 if descending sort order and B > A
     */
    @Override
    public int compare(Result contentA, Result contentB) {
        if (contentA != null && contentB != null) {

            Double distanceA = contentA.getDistanceInMeters();
            Double distanceB = contentB.getDistanceInMeters();

            if (distanceA == null && distanceB != null) {
                LOGGER.debug("distanceA is null and distanceB is not null: {}", distanceB);
                return 1;
            } else if (distanceA != null && distanceB == null) {
                LOGGER.debug("distanceA is not null: {} and distanceB is null", distanceA);
                return -1;
            } else if (distanceA == null && distanceB == null) {
                LOGGER.debug("both are null");
                return 0;
            } else if (SortOrder.ASCENDING.equals(distanceOrder)) {
                LOGGER.debug("Ascending sort");
                return distanceA.compareTo(distanceB);
            } else if (SortOrder.DESCENDING.equals(distanceOrder)) {
                LOGGER.debug("Descending sort");
                return distanceB.compareTo(distanceA);
            } else {
                LOGGER.warn("Unknown order type. Returning 0.");
                return 0;
            }

        } else {
            LOGGER.warn("Error comparing results, at least one was null.  Returning -1: ");
            return -1;
        }
    }

}
