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
package org.codice.ddf.security.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.shiro.util.ThreadContext;

public final class ThreadContextUtils {

  public static final String TRACE_CONTEXT_KEY = "trace-context";

  private static final String TRACE_ID = "trace-id";

  private ThreadContextUtils() {
    // as a utility this should never be constructed, hence it's private
  }

  /**
   * Adds the trace context and unique trace id to ThreadContext.
   *
   * @return traceId in ThreadContext
   */
  public static String addTraceIdToContext() {
    String traceId = getTraceIdFromContext();
    if (traceId == null) {
      Map<String, String> traceContextMap = new HashMap<>();
      traceId = UUID.randomUUID().toString().replaceAll("-", "");
      traceContextMap.put(TRACE_ID, traceId);
      ThreadContext.put(TRACE_CONTEXT_KEY, traceContextMap);
    }
    return traceId;
  }

  /** @return trace-id from ThreadContext if it exists otherwise returns null */
  public static String getTraceIdFromContext() {
    String traceId = null;
    Map<String, String> traceContextMap =
        (Map<String, String>) ThreadContext.get(TRACE_CONTEXT_KEY);
    if (traceContextMap != null && traceContextMap.size() > 0) {
      traceId = traceContextMap.get(TRACE_ID);
    }
    return traceId;
  }
}
