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
package org.codice.ddf.confluence.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Topic;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.opengis.filter.Filter;

public class ConfluenceFilterDelegateTest {
  private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm";

  private static final String UNKNOWN_CONFLUENCE_ATTRIBUTE = "Unknown Confluence Attribute Mapping";

  private static final String UNKNOWN_ATTRIBUTES_VALUE = "unknown attribute's value";

  private FilterBuilder builder = new GeotoolsFilterBuilder();

  private FilterAdapter adapter = new GeotoolsFilterAdapterImpl();

  @Test
  public void testAttributeAnd() throws Exception {
    Filter filter =
        builder.allOf(
            builder.attribute("title").is().equalTo().text("val1"),
            builder.attribute("id").is().equalTo().text("val2"));
    assertThat(
        adapter.adapt(filter, new ConfluenceFilterDelegate()),
        is("( title = \"val1\" AND id = \"val2\" )"));
  }

  @Test
  public void testInvalidAttributeAnd() throws Exception {
    Filter filter =
        builder.allOf(
            builder.attribute("title").is().equalTo().text("val1"),
            builder.attribute("invalid").is().equalTo().text("val2"));
    assertThat(adapter.adapt(filter, new ConfluenceFilterDelegate()), is("( title = \"val1\" )"));
  }

  @Test
  public void testTextAnd() throws Exception {
    Filter filter =
        builder.allOf(
            builder.attribute("anyText").is().like().text("val1"),
            builder.attribute("anyText").is().like().text("val2"));
    assertThat(
        adapter.adapt(filter, new ConfluenceFilterDelegate()),
        is("( ( text ~ \"val1\" ) AND ( text ~ \"val2\" ) )"));
  }

  @Test
  public void testAttributeOr() throws Exception {
    Filter filter =
        builder.anyOf(
            builder.attribute("contact.creator-name").is().equalTo().text("val1"),
            builder.attribute("contact.contributor-name").is().equalTo().text("val2"));
    assertThat(
        adapter.adapt(filter, new ConfluenceFilterDelegate()),
        is("( creator = \"val1\" OR contributor = \"val2\" )"));
  }

  @Test
  public void testInvalidAttributeOr() throws Exception {
    Filter filter =
        builder.anyOf(
            builder.attribute("contact.creator-name").is().equalTo().text("val1"),
            builder.attribute("invalid").is().equalTo().text("val2"));
    assertThat(adapter.adapt(filter, new ConfluenceFilterDelegate()), is("( creator = \"val1\" )"));
  }

  @Test
  public void testTextOr() throws Exception {
    Filter filter =
        builder.anyOf(
            builder.attribute("anyText").is().like().text("val1"),
            builder.attribute("title").is().like().text("val2"));
    assertThat(
        adapter.adapt(filter, new ConfluenceFilterDelegate()),
        is("( ( text ~ \"val1\" ) OR ( title ~ \"val2\" ) )"));
  }

  @Test
  public void testNot() throws Exception {
    Filter filter = builder.not(builder.attribute("title").is().equalTo().text("val1"));
    assertThat(adapter.adapt(filter, new ConfluenceFilterDelegate()), is("NOT title = \"val1\""));
  }

  @Test
  public void testMultiWordLike() throws Exception {
    Filter filter = builder.attribute("anyText").is().like().text("multi word test");
    assertThat(
        adapter.adapt(filter, new ConfluenceFilterDelegate()),
        is("( text ~ \"multi\" OR text ~ \"word\" OR text ~ \"test\" )"));
  }

  @Test
  public void testAfter() throws Exception {
    Filter filter =
        builder
            .attribute("created")
            .after()
            .date(new SimpleDateFormat(DATE_FORMAT).parse("2015-12-31 17:00"));
    assertThat(
        adapter.adapt(filter, new ConfluenceFilterDelegate()),
        is("created > \"2015-12-31 17:00\""));
  }

  @Test
  public void testBefore() throws Exception {
    Filter filter =
        builder
            .attribute("modified")
            .before()
            .date(new SimpleDateFormat(DATE_FORMAT).parse("2015-12-31 17:00"));
    assertThat(
        adapter.adapt(filter, new ConfluenceFilterDelegate()),
        is("lastmodified < \"2015-12-31 17:00\""));
  }

  @Test
  public void testAfterInvalidAttribute() throws Exception {
    Filter filter = builder.attribute("effective").after().date(new Date(10000));
    assertThat(adapter.adapt(filter, new ConfluenceFilterDelegate()), is(nullValue()));
  }

  @Test
  public void testBeforeInvalidAttribute() throws Exception {
    Filter filter = builder.attribute("effective").before().date(new Date(10000));
    assertThat(adapter.adapt(filter, new ConfluenceFilterDelegate()), is(nullValue()));
  }

  @Test
  public void testAfterInvalidDateAttribute() throws Exception {
    Filter filter = builder.attribute("id").after().date(new Date(10000));
    assertThat(adapter.adapt(filter, new ConfluenceFilterDelegate()), is(nullValue()));
  }

  @Test
  public void testBeforeInvalidDateAttribute() throws Exception {
    Filter filter = builder.attribute("id").before().date(new Date(10000));
    assertThat(adapter.adapt(filter, new ConfluenceFilterDelegate()), is(nullValue()));
  }

  @Test
  public void testDuring() throws Exception {
    Filter filter =
        builder
            .attribute("modified")
            .dateRange(
                new SimpleDateFormat(DATE_FORMAT).parse("2015-12-31 17:00"),
                new SimpleDateFormat(DATE_FORMAT).parse("2015-12-31 18:00"));
    assertThat(
        adapter.adapt(filter, new ConfluenceFilterDelegate()),
        is("lastmodified > \"2015-12-31 17:00\" AND lastmodified < \"2015-12-31 18:00\""));
  }

  @Test
  public void testDuringInvalidAttribute() throws Exception {
    Filter filter = builder.attribute("effective").dateRange(new Date(10000), new Date(10000000));
    assertThat(adapter.adapt(filter, new ConfluenceFilterDelegate()), is(nullValue()));
  }

  @Test
  public void testQueryCaptures() throws Exception {
    Filter filter =
        builder.allOf(
            builder.attribute("anyText").is().like().text("val1"),
            builder.attribute(Core.METACARD_TAGS).is().like().text("confluence"));
    ConfluenceFilterDelegate delegate = new ConfluenceFilterDelegate();
    adapter.adapt(filter, delegate);
    assertThat(delegate.isConfluenceQuery(), is(true));

    filter =
        builder.allOf(
            builder.attribute("anyText").is().like().text("val1"),
            builder.attribute(Core.METACARD_TAGS).is().like().text("resource"));
    delegate = new ConfluenceFilterDelegate();
    adapter.adapt(filter, delegate);
    assertThat(delegate.isConfluenceQuery(), is(true));

    filter = builder.attribute("anyText").is().like().text("val1");
    delegate = new ConfluenceFilterDelegate();
    adapter.adapt(filter, delegate);
    assertThat(delegate.isConfluenceQuery(), is(true));

    filter = builder.attribute("anyText").is().like().text("*");
    delegate = new ConfluenceFilterDelegate();
    adapter.adapt(filter, delegate);
    assertThat(delegate.isWildCardQuery(), is(true));

    filter =
        builder.allOf(
            builder.attribute("anyText").is().like().text("*"),
            builder.attribute(Core.TITLE).is().like().text("confluence"));
    delegate = new ConfluenceFilterDelegate();
    adapter.adapt(filter, delegate);
    assertThat(delegate.isWildCardQuery(), is(false));
  }

  @Test
  public void testLikeTransform() throws Exception {
    Filter filter = builder.allOf(builder.attribute(Topic.KEYWORD).is().like().text("val1"));
    ConfluenceFilterDelegate delegate = new ConfluenceFilterDelegate();
    assertThat(adapter.adapt(filter, delegate), is("( label = \"val1\" )"));
  }

  @Test
  public void testWildCardNotAllowed() throws Exception {
    Filter filter = builder.allOf(builder.attribute(Core.ID).is().like().text("val*"));
    ConfluenceFilterDelegate delegate = new ConfluenceFilterDelegate();
    assertThat(StringUtils.isEmpty(adapter.adapt(filter, delegate)), is(true));
  }

  @Test
  public void testWildCardAllowed() throws Exception {
    Filter filter = builder.allOf(builder.attribute(Core.TITLE).is().equalTo().text("val*"));
    ConfluenceFilterDelegate delegate = new ConfluenceFilterDelegate();
    assertThat(adapter.adapt(filter, delegate), is("title = \"val*\""));
  }

  @Test
  public void testUnsupportedQuery() throws Exception {
    Filter filter = builder.allOf(builder.attribute(Core.TITLE).is().greaterThan().number(0));
    ConfluenceFilterDelegate delegate = new ConfluenceFilterDelegate();
    adapter.adapt(filter, delegate);
    assertThat(delegate.isConfluenceQuery(), is(false));
  }

  @Test
  public void testPropertyGreaterThanDate() throws Exception {
    Filter filter =
        builder.allOf(
            builder
                .attribute(Core.MODIFIED)
                .is()
                .after()
                .date(new SimpleDateFormat(DATE_FORMAT).parse("2015-12-31 17:00")));
    ConfluenceFilterDelegate delegate = new ConfluenceFilterDelegate();
    assertThat(adapter.adapt(filter, delegate), is("lastmodified > \"2015-12-31 17:00\""));
  }

  @Test
  public void testAndWithBlankOperand() throws Exception {
    Filter filter =
        builder.allOf(
            builder.attribute(Core.TITLE).is().like().text("titleWithNoSpaces"),
            builder.attribute(Topic.KEYWORD).is().like().text("  "));
    ConfluenceFilterDelegate delegate = new ConfluenceFilterDelegate();
    assertThat(
        "No ANDs should be dangling with following blanks",
        adapter.adapt(filter, delegate),
        is("( ( title ~ \"titleWithNoSpaces\" ) )"));
  }

  @Test
  public void testOrWithBlankOperand() throws Exception {
    Filter filter =
        builder.anyOf(
            builder.attribute(Core.TITLE).is().like().text("with blanks"),
            builder.attribute(Topic.KEYWORD).is().like().text("  "));
    ConfluenceFilterDelegate delegate = new ConfluenceFilterDelegate();
    assertThat(
        "No ORs should be dangling with following blanks",
        adapter.adapt(filter, delegate),
        is("( ( title ~ \"with\" OR title ~ \"blanks\" ) )"));
  }

  @Test
  public void testNotWithUnknownConfluenceParameter() throws Exception {
    Filter filter =
        builder.allOf(
            builder.not(
                builder
                    .attribute(UNKNOWN_CONFLUENCE_ATTRIBUTE)
                    .is()
                    .like()
                    .text(UNKNOWN_ATTRIBUTES_VALUE)));
    ConfluenceFilterDelegate delegate = new ConfluenceFilterDelegate();
    assertThat(
        "Filter delegate NOT should return null", adapter.adapt(filter, delegate), nullValue());
  }
}
