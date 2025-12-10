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
package ddf.catalog.util.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ResultImpl;
import java.util.Comparator;
import org.geotools.api.filter.sort.SortOrder;
import org.junit.Test;

public class CollectionResultComparatorTest {

  @Test
  public void testCollectionComparator() {
    ResultImpl result1 = new ResultImpl();
    result1.setDistanceInMeters(10.0);
    result1.setRelevanceScore(10.0);

    ResultImpl result2 = new ResultImpl();
    result2.setDistanceInMeters(5.0);
    result2.setRelevanceScore(10.0);

    Comparator<Result> relevanceComparator = new RelevanceResultComparator(SortOrder.DESCENDING);
    Comparator<Result> distanceComparator = new DistanceResultComparator(SortOrder.ASCENDING);
    CollectionResultComparator collectionResultComparator = new CollectionResultComparator();
    collectionResultComparator.addComparator(relevanceComparator);
    collectionResultComparator.addComparator(distanceComparator);

    int compareResult = collectionResultComparator.compare(result1, result2);
    assertThat(compareResult, is(1));
  }
}
