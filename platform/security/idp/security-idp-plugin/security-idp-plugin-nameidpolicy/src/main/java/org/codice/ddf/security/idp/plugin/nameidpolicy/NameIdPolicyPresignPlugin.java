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

import ddf.security.SubjectUtils;
import ddf.security.samlp.SamlProtocol.Binding;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.security.idp.plugin.SamlPresignPlugin;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDPolicy;
import org.opensaml.saml.saml2.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This plugin is responsible for setting the <@code>NameID</@code>'s <@code>Format</@code> &
 * <@code>SPNameQualifier</@code> on the outgoing <@code>Response</@code>. It does this by parsing
 * the <@code>NameIDPolicy</@code> of the passed in <@code>AuthnRequest</@code>.
 *
 * <p>See section 3.4.1.1 of the SAML Core specification for more information.
 */
public class NameIdPolicyPresignPlugin implements SamlPresignPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(NameIdPolicyPresignPlugin.class);

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

    // the Assertion's NameID Format is set to the NameIDPolicy Format after the switch statement
    if (StringUtils.isNotEmpty(nameIdFormatPolicy)) {
      switch (nameIdFormatPolicy) {
          // supported NameIDFormats
        case NameID.UNSPECIFIED:
          return; // avoid changing the Format later
        case NameID.PERSISTENT:
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
          assertionNameId.setValue(resolveEmail(response, assertionNameId));
          break;

          // not supported NameIDFormats
        case NameID.TRANSIENT:
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

  /**
   * Attempts to find an email identifier in a given <@code>Response</@code>. More specifically,
   * this method looks in a <@code>Response</@code>'s <@code>AttributeStatements</@code> for the
   * email identifier. Throws an error if not found.
   *
   * @param response <@code>Response</@code> object
   * @return Email identifier
   * @throws UnsupportedOperationException if an email identifier could not be found
   */
  private String resolveEmail(Response response, NameID assertionNameId)
      throws UnsupportedOperationException {
    Predicate<Attribute> attributeHasEmailIdentifier =
        attribute ->
            SubjectUtils.EMAIL_ADDRESS_CLAIM_URI.equals(attribute.getName())
                || NameID.EMAIL.equals(attribute.getNameFormat());

    return extractAssertionAttributes(response)
        .filter(attributeHasEmailIdentifier)
        .flatMap(attribute -> attribute.getAttributeValues().stream())
        .filter(attributeValue -> attributeValue instanceof XSString)
        .map(attributeValue -> (XSString) attributeValue)
        .map(XSString::getValue)
        .filter(StringUtils::isNotBlank)
        .findFirst()
        .orElseThrow(
            () ->
                new UnsupportedOperationException(
                    String.format(
                        "The \"Email\" NameID could not be retrieved for the principal %s.",
                        assertionNameId.getValue())));
  }

  private Stream<Attribute> extractAssertionAttributes(Response response) {
    return response
        .getAssertions()
        .stream()
        .flatMap(assertion -> assertion.getAttributeStatements().stream())
        .flatMap(statement -> statement.getAttributes().stream());
  }
}
