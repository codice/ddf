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
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.LinkedAttribute;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({LDAPConnectionFactory.class})
public class RoleClaimsHandlerTest {
    public static final String USER_CN = "tstark";

    @Test
    public void testRetrieveClaimsValuesNullPrincipal() {
        RoleClaimsHandler claimsHandler = new RoleClaimsHandler();
        ClaimsParameters claimsParameters = new ClaimsParameters();
        ClaimCollection claimCollection = new ClaimCollection();
        ProcessedClaimCollection processedClaims =
                claimsHandler.retrieveClaimValues(claimCollection, claimsParameters);
        assertThat(processedClaims, is(empty()));
    }

    @Test
    public void testRetrieveClaimsValuesNotNullPrincipal()
            throws LdapException, SearchResultReferenceIOException {
        BindResult bindResult;
        ClaimsParameters claimsParameters;
        Connection connection;
        ConnectionEntryReader membershipReader, groupNameReader;
        LDAPConnectionFactory connectionFactory;
        LinkedAttribute membershipAttribute, groupNameAttribute;
        ProcessedClaimCollection processedClaims;
        RoleClaimsHandler claimsHandler;
        SearchResultEntry membershipSearchResult, groupNameSearchResult;
        String groupName = "avengers";

        bindResult = mock(BindResult.class);
        when(bindResult.isSuccess()).thenReturn(true);

        membershipSearchResult = mock(SearchResultEntry.class);
        membershipAttribute = new LinkedAttribute("uid");
        membershipAttribute.add("tstark");
        when(membershipSearchResult.getAttribute(anyString())).thenReturn(membershipAttribute);

        membershipReader = mock(ConnectionEntryReader.class);
        // hasNext() returns 'true' the first time, then 'false' every time after.
        when(membershipReader.hasNext()).thenReturn(true, false);
        when(membershipReader.readEntry()).thenReturn(membershipSearchResult);

        groupNameSearchResult = mock(SearchResultEntry.class);
        groupNameAttribute = new LinkedAttribute("cn");
        groupNameAttribute.add(groupName);
        when(groupNameSearchResult.getAttribute(anyString())).thenReturn(groupNameAttribute);

        groupNameReader = mock(ConnectionEntryReader.class);
        when(groupNameReader.hasNext()).thenReturn(true, false);
        when(groupNameReader.readEntry()).thenReturn(groupNameSearchResult);

        connection = mock(Connection.class);
        when(connection.bind(anyObject())).thenReturn(bindResult);
        when(connection.search(anyObject(),
                anyObject(),
                eq("(&(objectClass=groupOfNames)(member=uid=tstark,))"),
                anyVararg())).thenReturn(groupNameReader);
        when(connection.search(anyString(), anyObject(), anyString(), matches("uid"))).thenReturn(
                membershipReader);

        connectionFactory = PowerMockito.mock(LDAPConnectionFactory.class);
        when(connectionFactory.getConnection()).thenReturn(connection);

        claimsHandler = new RoleClaimsHandler();
        claimsHandler.setLdapConnectionFactory(connectionFactory);
        claimsHandler.setBindMethod("Simple");
        claimsHandler.setBindUserCredentials("foo");
        claimsHandler.setBindUserDN("bar");

        claimsParameters = new ClaimsParameters();
        claimsParameters.setPrincipal(new UserPrincipal(USER_CN));
        ClaimCollection claimCollection = new ClaimCollection();
        processedClaims = claimsHandler.retrieveClaimValues(claimCollection, claimsParameters);
        assertThat(processedClaims, hasSize(1));
        ProcessedClaim claim = processedClaims.get(0);
        assertThat(claim.getPrincipal(), equalTo(new UserPrincipal(USER_CN)));
        assertThat(claim.getValues(), hasSize(1));
        assertThat(claim.getValues()
                .get(0), equalTo(groupName));
    }

    @Test
    public void testSupportClaimTypes() {
        RoleClaimsHandler claimsHandler = new RoleClaimsHandler();
        List<URI> uris = claimsHandler.getSupportedClaimTypes();
        assertThat(uris, hasSize(greaterThan(0)));
    }
}