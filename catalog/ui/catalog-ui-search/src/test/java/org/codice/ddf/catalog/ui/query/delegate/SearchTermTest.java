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
package org.codice.ddf.catalog.ui.query.delegate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

public class SearchTermTest {
  @Test
  public void exactMatch() {
    String inputTerm = "abc";
    SearchTerm searchTerm = new SearchTerm(inputTerm);
    assertThat(searchTerm.getTerm(), is(equalTo(inputTerm)));
    assertThat(searchTerm.getCqlTerm(), is(equalTo(inputTerm)));
    assertThat(searchTerm.match("abc"), is(true));
    assertThat(searchTerm.match("XabcX"), is(false));
  }

  @Test
  public void lowCaseMatch() {
    String inputTerm = "ABC";
    SearchTerm searchTerm = new SearchTerm(inputTerm);
    assertThat(searchTerm.getTerm(), is(equalTo(inputTerm.toLowerCase())));
    assertThat(searchTerm.getCqlTerm(), is(equalTo(inputTerm)));
    assertThat(searchTerm.match("abc"), is(true));
    assertThat(searchTerm.match("XabcX"), is(false));
  }

  @Test
  public void wildcardSwitch() {
    String inputTerm = "a*B";
    SearchTerm searchTerm = new SearchTerm(inputTerm);
    assertThat(searchTerm.getTerm(), is(equalTo(inputTerm.toLowerCase())));
    assertThat(searchTerm.getCqlTerm(), is(equalTo("a%B")));
    assertThat(searchTerm.match("aXXXXb"), is(true));
    assertThat(searchTerm.match("xaXXXB"), is(false));
  }

  /**
   * While there is no absolute guarantee that this test will fail on any given machine, during
   * development and testing, the search against this input value of 1000 'a' with a terminal '!'
   * ran for well over a minute before being manually stopped. If performance improves to the point
   * where this test fails, we can bump up the search string by another order of magnitude.
   *
   * <p>This test will also fail if a future version of Java were to introduce an improved,
   * linear-time regex implementation (at which time we could yank this time-limited band-aid);
   * however, that's not on the table for J11 so we should expect this to be a necessary workaround
   * for the foreseeable future.
   */
  @Test
  public void questionableMatch() {
    String inputTerm = "*(a+)+";
    SearchTerm searchTerm = new SearchTerm(inputTerm);
    assertThat(searchTerm.match(StringUtils.repeat("a", 1000) + "!"), is(false));
  }
}
