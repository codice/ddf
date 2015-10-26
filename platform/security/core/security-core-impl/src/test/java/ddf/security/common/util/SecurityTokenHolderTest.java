/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.common.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.nullValue;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.junit.Test;

public class SecurityTokenHolderTest {

    @Test
    public void testRetrieveSecurityTokenByRealm() {
        // given
        SecurityTokenHolder securityTokenHolder = new SecurityTokenHolder();
        SecurityToken securityTokenOne = new SecurityToken();
        SecurityToken securityTokenTwo = new SecurityToken();
        String realmOne = "realmOne";
        String realmTwo = "realmTwo";

        // when
        securityTokenHolder.addSecurityToken(securityTokenOne, realmOne);
        securityTokenHolder.addSecurityToken(securityTokenTwo, realmTwo);

        // then
        assertThat(securityTokenOne, is(equalTo(securityTokenHolder.getSecurityToken(realmOne))));
        assertThat(securityTokenTwo, is(equalTo(securityTokenHolder.getSecurityToken(realmTwo))));
    }

    @Test
    public void testRemoveSecurityTokenByRealm() {
        // given
        SecurityTokenHolder securityTokenHolder = new SecurityTokenHolder();
        SecurityToken securityToken = new SecurityToken();
        String realm = "realm";
        securityTokenHolder.addSecurityToken(securityToken, realm);

        // when
        securityTokenHolder.removeByRealm(realm);

        // then
        assertThat(securityTokenHolder.getSecurityToken(realm), is(nullValue()));
    }

    @Test
    public void testRemoveAllSecurityTokens() {
        // given
        SecurityTokenHolder securityTokenHolder = new SecurityTokenHolder();
        SecurityToken securityTokenOne = new SecurityToken();
        SecurityToken securityTokenTwo = new SecurityToken();
        String realmOne = "realmOne";
        String realmTwo = "realmTwo";
        securityTokenHolder.addSecurityToken(securityTokenOne, realmOne);
        securityTokenHolder.addSecurityToken(securityTokenTwo, realmTwo);

        // when
        securityTokenHolder.removeAllRealms();

        // then
        assertThat(securityTokenHolder.getSecurityToken(realmOne), is(nullValue()));
        assertThat(securityTokenHolder.getSecurityToken(realmTwo), is(nullValue()));
    }
}
