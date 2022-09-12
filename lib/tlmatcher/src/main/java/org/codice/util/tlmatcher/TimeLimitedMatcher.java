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

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Customized and reduced-scope RegEx matcher class to protect against runaway searches.
 *
 * <p>Instances of this class, created through one of its two factory methods, will either succeed
 * or timeout, throwing a {@link TimeoutException}. It is incumbent on the caller to handle the
 * exception.
 *
 * <p>N.B. This matcher differs from the standard {@link Matcher} in that the factory method
 * executes the {@code matches()} check directly, failing and throwing an exception if it times out.
 * Subsequent calls to the {@link #matches()} method merely return the result of the initial check;
 * they do not re-execute the search.
 *
 * <p>Example usage:
 *
 * <pre>
 *   try {
 *     Pattern pattern = Pattern.compile("bc");
 *     TimeLimitedMatcher matcher = TimeLimitedMatcher.create(pattern, "abc123");
 *   } catch (TimeLimitedMatcher.TimeoutException e) {
 *     // handle exception
 *   }
 *   if (matcher.matches()) {
 *     // do something with the match
 *   }
 * </pre>
 */
public class TimeLimitedMatcher {
  // Limit the lifetime of this sequence to five seconds by default.
  private static final Duration DEFAULT_DURATION = Duration.ofSeconds(5L);

  private final Matcher matcher;

  private boolean matches;

  /**
   * Creates a new Time-limited matcher instance to search for the regular expression {@code
   * pattern} against the {@code inputString}.
   *
   * <p>This method executes the search before returning, throwing a {@link TimeoutException} if the
   * RegEx search runs longer than {@link #DEFAULT_DURATION}.
   *
   * @param pattern RegEx search pattern
   * @param inputString String to match against.
   * @return TimeLimitedMatcher instance to use for reporting out match details
   * @throws TimeoutException if the RegEx search runs longer than {@link #DEFAULT_DURATION}
   */
  public static TimeLimitedMatcher create(Pattern pattern, CharSequence inputString)
      throws TimeoutException {
    return create(pattern, inputString, DEFAULT_DURATION);
  }

  /**
   * Creates a new Time-limited matcher instance to search for the regular expression {@code
   * pattern} against the {@code inputString}.
   *
   * <p>This method executes the search before returning, throwing a {@code TimeoutException} if the
   * RegEx search runs longer than {@code duration}.
   *
   * @param pattern RegEx search pattern
   * @param inputString String to match against.
   * @param duration the length of time to continue processing the RegEx before failing
   * @return TimeLimitedMatcher instance to use for reporting out match details
   * @throws TimeoutException if the RegEx search runs longer than {@code duration}
   */
  public static TimeLimitedMatcher create(
      Pattern pattern, CharSequence inputString, Duration duration) throws TimeoutException {
    TLCharSequence tlCharSequence = new TLCharSequence(inputString, calculateEndTime(duration));

    Matcher matcher = pattern.matcher(tlCharSequence);
    try {
      boolean matches = matcher.matches();

      // If we've successfully matched, we can un-restrict the CharSequence.
      // This would not likely be an issue if it were left unrestricted, as the charAt() method
      // should not be called after the matches() method has completed; however, this will account
      // for any future implementation changes that might alter that behavior.
      tlCharSequence.unrestrict();

      return new TimeLimitedMatcher(matcher, matches);
    } catch (RTTimeoutException te) {
      throw new TimeoutException(te.getMessage());
    }
  }

  private TimeLimitedMatcher(Matcher matcher, boolean matches) {
    this.matcher = matcher;
    this.matches = matches;
  }

  /**
   * Returns the input subsequence matched by the previous match.
   *
   * @return The (possibly empty) subsequence matched by the previous match, in string form
   * @see Matcher#group()
   */
  public String group() {
    return matcher.group();
  }

  /**
   * Returns the input subsequence captured by the given group during the previous match operation.
   *
   * @param group The index of a capturing group in this matcher's pattern
   * @return The (possibly empty) subsequence captured by the group during the previous match, or
   *     null if the group failed to match part of the input
   * @see Matcher#group(int)
   */
  public String group(int group) {
    return matcher.group(group);
  }

  /**
   * Returns the input subsequence captured by the given named-capturing group during the previous
   * match operation.
   *
   * @param name The name of a named-capturing group in this matcher's pattern
   * @return The (possibly empty) subsequence captured by the named group during the previous match,
   *     or null if the group failed to match part of the input
   * @see Matcher#group(String)
   */
  public String group(String name) {
    return matcher.group(name);
  }

  /**
   * Returns the number of capturing groups in this matcher's pattern.
   *
   * @return The number of capturing groups in this matcher's pattern
   * @see Matcher#groupCount()
   */
  public int groupCount() {
    return matcher.groupCount();
  }

  /**
   * True if the entire region matched against the pattern.
   *
   * @return true if the entire region matched against the pattern.
   * @see Matcher#matches()
   */
  public boolean matches() {
    return matches;
  }

  public String toString() {
    return String.format("TimeLimitedMatcher of [%s]", matcher.toString());
  }

  private static long calculateEndTime(Duration duration) {
    return System.currentTimeMillis() + duration.toMillis();
  }

  /** Time-limited character sequence. */
  static class TLCharSequence implements CharSequence {
    private final CharSequence delegate;
    private final long endTime;
    private final AtomicBoolean isRestricted;

    /**
     * Constructor for new time-restricted CharSequence.
     *
     * @param delegate The CharSequence to wrap.
     * @param endTime When to abandon the RegEx.
     */
    private TLCharSequence(CharSequence delegate, long endTime) {
      this(delegate, endTime, new AtomicBoolean(true));
    }

    /**
     * Constructor for new time-restricted CharSequence with provided restriction rules.
     *
     * @param delegate The CharSequence to wrap.
     * @param endTime When to abandon the RegEx.
     * @param isRestricted shared flag indicating the restricted/not restricted state of the
     *     CharSequence
     */
    private TLCharSequence(CharSequence delegate, long endTime, AtomicBoolean isRestricted) {
      this.delegate = delegate;
      this.endTime = endTime;
      this.isRestricted = isRestricted;
    }

    private void unrestrict() {
      isRestricted.set(false);
    }

    @Override
    public char charAt(int index) {
      // This is an unavoidable slowdown
      if (isRestricted.get() && System.currentTimeMillis() > endTime) {
        throw new RTTimeoutException("RegEx timed out");
      }
      return delegate.charAt(index);
    }

    @Override
    public int length() {
      return delegate.length();
    }

    @Override
    public CharSequence subSequence(int start, int end) {
      // Ensure that any subsequence generated during a regular expression
      // operation is still going to fail on time, if this CharSequence is still time-restricted
      return new TLCharSequence(delegate.subSequence(start, end), endTime, isRestricted);
    }

    @Override
    public String toString() {
      return delegate.toString();
    }
  }

  /** Public, checked exception that is thrown when RegEx times out. */
  public static class TimeoutException extends Exception {
    public TimeoutException(String message) {
      super(message);
    }
  }

  private static class RTTimeoutException extends RuntimeException {
    RTTimeoutException(String message) {
      super(message);
    }
  }
}
