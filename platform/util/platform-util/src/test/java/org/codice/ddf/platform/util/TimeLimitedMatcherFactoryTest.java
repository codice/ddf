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
package org.codice.ddf.platform.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.util.TimeLimitedMatcherFactory.TimeoutException;
import org.junit.Before;
import org.junit.Test;

public class TimeLimitedMatcherFactoryTest {

  private Pattern pattern;

  @Before
  public void setup() {
    pattern = Pattern.compile("a(b[c|d])\\d*;");
  }

  @Test
  public void simpleMatchFindsResult() {
    Matcher matcher = TimeLimitedMatcherFactory.matcher(pattern, "abd123;");
    assertThat(matcher.matches(), is(equalTo(true)));
    assertThat(matcher.group(0), is(equalTo("abd123;")));
    assertThat(matcher.group(1), is(equalTo("bd")));
  }

  @Test
  public void simpleMatchFindsNoResult() {
    Matcher matcher = TimeLimitedMatcherFactory.matcher(pattern, "abE123;");
    assertThat(matcher.matches(), is(equalTo(false)));
  }

  @Test(expected = TimeoutException.class)
  public void questionableMatchFails() {
    Matcher matcher =
        TimeLimitedMatcherFactory.matcher(
            Pattern.compile("(a+)+"), StringUtils.repeat("a", 1000) + "!");
    matcher.matches();
  }
}
