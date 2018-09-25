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
package org.codice.ddf.security.idp.plugin.namequalifier;

import ddf.security.samlp.SamlProtocol;
import java.util.List;
import java.util.Set;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.security.idp.plugin.SamlPresignPlugin;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.Response;

/**
 * Pre-sign plugin that ensures that "Each bearer assertion must contain an {@code <nameQualifier>}"
 * as per section 8.3.7 of the SAML Spec.
 *
 * @see <a
 *     href="https://www.oasis-open.org/committees/download.php/56783/sstc-saml-profiles-errata-2.0-wd-07-diff.pdf">
 *     Profiles for the OASIS Security Assertion Markup Language (SAML) V2.0 – Errata Composite</a>
 */
public class NameQualifierPresignPlugin implements SamlPresignPlugin {

  private static final String NAME_QUALIFIER =
      SystemBaseUrl.EXTERNAL.constructUrl("idp/login", true);

  private static final String PERSISTENT = NameIDType.PERSISTENT;

  @Override
  public void processPresign(
      Response response,
      AuthnRequest authnRequest,
      List<String> spMetadata,
      Set<SamlProtocol.Binding> supportedBindings) {

    if (response.getIssuer() != null) {

      setNameQualifierIfPersistent(response.getIssuer());
    }

    for (Assertion assertion : response.getAssertions()) {

      if (assertion.getSubject() != null && assertion.getSubject().getNameID() != null) {

        setNameQualifierIfPersistent(assertion.getSubject().getNameID());
      }

      setNameQualifierIfPersistent(assertion.getIssuer());
    }
  }

  private void setNameQualifierIfPersistent(NameIDType nameIDType) {

    if (nameIDType.getFormat() != null && nameIDType.getFormat().equalsIgnoreCase(PERSISTENT)) {

      nameIDType.setNameQualifier(NAME_QUALIFIER);
    }
  }
}
