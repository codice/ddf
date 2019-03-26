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
package ddf.catalog.filter.delegate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import ddf.catalog.data.types.Core;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;
import org.opengis.filter.Filter;

public class TagsFilterDelegateTest {

  private FilterBuilder builder = new GeotoolsFilterBuilder();

  private FilterAdapter adapter = new GeotoolsFilterAdapterImpl();

  @Test
  public void testNoTags() throws Exception {
    Filter filter = builder.attribute("attribute1").is().like().text("value1");
    assertThat(adapter.adapt(filter, new TagsFilterDelegate()), is(false));
  }

  @Test
  public void testTagsBasic() throws Exception {
    Filter filter = builder.attribute(Core.METACARD_TAGS).is().like().text("value1");
    assertThat(adapter.adapt(filter, new TagsFilterDelegate()), is(true));
  }

  @Test
  public void testTagsInvalidOr() throws Exception {
    Filter filter1 = builder.attribute("attribute1").is().like().text("value1");
    Filter filter2 = builder.attribute(Core.METACARD_TAGS).is().like().text("value2");
    Filter filter = builder.anyOf(filter1, filter2);
    assertThat(adapter.adapt(filter, new TagsFilterDelegate()), is(false));
  }

  @Test
  public void testTagsOr() throws Exception {
    Filter filter1 = builder.attribute("attribute1").is().like().text("value1");
    Filter filter2 = builder.attribute(Core.METACARD_TAGS).is().like().text("value2");
    Filter filter3 = builder.attribute(Core.METACARD_TAGS).is().like().text("value3");
    Filter filter = builder.anyOf(filter2, builder.allOf(filter1, filter3));
    assertThat(adapter.adapt(filter, new TagsFilterDelegate()), is(true));
  }

  @Test
  public void testCollectionTypeAnd() throws Exception {
    Filter filter1 = builder.attribute("attribute1").is().like().text("value1");
    Filter filter2 = builder.attribute(Core.METACARD_TAGS).is().like().text("value2");
    Filter filter = builder.allOf(filter1, filter2);
    assertThat(adapter.adapt(filter, new TagsFilterDelegate()), is(true));
  }

  @Test
  public void testTagsWithParam() throws Exception {
    Filter filter = builder.attribute(Core.METACARD_TAGS).is().like().text("value1");
    assertThat(adapter.adapt(filter, new TagsFilterDelegate("value1")), is(true));
    assertThat(adapter.adapt(filter, new TagsFilterDelegate("value2")), is(false));
    assertThat(
        adapter.adapt(
            filter, new TagsFilterDelegate(new HashSet<>(Arrays.asList("value2", "value1")))),
        is(true));
  }

  @Test
  public void testTagsNot() throws Exception {
    Filter filter = builder.not(builder.attribute(Core.METACARD_TAGS).is().like().text("value1"));
    assertThat(adapter.adapt(filter, new TagsFilterDelegate()), is(true));
    assertThat(adapter.adapt(filter, new TagsFilterDelegate("value1")), is(false));
  }

  @Test
  public void testTagsNotWithoutTag() throws Exception {
    Filter filter = builder.not(builder.attribute(Core.TITLE).is().like().text("value1"));
    assertThat(adapter.adapt(filter, new TagsFilterDelegate()), is(false));
  }
}
