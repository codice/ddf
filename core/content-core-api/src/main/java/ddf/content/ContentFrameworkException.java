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
package ddf.content;


/**
 * The ContentFrameworkException is used to signal errors during Content
 * Framework CRUD operations.
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public class ContentFrameworkException extends Exception 
{
    private static final long serialVersionUID = 1L;

    
    /**
     * Instantiates a new ContentFrameworkException from a given string.
     *
     * @param message the string to use for the exception.
     */
    public ContentFrameworkException( String message ) 
    {
        super( message );
    }
    
    
    /**
     * Instantiates a new ContentFrameworkException.
     *
     * @param throwable the throwable
     */
    public ContentFrameworkException( Throwable throwable ) 
    {
        super( throwable );
    }

    
    /**
     * Instantiates a new ContentFrameworkException with a message.
     *
     * @param message the message
     * @param throwable the throwable
     */
    public ContentFrameworkException( String message, Throwable throwable ) 
    {
        super( message, throwable );
    }

}
