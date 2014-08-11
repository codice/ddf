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
package org.codice.ddf.security.filter.login;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

/**
 * Tests the SAMLCache class.
 */
public class SAMLCacheTest {

    private static final String TEST_REALM = "DDF";

    /**
     * Tests general operations of the cache.
     */
    @Test
    public void testCache() {
        SAMLCache cache = new SAMLCache();
        SecurityToken token = mock(SecurityToken.class);
        SecurityToken token2 = mock(SecurityToken.class);
        String ref1 = cache.put(TEST_REALM, token);
        String ref2 = cache.put(TEST_REALM, token2);
        // verify that the references are unique
        assertFalse(ref1.equals(ref2));
        // verify proper retrieval of the token
        assertEquals(token, cache.get(TEST_REALM, ref1));
        assertEquals(token2, cache.get(TEST_REALM, ref2));
        // check that no token comes back for incorrect realm
        assertNull(cache.get("BAD_REALM", ref1));
        cache.clearCache();
        // verify after clearing that all references return null.
        assertNull(cache.get(TEST_REALM, ref1));
        assertNull(cache.get(TEST_REALM, ref2));
    }

    /**
     * Tests that calling the set expiration time method does not throw an exception, even if the time is the same as the default.
     * Leaving the logic of the cache actually expiring up to the underlying library.
     */
    @Test
    public void testChangeExpiration() {
        SAMLCache cache = new SAMLCache();
        cache.setExpirationTime(31);
        cache.setExpirationTime(1);
    }

}
