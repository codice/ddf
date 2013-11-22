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

import org.apache.log4j.Logger;
import org.opengis.filter.sort.SortOrder;

import ddf.catalog.data.Result;

/**
 * Comparator for the relevance of 2 {@link Result} objects.
 * 
 * @deprecated Use ddf.catalog.util.impl.RelevanceResultComparator
 */
@Deprecated
public class RelevanceResultComparator implements Comparator<Result> {

    private static Logger logger = Logger.getLogger(RelevanceResultComparator.class);

    private SortOrder sortOrder;

    /**
     * Constructs the comparator with the specified sort order, either relevance ascending or
     * relevance descending.
     * 
     * @param relevanceOrder
     *            the relevance sort order
     */
    public RelevanceResultComparator(SortOrder relevanceOrder) {
        this.sortOrder = relevanceOrder;
    }

    /**
     * Compares the relevance between the two results.
     * 
     * @return 1 if A is null and B is non-null -1 if A is non-null and B is null 0 if both A and B
     *         are null 1 if ascending relevance and A > B; -1 if ascending relevance and B > A -1
     *         if descending relevance and A > B; 1 if descending relevance and B > A
     */
    @Override
    public int compare(Result contentA, Result contentB) {
        if (contentA != null && contentB != null) {

            Double relevanceScoreA = contentA.getRelevanceScore();
            Double relevanceScoreB = contentB.getRelevanceScore();

            if (relevanceScoreA == null && relevanceScoreB != null) {
                logger.debug("relevanceScoreA is null and relevanceScoreB is not null: "
                        + relevanceScoreB);
                return 1;
            } else if (relevanceScoreA != null && relevanceScoreB == null) {
                logger.debug("relevanceScoreA is not null: " + relevanceScoreA
                        + " and relevanceScoreB is null");
                return -1;
            } else if (relevanceScoreA == null && relevanceScoreB == null) {
                logger.debug("both are null");
                return 0;
            } else if (SortOrder.ASCENDING.equals(sortOrder)) {
                return relevanceScoreA.compareTo(relevanceScoreB);
            } else if (SortOrder.DESCENDING.equals(sortOrder)) {
                return relevanceScoreB.compareTo(relevanceScoreA);
            } else {
                logger.warn("Unknown order type. Returning 0.");
                return 0;
            }
        } else {
            logger.warn("Error comparing responses, at least one was null.  Returning -1: ");
            return -1;
        }
    }

}
