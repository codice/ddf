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
package org.codice.ddf.pax.web.jetty;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import ch.qos.logback.access.jetty.RequestLogImpl;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.junit.After;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AccessRequestLogTest {

  private static final String ENABLED_PROP = "org.codice.ddf.http.access.log.enabled";

  @Mock private Request mockRequest;

  @Mock private Response mockResponse;

  @After
  public void tearDown() {
    System.clearProperty(ENABLED_PROP);
  }

  @Test
  public void inheritsFromRequestLogImpl() {
    AccessRequestLog log = new AccessRequestLog();
    assertThat(log, instanceOf(RequestLogImpl.class));
  }

  @Test
  public void doesNotDelegateToSuperWhenDisabled() {
    System.setProperty(ENABLED_PROP, "false");
    MockitoAnnotations.openMocks(this);

    AccessRequestLog log = spy(new AccessRequestLog());
    log.log(mockRequest, mockResponse);

    // Disabled: short-circuits before super.log is invoked, so no jetty Request methods get hit.
    verify(mockRequest, never()).getMethod();
  }

  @Test
  public void invokesSuperWhenEnabled() {
    System.setProperty(ENABLED_PROP, "true");
    MockitoAnnotations.openMocks(this);

    AccessRequestLog log = new AccessRequestLog();
    // No appenders configured, so super.log() iterates an empty list and returns;
    // the call should not throw.
    log.log(mockRequest, mockResponse);
  }

  @Test
  public void defaultsToDisabledWhenSystemPropertyAbsent() {
    System.clearProperty(ENABLED_PROP);
    MockitoAnnotations.openMocks(this);

    AccessRequestLog log = new AccessRequestLog();
    log.log(mockRequest, mockResponse);

    verify(mockRequest, never()).getMethod();
  }
}
