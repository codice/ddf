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
package ddf.security.pdp.xacml;

/**
 * Exception thrown by the DDF implementation of the Balana PDP.
 */
public class PdpException extends Exception
{
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new PDP exception.
     */
    public PdpException()
    {
        super();
    }

    /**
     * Instantiates a new PDP exception with the provided message.
     * 
     * @param message the message
     */
    public PdpException( String message )
    {
        super(message);
    }

    /**
     * Instantiates a new PDP exception with the provided message and
     * {@link Throwable}.
     * 
     * @param message the message
     * @param throwable the throwable
     */
    public PdpException( String message, Throwable throwable )
    {
        super(message, throwable);
    }

    /**
     * Instantiates a newPDP exception with the provided {@link Throwable}.
     * 
     * @param throwable the throwable
     */
    public PdpException( Throwable throwable )
    {
        super(throwable);
    }

}
