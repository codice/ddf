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

import org.apache.shiro.authc.AuthenticationToken;

/**
 * Implementation of {@link org.apache.shiro.authc.AuthenticationToken} that is specific to CAS Proxy Tickets. 
 *
 */
public class CasAuthenticationToken implements AuthenticationToken
{

    private static final long serialVersionUID = 1L;

        private String ticket;
        
        private String service;
        
        /**
         * Creates a token with the given ticket as the credential.
         * @param ticket CAS Proxy Ticket to use as the credential.
         */
        public CasAuthenticationToken( String ticket )
        {
            this.ticket = ticket;
            this.service = null;
        }
        
        /**
         * Creates a token with the ticket as the credential and the service as a service.
         * @param ticket CAS Proxy Ticket to use as the credential and principal.
         * @param service Service that the ticket was made for.
         */
        public CasAuthenticationToken( String ticket, String service )
        {
            this.ticket = ticket;
            this.service = service;
        }
        
        @Override
        public Object getCredentials()
        {
            return ticket;
        }

        @Override
        public Object getPrincipal()
        {
            return ticket;
        }
        
        /**
         * Retrieves the CAS Proxy ticket for this token.
         * @return CAS Proxy Ticket
         */
        public String getTicket()
        {
            return ticket;
        }
        
        /**
         * Retrieves the Service that the CAS proxy ticket was made for (if set).
         * @return Service URL if set, null if not set.
         */
        public String getService()
        {
            return service;
        }
}
