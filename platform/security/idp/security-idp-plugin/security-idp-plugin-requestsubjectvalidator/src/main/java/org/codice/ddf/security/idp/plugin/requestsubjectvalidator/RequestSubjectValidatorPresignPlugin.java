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
package org.codice.ddf.security.idp.plugin.requestsubjectvalidator;

import ddf.security.samlp.SamlProtocol.Binding;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.security.idp.plugin.SamlPresignPlugin;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.common.SAMLRuntimeException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;

/**
 * This plugin is responsible for verifying that the {@code Subject} of the received {@code
 * AuthnRequest} refers to the same principal as the {@code Subject} of the {@code Response}.
 */
public class RequestSubjectValidatorPresignPlugin implements SamlPresignPlugin {

  @Override
  public void processPresign(
      Response response,
      AuthnRequest authnRequest,
      List<String> spMetadata,
      Set<Binding> supportedBindings) {

    Subject authnRequestSubject = authnRequest.getSubject();
    if (authnRequestSubject == null) {
      return;
    }

    if (!authnRequestSubject.getSubjectConfirmations().isEmpty()) {
      throw new SAMLRuntimeException(
          "The AuthnRequest contained a Subject with 1 or more SubjectConfirmations.");
    }

    NameID authnRequestNameId = authnRequestSubject.getNameID();
    if (authnRequestNameId == null) {
      return;
    }

    String authnRequestId = authnRequestNameId.getValue();
    if (StringUtils.isBlank(authnRequestId)) {
      return;
    }

    if (idsFromResponseAsStream(response).noneMatch(authnRequestId::equals)) {
      throw new SAMLRuntimeException(
          String.format(
              "The AuthnRequest's Subject (with the NameID of %s) could not be matched against any "
                  + "identifiers in the resulting Response (with the primary NameID of %s).",
              authnRequestId, nameIdsFromResponseAsStream(response).findFirst().orElse(null)));
    }
  }

  /** Returns all the identifiers that were returned on the response, i.e. username, email, ect. */
  private Stream<String> idsFromResponseAsStream(Response response) {
    return Stream.concat(
        nameIdsFromResponseAsStream(response), attributeIdsFromResponseAsStream(response));
  }

  private Stream<String> nameIdsFromResponseAsStream(Response response) {
    return response
        .getAssertions()
        .stream()
        .map(Assertion::getSubject)
        .map(Subject::getNameID)
        .filter(Objects::nonNull)
        .map(NameID::getValue)
        .filter(Objects::nonNull)
        .filter(StringUtils::isNotBlank);
  }

  private Stream<String> attributeIdsFromResponseAsStream(Response response) {
    return response
        .getAssertions()
        .stream()
        .flatMap(assertion -> assertion.getAttributeStatements().stream())
        .flatMap(statement -> statement.getAttributes().stream())
        .flatMap(attribute -> attribute.getAttributeValues().stream())
        .filter(XSString.class::isInstance)
        .map(XSString.class::cast)
        .map(XSString::getValue)
        .filter(StringUtils::isNotBlank);
  }
}
