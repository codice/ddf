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
package org.codice.ddf.admin.api.handler.commons;

public class UrlAvailability {

    private boolean available;
    private boolean trustedCertAuthority;
    private boolean certError;

    public UrlAvailability() {
        available = false;
        trustedCertAuthority = false;
        certError = false;
    }

    public boolean isAvailable() {
        return available;
    }

    public UrlAvailability available(boolean available) {
        this.available = available;
        return this;
    }

    public boolean isTrustedCertAuthority() {
        return trustedCertAuthority;
    }

    public UrlAvailability trustedCertAuthority(boolean trustedCertAuthority) {
        this.trustedCertAuthority = trustedCertAuthority;
        return this;
    }

    public boolean isCertError() {
        return certError;
    }

    public UrlAvailability certError(boolean certError) {
        this.certError = certError;
        return this;
    }

}
