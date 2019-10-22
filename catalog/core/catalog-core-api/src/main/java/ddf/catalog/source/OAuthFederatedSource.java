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
package ddf.catalog.source;

public interface OAuthFederatedSource extends FederatedSource {

  /** @return the authentication type to use when federating */
  String getAuthenticationType();

  /** @return the discovery url of the OAuth provider where the metadata is hosted */
  String getOauthDiscoveryUrl();

  /** @return the client ID registered with the OAuth provider */
  String getOauthClientId();

  /** @return the client secret given by the OAuth provider */
  String getOauthClientSecret();

  /** @return the OAuth flow to use when federating */
  String getOauthFlow();
}
