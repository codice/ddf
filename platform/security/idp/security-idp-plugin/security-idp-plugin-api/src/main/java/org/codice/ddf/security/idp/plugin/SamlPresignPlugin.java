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
package org.codice.ddf.security.idp.plugin;

import ddf.security.samlp.SamlProtocol.Binding;
import java.util.List;
import java.util.Set;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.opensaml.saml.saml2.core.Response;

/**
 * A SamlPresignPlugin is invoked on each authN request to modify the outgoing SAML response.
 *
 * <p><b> This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library. </b>
 */
public interface SamlPresignPlugin {

  /**
   * Modifies SAML response prior to re-signing and returning.
   *
   * @param response the SAML response to modify
   * @param authnRequest the associated authentication request
   * @param spMetadata list of Strings holding the Service Provider(s) metadata
   * @param supportedBindings set of supported SAML binding types, e.g. Post/Redirect/SOAP
   */
  void processPresign(
      Response response,
      AuthnRequest authnRequest,
      List<String> spMetadata,
      Set<Binding> supportedBindings);
}
