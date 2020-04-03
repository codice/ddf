/*
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

package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.mappings.SourceIdFilterVisitor;
import org.geotools.filter.FilterFactoryImpl;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

public class SourceIdFilterVisitorTest {
  private static final List<String> SOURCE_IDS =
      Arrays.asList("mySource", "anotherSource", "source with space");

  private SourceIdFilterVisitor sourceIdFilter;

  private FilterBuilder filterBuilder;

  private Filter filterWithoutSourceIds;

  @Before
  public void setUp() {
    sourceIdFilter = new SourceIdFilterVisitor();
    filterBuilder = new GeotoolsFilterBuilder();
    filterWithoutSourceIds = buildFilter(Collections.emptyList());
  }

  @Test
  public void testFilterWithoutSourceIdsIsUnchanged() {
    Filter result = (Filter) filterWithoutSourceIds.accept(sourceIdFilter, new FilterFactoryImpl());
    assertThat(result, equalTo(filterWithoutSourceIds));
    assertThat(sourceIdFilter.getSourceIds(), equalTo(Collections.emptyList()));
  }

  @Test
  public void testVisitorContainsSourceIds() {
    Filter filter = buildFilter(SOURCE_IDS);
    filter.accept(sourceIdFilter, new FilterFactoryImpl());

    List<String> result = sourceIdFilter.getSourceIds();
    assertThat(result, containsInAnyOrder(SOURCE_IDS.toArray()));
  }

  @Test
  public void testSourceIdsAreRemovedFromFilter() {
    Filter filter = buildFilter(SOURCE_IDS);
    Filter result = (Filter) filter.accept(sourceIdFilter, new FilterFactoryImpl());

    SourceIdFilterVisitor visitor = new SourceIdFilterVisitor();
    result.accept(visitor, new FilterFactoryImpl());
    assertThat(visitor.getSourceIds(), equalTo(Collections.emptyList()));
  }

  private Filter buildFilter(List<String> sourceIds) {
    Filter equalToFilter = filterBuilder.attribute("equalToFilter").is().equalTo().text("value");

    Filter isLikeFilter = filterBuilder.attribute("isLikeFilter").is().like().text("value");

    List<Filter> filters =
        sourceIds.stream()
            .map(id -> filterBuilder.attribute(Core.SOURCE_ID).is().equalTo().text(id))
            .collect(Collectors.toList());

    filters.add(equalToFilter);
    filters.add(isLikeFilter);

    return filterBuilder.allOf(filters);
  }
}
