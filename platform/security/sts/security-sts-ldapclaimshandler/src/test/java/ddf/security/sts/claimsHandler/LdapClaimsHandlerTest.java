/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package ddf.security.sts.claimsHandler;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LdapException;

import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@RunWith(PowerMockRunner.class)
@PrepareForTest(LDAPConnectionFactory.class)

public class LdapClaimsHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapClaimsHandlerTest.class);

    @Test
    public void testUnsuccessfulConnectionBind() {
        LDAPConnectionFactory mockedConnectionFactory = PowerMockito
                .mock(LDAPConnectionFactory.class);
        BindResult mockedBindResult = mock(BindResult.class);
        when(mockedBindResult.isSuccess()).thenReturn(false);
        Connection mockedConnection = mock(Connection.class);
        try {
            when(mockedConnectionFactory.getConnection()).thenReturn(mockedConnection);
            when(mockedConnection.bind(anyString(), any(char[].class)))
                    .thenReturn(mockedBindResult);
        } catch (LdapException e) {
            LOGGER.error("LDAP Exception", e);
        }
        LdapClaimsHandler ldapClaimsHandler = new LdapClaimsHandler();
        ldapClaimsHandler.setLdapConnectionFactory(mockedConnectionFactory);

        ProcessedClaimCollection testClaimCollection = ldapClaimsHandler
                .retrieveClaimValues(new ClaimCollection(), new ClaimsParameters());
        assertThat(testClaimCollection.isEmpty(), is(true));
    }



}
