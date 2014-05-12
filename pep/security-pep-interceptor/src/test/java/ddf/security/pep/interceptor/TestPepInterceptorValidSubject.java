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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.isA;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.headers.Header;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;

import org.w3c.dom.Element;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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
        
        Header mockHeader = mock(Header.class);
        when(mockHeader.getName()).thenReturn(new QName("http://www.w3.org/2005/08/addressing", "Action"));
        Element mockElement = mock(Element.class);
        when(mockHeader.getObject()).thenReturn(mockElement);
        when(mockElement.getTextContent()).thenReturn("");
        List<Header> headerList = new ArrayList<Header>();
        headerList.add(mockHeader);
        when(messageWithValidSecurityAssertion.get(Header.HEADER_LIST)).thenReturn(headerList);
        when(mockSubject.isPermitted(isA(ActionPermission.class))).thenReturn(true);
        // This should work.
        interceptor.handleMessage(messageWithValidSecurityAssertion);

        PowerMockito.verifyStatic();
    }
}
