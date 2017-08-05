/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.test.util;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * This class provides matchers to help validating exception causes.
 */
public class ThrowableMatchers {
    public static Matcher hasSuppressedMatching(Matcher<? extends Throwable[]> matcher) {
        return new TypeSafeMatcher<Throwable>() {
            @Override
            protected boolean matchesSafely(Throwable item) {
                return matcher.matches(item.getSuppressed());
            }

            @Override
            protected void describeMismatchSafely(Throwable item, Description description) {
                description.appendText("suppressed ");
                matcher.describeMismatch(item, description);
            }

            @Override
            public void describeTo(Description description) {
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
                description.appendText("cause ");
                matcher.describeMismatch(item, description);
            }

            @Override
            public void describeTo(Description description) {
                matcher.describeTo(description);
            }
        };
    }

    public static Matcher hasCauseMessageMatching(Matcher<String> matcher) {
        return new TypeSafeMatcher<Throwable>() {
            @Override
            protected boolean matchesSafely(Throwable item) {
                item = item.getCause();
                if (item == null) {
                    return false;
                }
                return matcher.matches(item.getMessage());
            }

            @Override
            protected void describeMismatchSafely(Throwable item, Description description) {
                item = item.getCause();
                if (item == null) {
                    description.appendText("had no cause");
                } else {
                    matcher.describeMismatch(item.getMessage(), description);
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
                while (item.getCause() != null) {
                    item = item.getCause();
                }
                return matcher.matches(item);
            }

            @Override
            protected void describeMismatchSafely(Throwable item, Description description) {
                while (item.getCause() != null) {
                    item = item.getCause();
                }
                matcher.describeMismatch(item, description);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("initial cause ");
                matcher.describeTo(description);
            }
        };
    }

    public static Matcher hasInitialCauseMessageMatching(Matcher<String> matcher) {
        return new TypeSafeMatcher<Throwable>() {
            @Override
            protected boolean matchesSafely(Throwable item) {
                while (item.getCause() != null) {
                    item = item.getCause();
                }
                if (item == null) {
                    return false;
                }
                return matcher.matches(item.getMessage());
            }

            @Override
            protected void describeMismatchSafely(Throwable item, Description description) {
                while (item.getCause() != null) {
                    item = item.getCause();
                }
                if (item == null) {
                    description.appendText("had no cause");
                } else {
                    matcher.describeMismatch(item.getMessage(), description);
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