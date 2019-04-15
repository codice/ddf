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
package org.codice.ddf.catalog.ui.security.accesscontrol;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import java.util.Date;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

/**
 * Validates the guarantee made by the {@link TagAggregationVisitor}'s javadoc.
 *
 * <p>The tests are organized into several sections:
 *
 * <ul>
 *   <li>A Singleton Filters section, which represent the most simplistic cases.
 *   <li>Mirroring sections for single AND predicates and single OR predicates.
 *   <li>Mirroring sections for composed AND/OR predicates and OR/AND predicates.
 * </ul>
 *
 * Mirroring sections define the same set of tests in the same order, despite some coverage
 * redundancy, so it is very clear how the class must behave for any input {@link Filter}. This also
 * makes the expected results of said tests easily comparable so it is clear how the logical
 * operators, and similar combinations of the operators, impact filter introspection.
 */
public class TagAggregationVisitorTest {
  private static final String ANY_TEXT = "anytext";

  private static final Date NOW = new Date();

  private static final String TAG_A = "A";

  private static final String TAG_B = "B";

  private static final String TAG_C = "C";

  private static final FilterBuilder FILTER_BUILDER = new GeotoolsFilterBuilder();

  private TagAggregationVisitor tagAggregationVisitor;

  @Before
  public void setup() {
    tagAggregationVisitor = new TagAggregationVisitor();
  }

  /*
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * Singleton Filters (Only one attribute or key-value pair in the predicate)
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   */

  @Test
  public void testSingletonFilter() {
    Filter filter = FILTER_BUILDER.attribute(ANY_TEXT).is().like().text("*");
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSingletonFilterNegated() {
    Filter filter = FILTER_BUILDER.not(FILTER_BUILDER.attribute(ANY_TEXT).is().like().text("*"));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSingletonFilterPropertyNonStringForEquals() {
    // DDF's filter builder doesn't support complex property expressions; need to use Geotools
    FilterFactory ff = CommonFactoryFinder.getFilterFactory();
    Filter filter = ff.equal(ff.add(ff.literal(1), ff.literal(2)), ff.literal(1), false);
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSingletonFilterPropertyNonStringForLike() {
    // DDF's filter builder doesn't support complex property expressions; need to use Geotools
    FilterFactory ff = CommonFactoryFinder.getFilterFactory();
    Filter filter = ff.like(ff.add(ff.literal(1), ff.literal(2)), "pattern*");
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSingletonFilterValueNonString() {
    Filter filter = FILTER_BUILDER.attribute(ANY_TEXT).is().equalTo().number(0);
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSingletonFilterWithTags() {
    Filter filter = FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A);
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(1));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_A));
  }

  @Test
  public void testSingletonFilterWithBlankTags() {
    Filter filter = FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text("");
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSingletonFilterWithTagsNegated() {
    Filter filter =
        FILTER_BUILDER.not(FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  /*
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * Simple AND Filters
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   */

  @Test
  public void testSimpleAndFilter() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.attribute(Core.CREATED).is().before().date(NOW));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleAndFilterWithTags() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(2));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_A, TAG_B));
  }

  @Test
  public void testSimpleAndFilterWithTagAndNotTag() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().notEqualTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(1));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_A));
  }

  @Test
  public void testSimpleAndFilterWithTagsNegateOtherCriteria() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.not(FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*")),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(2));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_A, TAG_B));
  }

  @Test
  public void testSimpleAndFilterWithTagsNegateTagA() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.not(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A)),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(1));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_B));
  }

  @Test
  public void testSimpleAndFilterWithTagsNegateTagB() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.not(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(1));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_A));
  }

  @Test
  public void testSimpleAndFilterWithTagsNegatedEntirely() {
    Filter filter =
        FILTER_BUILDER.not(
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleAndFilterWithOnlyTags() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(2));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_A, TAG_B));
  }

  @Test
  public void testSimpleAndFilterWithOnlyNotTags() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().notEqualTo().text(TAG_A),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().notEqualTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleAndFilterWithOnlyTagsNegateTagA() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.not(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A)),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(1));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_B));
  }

  @Test
  public void testSimpleAndFilterWithOnlyTagsNegateTagB() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.not(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(1));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_A));
  }

  @Test
  public void testSimpleAndFilterWithOnlyTagsNegatedEntirely() {
    Filter filter =
        FILTER_BUILDER.not(
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  /*
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * Simple OR Filters
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   */

  @Test
  public void testSimpleOrFilter() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.attribute(Core.CREATED).is().before().date(NOW));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleOrFilterWithTags() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleOrFilterWithTagAndNotTag() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().notEqualTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleOrFilterWithTagsNegateOtherCriteria() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.not(FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*")),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleOrFilterWithTagsNegateTagA() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.not(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A)),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleOrFilterWithTagsNegateTagB() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.not(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleOrFilterWithTagsNegatedEntirely() {
    Filter filter =
        FILTER_BUILDER.not(
            FILTER_BUILDER.anyOf(
                FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleOrFilterWithOnlyTags() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(2));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_A, TAG_B));
  }

  @Test
  public void testSimpleOrFilterWithOnlyNotTags() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().notEqualTo().text(TAG_A),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().notEqualTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleOrFilterWithOnlyTagsNegateTagA() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.not(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A)),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleOrFilterWithOnlyTagsNegateTagB() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.not(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testSimpleOrFilterWithOnlyTagsNegatedEntirely() {
    Filter filter =
        FILTER_BUILDER.not(
            FILTER_BUILDER.anyOf(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  /*
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * OR/AND Cases (An OR predicate with an AND child)
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   */

  @Test
  public void testNestedOrAndFilterWithTags() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.CREATED).is().before().date(NOW),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testNestedOrAndFilterWithOnlyTags() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testNestedOrAndFilterWithOnlyTagsAndTagsElsewhere() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_C),
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testNestedOrAndFilterWithOnlyTagsAndTagsElsewhereNegatedPartially() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_C),
            FILTER_BUILDER.not(
                FILTER_BUILDER.allOf(
                    FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                    FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B))));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testNestedOrAndFilterWithOnlyTagsAndTagsElsewhereNegatedEntirely() {
    Filter filter =
        FILTER_BUILDER.not(
            FILTER_BUILDER.anyOf(
                FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_C),
                FILTER_BUILDER.allOf(
                    FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                    FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B))));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testNestedOrAndFilterWithNoTagsAndOnlyTagsElsewhere() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_B),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_C),
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.DATATYPE).is().like().text("media"),
                FILTER_BUILDER.attribute(Core.LANGUAGE).is().equalTo().text("eng")));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testNestedOrAndFilterWithNoTagsAndOnlyTagsElsewhereNegatedPartially() {
    Filter filter =
        FILTER_BUILDER.anyOf(
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_B),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_C),
            FILTER_BUILDER.not(
                FILTER_BUILDER.allOf(
                    FILTER_BUILDER.attribute(Core.DATATYPE).is().like().text("media"),
                    FILTER_BUILDER.attribute(Core.LANGUAGE).is().equalTo().text("eng"))));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testNestedOrAndFilterWithNoTagsAndOnlyTagsElsewhereNegatedEntirely() {
    Filter filter =
        FILTER_BUILDER.not(
            FILTER_BUILDER.anyOf(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_B),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_C),
                FILTER_BUILDER.allOf(
                    FILTER_BUILDER.attribute(Core.DATATYPE).is().like().text("media"),
                    FILTER_BUILDER.attribute(Core.LANGUAGE).is().equalTo().text("eng"))));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  /*
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   * AND/OR Cases (An AND predicate with an OR child)
   * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
   */

  @Test
  public void testNestedAndOrFilterWithTags() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.anyOf(
                FILTER_BUILDER.attribute(Core.CREATED).is().before().date(NOW),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testNestedAndOrFilterWithOnlyTags() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.anyOf(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(2));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_A, TAG_B));
  }

  @Test
  public void testNestedAndOrFilterWithOnlyTagsAndTagsElsewhere() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_C),
            FILTER_BUILDER.anyOf(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B)));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(3));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_A, TAG_B, TAG_C));
  }

  @Test
  public void testNestedAndOrFilterWithOnlyTagsAndTagsElsewhereNegatedPartially() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_C),
            FILTER_BUILDER.not(
                FILTER_BUILDER.anyOf(
                    FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                    FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B))));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(1));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_C));
  }

  @Test
  public void testNestedAndOrFilterWithOnlyTagsAndTagsElsewhereNegatedEntirely() {
    Filter filter =
        FILTER_BUILDER.not(
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.TITLE).is().like().text("*term*"),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_C),
                FILTER_BUILDER.anyOf(
                    FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                    FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B))));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }

  @Test
  public void testNestedAndOrFilterWithNoTagsAndOnlyTagsElsewhere() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B),
            FILTER_BUILDER.anyOf(
                FILTER_BUILDER.attribute(Core.DATATYPE).is().like().text("media"),
                FILTER_BUILDER.attribute(Core.LANGUAGE).is().equalTo().text("eng")));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(2));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_A, TAG_B));
  }

  @Test
  public void testNestedAndOrFilterWithNoTagsAndOnlyTagsElsewhereNegatedPartially() {
    Filter filter =
        FILTER_BUILDER.allOf(
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
            FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B),
            FILTER_BUILDER.not(
                FILTER_BUILDER.anyOf(
                    FILTER_BUILDER.attribute(Core.DATATYPE).is().like().text("media"),
                    FILTER_BUILDER.attribute(Core.LANGUAGE).is().equalTo().text("eng"))));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), hasSize(2));
    assertThat(tagAggregationVisitor.getTags(), hasItems(TAG_A, TAG_B));
  }

  @Test
  public void testNestedAndOrFilterWithNoTagsAndOnlyTagsElsewhereNegatedEntirely() {
    Filter filter =
        FILTER_BUILDER.not(
            FILTER_BUILDER.allOf(
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().like().text(TAG_A),
                FILTER_BUILDER.attribute(Core.METACARD_TAGS).is().equalTo().text(TAG_B),
                FILTER_BUILDER.anyOf(
                    FILTER_BUILDER.attribute(Core.DATATYPE).is().like().text("media"),
                    FILTER_BUILDER.attribute(Core.LANGUAGE).is().equalTo().text("eng"))));
    filter.accept(tagAggregationVisitor, null);
    assertThat(tagAggregationVisitor.getTags(), is(empty()));
  }
}
