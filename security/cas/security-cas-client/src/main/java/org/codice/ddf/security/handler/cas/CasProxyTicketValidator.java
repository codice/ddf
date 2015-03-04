/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.security.handler.cas;

import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.jasig.cas.client.validation.TicketValidator;

public class CasProxyTicketValidator implements TicketValidator {
    private Cas20ProxyTicketValidator proxyTicketValidator;

    private String proxyCallbackUrl;

    private boolean acceptAnyProxy;

    private ProxyGrantingTicketStorage proxyGrantingTicketStorage;

    @Override
    public Assertion validate(String ticket, String service) throws TicketValidationException {
        return proxyTicketValidator.validate(ticket, service);
    }

    public void setCasServerUrl(String serverUrl) {
        proxyTicketValidator = new Cas20ProxyTicketValidator(serverUrl);
        proxyTicketValidator.setProxyCallbackUrl(proxyCallbackUrl);
        proxyTicketValidator.setAcceptAnyProxy(acceptAnyProxy);
        proxyTicketValidator.setProxyGrantingTicketStorage(proxyGrantingTicketStorage);
    }

    public void setProxyCallbackUrl(String proxyCallbackUrl) {
        this.proxyCallbackUrl = proxyCallbackUrl;
        if (proxyTicketValidator != null) {
            proxyTicketValidator.setProxyCallbackUrl(proxyCallbackUrl);
        }
    }

    public void setAcceptAnyProxy(boolean acceptAnyProxy) {
        this.acceptAnyProxy = acceptAnyProxy;
        if (proxyTicketValidator != null) {
            proxyTicketValidator.setAcceptAnyProxy(acceptAnyProxy);
        }
    }

    public void setProxyGrantingTicketStorage(
            ProxyGrantingTicketStorage proxyGrantingTicketStorage) {
        this.proxyGrantingTicketStorage = proxyGrantingTicketStorage;
        if (proxyTicketValidator != null) {
            proxyTicketValidator.setProxyGrantingTicketStorage(proxyGrantingTicketStorage);
        }
    }
}
