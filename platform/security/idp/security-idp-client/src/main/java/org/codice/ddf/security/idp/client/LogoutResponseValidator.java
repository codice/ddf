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
package org.codice.ddf.security.idp.client;

import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.samlp.SimpleSign;

//TODO move to the logoutservice?
public class LogoutResponseValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(LogoutResponseValidator.class);

    private final SimpleSign simpleSign;

    public LogoutResponseValidator(SimpleSign simpleSign) {
        this.simpleSign = simpleSign;
    }

    @Override
    public void validate(XMLObject xmlObject) throws ValidationException {
        if (!(xmlObject instanceof LogoutResponse)) {
            throw new ValidationException("Invalid LogoutRequest response XML.");
        }
        //TODO do more validation..

        LogoutResponse logoutResponse = (LogoutResponse) xmlObject;
        String status = logoutResponse.getStatus()
                .getStatusCode()
                .getValue();
        if (logoutResponse.getSignature() != null) {
            try {
                simpleSign.validateSignature(logoutResponse.getSignature(),
                        logoutResponse.getDOM()
                                .getOwnerDocument());
            } catch (SimpleSign.SignatureException e) {
                throw new ValidationException("Invalid or untrusted signature.");
            }
        }
        LOGGER.debug("Request status was [{}] for id [{}] from issuer [{}]",
                status,
                logoutResponse.getID(),
                logoutResponse.getIssuer()
                        .getValue());
    }
}