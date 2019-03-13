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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Paths;
import java.security.Principal;
import javax.security.auth.kerberos.KerberosPrincipal;
import javax.security.auth.x500.X500Principal;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.codice.ddf.configuration.AbsolutePathResolver;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PropertyFileClaimsHandlerTest {

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Before
  public void setup() throws IOException {
    temporaryFolder.create();
  }

  @After
  public void cleanup() throws IOException {
    temporaryFolder.delete();
  }

  @Test
  public void testGetPropertyFileLocation() {
    PropertyFileClaimsHandler claimsHandler = new PropertyFileClaimsHandler();
    String propertyFileLocation = claimsHandler.getPropertyFileLocation();
    String defaultPath =
        Paths.get(new AbsolutePathResolver("etc/users.properties").getPath()).toString();

    assertEquals(defaultPath, propertyFileLocation);

    claimsHandler.setPropertyFileLocation("users.properties");
    String newPropertyFileLocation = claimsHandler.getPropertyFileLocation();
    String testPath = Paths.get("users.properties").toString();
    assertEquals(testPath, newPropertyFileLocation);
  }

  @Test
  public void testRetrieveClaimValues() throws IOException {

    PropertyFileClaimsHandler propertyFileClaimsHandler = new PropertyFileClaimsHandler();
    String oldPropFileLocation = propertyFileClaimsHandler.getPropertyFileLocation();
    propertyFileClaimsHandler.setPropertyFileLocation(
        createTempFilePathFromResourceFileName("users.properties"));
    propertyFileClaimsHandler.setRoleClaimType("http://myroletype");
    propertyFileClaimsHandler.setIdClaimType("http://myidtype");

    Claim claimRole = new Claim();
    claimRole.setClaimType(URI.create("http://myroletype"));
    Claim claimId = new Claim();
    claimId.setClaimType(URI.create("http://myidtype"));
    ClaimCollection claimCollection = new ClaimCollection();
    claimCollection.add(claimRole);
    claimCollection.add(claimId);

    ClaimsParameters claimsParametersAdmin = mock(ClaimsParameters.class);
    Principal principalAdmin = mock(Principal.class);
    when(principalAdmin.getName()).thenReturn("admin");
    when(claimsParametersAdmin.getPrincipal()).thenReturn(principalAdmin);
    ProcessedClaimCollection processedClaimCollection =
        propertyFileClaimsHandler.retrieveClaimValues(claimCollection, claimsParametersAdmin);

    assertEquals(2, processedClaimCollection.size());
    assertEquals(5, processedClaimCollection.get(0).getValues().size());
    assertEquals("admin", processedClaimCollection.get(1).getValues().get(0));
    assertEquals("admin", processedClaimCollection.get(0).getValues().get(0));
    assertEquals("manager", processedClaimCollection.get(0).getValues().get(1));
    assertEquals("viewer", processedClaimCollection.get(0).getValues().get(2));
    assertEquals("editor", processedClaimCollection.get(0).getValues().get(3));
    assertEquals("writer", processedClaimCollection.get(0).getValues().get(4));

    ClaimsParameters claimsParametersUser1 = mock(ClaimsParameters.class);
    Principal principalUser1 = mock(Principal.class);
    when(principalUser1.getName()).thenReturn("User1");
    when(claimsParametersUser1.getPrincipal()).thenReturn(principalUser1);
    ProcessedClaimCollection processedClaimCollectionUser1 =
        propertyFileClaimsHandler.retrieveClaimValues(claimCollection, claimsParametersUser1);

    assertEquals(2, processedClaimCollectionUser1.size());
    assertEquals(2, processedClaimCollectionUser1.get(0).getValues().size());
    assertEquals("User1", processedClaimCollectionUser1.get(1).getValues().get(0));
    assertEquals("editor", processedClaimCollectionUser1.get(0).getValues().get(0));
    assertEquals("writer", processedClaimCollectionUser1.get(0).getValues().get(1));

    propertyFileClaimsHandler.setPropertyFileLocation(oldPropFileLocation);
  }

  @Test
  public void testGetUser() {
    PropertyFileClaimsHandler propertyFileClaimsHandler = new PropertyFileClaimsHandler();

    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn("mydude");
    String user = propertyFileClaimsHandler.getUser(principal);
    assertEquals("mydude", user);

    principal = new X500Principal("cn=myxman,ou=someunit,o=someorg");
    user = propertyFileClaimsHandler.getUser(principal);
    assertEquals("myxman", user);

    principal = new KerberosPrincipal("mykman@SOMEDOMAIN.COM");
    user = propertyFileClaimsHandler.getUser(principal);
    assertEquals("mykman", user);
  }

  @Test
  public void testRetrieveClaimsValuesNullPrincipal() {
    PropertyFileClaimsHandler claimsHandler = new PropertyFileClaimsHandler();
    ClaimsParameters claimsParameters = new ClaimsParameters();
    ClaimCollection claimCollection = new ClaimCollection();
    ProcessedClaimCollection processedClaims =
        claimsHandler.retrieveClaimValues(claimCollection, claimsParameters);

    Assert.assertThat(processedClaims.size(), CoreMatchers.is(equalTo(0)));
  }

  private String createTempFilePathFromResourceFileName(final String resourceFileName)
      throws IOException {
    final InputStream resourceAsStream =
        PropertyFileClaimsHandlerTest.class.getResourceAsStream("/" + resourceFileName);
    final File userFile = temporaryFolder.newFile(resourceFileName);
    final FileOutputStream userFileOs = new FileOutputStream(userFile);
    IOUtils.copy(resourceAsStream, userFileOs);

    return userFile.getAbsolutePath();
  }
}
