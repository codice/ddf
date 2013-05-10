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
package ddf.content.storage;


/**
 * Exception thrown when a {@link StorageProvider} encounters problems during its
 * execution of CRUD operations.
 * 
 * @version 0.1.0
 * @since 2.1.0
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 *
 */
public class StorageException extends Exception 
{
    private static final long serialVersionUID = 1L;
    
    
    /**
     * Instantiates a new StorageException from a given string.
     *
     * @param message the string to use for the exception.
     */
    public StorageException( String message ) 
    {
        super( message );
    }

    
    /**
     * Instantiates a new StorageException with given {@link Throwable}.
     *
     * @param throwable the throwable
     */
    public StorageException( Throwable throwable ) 
    {
        super( throwable );
    }

    
    /**
     * Instantiates a new StorageException with a message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public StorageException( String message, Throwable throwable ) 
    {
        super( message, throwable );
    }
        
}
