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

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.xml.namespace.QName;

import org.apache.cxf.binding.soap.model.SoapOperationInfo;
import org.apache.cxf.message.Exchange;
import org.apache.cxf.message.Message;
import org.apache.cxf.service.model.BindingOperationInfo;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.permission.ActionPermission;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import ddf.security.service.impl.SecurityAssertionStore;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SecurityAssertionStore.class, SecurityLogger.class })
public class TestPepInterceptorValidSubject {

    @Test
    public void testMessageValidSecurityAssertionToken() throws SecurityServiceException {
        PEPAuthorizingInterceptor interceptor = new PEPAuthorizingInterceptor();

        SecurityManager mockSecurityManager = mock(SecurityManager.class);
        interceptor.setSecurityManager(mockSecurityManager);

        Message messageWithValidSecurityAssertion = mock(Message.class);
        SecurityAssertion mockSecurityAssertion = mock(SecurityAssertion.class);
        SecurityToken mockSecurityToken = mock(SecurityToken.class);
        Subject mockSubject = mock(Subject.class);
        assertNotNull(mockSecurityAssertion);

        PowerMockito.mockStatic(SecurityAssertionStore.class);
        PowerMockito.mockStatic(SecurityLogger.class);
        when(SecurityAssertionStore.getSecurityAssertion(messageWithValidSecurityAssertion)).thenReturn(mockSecurityAssertion);
        // SecurityLogger is already stubbed out
        when(mockSecurityAssertion.getSecurityToken()).thenReturn(mockSecurityToken);
        when(mockSecurityToken.getToken()).thenReturn(null);

        when(mockSecurityManager.getSubject(mockSecurityToken)).thenReturn(mockSubject);
        
        QName op = new QName("urn:catalog:query", "search", "ns1");
        QName port = new QName("urn:catalog:query", "query-port", "ns1");
        when(messageWithValidSecurityAssertion.get("javax.xml.ws.wsdl.operation")).thenReturn(op);
        when(messageWithValidSecurityAssertion.get("javax.xml.ws.wsdl.port")).thenReturn(port);

        Exchange mockExchange = mock(Exchange.class);
        BindingOperationInfo mockBOI = mock(BindingOperationInfo.class);
        when(messageWithValidSecurityAssertion.getExchange()).thenReturn(mockExchange);
        when(mockExchange.get(BindingOperationInfo.class)).thenReturn(mockBOI);
        when(mockBOI.getExtensor(SoapOperationInfo.class)).thenReturn(null);
        
        when(mockSubject.isPermitted(isA(ActionPermission.class))).thenReturn(true);
        
        // This should work.
        interceptor.handleMessage(messageWithValidSecurityAssertion);

        PowerMockito.verifyStatic();
    }
}
