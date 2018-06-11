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
package ddf.catalog.filter;

import org.opengis.filter.Filter;

/**
 * Continues the fluent API to create {@link Filter} based on a particular {@link
 * ddf.catalog.data.Attribute}
 *
 * @author Michael Menousek
 */
public interface ExpressionBuilder extends EqualityExpressionBuilder {

  /*
   * SCALAR
   */

  /**
   * Continue building a Filter with the "less than" operator ( {@link
   * org.opengis.filter.PropertyIsLessThan})
   *
   * @return {@link NumericalExpressionBuilder} to continue building the {@link Filter}
   */
  public NumericalExpressionBuilder lessThan();

  /**
   * Continue building a Filter with the "less than or equal to" operator ( {@link
   * org.opengis.filter.PropertyIsLessThanOrEqualTo})
   *
   * @return {@link NumericalExpressionBuilder} to continue building the {@link Filter}
   */
  public NumericalExpressionBuilder lessThanOrEqualTo();

  /**
   * Continue building a Filter with the "greater than" operator ( {@link
   * org.opengis.filter.PropertyIsGreaterThan})
   *
   * @return {@link NumericalExpressionBuilder} to continue building the {@link Filter}
   */
  public NumericalExpressionBuilder greaterThan();

  /**
   * Continue building a Filter with the "greater than or equal to" operator ( {@link
   * org.opengis.filter.PropertyIsGreaterThanOrEqualTo})
   *
   * @return {@link NumericalExpressionBuilder} to continue building the {@link Filter}
   */
  public NumericalExpressionBuilder greaterThanOrEqualTo();

  /**
   * Continue building a Filter with the "equal to" operator ( {@link
   * org.opengis.filter.PropertyIsEqualTo})
   *
   * @return {@link NumericalExpressionBuilder} to continue building the {@link Filter}
   */
  public EqualityExpressionBuilder equalTo();

  /*
   * CONTEXTUAL
   */

  /**
   * Continue building a Filter with the "like" operator ( {@link
   * org.opengis.filter.PropertyIsLike})
   *
   * @return {@link ContextualExpressionBuilder} to continue building the {@link Filter}
   */
  public ContextualExpressionBuilder like();

  /**
   * Continue building a Filter with the "between" operator ( {@link
   * org.opengis.filter.PropertyIsBetween})
   *
   * @return {@link NumericalRangeExpressionBuilder} to continue building the {@link Filter}
   */
  public NumericalRangeExpressionBuilder between();

  /**
   * Continue building a Filter with the "not equal" operator ( {@link
   * org.opengis.filter.PropertyIsNotEqualTo})
   *
   * @return {@link EqualityExpressionBuilder} to continue building the {@link Filter}
   */
  public EqualityExpressionBuilder notEqualTo();

  /**
   * Complete building a Filter with the "is null" operator ( {@link
   * org.opengis.filter.PropertyIsNull})
   *
   * @return {@link Filter}
   */
  public Filter empty();

  /**
   * Continue building a Filter with the "after" operator ( {@link
   * org.opengis.filter.temporal.After}) for a moment in time
   *
   * @return {@link TemporalInstantExpressionBuilder} to continue building the {@link Filter}
   */
  public TemporalInstantExpressionBuilder after();

  /**
   * Continue building a Filter with the "before" operator ( {@link
   * org.opengis.filter.temporal.Before}) for a moment in time
   *
   * @return {@link TemporalInstantExpressionBuilder} to continue building the {@link Filter}
   */
  public TemporalInstantExpressionBuilder before();

  /**
   * Continue building a Filter with the "during" operator ( {@link
   * org.opengis.filter.temporal.During}) for a timne range (inclusive)
   *
   * @return {@link TemporalRangeExpressionBuilder} to continue building the {@link Filter}
   */
  public TemporalRangeExpressionBuilder during();

  // TODO is this needed?

  /**
   * Continue building a Filter with the "overlaps" operator ( {@link
   * org.opengis.filter.temporal.OverlappedBy}) for a time range (inclusive)
   *
   * @return {@link TemporalRangeExpressionBuilder} to continue building the {@link Filter}
   */
  public TemporalRangeExpressionBuilder overlapping();

  /*
   * SPATIAL
   */

  /**
   * Continue building a Filter with the "intersects" operator ( {@link
   * org.opengis.filter.spatial.Intersects})
   *
   * @return {@link SpatialExpressionBuilder} to continue building the {@link Filter}
   */
  public SpatialExpressionBuilder intersecting();

  /**
   * Continue building a Filter with the "contains" operator ( {@link
   * org.opengis.filter.spatial.Contains})
   *
   * @return {@link SpatialExpressionBuilder} to continue building the {@link Filter}
   */
  public SpatialExpressionBuilder containing();

  /**
   * Continue building a Filter with the "beyond" operator ( {@link
   * org.opengis.filter.spatial.Beyond})
   *
   * @return {@link BufferedSpatialExpressionBuilder} to continue building the {@link Filter}
   */
  public BufferedSpatialExpressionBuilder beyond();

  /**
   * Continue building a Filter with the "within" operator ( {@link
   * org.opengis.filter.spatial.Within})
   *
   * @return {@link SpatialExpressionBuilder} to continue building the {@link Filter}
   */
  public SpatialExpressionBuilder within();

  /**
   * Continue building a Filter with the "within distance" operator ( {@link
   * org.opengis.filter.spatial.DWithin})
   *
   * @return {@link SpatialExpressionBuilder} to continue building the {@link Filter}
   */
  public BufferedSpatialExpressionBuilder withinBuffer();

  /**
   * Continue building a Filter with the "nearest to" operator
   *
   * @return {@link SpatialExpressionBuilder} to continue building the {@link Filter}
   */
  public SpatialExpressionBuilder nearestTo();
}
