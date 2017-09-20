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
package org.codice.ddf.security.idp.client;

import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.ValidationException;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.StatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthnResponseValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(AuthnResponseValidator.class);

  private final SimpleSign simpleSign;

  public AuthnResponseValidator(SimpleSign simpleSign) {
    this.simpleSign = simpleSign;
  }

  public void validate(XMLObject xmlObject) throws ValidationException {
    if (!(xmlObject instanceof Response)) {
      throw new ValidationException("Invalid AuthN response XML.");
    }

    Response authnResponse = (Response) xmlObject;

    String status = authnResponse.getStatus().getStatusCode().getValue();
    if (!StatusCode.SUCCESS.equals(status)) {
      throw new ValidationException("AuthN request was unsuccessful.  Received status: " + status);
    }

    if (authnResponse.getAssertions().size() < 1) {
      throw new ValidationException("Assertion missing in AuthN response.");
    }

    if (authnResponse.getAssertions().size() > 1) {
      LOGGER.info(
          "Received multiple assertions in AuthN response.  Only using the first assertion.");
    }

    if (authnResponse.getSignature() != null) {
      try {
        simpleSign.validateSignature(
            authnResponse.getSignature(), authnResponse.getDOM().getOwnerDocument());
      } catch (SimpleSign.SignatureException e) {
        throw new ValidationException("Invalid or untrusted signature.");
      }
    }
  }
}
