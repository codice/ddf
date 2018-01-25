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
package org.codice.ddf.security.idp.binding.redirect;

import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.ValidationException;
import ddf.security.samlp.impl.EntityInformation;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.security.idp.binding.api.Validator;
import org.codice.ddf.security.idp.binding.api.impl.ValidatorImpl;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedirectValidator extends ValidatorImpl implements Validator {

  private static final Logger LOGGER = LoggerFactory.getLogger(RedirectValidator.class);

  public RedirectValidator(
      SystemCrypto systemCrypto, Map<String, EntityInformation> serviceProviders) {
    super(systemCrypto, serviceProviders);
  }

  @Override
  public void validateAuthnRequest(
      AuthnRequest authnRequest,
      String samlRequest,
      String relayState,
      String signatureAlgorithm,
      String signature,
      boolean strictSignature)
      throws SimpleSign.SignatureException, ValidationException {
    LOGGER.debug("Validating AuthnRequest required attributes and signature");
    if (strictSignature) {
      if (!StringUtils.isEmpty(signature) && !StringUtils.isEmpty(signatureAlgorithm)) {
        StringBuilder signedParts;
        try {
          signedParts =
              new StringBuilder("SAMLRequest=")
                  .append(URLEncoder.encode(samlRequest, StandardCharsets.UTF_8.name()));
          if (relayState != null) {
            signedParts.append("&RelayState=").append(relayState);
          }
          signedParts
              .append("&SigAlg=")
              .append(URLEncoder.encode(signatureAlgorithm, StandardCharsets.UTF_8.name()));
        } catch (UnsupportedEncodingException e) {
          throw new SimpleSign.SignatureException("Unable to construct signed query parts.", e);
        }
        EntityInformation entityInformation =
            getServiceProviders().get(authnRequest.getIssuer().getValue());
        if (entityInformation == null) {
          throw new ValidationException(
              String.format("Unable to find metadata for %s", authnRequest.getIssuer().getValue()));
        }

        String signingCertificate = entityInformation.getSigningCertificate();
        if (signingCertificate == null) {
          throw new ValidationException(
              "Unable to find signing certificate in metadata. Please check metadata.");
        }
        boolean result =
            getSimpleSign()
                .validateSignature(signedParts.toString(), signature, signingCertificate);
        if (!result) {
          throw new ValidationException("Signature verification failed for redirect binding.");
        }
        checkDestination(authnRequest);
      } else {
        throw new SimpleSign.SignatureException("No signature present for AuthnRequest.");
      }
    }

    super.validateAuthnRequest(
        authnRequest, samlRequest, relayState, signatureAlgorithm, signature, strictSignature);
  }

  @Override
  public void validateRelayState(String relayState) {
    if (relayState != null) {
      try {
        relayState = URLDecoder.decode(relayState, StandardCharsets.UTF_8.name());
      } catch (UnsupportedEncodingException e) {
        LOGGER.info("Unable to URL decode relay state, it may already be decoded.", e);
      }
    }

    super.validateRelayState(relayState);
  }
}
