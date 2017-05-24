/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.subscriptionstore.internal;

/**
 * Exception specific to subscription registration with OSGi, where the registration process either
 * did not succeed or was invoked incorrectly.
 * <p>
 * <b>This interface is for internal use only and should not be implemented by a third party. </b>
 * <i>This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library.</i>
 */
public class SubscriptionRegistrationException extends RuntimeException {
    public SubscriptionRegistrationException() {
    }

    public SubscriptionRegistrationException(String message) {
        super(message);
    }

    public SubscriptionRegistrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public SubscriptionRegistrationException(Throwable cause) {
        super(cause);
    }

    public SubscriptionRegistrationException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
