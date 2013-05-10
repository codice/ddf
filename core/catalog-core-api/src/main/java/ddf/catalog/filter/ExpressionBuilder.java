/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.filter;

import org.opengis.filter.Filter;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.PropertyIsGreaterThan;
import org.opengis.filter.PropertyIsGreaterThanOrEqualTo;
import org.opengis.filter.PropertyIsLessThan;
import org.opengis.filter.PropertyIsLessThanOrEqualTo;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.PropertyIsNotEqualTo;
import org.opengis.filter.PropertyIsNull;
import org.opengis.filter.spatial.Beyond;
import org.opengis.filter.spatial.Contains;
import org.opengis.filter.spatial.DWithin;
import org.opengis.filter.spatial.Intersects;
import org.opengis.filter.spatial.Within;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;
import org.opengis.filter.temporal.During;
import org.opengis.filter.temporal.OverlappedBy;

import ddf.catalog.data.Attribute;

/**
 * Continues the fluent API to create {@link Filter} based on a particular
 * {@link Attribute}
 * 
 * @author Michael Menousek, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface ExpressionBuilder extends EqualityExpressionBuilder {

    /*
     * SCALAR
     */

    /**
     * Continue building a Filter with the "less than" operator (
     * {@link PropertyIsLessThan})
     * 
     * @return {@link NumericalExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public NumericalExpressionBuilder lessThan();

    /**
     * Continue building a Filter with the "less than or equal to" operator (
     * {@link PropertyIsLessThanOrEqualTo})
     * 
     * @return {@link NumericalExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public NumericalExpressionBuilder lessThanOrEqualTo();

    /**
     * Continue building a Filter with the "greater than" operator (
     * {@link PropertyIsGreaterThan})
     * 
     * @return {@link NumericalExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public NumericalExpressionBuilder greaterThan();

    /**
     * Continue building a Filter with the "greater than or equal to" operator (
     * {@link PropertyIsGreaterThanOrEqualTo})
     * 
     * @return {@link NumericalExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public NumericalExpressionBuilder greaterThanOrEqualTo();

    /**
     * Continue building a Filter with the "equal to" operator (
     * {@link PropertyIsEqualTo})
     * 
     * @return {@link NumericalExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public EqualityExpressionBuilder equalTo();

    /*
     * CONTEXTUAL
     */

    /**
     * Continue building a Filter with the "like" operator (
     * {@link PropertyIsLike})
     * 
     * @return {@link ContextualExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public ContextualExpressionBuilder like();

    /**
     * Continue building a Filter with the "between" operator (
     * {@link PropertyIsBetween})
     * 
     * @return {@link NumericalRangeExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public NumericalRangeExpressionBuilder between();

    /**
     * Continue building a Filter with the "not equal" operator (
     * {@link PropertyIsNotEqualTo})
     * 
     * @return {@link EqualityExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public EqualityExpressionBuilder notEqualTo();

    /**
     * Complete building a Filter with the "is null" operator (
     * {@link PropertyIsNull})
     * 
     * @return {@link Filter}
     */
    public Filter empty();

    /**
     * Continue building a Filter with the "after" operator ( {@link After}) for
     * a moment in time
     * 
     * @return {@link TemporalInstantExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public TemporalInstantExpressionBuilder after();

    /**
     * Continue building a Filter with the "before" operator ( {@link Before})
     * for a moment in time
     * 
     * @return {@link TemporalInstantExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public TemporalInstantExpressionBuilder before();

    /**
     * Continue building a Filter with the "during" operator ( {@link During})
     * for a timne range (inclusive)
     * 
     * @return {@link TemporalRangeExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public TemporalRangeExpressionBuilder during();

    // TODO is this needed?
    /**
     * Continue building a Filter with the "overlaps" operator (
     * {@link OverlappedBy}) for a time range (inclusive)
     * 
     * @return {@link TemporalRangeExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public TemporalRangeExpressionBuilder overlapping();

    /*
     * SPATIAL
     */

    /**
     * Continue building a Filter with the "intersects" operator (
     * {@link Intersects})
     * 
     * @return {@link SpatialExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public SpatialExpressionBuilder intersecting();

    /**
     * Continue building a Filter with the "contains" operator (
     * {@link Contains})
     * 
     * @return {@link SpatialExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public SpatialExpressionBuilder containing();

    /**
     * Continue building a Filter with the "beyond" operator ( {@link Beyond})
     * 
     * @return {@link BufferedSpatialExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public BufferedSpatialExpressionBuilder beyond();

    /**
     * Continue building a Filter with the "within" operator ( {@link Within})
     * 
     * @return {@link SpatialExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public SpatialExpressionBuilder within();

    /**
     * Continue building a Filter with the "within distance" operator (
     * {@link DWithin})
     * 
     * @return {@link SpatialExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public BufferedSpatialExpressionBuilder withinBuffer();

    /**
     * Continue building a Filter with the "nearest to" operator
     * 
     * @return {@link SpatialExpressionBuilder} to continue building the
     *         {@link Filter}
     */
    public SpatialExpressionBuilder nearestTo();

}