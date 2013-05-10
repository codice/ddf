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

public class CasAuthenticationToken implements AuthenticationToken
{

    private static final long serialVersionUID = 1L;

        private String ticket;
        
        private String service;
        
        public CasAuthenticationToken( String ticket )
        {
            this.ticket = ticket;
        }
        
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
        
        public String getTicket()
        {
            return ticket;
        }
        
        public String getService()
        {
            return service;
        }
}
