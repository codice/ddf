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
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import ddf.security.SubjectOperations;
import ddf.security.claims.Claim;
import ddf.security.claims.ClaimsCollection;
import ddf.security.claims.ClaimsParameters;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import org.apache.karaf.jaas.boot.principal.UserPrincipal;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.ConnectionFactory;
import org.forgerock.opendj.ldap.LdapException;
import org.forgerock.opendj.ldap.LinkedAttribute;
import org.forgerock.opendj.ldap.requests.BindRequest;
import org.forgerock.opendj.ldap.responses.BindResult;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;

public class LdapClaimsHandlerTest {

  public static final String BINDING_TYPE = "Simple";

  public static final String BIND_USER_CREDENTIALS = "test";

  public static final String KCD = "Kerberos Constrained Delegation";

  public static final String ATTRIBUTE_NAME = "cn";

  public static final String USER_BASE_DN = "ou=avengers,dc=marvel,dc=com";

  public static final String DUMMY_VALUE = "Tony Stark";

  public static final String USER_DN =
      String.format("%s=%s,%s", ATTRIBUTE_NAME, DUMMY_VALUE, USER_BASE_DN);

  LdapClaimsHandler claimsHandler;

  BindResult mockBindResult;

  BindRequest mockBindRequest;

  Connection mockConnection;

  ConnectionEntryReader mockEntryReader;

  SearchResultEntry mockEntry;

  ClaimsParameters claimsParameters;

  @Before
  public void setup() throws Exception {
    claimsParameters = mock(ClaimsParameters.class);
    when(claimsParameters.getPrincipal()).thenReturn(new UserPrincipal(USER_DN));
    mockEntry = mock(SearchResultEntry.class);
    LinkedAttribute attribute = new LinkedAttribute(ATTRIBUTE_NAME);
    attribute.add(USER_DN);
    mockEntryReader = mock(ConnectionEntryReader.class);
    mockBindRequest = mock(BindRequest.class);
    Map<String, String> map = new HashMap<>();
    map.put(SubjectOperations.NAME_IDENTIFIER_CLAIM_URI, ATTRIBUTE_NAME);
    AttributeMapLoader mockAttributeMapLoader = mock(AttributeMapLoader.class);
    when(mockAttributeMapLoader.buildClaimsMapFile(anyString())).thenReturn(map);
    when(mockAttributeMapLoader.getUser(any(Principal.class)))
        .then(i -> ((Principal) i.getArgument(0)).getName());
    when(mockAttributeMapLoader.getBaseDN(any(Principal.class), anyString(), eq(false)))
        .then(i -> i.getArgument(1));
    claimsHandler = spy(new LdapClaimsHandler(mockAttributeMapLoader));
    doReturn(mockBindRequest).when(claimsHandler).selectBindMethod();
    mockBindResult = mock(BindResult.class);
    mockConnection = mock(Connection.class);
    when(mockConnection.bind(anyString(), any(char[].class))).thenReturn(mockBindResult);
    when(mockConnection.bind(any(BindRequest.class))).thenReturn(mockBindResult);
    when(mockConnection.search(anyObject(), anyObject(), anyObject(), anyObject()))
        .thenReturn(mockEntryReader);
    // two item list (reference and entry)
    when(mockEntryReader.hasNext()).thenReturn(true, true, false);
    // first time indicate a reference followed by entries
    when(mockEntryReader.isEntry()).thenReturn(false, true);
    when(mockEntryReader.readEntry()).thenReturn(mockEntry);
    when(mockEntry.getAttribute(anyString())).thenReturn(attribute);
    ConnectionFactory mockConnectionFactory = mock(ConnectionFactory.class);
    when(mockConnectionFactory.getConnection()).thenReturn(mockConnection);
    claimsHandler.setLdapConnectionFactory(mockConnectionFactory);
    claimsHandler.setPropertyFileLocation("thisstringisnotempty");
    claimsHandler.setBindMethod(BINDING_TYPE);
    claimsHandler.setBindUserCredentials(BIND_USER_CREDENTIALS);
    claimsHandler.setKdcAddress(KCD);
    claimsHandler.setUserBaseDN(USER_BASE_DN);
  }

  @Test
  public void testUnsuccessfulConnectionBind() throws LdapException {
    when(mockBindResult.isSuccess()).thenReturn(false);
    ClaimsCollection testClaimCollection = claimsHandler.retrieveClaims(claimsParameters);
    assertThat(testClaimCollection.isEmpty(), is(true));
  }

  @Test
  public void testRetrieveClaimsValuesNullPrincipal() throws LdapException {
    when(mockBindResult.isSuccess()).thenReturn(false);
    ClaimsCollection processedClaims = claimsHandler.retrieveClaims(claimsParameters);
    assertThat(processedClaims.size(), CoreMatchers.is(equalTo(0)));
  }

  @Test
  public void testRetrieveClaimsValues() throws URISyntaxException, LdapException {
    when(mockBindResult.isSuccess()).thenReturn(true);
    ClaimsCollection processedClaims = claimsHandler.retrieveClaims(claimsParameters);

    assertThat(processedClaims, hasSize(1));
    Claim claim = processedClaims.get(0);
    assertThat(claim.getValues(), contains(DUMMY_VALUE));
  }
}
