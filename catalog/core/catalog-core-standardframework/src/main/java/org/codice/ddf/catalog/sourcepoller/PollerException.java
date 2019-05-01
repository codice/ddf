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
package org.codice.ddf.catalog.sourcepoller;

import static org.apache.commons.lang3.Validate.notEmpty;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;

/**
 * Thrown by {@link Poller#pollItems(long, TimeUnit, ImmutableMap)} to indicate that some of the
 * polled items could not be updated
 */
public class PollerException extends Exception {

  private final transient Map<?, Throwable> causes;

  /**
   * @param causes a non-empty {@link Map} of keys of polled items that could not be updated and the
   *     reasons why they failed
   * @throws NullPointerException if the map is {@code null}
   * @throws IllegalArgumentException if the map is empty
   */
  public PollerException(final Map<?, Throwable> causes) {
    super("Failed to update the Poller for: " + StringUtils.join(notEmpty(causes).keySet(), ", "));
    this.causes = causes;
  }

  /**
   * @return a non-empty {@link Map} of keys of polled items that could not be updated and the
   *     reasons why they failed
   */
  public synchronized Map<?, Throwable> getCauses() {
    return causes;
  }
}
