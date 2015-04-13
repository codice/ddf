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
package org.codice.ddf.security.session;

import org.eclipse.jetty.server.session.HashedSession;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestHashSessionIdManager {

    @Test
    public void testAddSessionAndInvalidate() {
        HashSessionIdManager hashSessionIdManager = new HashSessionIdManager();
        //create a mock session
        HashedSession session = mock(HashedSession.class);
        when(session.getId()).thenReturn("1234");
        doCallRealMethod().when(session).setAttribute(anyString(), anyObject());
        doCallRealMethod().when(session).setMaxInactiveInterval(anyInt());
        doCallRealMethod().when(session).isValid();
        Enumeration<String> enumeration = Collections.enumeration(Arrays.asList("myattr"));
        when(session.getAttributeNames()).thenReturn(enumeration);
        when(session.getMaxInactiveInterval()).thenReturn(1);
        when(session.getAttribute(anyString())).thenReturn("myobj");
        //check that our manager has nothing currently
        assertEquals(0, hashSessionIdManager.getSessions().size());
        hashSessionIdManager.addSession(session);
        //now we have 1 session
        assertEquals(1, hashSessionIdManager.getSessions().size());

        //create another session with the same id
        HashedSession session1 = mock(HashedSession.class);
        when(session1.getId()).thenReturn("1234");
        doCallRealMethod().when(session1).isValid();
        ArgumentCaptor<String> stringArgumentCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> integerArgumentCaptor = ArgumentCaptor.forClass(Integer.class);
        hashSessionIdManager.addSession(session1);
        //capture arguments to these calls
        verify(session1).setAttribute(anyString(), stringArgumentCaptor.capture());
        verify(session1).setMaxInactiveInterval(integerArgumentCaptor.capture());
        //we still have 1 "session"
        assertEquals(1, hashSessionIdManager.getSessions().size());
        //but our session cluster now has 2 sessions
        assertEquals(2, hashSessionIdManager.getSession("1234").size());
        //make sure we set the inactive interval
        assertEquals(1, integerArgumentCaptor.getValue().intValue());
        //make sure that all attributes have been set
        assertEquals("myobj", stringArgumentCaptor.getValue());

        //create a mock session with a different id
        HashedSession session2 = mock(HashedSession.class);
        when(session2.getId()).thenReturn("4321");
        hashSessionIdManager.addSession(session2);
        //make sure we now have 2 clusters
        assertEquals(2, hashSessionIdManager.getSessions().size());

        //invalidate the 1234 session and make sure that both sessions got killed but the 4321 session is still good
        hashSessionIdManager.invalidateAll("1234");
        verify(session).invalidate();
        verify(session1).invalidate();
        verify(session2, never()).invalidate();
    }

    @Test
    public void testRemoveSession() {
        HashSessionIdManager hashSessionIdManager = new HashSessionIdManager();
        HashedSession session2 = mock(HashedSession.class);
        when(session2.getId()).thenReturn("4321");
        hashSessionIdManager.addSession(session2);
        assertEquals(1, hashSessionIdManager.getSessions().size());

        hashSessionIdManager.removeSession(session2);
        assertEquals(0, hashSessionIdManager.getSessions().size());
    }
}
