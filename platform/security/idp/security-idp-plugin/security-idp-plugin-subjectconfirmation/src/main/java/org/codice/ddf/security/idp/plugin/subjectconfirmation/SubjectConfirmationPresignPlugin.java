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

import static java.util.Objects.nonNull;

import com.google.common.collect.Maps;
import ddf.security.samlp.MetadataConfigurationParser;
import ddf.security.samlp.SamlProtocol.Binding;
import ddf.security.samlp.impl.EntityInformation;
import ddf.security.samlp.impl.EntityInformation.ServiceInfo;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.codice.ddf.security.idp.plugin.SamlPresignPlugin;
import org.joda.time.DateTime;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.core.impl.AudienceBuilder;
import org.opensaml.saml.saml2.core.impl.AudienceRestrictionBuilder;
import org.opensaml.saml.saml2.core.impl.SubjectConfirmationDataBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
  private static final Logger LOGGER =
      LoggerFactory.getLogger(SubjectConfirmationPresignPlugin.class);

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

    // TODO: 12/11/17 DDF-3494 extract to new plugin
    addAudiences(authnRequest, response);
  }

  private Audience buildAudience(AudienceBuilder audienceBuilder, String uri) {
    Audience audience = audienceBuilder.buildObject();
    audience.setAudienceURI(uri);
    return audience;
  }

  private void addAudiences(AuthnRequest authnRequest, Response response) {
    AudienceBuilder audienceBuilder = new AudienceBuilder();
    AudienceRestrictionBuilder audienceRestrictionBuilder = new AudienceRestrictionBuilder();

    // According to the SAML spec, on an AuthnRequest, "[t]he <Issuer> element MUST be present and
    // MUST contain the unique identifier of the requesting service provider".
    Audience audience = buildAudience(audienceBuilder, authnRequest.getIssuer().getValue());

    for (Assertion assertion : response.getAssertions()) {
      List<AudienceRestriction> audienceRestrictions =
          assertion.getConditions().getAudienceRestrictions();
      if (audienceRestrictions.isEmpty()) {
        AudienceRestriction audienceRestriction = audienceRestrictionBuilder.buildObject();
        audienceRestrictions.add(audienceRestriction);
      }

      for (AudienceRestriction restriction : audienceRestrictions) {
        restriction.getAudiences().add(audience);
      }
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
        parseServiceProviderMetadata(spMetadata, supportedBindings);

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

  // TODO: 12/11/17 Extract to service DDF-3493
  private Map<String, EntityInformation> parseServiceProviderMetadata(
      List<String> spMetadata, Set<Binding> bindingSet) {
    if (spMetadata == null) {
      return Collections.emptyMap();
    }

    Map<String, EntityInformation> spMap = new ConcurrentHashMap<>();
    try {
      MetadataConfigurationParser metadataConfigurationParser =
          new MetadataConfigurationParser(
              spMetadata,
              ed -> {
                EntityInformation entityInfo =
                    new EntityInformation.Builder(ed, bindingSet).build();
                if (entityInfo != null) {
                  spMap.put(ed.getEntityID(), entityInfo);
                }
              });

      spMap.putAll(
          metadataConfigurationParser
              .getEntryDescriptions()
              .entrySet()
              .stream()
              .map(
                  e ->
                      Maps.immutableEntry(
                          e.getKey(),
                          new EntityInformation.Builder(e.getValue(), bindingSet).build()))
              .filter(e -> nonNull(e.getValue()))
              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

    } catch (IOException e) {
      LOGGER.warn(
          "Unable to parse SP metadata configuration. Check the configuration for SP metadata.", e);
    }

    return spMap;
  }
}
