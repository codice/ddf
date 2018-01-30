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
package org.codice.ddf.security.idp.plugin.audiencerestriction;

import ddf.security.samlp.SamlProtocol;
import java.util.List;
import java.util.Set;
import org.codice.ddf.security.idp.plugin.SamlPresignPlugin;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Audience;
import org.opensaml.saml.saml2.core.AudienceRestriction;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.impl.AudienceBuilder;
import org.opensaml.saml.saml2.core.impl.AudienceRestrictionBuilder;

/**
 * Pre-sign plugin that ensures that "Each bearer assertion must contain an {@code
 * <audienceRestriction>} including the service provider's unique identifier as an {@code
 * <audience>}" as per section 4.1.4.2 of the SAML Spec.
 *
 * @see <a
 *     href="https://www.oasis-open.org/committees/download.php/56783/sstc-saml-profiles-errata-2.0-wd-07-diff.pdf">
 *     Profiles for the OASIS Security Assertion Markup Language (SAML) V2.0 – Errata Composite</a>
 */
public class AudienceRestrictionPresignPlugin implements SamlPresignPlugin {

  @Override
  public void processPresign(
      Response response,
      AuthnRequest authnRequest,
      List<String> spMetadata,
      Set<SamlProtocol.Binding> supportedBindings) {
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

  private Audience buildAudience(AudienceBuilder audienceBuilder, String uri) {
    Audience audience = audienceBuilder.buildObject();
    audience.setAudienceURI(uri);
    return audience;
  }
}
