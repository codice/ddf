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
package ddf.metrics.reporting.internal;



/**
 * Exception thrown when a {@link Collector} encounters problems during its
 * execution.
 * 
 * @since 2.1.0
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 *
 */
public class CollectorException extends Exception
{
    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;
    
    
    /**
     * Instantiates a new CollectorException from a given string.
     *
     * @param message the string to use for the exception.
     */
    public CollectorException(String message) 
    {
        super(message);
    }
    

    /**
     * Instantiates a new CollectorException.
     */
    public CollectorException() 
    {
        super();
    }
    

    /**
     * Instantiates a new CollectorException with a message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public CollectorException(String message, Throwable throwable) 
    {
        super(message, throwable);
    }
    

    /**
     * Instantiates a new CollectorExceptionn.
     *
     * @param throwable the throwable
     */
    public CollectorException(Throwable throwable) 
    {
        super(throwable);
    }

}
