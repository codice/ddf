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
package org.codice.ddf.security.idp.plugin.subjectconfirmation;

import ddf.security.samlp.SamlProtocol.Binding;
import ddf.security.samlp.impl.EntityInformation;
import ddf.security.samlp.impl.EntityInformation.ServiceInfo;
import ddf.security.samlp.impl.SPMetadataParser;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.codice.ddf.security.idp.plugin.SamlPresignPlugin;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.core.impl.SubjectConfirmationDataBuilder;

/**
 * Pre-sign plugin that ensures that "At lease<em>[sic]</em> one bearer {@code
 * <SubjectConfirmation>} element MUST contain a {@code <SubjectConfirmationData>} element that
 * itself MUST contain a {@code Recipient} attribute containing the service provider's assertion
 * consumer service URL and a {@code NotOnOrAfter} attribute that limits the window during which the
 * assertion can be confirmed by the relying party." as per section 4.1.4.2 of the SAML Spec.
 *
 * @see <a
 *     href="https://www.oasis-open.org/committees/download.php/56783/sstc-saml-profiles-errata-2.0-wd-07-diff.pdf">
 *     Profiles for the OASIS Security Assertion Markup Language (SAML) V2.0 – Errata Composite</a>
 */
public class SubjectConfirmationPresignPlugin implements SamlPresignPlugin {

  @Override
  public void processPresign(
      Response response,
      AuthnRequest authnRequest,
      List<String> spMetadata,
      Set<Binding> supportedBindings) {

    String inResponseTo = response.getInResponseTo();

    String acsUrl = getAssertionConsumerServiceURL(authnRequest, spMetadata, supportedBindings);

    for (Assertion assertion : response.getAssertions()) {
      DateTime notOnOrAfter = assertion.getConditions().getNotOnOrAfter();
      assertion
          .getSubject()
          .getSubjectConfirmations()
          .forEach(sc -> setConfirmationData(sc, notOnOrAfter, acsUrl, inResponseTo));
    }
  }

  private void setConfirmationData(
      SubjectConfirmation subjectConfirmation,
      DateTime notOnOrAfter,
      String acsUrl,
      String inResponseTo) {
    if (subjectConfirmation.getSubjectConfirmationData() == null) {
      subjectConfirmation.setSubjectConfirmationData(
          new SubjectConfirmationDataBuilder().buildObject());
    }

    SubjectConfirmationData scd = subjectConfirmation.getSubjectConfirmationData();
    scd.setNotOnOrAfter(notOnOrAfter);
    scd.setRecipient(acsUrl);
    scd.setInResponseTo(inResponseTo);
  }

  private String getAssertionConsumerServiceURL(
      AuthnRequest authnRequest, List<String> spMetadata, Set<Binding> supportedBindings) {
    final Map<String, EntityInformation> serviceProviders =
        SPMetadataParser.parse(spMetadata, supportedBindings);

    return Optional.of(serviceProviders)
        .map(sp -> sp.get(authnRequest.getIssuer().getValue()))
        .map(
            ei ->
                ei.getAssertionConsumerService(
                    authnRequest, null, authnRequest.getAssertionConsumerServiceIndex()))
        .map(ServiceInfo::getUrl)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "No valid AssertionConsumerServiceURL available for given AuthnRequest."));
  }
}
