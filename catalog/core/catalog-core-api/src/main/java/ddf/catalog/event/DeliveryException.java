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
package ddf.catalog.event;


/**
 * The exception thrown when problems are detected during the delivery of a
 * created, updated, or deleted event.
 * 
 * @author ddf.isgs@lmco.com
 *
 */
public class DeliveryException extends EventException
{

    /** The constant serialVersionUID. */
    private static final long serialVersionUID = 4505156981841450163L;


    /**
     * Instantiates a new Delivery exception with the provided message.
     * 
     * @param message the message
     */
    public DeliveryException( String message )
    {
        super( message );
    }


    /**
     * Instantiates a new delivery exception with the provided message and
     * throwable.
     * 
     * @param message the message
     * @param throwable the throwable
     */
    public DeliveryException( String message, Throwable throwable )
    {
        super( message, throwable );
    }


    /**
     * Instantiates a new delivery exception with the provided throwable.
     * 
     * @param throwable the throwable
     */
    public DeliveryException( Throwable throwable )
    {
        super( throwable );
    }
}
