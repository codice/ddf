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
 * The exception thrown when problems creating a subscription are detected due to
 * a {@link Subscription} with the same ID already existing in the OSGi registry.
 * 
 * @author ddf.isgs@lmco.com
 *
 */
public class SubscriptionExistsException extends EventException {

    /** The constant serialVersionUID. */
    private static final long serialVersionUID = -4381522776151289637L;

}
