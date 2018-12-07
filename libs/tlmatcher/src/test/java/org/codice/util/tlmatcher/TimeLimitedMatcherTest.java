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
package org.codice.util.tlmatcher;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.codice.util.tlmatcher.TimeLimitedMatcher.TimeoutException;
import org.junit.Before;
import org.junit.Test;

public class TimeLimitedMatcherTest {
  private Pattern pattern;

  @Before
  public void setup() {
    pattern = Pattern.compile("a(b[c|d])\\d*;");
  }

  @Test
  public void simpleMatchFindsResult() throws Exception {
    TimeLimitedMatcher matcher = TimeLimitedMatcher.create(pattern, "abd123;");
    assertThat(matcher.matches(), is(equalTo(true)));
    assertThat(matcher.group(0), is(equalTo("abd123;")));
    assertThat(matcher.group(1), is(equalTo("bd")));
  }

  @Test
  public void simpleMatchFindsNoResult() throws Exception {
    TimeLimitedMatcher matcher = TimeLimitedMatcher.create(pattern, "abE123;");
    assertThat(matcher.matches(), is(equalTo(false)));
  }

  @Test(expected = TimeoutException.class)
  public void questionableMatchFails() throws Exception {
    TimeLimitedMatcher.create(Pattern.compile("(a+)+"), StringUtils.repeat("a", 1000000) + "!");
  }
}
