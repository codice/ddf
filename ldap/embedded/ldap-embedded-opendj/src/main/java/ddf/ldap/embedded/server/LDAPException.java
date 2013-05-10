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
package ddf.ldap.embedded.server;


/**
 * General exception class used within the embedded ldap project.
 * 
 */
public class LDAPException extends Exception
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new LDAP exception.
     */
    public LDAPException()
    {
        super();
    }

    /**
     * Instantiates a new LDAP exception with the provided message.
     * 
     * @param message the message
     */
    public LDAPException( String message )
    {
        super(message);
    }

    /**
     * Instantiates a new LDAP exception with the provided message and
     * {@link Throwable}.
     * 
     * @param message the message
     * @param throwable the throwable
     */
    public LDAPException( String message, Throwable throwable )
    {
        super(message, throwable);
    }

    /**
     * Instantiates a new LDAP exception with the provided {@link Throwable}.
     * 
     * @param throwable the throwable
     */
    public LDAPException( Throwable throwable )
    {
        super(throwable);
    }

}
