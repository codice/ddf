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

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/** This class provides matchers to help validating exception causes. */
public class ThrowableMatchers {
  private ThrowableMatchers() {}

  public static Matcher hasSuppressedMatching(Matcher<? extends Throwable[]> matcher) {
    return new TypeSafeMatcher<Throwable>() {
      @Override
      protected boolean matchesSafely(Throwable item) {
        return matcher.matches(item.getSuppressed());
      }

      @Override
      protected void describeMismatchSafely(Throwable item, Description description) {
        description.appendValue(item).appendText(" suppressed exceptions ");
        matcher.describeMismatch(item.getSuppressed(), description);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("suppressed exceptions ");
        matcher.describeTo(description);
      }
    };
  }

  public static Matcher hasCauseMatching(Matcher<? extends Throwable> matcher) {
    return new TypeSafeMatcher<Throwable>() {
      @Override
      protected boolean matchesSafely(Throwable item) {
        return matcher.matches(item.getCause());
      }

      @Override
      protected void describeMismatchSafely(Throwable item, Description description) {
        description.appendValue(item).appendText(" cause exception ");
        matcher.describeMismatch(item.getCause(), description);
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("cause exception ");
        matcher.describeTo(description);
      }
    };
  }

  public static Matcher hasCauseMessageMatching(Matcher<String> matcher) {
    return new TypeSafeMatcher<Throwable>() {
      @Override
      protected boolean matchesSafely(Throwable item) {
        final Throwable cause = item.getCause();

        if (cause == null) {
          return false;
        }
        return matcher.matches(cause.getMessage());
      }

      @Override
      protected void describeMismatchSafely(Throwable item, Description description) {
        final Throwable cause = item.getCause();

        if (cause == null) {
          description.appendValue(item).appendText(" had no cause");
        } else {
          description.appendValue(item).appendText(" cause message ");
          matcher.describeMismatch(cause.getMessage(), description);
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("cause message ");
        matcher.describeTo(description);
      }
    };
  }

  public static Matcher hasInitialCauseMatching(Matcher<? extends Throwable> matcher) {
    return new TypeSafeMatcher<Throwable>() {
      @Override
      protected boolean matchesSafely(Throwable item) {
        Throwable cause = item;

        while (cause.getCause() != null) {
          cause = cause.getCause();
        }
        return matcher.matches(cause);
      }

      @SuppressWarnings(
          "PMD.CompareObjectsWithEquals" /* purposely testing for identity and not equality */)
      @Override
      protected void describeMismatchSafely(Throwable item, Description description) {
        Throwable cause = item;

        while (cause.getCause() != null) {
          cause = cause.getCause();
        }
        if (cause == item) { // testing identity!!!
          description.appendValue(item).appendText(" had no initial cause exception");
        } else {
          description.appendValue(item).appendText(" ").appendText(" initial cause exception ");
          matcher.describeMismatch(cause, description);
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("initial cause exception ");
        matcher.describeTo(description);
      }
    };
  }

  public static Matcher hasInitialCauseMessageMatching(Matcher<String> matcher) {
    return new TypeSafeMatcher<Throwable>() {
      @SuppressWarnings(
          "PMD.CompareObjectsWithEquals" /* purposely testing for identity and not equality */)
      @Override
      protected boolean matchesSafely(Throwable item) {
        Throwable cause = item;

        while (cause.getCause() != null) {
          cause = cause.getCause();
        }
        if (cause == item) { // testing identity!!!
          return false;
        }
        return matcher.matches(cause.getMessage());
      }

      @SuppressWarnings(
          "PMD.CompareObjectsWithEquals" /* purposely testing for identity and not equality */)
      @Override
      protected void describeMismatchSafely(Throwable item, Description description) {
        Throwable cause = item;

        while (cause.getCause() != null) {
          cause = cause.getCause();
        }
        if (cause == item) { // testing identity!!!
          description.appendText(" had no initial cause exception");
        } else {
          description.appendValue(item).appendText(" initial cause message ");
          matcher.describeMismatch(cause.getMessage(), description);
        }
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("initial cause message ");
        matcher.describeTo(description);
      }
    };
  }
}
