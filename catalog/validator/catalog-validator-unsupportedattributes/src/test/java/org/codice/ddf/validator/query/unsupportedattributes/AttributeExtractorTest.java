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
package org.codice.ddf.validator.query.unsupportedattributes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.ImmutableSet;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.source.UnsupportedQueryException;
import java.util.Date;
import java.util.Set;
import org.junit.Test;
import org.opengis.filter.Filter;

public class AttributeExtractorTest {

  private FilterAdapter filterAdapter = new GeotoolsFilterAdapterImpl();

  private FilterBuilder filterBuilder = new GeotoolsFilterBuilder();

  private AttributeExtractor attributeExtractor = new AttributeExtractor(filterAdapter);

  @Test
  public void testExtractAttributesComparisonFilter() throws UnsupportedQueryException {
    Filter filter = filterBuilder.attribute("attr").is().text("text");
    Set<String> attributes = attributeExtractor.extractAttributes(filter);

    assertThat(attributes, is(ImmutableSet.of("attr")));
  }

  @Test
  public void testExtractAttributesTemporalFilter() throws UnsupportedQueryException {
    Filter filter = filterBuilder.attribute("attr").before().date(new Date(1562112000L));
    Set<String> attributes = attributeExtractor.extractAttributes(filter);

    assertThat(attributes, is(ImmutableSet.of("attr")));
  }

  @Test
  public void testExtractAttributesSpatialFilter() throws UnsupportedQueryException {
    Filter filter =
        filterBuilder.attribute("attr").within().wkt("POLYGON((51 11,42 0,41 10,51 11))");
    Set<String> attributes = attributeExtractor.extractAttributes(filter);

    assertThat(attributes, is(ImmutableSet.of("attr")));
  }

  @Test
  public void testExtractAttributesAndFilter() throws UnsupportedQueryException {
    Filter filter =
        filterBuilder.allOf(
            filterBuilder.attribute("attr-1").is().text("text-1"),
            filterBuilder.attribute("attr-2").is().text("text-2"));
    Set<String> attributes = attributeExtractor.extractAttributes(filter);

    assertThat(attributes, is(ImmutableSet.of("attr-1", "attr-2")));
  }

  @Test
  public void testExtractAttributesOrFilter() throws UnsupportedQueryException {
    Filter filter =
        filterBuilder.anyOf(
            filterBuilder.attribute("attr-1").is().text("text-1"),
            filterBuilder.attribute("attr-2").is().text("text-2"));
    Set<String> attributes = attributeExtractor.extractAttributes(filter);

    assertThat(attributes, is(ImmutableSet.of("attr-1", "attr-2")));
  }

  @Test
  public void testExtractAttributesNotFilter() throws UnsupportedQueryException {
    Filter filter = filterBuilder.not(filterBuilder.attribute("attr").is().text("text"));
    Set<String> attributes = attributeExtractor.extractAttributes(filter);

    assertThat(attributes, is(ImmutableSet.of("attr")));
  }
}
