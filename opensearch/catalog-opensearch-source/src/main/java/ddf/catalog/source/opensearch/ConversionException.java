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
package ddf.catalog.source.opensearch;


/**
 * {@link ConversionException} is thrown by a {@link OpenSearchSite} when it
 * cannot convert an incoming atom response.
 * 
 */
public class ConversionException extends Exception
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new unsupported query exception.
     */
    public ConversionException()
    {
        super();
    }

    /**
     * Instantiates a new conversion exception with the provided message.
     * 
     * @param message the message
     */
    public ConversionException( String message )
    {
        super(message);
    }

    /**
     * Instantiates a new conversion exception with the provided message
     * and {@link Throwable}.
     * 
     * @param message the message
     * @param throwable the throwable
     */
    public ConversionException( String message, Throwable throwable )
    {
        super(message, throwable);
    }

    /**
     * Instantiates a new conversion exception with the provided
     * {@link Throwable}.
     * 
     * @param throwable the throwable
     */
    public ConversionException( Throwable throwable )
    {
        super(throwable);
    }

}
