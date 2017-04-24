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

package ddf.security.sts.claimsHandler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.requests.BindRequest;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.SubjectUtils;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LDAPConnectionFactory.class, AttributeMapLoader.class, BindMethodChooser.class})

public class LdapClaimsHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapClaimsHandlerTest.class);

    private static final String USER_NAME = "USER_NAME";

    LdapClaimsHandler claimsHandler;

    LDAPConnectionFactory mockConnectionFactory;

    BindResult mockBindResult;

    BindRequest mockBindRequest;

    Connection mockConnection;

    ClaimCollection claims;

    @Before
    public void setup() throws LdapException, URISyntaxException {

        mockBindRequest = mock(BindRequest.class);
        PowerMockito.mockStatic(BindMethodChooser.class);
        when(BindMethodChooser.selectBindMethod(eq("Simple"),
                eq("cn=admin"),
                eq("test"),
                eq("kualdgalain"),
                eq("Kerberos Constrained Delegation"))).thenReturn(mockBindRequest);

        Map<String, String> map = new HashMap<>();
        map.put("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress",
                "emailaddress");
        PowerMockito.mockStatic(AttributeMapLoader.class);
        when(AttributeMapLoader.buildClaimsMapFile(anyString())).thenReturn(map);
        when(AttributeMapLoader.getUser(anyObject())).thenReturn(USER_NAME);
        claimsHandler = new LdapClaimsHandler();
        mockBindResult = mock(BindResult.class);
        mockConnection = mock(Connection.class);
        mockConnectionFactory = PowerMockito.mock(LDAPConnectionFactory.class);
        when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
        when(mockConnection.bind(anyString(), any(char[].class))).thenReturn(mockBindResult);
        when(mockConnection.bind(any(BindRequest.class))).thenReturn(mockBindResult);
        claimsHandler.setLdapConnectionFactory(mockConnectionFactory);
        claimsHandler.setPropertyFileLocation("thisstringisnotempty");
        claimsHandler.setBindMethod("Simple");
        claimsHandler.setBindUserCredentials("cn=admin");
        claimsHandler.setBindUserCredentials("test");
        claimsHandler.setRealm("kualdgalain");
        claimsHandler.setKdcAddress("Kerberos Constrained Delegation");
        claims = new ClaimCollection();
        Claim claim = new Claim();
        claim.setClaimType(new URI(SubjectUtils.EMAIL_ADDRESS_CLAIM_URI));
        claims.add(claim);
    }

    @Test
    public void testUnsuccessfulConnectionBind() {
        when(mockBindResult.isSuccess()).thenReturn(false);

        ProcessedClaimCollection testClaimCollection =
                claimsHandler.retrieveClaimValues(new ClaimCollection(), new ClaimsParameters());
        assertThat(testClaimCollection.isEmpty(), is(true));
    }

    @Test
    public void testRetrieveClaimsValuesNullPrincipal() {
        when(mockBindResult.isSuccess()).thenReturn(false);

        ProcessedClaimCollection processedClaims =
                claimsHandler.retrieveClaimValues(new ClaimCollection(), new ClaimsParameters());
        assertThat(processedClaims.size(), CoreMatchers.is(equalTo(0)));
    }

    @Test
    public void testRetrieveClaimsValues() throws URISyntaxException {
        when(mockBindResult.isSuccess()).thenReturn(true);
        ClaimsParameters claimsParameters = mock(ClaimsParameters.class);
        when(claimsParameters.getPrincipal()).thenReturn(new UserPrincipal(USER_NAME));

        ProcessedClaimCollection processedClaims = claimsHandler.retrieveClaimValues(claims,
                claimsParameters);

        assertThat(processedClaims.size(), CoreMatchers.is(equalTo(0)));
    }

}
