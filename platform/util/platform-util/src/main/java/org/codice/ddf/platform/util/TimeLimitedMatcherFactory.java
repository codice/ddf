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

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Factory class used to generate time-limited Matcher instances.
 *
 * <p>If the regex runs for too long, this matcher will throw a RuntimeException - specifically a
 * {@link TimeoutException} - that will throw if in use after their timeout. It is incumbent upon
 * the caller so handle the runtime exception.
 *
 * <p>Example usage:
 *
 * <pre>
 *     try {
 *       Matcher matcher =
 *           TimeLimitedMatcherFactory.matcher(
 *               Pattern.compile(".*(this string).*"), "search this string for 'this string'");
 *       if (matcher.matches()) {
 *         // do something with the match
 *       }
 *     } catch (TimeLimitedMatcherFactory.TimeoutException e) {
 *       // do something
 *     }
 * </pre>
 *
 * <p>Inspired by <a
 * href="https://www.exratione.com/2017/06/preventing-unbounded-regular-expression-operations-in-java/">
 * this post at ExRatione</a>
 */
public class TimeLimitedMatcherFactory {
  // Limit regular expression matching to five seconds. Any longer indicates an expression that is
  // likely to spin indefinitely.
  private static final Duration RE_DURATION = Duration.ofSeconds(5L);

  private TimeLimitedMatcherFactory() {
    // Prevent construction of factory
  }

  /**
   * Generate a Matcher instance that will timeout after more than {@link #RE_DURATION} seconds
   * after its instantiation.
   *
   * <p>Use the instance immediately and then discard it.
   *
   * @param pattern The Pattern instance.
   * @param charSequence The CharSequence to operate on.
   */
  public static Matcher matcher(Pattern pattern, CharSequence charSequence) {
    return matcher(pattern, charSequence, RE_DURATION);
  }

  /**
   * Generate a Matcher instance that will timeout after more than {@code duration} after its
   * instantiation.
   *
   * <p>Use the instance immediately and then discard it.
   *
   * @param pattern The Pattern instance.
   * @param charSequence The CharSequence to operate on.
   * @param duration Throw after this timeout is reached.
   */
  public static Matcher matcher(Pattern pattern, CharSequence charSequence, Duration duration) {

    // Substitute in our exploding CharSequence implementation.
    if (!(charSequence instanceof TimeLimitedCharSequence)) {

      charSequence =
          new TimeLimitedCharSequence(
              charSequence,
              System.currentTimeMillis() + duration.toMillis(),
              pattern,
              charSequence);
    }

    return pattern.matcher(charSequence);
  }

  /**
   * A CharSequence implementation that will throw a RuntimeException - specifically a {@link
   * TimeoutException} - when charAt() is called after a given timeout.
   *
   * <p>Since charAt() is invoked frequently in regular expression operations on a string, this
   * gives a way to abort long-running regular expression operations.
   */
  private static class TimeLimitedCharSequence implements CharSequence {
    private final CharSequence inner;
    private final long endTime;
    private final Pattern pattern;
    private final CharSequence originalCharSequence;

    /**
     * Default constructor.
     *
     * @param inner The CharSequence to wrap. This may be a subsequence of the original.
     * @param endTime When to abandon the regex.
     * @param pattern The Pattern instance; only used for logging purposes.
     * @param originalCharSequence originalCharSequence The original sequence, used for logging
     */
    TimeLimitedCharSequence(
        CharSequence inner, long endTime, Pattern pattern, CharSequence originalCharSequence) {
      super();
      this.inner = inner;
      this.endTime = endTime;
      this.pattern = pattern;
      this.originalCharSequence = originalCharSequence;
    }

    @Override
    public char charAt(int index) {
      // This is an unavoidable slowdown
      if (System.currentTimeMillis() > endTime) {
        // Note that we add the original charsequence to the exception
        // message. This condition can be met on a subsequence of the
        // original sequence, and the subsequence string is rarely
        // anywhere near as helpful.
        throw new TimeoutException(
            String.format(
                "Regular expression timeout for [ %s ] operating on [ %s ]",
                pattern.pattern(), originalCharSequence));
      }
      return inner.charAt(index);
    }

    @Override
    public int length() {
      return inner.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      // Ensure that any subsequence generated during a regular expression
      // operation is still going to explode on time.
      return new TimeLimitedCharSequence(
          inner.subSequence(start, end), endTime, pattern, originalCharSequence);
    }

    @Override
    public String toString() {
      return inner.toString();
    }
  }

  /** An exception to indicate that a regular expression operation timed out. */
  public static class TimeoutException extends RuntimeException {
    TimeoutException(String message) {
      super(message);
    }
  }
}
