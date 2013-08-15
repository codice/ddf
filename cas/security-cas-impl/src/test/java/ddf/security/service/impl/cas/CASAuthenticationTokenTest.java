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
package ddf.security.service.impl.cas;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;


public class CASAuthenticationTokenTest
{

    private static final String TEST_TICKET = "ST-956-Lyg0BdLkgdrBO9W17bXS";
    private static final String TEST_SERVICE = "http://localhost/test";

    /**
     * Tests creating a token from just a ticket.
     */
    @Test
    public void testCreateTicketToken()
    {
        CasAuthenticationToken token = new CasAuthenticationToken(TEST_TICKET);
        assertEquals(TEST_TICKET, token.getTicket());
        assertEquals(TEST_TICKET, token.getCredentials());
        assertEquals(TEST_TICKET, token.getPrincipal());
        assertNull(token.getService());
    }

    /**
     * Tests creating a token from a ticket and a service.
     */
    @Test
    public void testCreateTicketServiceToken()
    {
        CasAuthenticationToken token = new CasAuthenticationToken(TEST_TICKET, TEST_SERVICE);
        assertEquals(TEST_TICKET, token.getTicket());
        assertEquals(TEST_TICKET+"|"+TEST_SERVICE, token.getCredentials());
        assertEquals(TEST_TICKET+"|"+TEST_SERVICE, token.getPrincipal());
        assertEquals(TEST_SERVICE, token.getService());
    }
}
