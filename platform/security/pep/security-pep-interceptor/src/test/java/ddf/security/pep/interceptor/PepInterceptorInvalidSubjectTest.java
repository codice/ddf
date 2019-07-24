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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.permission.CollectionPermission;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import javax.xml.namespace.QName;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class PepInterceptorInvalidSubjectTest {

  @Rule
  // CHECKSTYLE.OFF: VisibilityModifier - Needs to be public for Mockito
  public ExpectedException expectedExForInvalidSubject = ExpectedException.none();
  // CHECKSTYLE.ON: VisibilityModifier

  @Test
  public void testMessageInvalidSecurityAssertionToken() throws SecurityServiceException {
    SecurityAssertion mockSecurityAssertion = mock(SecurityAssertion.class);
    PEPAuthorizingInterceptor interceptor =
        spy(new PEPAuthorizingInterceptor(m -> mockSecurityAssertion));

    SecurityManager mockSecurityManager = mock(SecurityManager.class);
    interceptor.setSecurityManager(mockSecurityManager);

    Message messageWithInvalidSecurityAssertion = mock(Message.class);
    SecurityToken mockSecurityToken = mock(SecurityToken.class);
    Subject mockSubject = mock(Subject.class);
    assertNotNull(mockSecurityAssertion);

    // SecurityLogger is already stubbed out
    when(mockSecurityAssertion.getToken()).thenReturn(mockSecurityToken);
    when(mockSecurityToken.getToken()).thenReturn(null);

    when(mockSecurityManager.getSubject(mockSecurityToken)).thenReturn(mockSubject);

    QName op = new QName("urn:catalog:query", "search", "ns1");
    QName port = new QName("urn:catalog:query", "query-port", "ns1");
    when(messageWithInvalidSecurityAssertion.get("javax.xml.ws.wsdl.operation")).thenReturn(op);
    when(messageWithInvalidSecurityAssertion.get("javax.xml.ws.wsdl.port")).thenReturn(port);

    Exchange mockExchange = mock(Exchange.class);
    BindingOperationInfo mockBOI = mock(BindingOperationInfo.class);
    when(messageWithInvalidSecurityAssertion.getExchange()).thenReturn(mockExchange);
    when(mockExchange.get(BindingOperationInfo.class)).thenReturn(mockBOI);
    when(mockBOI.getExtensor(SoapOperationInfo.class)).thenReturn(null);

    when(mockSubject.isPermitted(isA(CollectionPermission.class))).thenReturn(false);
    expectedExForInvalidSubject.expect(AccessDeniedException.class);
    expectedExForInvalidSubject.expectMessage("Unauthorized");
    // This should throw
    interceptor.handleMessage(messageWithInvalidSecurityAssertion);
  }
}
