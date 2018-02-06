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
package org.codice.ddf.test.common.matchers;

import java.util.function.Function;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/** This class provides matchers to help mapping objects to values before matching the value. */
public class MappingMatchers {
  private MappingMatchers() {}

  public static <T, V> Matcher<T> map(Function<T, V> mapper, Matcher<? extends V> matcher) {
    return new TypeSafeMatcher<T>() {
      @Override
      protected boolean matchesSafely(T item) {
        return matcher.matches(mapper.apply(item));
      }

      @Override
      protected void describeMismatchSafely(T item, Description description) {
        description.appendValue(item).appendText(" mapped ");
        matcher.describeMismatch(mapper.apply(item), description);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("mapped ");
        matcher.describeTo(description);
      }
    };
  }
}
