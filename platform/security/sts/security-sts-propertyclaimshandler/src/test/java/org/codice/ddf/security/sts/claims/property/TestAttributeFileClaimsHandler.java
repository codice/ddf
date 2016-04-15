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
 **/
package org.codice.ddf.security.sts.claims.property;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.security.Principal;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.cxf.rt.security.claims.Claim;
import org.apache.cxf.rt.security.claims.ClaimCollection;
import org.apache.cxf.sts.claims.ClaimsParameters;
import org.apache.cxf.sts.claims.ProcessedClaim;
import org.apache.cxf.sts.claims.ProcessedClaimCollection;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class TestAttributeFileClaimsHandler {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    AttributeFileClaimsHandler attributeFileClaimsHandler = new AttributeFileClaimsHandler();

    ClaimCollection claimCollection;

    ClaimsParameters adminClaimsParameters;

    ClaimsParameters localhostClaimsParameters;

    ClaimsParameters regexClaimsParameters;

    ClaimsParameters unknownClaimsParameters;

    File userFile;

    @Before
    public void setup() throws IOException {
        System.setProperty("ddf.home", "testdir");
        InputStream resourceAsStream = TestAttributeFileClaimsHandler.class.getResourceAsStream(
                "/users.attributes");
        userFile = temporaryFolder.newFile("users.attributes");
        FileOutputStream userFileOs = new FileOutputStream(userFile);
        IOUtils.copy(resourceAsStream, userFileOs);

        attributeFileClaimsHandler.setAttributeFileLocation(userFile.getAbsolutePath());
        attributeFileClaimsHandler.init();

        claimCollection = new ClaimCollection();
        Claim claim = new Claim();
        claim.setClaimType(URI.create("test"));
        claimCollection.add(claim);
        Claim claim1 = new Claim();
        claim1.setClaimType(URI.create("test1"));
        claimCollection.add(claim1);
        Claim claim2 = new Claim();
        claim2.setClaimType(URI.create("reg"));
        claimCollection.add(claim2);

        adminClaimsParameters = new ClaimsParameters();
        Principal principal = mock(Principal.class);
        when(principal.getName()).thenReturn("admin");
        adminClaimsParameters.setPrincipal(principal);

        localhostClaimsParameters = new ClaimsParameters();
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("localhost");
        localhostClaimsParameters.setPrincipal(principal);

        regexClaimsParameters = new ClaimsParameters();
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("myhostname");
        regexClaimsParameters.setPrincipal(principal);

        unknownClaimsParameters = new ClaimsParameters();
        principal = mock(Principal.class);
        when(principal.getName()).thenReturn("unknown");
        unknownClaimsParameters.setPrincipal(principal);
    }

    @Test
    public void testGetSupportedClaimTypes() {
        List<URI> supportedClaimTypes = attributeFileClaimsHandler.getSupportedClaimTypes();
        assertTrue(supportedClaimTypes.contains(URI.create("test")));
        assertTrue(supportedClaimTypes.contains(URI.create("test1")));
    }

    @Test
    public void testRetrieveClaimValuesAdmin() {
        ProcessedClaimCollection processedClaims = attributeFileClaimsHandler.retrieveClaimValues(
                claimCollection, adminClaimsParameters);
        assertThat(processedClaims.size(), is(2));

        for (ProcessedClaim processedClaim : processedClaims) {
            if (processedClaim.getClaimType()
                    .toString()
                    .equals("test")) {
                assertThat(processedClaim.getValues()
                        .size(), is(1));
                assertThat(processedClaim.getValues()
                        .get(0), is("testValue"));
            } else if (processedClaim.getClaimType()
                    .toString()
                    .equals("test1")) {
                assertThat(processedClaim.getValues()
                        .size(), is(3));
                assertTrue(processedClaim.getValues()
                        .contains("testing1"));
                assertTrue(processedClaim.getValues()
                        .contains("testing2"));
                assertTrue(processedClaim.getValues()
                        .contains("testing3"));
            }
        }
    }

    @Test
    public void testRetrieveClaimValuesLocalhost() {
        ProcessedClaimCollection processedClaims = attributeFileClaimsHandler.retrieveClaimValues(
                claimCollection, localhostClaimsParameters);
        assertThat(processedClaims.size(), is(0));
    }

    @Test
    public void testRetrieveClaimValuesRegex() {
        ProcessedClaimCollection processedClaims = attributeFileClaimsHandler.retrieveClaimValues(
                claimCollection, regexClaimsParameters);
        assertThat(processedClaims.size(), is(1));
        assertThat(processedClaims.get(0)
                .getClaimType()
                .toString(), is("reg"));
        assertThat(processedClaims.get(0)
                .getValues()
                .get(0), is("ex"));
    }

    @Test
    public void testNoMatchRetrieveClaimValues() {
        ProcessedClaimCollection processedClaims = attributeFileClaimsHandler.retrieveClaimValues(
                claimCollection, unknownClaimsParameters);
        assertThat(processedClaims.size(), is(0));
    }

    @Test
    public void testRetrieveClaimsValuesNullPrincipal() {
        ClaimsParameters claimsParameters = new ClaimsParameters();
        ClaimCollection claimCollection = new ClaimCollection();
        ProcessedClaimCollection processedClaims = attributeFileClaimsHandler.retrieveClaimValues(
                claimCollection, claimsParameters);

        Assert.assertThat(processedClaims.size(), CoreMatchers.is(equalTo(0)));
    }
}
