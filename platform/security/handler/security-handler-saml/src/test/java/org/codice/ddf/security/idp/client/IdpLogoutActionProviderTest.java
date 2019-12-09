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
package org.codice.ddf.security.idp.client;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import ddf.action.Action;
import ddf.security.SecurityConstants;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.encryption.EncryptionService;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Collections;
import java.util.List;
import junit.framework.Assert;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;

public class IdpLogoutActionProviderTest {

  private IdpLogoutActionProvider idpLogoutActionProvider;

  private EncryptionService encryptionService;

  private String nameIdTime = "nameId\n" + System.currentTimeMillis();

  @Before
  public void setup() {
    encryptionService = mock(EncryptionService.class);
    when(encryptionService.encrypt(any(String.class))).thenReturn(nameIdTime);

    idpLogoutActionProvider = new IdpLogoutActionProvider();
    idpLogoutActionProvider.setEncryptionService(encryptionService);
  }

  @Test
  public void testGetAction() throws Exception {
    SecurityAssertion assertion = mock(SecurityAssertion.class);
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("name");

    when(assertion.getPrincipal()).thenReturn(principal);
    when(assertion.getTokenType())
        .thenReturn("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0");

    PrincipalCollection principalCollection = mock(PrincipalCollection.class);
    List<SecurityAssertion> securityAssertions = Collections.singletonList(assertion);
    when(principalCollection.byType(SecurityAssertion.class)).thenReturn(securityAssertions);

    Subject subject = mock(Subject.class);
    when(subject.getPrincipals()).thenReturn(principalCollection);

    Action action =
        idpLogoutActionProvider.getAction(
            ImmutableMap.of(SecurityConstants.SECURITY_SUBJECT, subject));
    Assert.assertTrue(
        "Expected the encrypted nameId and time",
        action.getUrl().getQuery().contains(URLEncoder.encode(nameIdTime)));
  }

  @Test
  public void testGetActionFailure() throws Exception {
    Object notsubject = new Object();
    when(encryptionService.encrypt(any(String.class))).thenReturn(nameIdTime);
    Action action = idpLogoutActionProvider.getAction(notsubject);
    Assert.assertNull("Expected the url to be null", action);
  }
}
