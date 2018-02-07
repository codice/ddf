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

import org.apache.commons.lang.ClassUtils;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/** This class provides matchers to help cast objects before matching them. */
public class CastingMatchers {
  private CastingMatchers() {}

  public static <T, S extends T> Matcher<T> cast(Class<S> clazz, Matcher<? extends S> matcher) {
    return new TypeSafeMatcher<T>() {
      private final Class<?> matchableClass = ClassUtils.primitiveToWrapper(clazz);

      @Override
      protected boolean matchesSafely(T item) {
        if (!matchableClass.isInstance(item)) {
          return false;
        }
        return matcher.matches(clazz.cast(item));
      }

      @Override
      protected void describeMismatchSafely(T item, Description description) {
        if (!matchableClass.isInstance(item)) {
          description.appendValue(item).appendText(" is a ").appendText(item.getClass().getName());
        } else {
          description.appendText("as an instance of ").appendText(clazz.getName()).appendText(" ");
          matcher.describeMismatch(item, description);
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("as an instance of ").appendText(clazz.getName()).appendText(" ");
        matcher.describeTo(description);
      }
    };
  }
}
