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
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.core.impl.SubjectConfirmationDataBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    final DateTime notOnOrAfter = DateTime.now().plusMinutes(30);

    String acsUrl = getAssertionConsumerServiceURL(authnRequest, spMetadata, supportedBindings);

    for (Assertion assertion : response.getAssertions()) {
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
