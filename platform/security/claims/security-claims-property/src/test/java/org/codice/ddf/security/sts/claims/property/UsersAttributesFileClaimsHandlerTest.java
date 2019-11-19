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
package org.codice.ddf.security.sts.claims.property;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.security.claims.ClaimsCollection;
import ddf.security.claims.ClaimsParameters;
import ddf.security.claims.impl.ClaimImpl;
import ddf.security.claims.impl.ClaimsCollectionImpl;
import ddf.security.claims.impl.ClaimsParametersImpl;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UsersAttributesFileClaimsHandlerTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setup() {
    System.setProperty("ddf.home", "testdir");
  }

  @Test
  public void testRetrieveClaimValuesTestHostname() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        getPathForValidTestAttributesFile());

    final ClaimsCollection ClaimsCollection = getClaimsCollectionForValidTestAttributesFile();

    final Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("testHostname");
    final ClaimsParameters testHostnameClaimsParameters =
        new ClaimsParametersImpl(principal, new HashSet<>(), new HashMap<>());

    // when
    final ClaimsCollection processedClaims =
        usersAttributesFileClaimsHandler.retrieveClaims(testHostnameClaimsParameters);

    // then
    assertThat(
        processedClaims,
        containsInAnyOrder(
            allOf(
                hasProperty("name", is("Clearance")),
                hasProperty("values", containsInAnyOrder("U"))),
            allOf(
                hasProperty("name", is("CountryOfAffiliation")),
                hasProperty("values", containsInAnyOrder("USA"))),
            allOf(
                hasProperty("name", is("classification")),
                hasProperty("values", containsInAnyOrder("U"))),
            allOf(
                hasProperty(
                    "name",
                    is("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress")),
                hasProperty("values", containsInAnyOrder("system@testHostname"))),
            allOf(
                hasProperty("name", is("ownerProducer")),
                hasProperty("values", containsInAnyOrder("USA"))),
            allOf(
                hasProperty("name", is("releasableTo")),
                hasProperty("values", containsInAnyOrder("USA"))),
            allOf(
                hasProperty("name", is("FineAccessControls")),
                hasProperty("values", containsInAnyOrder("SCI1", "SCI2"))),
            allOf(
                hasProperty("name", is("disseminationControls")),
                hasProperty("values", containsInAnyOrder("NF")))));
  }

  @Test
  public void testRetrieveClaimValuesAdmin() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        getPathForValidTestAttributesFile());

    final ClaimsCollection ClaimsCollection = getClaimsCollectionForValidTestAttributesFile();

    final ClaimsParameters localhostClaimsParameters;
    final Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("admin");
    localhostClaimsParameters =
        new ClaimsParametersImpl(principal, new HashSet<>(), new HashMap<>());

    // when
    final ClaimsCollection processedClaims =
        usersAttributesFileClaimsHandler.retrieveClaims(localhostClaimsParameters);

    // then
    assertThat(
        processedClaims,
        contains(
            allOf(
                hasProperty(
                    "name",
                    is("http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress")),
                hasProperty("values", containsInAnyOrder("admin@testHostname")))));
  }

  @Test
  public void testRetrieveClaimValuesRegex() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        getPathForValidTestAttributesFile());

    final ClaimsCollection ClaimsCollection = getClaimsCollectionForValidTestAttributesFile();

    final Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("myhostname");
    final ClaimsParameters regexClaimsParameters =
        new ClaimsParametersImpl(principal, new HashSet<>(), new HashMap<>());

    // when
    ClaimsCollection processedClaims =
        usersAttributesFileClaimsHandler.retrieveClaims(regexClaimsParameters);

    // then
    assertThat(
        processedClaims,
        contains(
            allOf(
                hasProperty("name", is("reg")), hasProperty("values", containsInAnyOrder("ex")))));
  }

  @Test
  public void testNoMatchRetrieveClaimValues() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        getPathForValidTestAttributesFile());

    final ClaimsCollection ClaimsCollection = getClaimsCollectionForValidTestAttributesFile();

    final Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("someNameThat'sNotInTheUsersAttributesFile");
    final ClaimsParameters unknownClaimsParameters =
        new ClaimsParametersImpl(principal, new HashSet<>(), new HashMap<>());

    // when
    final ClaimsCollection processedClaims =
        usersAttributesFileClaimsHandler.retrieveClaims(unknownClaimsParameters);

    // then
    assertThat(processedClaims, is(empty()));
  }

  @Test
  public void testGetSingleValuedSystemHighAttribute() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        getPathForValidTestAttributesFile());

    // expect
    assertThat(usersAttributesFileClaimsHandler.getValues("Clearance"), containsInAnyOrder("U"));
  }

  @Test
  public void testGetMultiValuedSystemHighAttribute() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        getPathForValidTestAttributesFile());

    // expect
    assertThat(
        usersAttributesFileClaimsHandler.getValues("FineAccessControls"),
        containsInAnyOrder("SCI1", "SCI2"));
  }

  @Test
  public void testNoMatchGetSystemHighAttribute() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        getPathForValidTestAttributesFile());

    // expect
    assertThat(usersAttributesFileClaimsHandler.getValues("reg"), is(empty()));
  }

  @Test(expected = IllegalStateException.class)
  public void testCantFindSystemHighUserInUsersDotAttributesFile() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "someUserThatIsntInTheUsersDotAttributesFile");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();

    // when
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        getPathForValidTestAttributesFile());
  }

  @Test(expected = NullPointerException.class)
  public void testSetNullAttributeFileLocation() {
    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(null);
  }

  @Test(expected = IllegalStateException.class)
  public void testNoUsersDotAttributesFile() {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();

    // when
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation("noFileHere");
  }

  @Test(expected = IllegalStateException.class)
  public void testUnexpectedFormatInUsersDotAttributesFile() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();

    // when
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        createAttributeFilePathFromResourceFileName("users.attributes-unexpectedFormat"));
  }

  @Test(expected = IllegalStateException.class)
  public void testEmptyListValueInUsersDotAttributesFile() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();

    // when
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        createAttributeFilePathFromResourceFileName("users.attributes-emptyListValue"));
  }

  @Test(expected = IllegalStateException.class)
  public void testNonStringValueInUsersDotAttributesFile() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();

    // when
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        createAttributeFilePathFromResourceFileName("users.attributes-nonStringValue"));
  }

  @Test(expected = IllegalStateException.class)
  public void testNonStringListValueInUsersDotAttributesFile() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();

    // when
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        createAttributeFilePathFromResourceFileName("users.attributes-nonStringListValue"));
  }

  @Test
  public void testUpdateAttributeFileLocation() throws IOException {
    // given
    System.setProperty(SystemBaseUrl.INTERNAL_HOST, "testHostname");

    final UsersAttributesFileClaimsHandler usersAttributesFileClaimsHandler =
        new UsersAttributesFileClaimsHandler();
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        getPathForValidTestAttributesFile());

    // when
    usersAttributesFileClaimsHandler.setUsersAttributesFileLocation(
        createAttributeFilePathFromResourceFileName("users.attributes-anotherValidFile"));

    // then
    assertThat(
        "UsersAttributesFileClaimsHandler should have been initialized with the new file contents",
        usersAttributesFileClaimsHandler.getValues(
            "anAttributeThatIsntInTheOtherValidUsersAttributesFile"),
        containsInAnyOrder("theValueOfTheNewAttributeValue"));
  }

  private String getPathForValidTestAttributesFile() throws IOException {
    return createAttributeFilePathFromResourceFileName("users.attributes");
  }

  private static ClaimsCollection getClaimsCollectionForValidTestAttributesFile() {
    // all attribute names in resources/users.attributes
    final String[] attributeNames = {
      "Clearance",
      "CountryOfAffiliation",
      "classification",
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress",
      "ownerProducer",
      "releasableTo",
      "FineAccessControls",
      "disseminationControls",
      "reg"
    };

    return Arrays.stream(attributeNames)
        .map(ClaimImpl::new)
        .collect(Collectors.toCollection(ClaimsCollectionImpl::new));
  }

  private String createAttributeFilePathFromResourceFileName(final String resourceFileName)
      throws IOException {
    final InputStream resourceAsStream =
        UsersAttributesFileClaimsHandlerTest.class.getResourceAsStream("/" + resourceFileName);
    final File userFile = temporaryFolder.newFile(resourceFileName);
    final FileOutputStream userFileOs = new FileOutputStream(userFile);
    IOUtils.copy(resourceAsStream, userFileOs);

    return userFile.getAbsolutePath();
  }
}
