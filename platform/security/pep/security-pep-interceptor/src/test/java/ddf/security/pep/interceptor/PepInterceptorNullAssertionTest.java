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
package ddf.security.pep.interceptor;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PepInterceptorNullAssertionTest {
  @Rule public ExpectedException expectedExForNullMessage = ExpectedException.none();

  @Test
  public void testMessageNullSecurityAssertion() {
    PEPAuthorizingInterceptor interceptor = spy(new PEPAuthorizingInterceptor(m -> null));

    Message messageWithNullSecurityAssertion = mock(Message.class);
    // SecurityLogger is already stubbed out
    expectedExForNullMessage.expect(AccessDeniedException.class);
    expectedExForNullMessage.expectMessage("Unauthorized");
    interceptor.handleMessage(messageWithNullSecurityAssertion);
  }
}
