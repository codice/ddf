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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import ddf.security.common.audit.SecurityLogger;
import ddf.security.service.impl.SecurityAssertionStore;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ SecurityAssertionStore.class, SecurityLogger.class })
public class TestPepInterceptorNullAssertion {

    @Rule
    public ExpectedException expectedExForNullMessage = ExpectedException.none();

    @Test
    public void testMessageNullSecurityAssertion() {
        PEPAuthorizingInterceptor interceptor = new PEPAuthorizingInterceptor();

        Message messageWithNullSecurityAssertion = mock(Message.class);
        PowerMockito.mockStatic(SecurityAssertionStore.class);
        PowerMockito.mockStatic(SecurityLogger.class);
        when(SecurityAssertionStore.getSecurityAssertion(messageWithNullSecurityAssertion)).thenReturn(null);
        // SecurityLogger is already stubbed out
        expectedExForNullMessage.expect(AccessDeniedException.class);
        expectedExForNullMessage.expectMessage("Unauthorized");
        interceptor.handleMessage(messageWithNullSecurityAssertion);

        PowerMockito.verifyStatic();
    }
}
