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
package ddf.security.common;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNull;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.junit.Test;

public class SecurityTokenHolderTest {

  @Test
  public void testRetrieveSecurityTokens() {
    // given
    SecurityTokenHolder securityTokenHolder = new SecurityTokenHolder();
    SecurityToken securityToken = new SecurityToken();

    // when
    securityTokenHolder.setSecurityToken(securityToken);

    // then
    assertThat(securityTokenHolder.getSecurityToken(), is(securityToken));
  }

  @Test
  public void testSettingMultipleSecurityTokens() {
    // given
    SecurityTokenHolder securityTokenHolder = new SecurityTokenHolder();
    SecurityToken securityTokenOne = new SecurityToken();
    SecurityToken securityTokenTwo = new SecurityToken();

    // when
    securityTokenHolder.setSecurityToken(securityTokenOne);
    securityTokenHolder.setSecurityToken(securityTokenTwo);

    // then
    assertThat(securityTokenHolder.getSecurityToken(), is(securityTokenTwo));
  }

  @Test
  public void testRemoveSecurityToken() {
    // given
    SecurityTokenHolder securityTokenHolder = new SecurityTokenHolder();
    SecurityToken securityToken = new SecurityToken();
    securityTokenHolder.setSecurityToken(securityToken);

    // when
    securityTokenHolder.remove();

    // then
    assertNull(securityTokenHolder.getSecurityToken());
  }
}
