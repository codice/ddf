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

import static ddf.security.audit.impl.TraceIdAuditPropertiesPlugin.NONE;
import static ddf.security.audit.impl.TraceIdAuditPropertiesPlugin.TRACE_ID;
import static org.codice.ddf.security.util.ThreadContextProperties.TRACE_CONTEXT_KEY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.security.util.ThreadContextProperties;
import org.junit.Before;
import org.junit.Test;

public class TraceIdAuditPropertiesPluginTest {

  private TraceIdAuditPropertiesPlugin plugin;

  private final Map<String, String> traceContext = new HashMap<>();

  @Before
  public void setup() {
    plugin = new TraceIdAuditPropertiesPlugin();
    traceContext.clear();
    ThreadContext.put(TRACE_CONTEXT_KEY, traceContext);
  }

  @Test
  public void testTraceIdFound() {
    String traceId = "foo";

    traceContext.put(ThreadContextProperties.TRACE_ID, traceId);

    Pair<String, String> pair = plugin.generate();

    assertThat(pair, is(Pair.of(TRACE_ID, traceId)));
  }

  @Test
  public void testTraceIdNotFound() {
    Pair<String, String> pair = plugin.generate();

    assertThat(pair, is(Pair.of(TRACE_ID, NONE)));
  }
}
