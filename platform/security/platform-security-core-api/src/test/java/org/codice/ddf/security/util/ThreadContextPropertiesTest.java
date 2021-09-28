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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;

import java.util.Map;
import org.apache.shiro.util.ThreadContext;
import org.junit.Test;

public class ThreadContextPropertiesTest {

  public static final String TRACE_CONTEXT_KEY = "trace-context";

  private static final String TRACE_ID = "trace-id";

  @Test
  public void testAddTraceContext() throws Exception {
    String traceId = ThreadContextProperties.addTraceId();
    assertThatMapIsAccurate(traceId);
    assertThat(traceId.equals(ThreadContextProperties.getTraceId()), is(true));
    ThreadContextProperties.removeTraceId();
    assertThat(ThreadContextProperties.getTraceId(), nullValue());
  }

  @Test
  public void testGetTraceContextWhenMissing() throws Exception {
    String traceId = ThreadContextProperties.getTraceId();
    assertThat(traceId, is(nullValue()));
  }

  private void assertThatMapIsAccurate(String expectedTraceId) throws Exception {
    Map<String, String> traceContext = (Map<String, String>) ThreadContext.get(TRACE_CONTEXT_KEY);
    assertThat(traceContext, notNullValue());
    String traceId = traceContext.get(TRACE_ID);
    assertThat(traceId, notNullValue());
    assertThat(expectedTraceId.equals(traceId), is(true));
  }
}
