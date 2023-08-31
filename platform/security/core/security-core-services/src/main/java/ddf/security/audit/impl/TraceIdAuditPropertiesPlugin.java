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
package ddf.security.audit.impl;

import ddf.security.audit.AuditPropertiesPlugin;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.codice.ddf.security.util.ThreadContextProperties;

public class TraceIdAuditPropertiesPlugin implements AuditPropertiesPlugin {

  public static final String TRACE_ID = "trace-id";

  public static final String NONE = "none";

  @Override
  public Pair<String, String> generate() {

    String traceId = ThreadContextProperties.getTraceId();
    if (StringUtils.isNotEmpty(traceId)) {
      return Pair.of(TRACE_ID, traceId);
    }

    return Pair.of(TRACE_ID, NONE);
  }
}
