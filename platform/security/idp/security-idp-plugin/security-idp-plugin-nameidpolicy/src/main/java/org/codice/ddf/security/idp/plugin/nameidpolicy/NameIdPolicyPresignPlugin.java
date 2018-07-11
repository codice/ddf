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
package org.codice.ddf.security.idp.plugin.nameidpolicy;

import ddf.security.samlp.SamlProtocol.Binding;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.security.idp.plugin.SamlPresignPlugin;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin is responsible for setting the NameID Format/SPNameQualifier on the outgoing Response
 * according to the values found in the NameIDPolicy of the passed in AuthnRequest.
 */
public class NameIdPolicyPresignPlugin implements SamlPresignPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(NameIdPolicyPresignPlugin.class);

  private static final String EMAIL_ATTRIBUTE_NAME =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";

  @Override
  public void processPresign(
      Response response,
      AuthnRequest authnRequest,
      List<String> spMetadata,
      Set<Binding> supportedBindings) {
    NameIDPolicy nameIdPolicy = authnRequest.getNameIDPolicy();

    if (nameIdPolicy == null
        || (StringUtils.isEmpty(nameIdPolicy.getFormat())
            && StringUtils.isEmpty(nameIdPolicy.getSPNameQualifier()))) {
      return;
    }

    response
        .getAssertions()
        .stream()
        .map(assertion -> assertion.getSubject().getNameID())
        .forEach(
            assertionNameId ->
                setAssertionNameIdQualifierAndFormat(
                    assertionNameId,
                    nameIdPolicy.getFormat(),
                    nameIdPolicy.getSPNameQualifier(),
                    response));
  }

  private void setAssertionNameIdQualifierAndFormat(
      NameID assertionNameId,
      @Nullable String nameIdFormatPolicy,
      @Nullable String spNameQualifierPolicy,
      Response response) {

    if (StringUtils.isNotEmpty(spNameQualifierPolicy)) {
      assertionNameId.setSPNameQualifier(spNameQualifierPolicy);
    }

    if (StringUtils.isNotEmpty(nameIdFormatPolicy)) {
      switch (nameIdFormatPolicy) {
          // supported NameIDFormats
        case NameID.UNSPECIFIED:
          return; // avoid changing the Format later
        case NameID.PERSISTENT:
          // TODO DDF-3965
          break;
        case NameID.TRANSIENT:
          // TODO DDF-3965
          break;

          // partially supported NameIDFormats
        case NameID.X509_SUBJECT:
          LOGGER.warn("Specifying the \"X509Subject\" NameIDPolicy Format is not fully supported.");
          if (!NameID.X509_SUBJECT.equals(assertionNameId.getFormat())) {
            throw new UnsupportedOperationException(
                "The \"X509Subject\" NameID could not be retrieved.");
          }
          break;
        case NameID.EMAIL:
          LOGGER.warn("Specifying the \"Email\" NameIDPolicy Format is not fully supported.");
          assertionNameId.setValue(resolveEmail(response));
          break;

          // not supported NameIDFormats
        case NameID.WIN_DOMAIN_QUALIFIED:
        case NameID.KERBEROS:
        case NameID.ENTITY:
        default:
          throw new UnsupportedOperationException(
              String.format(
                  "The NameIDPolicy Format of value %s is not supported.", nameIdFormatPolicy));
      }
      assertionNameId.setFormat(nameIdFormatPolicy);
    }
  }

  private String resolveEmail(Response response) {
    return response
        .getAssertions()
        .stream()
        .flatMap(assertion -> assertion.getAttributeStatements().stream())
        .flatMap(statement -> statement.getAttributes().stream())
        .filter(
            attribute ->
                EMAIL_ATTRIBUTE_NAME.equals(attribute.getName())
                    || NameID.EMAIL.equals(attribute.getNameFormat()))
        .flatMap(attribute -> attribute.getAttributeValues().stream())
        .filter(attributeValue -> attributeValue instanceof XSString)
        .map(attributeValue -> (XSString) attributeValue)
        .map(XSString::getValue)
        .filter(StringUtils::isNotBlank)
        .findFirst()
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    "The \"Email\" NameID could not be retrieved for this principal."));
  }
}
