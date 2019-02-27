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

import ddf.action.Action;
import ddf.security.encryption.EncryptionService;
import java.net.URLEncoder;
import java.util.HashMap;
import junit.framework.Assert;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;

public class IdpLogoutActionProviderTest {

  IdpLogoutActionProvider idpLogoutActionProvider;

  private EncryptionService encryptionService;

  String nameIdTime = "nameId\n" + System.currentTimeMillis();

  @Before
  public void setup() {
    encryptionService = mock(EncryptionService.class);
    when(encryptionService.encrypt(any(String.class))).thenReturn(nameIdTime);

    idpLogoutActionProvider = new IdpLogoutActionProvider();
    idpLogoutActionProvider.setEncryptionService(encryptionService);
  }

  @Test
  public void testGetAction() throws Exception {
    Subject subject = mock(Subject.class);
    HashMap map = new HashMap();
    map.put("idp", subject);
    Action action = idpLogoutActionProvider.getAction(map);
    Assert.assertTrue(
        "Expected the encrypted nameId and time",
        action.getUrl().getQuery().contains(URLEncoder.encode(nameIdTime)));
  }

  @Test
  public void testGetActionFailure() throws Exception {
    Object notsubject = new Object();
    HashMap map = new HashMap();
    map.put("idp", notsubject);
    when(encryptionService.encrypt(any(String.class))).thenReturn(nameIdTime);
    Action action = idpLogoutActionProvider.getAction(map);
    Assert.assertNull("Expected the url to be null", action.getUrl());
  }
}
