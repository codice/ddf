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
package ddf.security.sts.claimsHandler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
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
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.DN;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.LinkedAttribute;
import org.forgerock.opendj.ldap.SearchResultReferenceIOException;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.forgerock.util.promise.Promise;
import org.junit.Test;

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
    BindResult bindResult = mock(BindResult.class);
    ClaimsParameters claimsParameters;
    Connection connection = mock(Connection.class);
    ConnectionEntryReader membershipReader = mock(ConnectionEntryReader.class);
    ConnectionEntryReader groupNameReader = mock(ConnectionEntryReader.class);
    LinkedAttribute membershipAttribute = new LinkedAttribute("uid");
    LinkedAttribute groupNameAttribute = new LinkedAttribute("cn");
    ProcessedClaimCollection processedClaims;
    RoleClaimsHandler claimsHandler;
    SearchResultEntry membershipSearchResult = mock(SearchResultEntry.class);
    DN resultDN = DN.valueOf("uid=tstark,");
    SearchResultEntry groupNameSearchResult = mock(SearchResultEntry.class);
    String groupName = "avengers";

    when(bindResult.isSuccess()).thenReturn(true);

    membershipAttribute.add("tstark");
    when(membershipSearchResult.getAttribute(anyString())).thenReturn(membershipAttribute);

    // hasNext() returns 'true' the first time, then 'false' every time after.
    when(membershipReader.hasNext()).thenReturn(true, false);
    when(membershipReader.isEntry()).thenReturn(true);
    when(membershipReader.readEntry()).thenReturn(membershipSearchResult);

    when(membershipSearchResult.getName()).thenReturn(resultDN);

    groupNameAttribute.add(groupName);
    when(groupNameSearchResult.getAttribute(anyString())).thenReturn(groupNameAttribute);

    when(groupNameReader.hasNext()).thenReturn(true, false);
    when(groupNameReader.isEntry()).thenReturn(true);
    when(groupNameReader.readEntry()).thenReturn(groupNameSearchResult);

    when(connection.bind(anyObject())).thenReturn(bindResult);
    when(connection.search(
            anyObject(),
            anyObject(),
            eq("(&(objectClass=groupOfNames)(|(member=uid=tstark,)(member=uid=tstark,)))"),
            anyVararg()))
        .thenReturn(groupNameReader);
    when(connection.search(anyString(), anyObject(), anyString(), matches("uid")))
        .thenReturn(membershipReader);

    claimsHandler = new RoleClaimsHandler();
    claimsHandler.setLdapConnectionFactory(new MockConnectionFactory(connection));
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
    assertThat(claim.getValues().get(0), equalTo(groupName));
  }

  @Test
  public void testRetrieveClaimsValuesNestedUserOU()
      throws LdapException, SearchResultReferenceIOException {
    BindResult bindResult = mock(BindResult.class);
    ClaimsParameters claimsParameters;
    Connection connection = mock(Connection.class);
    ConnectionEntryReader membershipReader = mock(ConnectionEntryReader.class);
    ConnectionEntryReader groupNameReader = mock(ConnectionEntryReader.class);
    LinkedAttribute membershipAttribute = new LinkedAttribute("cn");
    LinkedAttribute groupNameAttribute = new LinkedAttribute("cn");
    ProcessedClaimCollection processedClaims;
    RoleClaimsHandler claimsHandler;
    SearchResultEntry membershipSearchResult = mock(SearchResultEntry.class);
    DN resultDN = DN.valueOf("uid=tstark,OU=nested,");
    SearchResultEntry groupNameSearchResult = mock(SearchResultEntry.class);
    String groupName = "avengers";

    when(bindResult.isSuccess()).thenReturn(true);

    membershipAttribute.add("tstark");
    when(membershipSearchResult.getAttribute(anyString())).thenReturn(membershipAttribute);

    // hasNext() returns 'true' the first time, then 'false' every time after.
    when(membershipReader.hasNext()).thenReturn(true, false);
    when(membershipReader.isEntry()).thenReturn(true);
    when(membershipReader.readEntry()).thenReturn(membershipSearchResult);

    when(membershipSearchResult.getName()).thenReturn(resultDN);

    groupNameAttribute.add(groupName);
    when(groupNameSearchResult.getAttribute(anyString())).thenReturn(groupNameAttribute);

    when(groupNameReader.hasNext()).thenReturn(true, false);
    when(groupNameReader.isEntry()).thenReturn(true);
    when(groupNameReader.readEntry()).thenReturn(groupNameSearchResult);

    when(connection.bind(anyObject())).thenReturn(bindResult);
    when(connection.search(
            anyObject(),
            anyObject(),
            eq(
                "(&(objectClass=groupOfNames)(|(member=cn=tstark,OU=nested,)(member=uid=tstark,OU=nested,)))"),
            anyVararg()))
        .thenReturn(groupNameReader);
    when(connection.search(anyString(), anyObject(), anyString(), matches("cn")))
        .thenReturn(membershipReader);

    claimsHandler = new RoleClaimsHandler();
    claimsHandler.setLdapConnectionFactory(new MockConnectionFactory(connection));
    claimsHandler.setBindMethod("Simple");
    claimsHandler.setBindUserCredentials("foo");
    claimsHandler.setBindUserDN("bar");
    claimsHandler.setMembershipUserAttribute("cn");
    claimsHandler.setLoginUserAttribute("uid");

    claimsParameters = new ClaimsParameters();
    claimsParameters.setPrincipal(new UserPrincipal(USER_CN));
    ClaimCollection claimCollection = new ClaimCollection();
    processedClaims = claimsHandler.retrieveClaimValues(claimCollection, claimsParameters);
    assertThat(processedClaims, hasSize(1));
    ProcessedClaim claim = processedClaims.get(0);
    assertThat(claim.getPrincipal(), equalTo(new UserPrincipal(USER_CN)));
    assertThat(claim.getValues(), hasSize(1));
    assertThat(claim.getValues().get(0), equalTo(groupName));
  }

  @Test
  public void testRetrieveClaimsValuesIgnoredReferences()
      throws LdapException, SearchResultReferenceIOException {
    BindResult bindResult = mock(BindResult.class);
    ClaimsParameters claimsParameters;
    Connection connection = mock(Connection.class);
    ConnectionEntryReader membershipReader = mock(ConnectionEntryReader.class);
    ConnectionEntryReader groupNameReader = mock(ConnectionEntryReader.class);
    LinkedAttribute membershipAttribute = new LinkedAttribute("uid");
    LinkedAttribute groupNameAttribute = new LinkedAttribute("cn");
    ProcessedClaimCollection processedClaims;
    RoleClaimsHandler claimsHandler;
    SearchResultEntry membershipSearchResult = mock(SearchResultEntry.class);
    DN resultDN = DN.valueOf("uid=tstark,");
    SearchResultEntry groupNameSearchResult = mock(SearchResultEntry.class);
    String groupName = "avengers";

    when(bindResult.isSuccess()).thenReturn(true);

    membershipAttribute.add("tstark");
    when(membershipSearchResult.getAttribute(anyString())).thenReturn(membershipAttribute);

    // simulate two items in the list (a reference and an entry)
    when(membershipReader.hasNext()).thenReturn(true, true, false);
    // test a reference followed by entries thereafter
    when(membershipReader.isEntry()).thenReturn(false, true);
    when(membershipReader.readEntry()).thenReturn(membershipSearchResult);

    when(membershipSearchResult.getName()).thenReturn(resultDN);

    groupNameAttribute.add(groupName);
    when(groupNameSearchResult.getAttribute(anyString())).thenReturn(groupNameAttribute);

    when(groupNameReader.hasNext()).thenReturn(true, true, false);
    when(groupNameReader.isEntry()).thenReturn(false, true);
    when(groupNameReader.readEntry()).thenReturn(groupNameSearchResult);

    when(connection.bind(anyObject())).thenReturn(bindResult);
    when(connection.search(
            anyObject(),
            anyObject(),
            eq("(&(objectClass=groupOfNames)(|(member=uid=tstark,)(member=uid=tstark,)))"),
            anyVararg()))
        .thenReturn(groupNameReader);
    when(connection.search(anyString(), anyObject(), anyString(), matches("uid")))
        .thenReturn(membershipReader);

    claimsHandler = new RoleClaimsHandler();
    claimsHandler.setLdapConnectionFactory(new MockConnectionFactory(connection));
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
    assertThat(claim.getValues().get(0), equalTo(groupName));
  }

  @Test
  public void testSupportClaimTypes() {
    RoleClaimsHandler claimsHandler = new RoleClaimsHandler();
    List<URI> uris = claimsHandler.getSupportedClaimTypes();
    assertThat(uris, hasSize(1));
  }

  private class MockConnectionFactory implements ConnectionFactory {
    private Connection connection;

    public MockConnectionFactory(Connection connection) {
      this.connection = connection;
    }

    @Override
    public void close() {
      // no-op
    }

    @Override
    public Promise<Connection, LdapException> getConnectionAsync() {
      return null;
    }

    @Override
    public Connection getConnection() throws LdapException {
      return connection;
    }
  }
}
