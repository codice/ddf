/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.pep.interceptor;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.ActionPermission;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import ddf.security.service.impl.SecurityAssertionStore;
import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;

import javax.xml.namespace.QName;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@PrepareForTest({SecurityAssertionStore.class, SecurityLogger.class})
public class TestPepInterceptorInvalidSubject {

    @Rule
    public PowerMockRule rule = new PowerMockRule();
    @Rule
    // CHECKSTYLE.OFF: VisibilityModifier - Needs to be public for PowerMockito
    public ExpectedException expectedExForInvalidSubject = ExpectedException.none();
    // CHECKSTYLE.ON: VisibilityModifier

    @Test
    public void testMessageInvalidSecurityAssertionToken() throws SecurityServiceException {
        PEPAuthorizingInterceptor interceptor = new PEPAuthorizingInterceptor();

        SecurityManager mockSecurityManager = mock(SecurityManager.class);
        interceptor.setSecurityManager(mockSecurityManager);

        Message messageWithInvalidSecurityAssertion = mock(Message.class);
        SecurityAssertion mockSecurityAssertion = mock(SecurityAssertion.class);
        SecurityToken mockSecurityToken = mock(SecurityToken.class);
        Subject mockSubject = mock(Subject.class);
        assertNotNull(mockSecurityAssertion);

        PowerMockito.mockStatic(SecurityAssertionStore.class);
        PowerMockito.mockStatic(SecurityLogger.class);
        when(SecurityAssertionStore.getSecurityAssertion(messageWithInvalidSecurityAssertion)).thenReturn(mockSecurityAssertion);
        // SecurityLogger is already stubbed out
        when(mockSecurityAssertion.getSecurityToken()).thenReturn(mockSecurityToken);
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

        when(mockSubject.isPermitted(isA(ActionPermission.class))).thenReturn(false);
        expectedExForInvalidSubject.expect(AccessDeniedException.class);
        expectedExForInvalidSubject.expectMessage("Unauthorized");
        // This should throw
        interceptor.handleMessage(messageWithInvalidSecurityAssertion);

        PowerMockito.verifyStatic();
    }
}
