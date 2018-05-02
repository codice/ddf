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
package net.jodah.failsafe.internal;

import net.jodah.failsafe.RetryPolicy;
import net.jodah.failsafe.util.Duration;

/**
 * This retry policy is designed to clone an existing policy while disabling all timing aspects. It
 * will also register a new abort condition for the {@link AssertionError} exception.
 */
public class TimelessRetryPolicy extends RetryPolicy {

  /**
   * Constructs a new timeless retry policy.
   *
   * @param policy the policy to wrap
   */
  public TimelessRetryPolicy(RetryPolicy policy) {
    super(policy);
    if (!(policy instanceof TimelessRetryPolicy)) {
      abortOn(AssertionError.class);
    }
  }

  @Override
  public RetryPolicy copy() {
    return new TimelessRetryPolicy(this);
  }

  @Override
  public Duration getDelay() { // force no duration
    return Duration.NONE;
  }

  @Override
  public double getDelayFactor() { // force no delay factor
    return 0.0D;
  }

  @Override
  public Duration getJitter() { // disables jitter
    return null;
  }

  @Override
  public double getJitterFactor() { // force no jitter factor
    return 0.0D;
  }

  @Override
  public Duration getMaxDelay() { // disables max delay
    return null;
  }

  @Override
  public Duration getMaxDuration() { // disables max duration
    return null;
  }
}
