/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.idp.binding.api;

import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.ValidationException;
import org.opensaml.saml.saml2.core.AuthnRequest;

/** Validates an AuthnRequest */
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
  void validateAuthnRequest(
      AuthnRequest authnRequest,
      String samlRequest,
      String relayState,
      String signatureAlgorithm,
      String signature,
      boolean strictSignature)
      throws SimpleSign.SignatureException, ValidationException;

  /**
   * @param relayState
   * @param strictRelayState If true, disallows relay states greater than 80 bytes
   */
  void validateRelayState(String relayState, boolean strictRelayState);
}
