/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.idp.binding.api;

import org.opensaml.saml2.core.AuthnRequest;
import org.opensaml.xml.validation.ValidationException;

import ddf.security.samlp.SimpleSign;

/**
 * Validates an AuthnRequest
 */
public interface Validator {

    /**
     * Validates the given AuthnRequest using the provided signatureAlgorithm and signature, if
     * applicable.
     *
     * @param authnRequest
     * @param samlRequest
     * @param relayState
     * @param signatureAlgorithm
     * @param signature
     * @param strictSignature
     * @throws SimpleSign.SignatureException
     * @throws ValidationException
     */
    void validateAuthnRequest(AuthnRequest authnRequest, String samlRequest, String relayState,
            String signatureAlgorithm, String signature, boolean strictSignature)
            throws SimpleSign.SignatureException, ValidationException;

    /**
     * Validates the given relayState.
     *
     * @param relayState
     */
    void validateRelayState(String relayState);
}
