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
package org.codice.ddf.endpoints.subscriptions;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;


public class SubscriptionServiceException extends WebApplicationException
{
    public static final long serialVersionUID = 1L;
    
    
    public SubscriptionServiceException( String message ) 
    { 
        super( Response.status( 500 ).entity( message ).type( MediaType.TEXT_PLAIN ).build() ); 
    }
    
    
    public SubscriptionServiceException( String message, Throwable e ) 
    { 
        super( e, Response.status( 500 ).entity( message ).type( MediaType.TEXT_PLAIN ).build() ); 
    }
}
