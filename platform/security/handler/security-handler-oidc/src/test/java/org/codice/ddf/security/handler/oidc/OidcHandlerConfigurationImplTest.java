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
package org.codice.ddf.security.handler.oidc;

import static org.codice.ddf.security.handler.oidc.OidcHandlerConfigurationImpl.DEFAULT_CALLBACK_URL;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.util.HashMap;
import java.util.Map;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.pac4j.oidc.client.AzureAdClient;
import org.pac4j.oidc.client.GoogleOidcClient;
import org.pac4j.oidc.client.KeycloakOidcClient;
import org.pac4j.oidc.client.OidcClient;
import org.pac4j.oidc.config.AzureAdOidcConfiguration;
import org.pac4j.oidc.config.KeycloakOidcConfiguration;
import org.pac4j.oidc.config.OidcConfiguration;

@RunWith(MockitoJUnitRunner.class)
public class OidcHandlerConfigurationImplTest {

  private static OidcHandlerConfigurationImpl handlerConfiguration;

  @BeforeClass
  public static void setupClass() {
    handlerConfiguration = new OidcHandlerConfigurationImpl();
  }

  @Test
  public void testUpdateNullProperties() {
    handlerConfiguration.setProperties(null);
  }

  @Test
  public void testUpdateEmptyProperties() {
    Map<String, Object> properties = spy(new HashMap<>());
    handlerConfiguration.setProperties(properties);
    verify(properties, never()).getOrDefault(anyString(), any());
  }

  @Test
  public void testCreateOidcConfigurationKeycloak() {
    OidcConfiguration oidcConfiguration =
        handlerConfiguration.createOidcConfiguration("Keycloak", "master", "https://base/uri");
    assertTrue(oidcConfiguration instanceof KeycloakOidcConfiguration);
    assertThat(((KeycloakOidcConfiguration) oidcConfiguration).getRealm(), is("master"));
    assertThat(
        ((KeycloakOidcConfiguration) oidcConfiguration).getBaseUri(), is("https://base/uri"));
  }

  @Test
  public void testCreateOidcConfigurationAzure() {
    OidcConfiguration oidcConfiguration =
        handlerConfiguration.createOidcConfiguration("Azure", "master", "https://base/uri");
    assertTrue(oidcConfiguration instanceof AzureAdOidcConfiguration);
    assertThat(((AzureAdOidcConfiguration) oidcConfiguration).getTenant(), is("master"));
  }

  @Test
  public void testCreateOidcClientKeycloak() {
    OidcConfiguration oidcConfiguration = mock(KeycloakOidcConfiguration.class);
    OidcClient oidcClient =
        handlerConfiguration.createOidcClient("Keycloak", oidcConfiguration, DEFAULT_CALLBACK_URL);
    assertTrue(oidcClient instanceof KeycloakOidcClient);
  }

  @Test
  public void testCreateOidcClientAzure() {
    OidcConfiguration oidcConfiguration = mock(AzureAdOidcConfiguration.class);
    OidcClient oidcClient =
        handlerConfiguration.createOidcClient("Azure", oidcConfiguration, DEFAULT_CALLBACK_URL);
    assertTrue(oidcClient instanceof AzureAdClient);
  }

  @Test
  public void testCreateOidcClientGoogle() {
    OidcConfiguration oidcConfiguration = mock(OidcConfiguration.class);
    OidcClient oidcClient =
        handlerConfiguration.createOidcClient("Google", oidcConfiguration, DEFAULT_CALLBACK_URL);
    assertTrue(oidcClient instanceof GoogleOidcClient);
  }
}
