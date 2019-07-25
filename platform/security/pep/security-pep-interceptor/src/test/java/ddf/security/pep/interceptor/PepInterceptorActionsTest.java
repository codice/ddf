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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.permission.CollectionPermission;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import javax.xml.namespace.QName;
import javax.xml.ws.handler.MessageContext;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.service.model.MessageInfo;
import org.apache.cxf.ws.addressing.Names;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class PepInterceptorActionsTest {

  private PEPAuthorizingInterceptor interceptor;

  private SecurityAssertion mockSecurityAssertion = mock(SecurityAssertion.class);

  @Before
  public void setup() {
    interceptor = new PEPAuthorizingInterceptor(m -> mockSecurityAssertion);
  }

  @Test
  public void testMessageWithDefaultUriAction() throws SecurityServiceException {
    SecurityManager mockSecurityManager = mock(SecurityManager.class);
    interceptor.setSecurityManager(mockSecurityManager);

    Message messageWithAction = mock(Message.class);
    SecurityToken mockSecurityToken = mock(SecurityToken.class);
    Subject mockSubject = mock(Subject.class);
    assertNotNull(mockSecurityAssertion);

    // SecurityLogger is already stubbed out
    when(mockSecurityAssertion.getToken()).thenReturn(mockSecurityToken);
    when(mockSecurityToken.getToken()).thenReturn(null);

    when(mockSecurityManager.getSubject(mockSecurityToken)).thenReturn(mockSubject);

    QName op = new QName("urn:catalog:query", "search", "ns1");
    QName port = new QName("urn:catalog:query", "query-port", "ns1");
    when(messageWithAction.get(MessageContext.WSDL_OPERATION)).thenReturn(op);
    when(messageWithAction.get(MessageContext.WSDL_PORT)).thenReturn(port);

    Exchange mockExchange = mock(Exchange.class);
    BindingOperationInfo mockBOI = mock(BindingOperationInfo.class);
    when(messageWithAction.getExchange()).thenReturn(mockExchange);
    when(mockExchange.get(BindingOperationInfo.class)).thenReturn(mockBOI);
    when(mockBOI.getExtensor(SoapOperationInfo.class)).thenReturn(null);

    doAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                CollectionPermission perm = (CollectionPermission) invocation.getArguments()[0];
                assertEquals("urn:catalog:query:query-port:searchRequest", perm.getAction());
                return true;
              }
            })
        .when(mockSubject)
        .isPermitted(isA(CollectionPermission.class));

    // This should work.
    interceptor.handleMessage(messageWithAction);
  }

  @Test
  public void testMessageWithDefaultUrlAction() throws SecurityServiceException {
    SecurityManager mockSecurityManager = mock(SecurityManager.class);
    interceptor.setSecurityManager(mockSecurityManager);

    Message messageWithAction = mock(Message.class);
    SecurityToken mockSecurityToken = mock(SecurityToken.class);
    Subject mockSubject = mock(Subject.class);
    assertNotNull(mockSecurityAssertion);

    // SecurityLogger is already stubbed out
    when(mockSecurityAssertion.getToken()).thenReturn(mockSecurityToken);
    when(mockSecurityToken.getToken()).thenReturn(null);

    when(mockSecurityManager.getSubject(mockSecurityToken)).thenReturn(mockSubject);

    QName op = new QName("http://catalog/query/", "Search", "ns1");
    QName port = new QName("http://catalog/query/", "QueryPort", "ns1");
    when(messageWithAction.get(MessageContext.WSDL_OPERATION)).thenReturn(op);
    when(messageWithAction.get(MessageContext.WSDL_PORT)).thenReturn(port);

    Exchange mockExchange = mock(Exchange.class);
    BindingOperationInfo mockBOI = mock(BindingOperationInfo.class);
    when(messageWithAction.getExchange()).thenReturn(mockExchange);
    when(mockExchange.get(BindingOperationInfo.class)).thenReturn(mockBOI);
    when(mockBOI.getExtensor(SoapOperationInfo.class)).thenReturn(null);

    doAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                CollectionPermission perm = (CollectionPermission) invocation.getArguments()[0];
                assertEquals("http://catalog/query/QueryPort/SearchRequest", perm.getAction());
                return true;
              }
            })
        .when(mockSubject)
        .isPermitted(isA(CollectionPermission.class));

    // This should work.
    interceptor.handleMessage(messageWithAction);
  }

  @Test
  public void testMessageWithMessageAction() throws SecurityServiceException {
    SecurityManager mockSecurityManager = mock(SecurityManager.class);
    interceptor.setSecurityManager(mockSecurityManager);

    Message messageWithAction = mock(Message.class);
    SecurityToken mockSecurityToken = mock(SecurityToken.class);
    Subject mockSubject = mock(Subject.class);
    assertNotNull(mockSecurityAssertion);

    // SecurityLogger is already stubbed out
    when(mockSecurityAssertion.getToken()).thenReturn(mockSecurityToken);
    when(mockSecurityToken.getToken()).thenReturn(null);

    when(mockSecurityManager.getSubject(mockSecurityToken)).thenReturn(mockSubject);

    MessageInfo mockMessageInfo = mock(MessageInfo.class);
    when(messageWithAction.get(MessageInfo.class.getName())).thenReturn(mockMessageInfo);
    when(mockMessageInfo.getExtensionAttribute(
            new QName(Names.WSA_NAMESPACE_WSDL_METADATA, Names.WSAW_ACTION_NAME)))
        .thenReturn("urn:catalog:query:query-port:search");

    doAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                CollectionPermission perm = (CollectionPermission) invocation.getArguments()[0];
                assertEquals("urn:catalog:query:query-port:search", perm.getAction());
                return true;
              }
            })
        .when(mockSubject)
        .isPermitted(isA(CollectionPermission.class));

    // This should work.
    interceptor.handleMessage(messageWithAction);
  }

  @Test
  public void testMessageWithOperationAction() throws SecurityServiceException {
    SecurityManager mockSecurityManager = mock(SecurityManager.class);
    interceptor.setSecurityManager(mockSecurityManager);

    Message messageWithAction = mock(Message.class);
    SecurityToken mockSecurityToken = mock(SecurityToken.class);
    Subject mockSubject = mock(Subject.class);
    assertNotNull(mockSecurityAssertion);

    // SecurityLogger is already stubbed out
    when(mockSecurityAssertion.getToken()).thenReturn(mockSecurityToken);
    when(mockSecurityToken.getToken()).thenReturn(null);

    when(mockSecurityManager.getSubject(mockSecurityToken)).thenReturn(mockSubject);

    Exchange mockExchange = mock(Exchange.class);
    BindingOperationInfo mockBOI = mock(BindingOperationInfo.class);
    SoapOperationInfo mockSOI = mock(SoapOperationInfo.class);
    when(messageWithAction.getExchange()).thenReturn(mockExchange);
    when(mockExchange.get(BindingOperationInfo.class)).thenReturn(mockBOI);
    when(mockBOI.getExtensor(SoapOperationInfo.class)).thenReturn(mockSOI);
    when(mockSOI.getAction()).thenReturn("urn:catalog:query:query-port:search");

    doAnswer(
            new Answer<Boolean>() {
              @Override
              public Boolean answer(InvocationOnMock invocation) throws Throwable {
                CollectionPermission perm = (CollectionPermission) invocation.getArguments()[0];
                assertEquals("urn:catalog:query:query-port:search", perm.getAction());
                return true;
              }
            })
        .when(mockSubject)
        .isPermitted(isA(CollectionPermission.class));

    // This should work.
    interceptor.handleMessage(messageWithAction);
  }

  @Test(expected = AccessDeniedException.class)
  public void testMessageWithNoAction() throws SecurityServiceException {
    SecurityManager mockSecurityManager = mock(SecurityManager.class);
    interceptor.setSecurityManager(mockSecurityManager);

    Message messageWithoutAction = mock(Message.class);
    SecurityToken mockSecurityToken = mock(SecurityToken.class);
    Subject mockSubject = mock(Subject.class);
    assertNotNull(mockSecurityAssertion);

    // SecurityLogger is already stubbed out
    when(mockSecurityAssertion.getToken()).thenReturn(mockSecurityToken);
    when(mockSecurityToken.getToken()).thenReturn(null);

    when(mockSecurityManager.getSubject(mockSecurityToken)).thenReturn(mockSubject);

    Exchange mockExchange = mock(Exchange.class);
    BindingOperationInfo mockBOI = mock(BindingOperationInfo.class);
    when(messageWithoutAction.getExchange()).thenReturn(mockExchange);
    when(mockExchange.get(BindingOperationInfo.class)).thenReturn(mockBOI);
    when(mockBOI.getExtensor(SoapOperationInfo.class)).thenReturn(null);

    when(mockSubject.isPermitted(isA(CollectionPermission.class))).thenReturn(false);

    // This should throw an exception.
    interceptor.handleMessage(messageWithoutAction);
  }
}
