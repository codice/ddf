/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.impl;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.UUID;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DelegatingSubject;
import org.junit.Test;


/**
 * Checks each of the SubjectImpl constructors to verify that they are correctly
 * passed the parameters into the underlying Subject implementation.
 * 
 * 
 */
public class SubjectImplTest
{

    public static final String TEST_SUBJECT_NAME = "SIR.TEST";
    public static final String TEST_REALM_NAME = "TEST REALM";
    public static final String TEST_HOST = "hostName";
    public static final Session TEST_SESSION = new SimpleSession(UUID.randomUUID().toString());
    public static final DefaultSecurityManager TEST_MANAGER = new DefaultSecurityManager();

    /**
     * Checks to make sure that the values are being passed through our
     * implementation to the backed implementation correctly.
     */
    @Test
    public void testSixParamConstructor()
    {
        DelegatingSubject testSubject = new SubjectImpl(createTestCollection(), false, TEST_HOST, TEST_SESSION, false,
            TEST_MANAGER);
        assertEquals(createTestCollection(), testSubject.getPrincipals());
        assertFalse(testSubject.isAuthenticated());
        assertEquals(TEST_HOST, testSubject.getHost());
        assertEquals(TEST_SESSION.getId(), testSubject.getSession().getId());
        assertEquals(TEST_MANAGER, testSubject.getSecurityManager());
    }

    @Test
    public void testFiveParamConstructor()
    {
        DelegatingSubject testSubject = new SubjectImpl(createTestCollection(), false, TEST_HOST, TEST_SESSION,
            TEST_MANAGER);
        assertEquals(createTestCollection(), testSubject.getPrincipals());
        assertFalse(testSubject.isAuthenticated());
        assertEquals(TEST_HOST, testSubject.getHost());
        assertEquals(TEST_SESSION.getId(), testSubject.getSession().getId());
        assertEquals(TEST_MANAGER, testSubject.getSecurityManager());
    }

    @Test
    public void testFourParamConstructor()
    {
        DelegatingSubject testSubject = new SubjectImpl(createTestCollection(), false, TEST_SESSION,
            TEST_MANAGER);
        assertEquals(createTestCollection(), testSubject.getPrincipals());
        assertFalse(testSubject.isAuthenticated());
        assertEquals(TEST_SESSION.getId(), testSubject.getSession().getId());
        assertEquals(TEST_MANAGER, testSubject.getSecurityManager());
    }

    private PrincipalCollection createTestCollection()
    {
        SimplePrincipalCollection collection = new SimplePrincipalCollection();
        collection.add(TEST_SUBJECT_NAME, TEST_REALM_NAME);
        return collection;
    }
}
