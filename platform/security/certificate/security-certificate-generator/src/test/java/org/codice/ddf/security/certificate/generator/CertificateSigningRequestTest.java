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
package org.codice.ddf.security.certificate.generator;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.mockito.Mockito.mock;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CertificateSigningRequestTest {

    CertificateSigningRequest csr;

    @Before
    public void setUp() {
        csr = new CertificateSigningRequest();
    }

    @Test
    public void testKeys() throws Exception {
        assertThat("CSR failed to auto-generate RSA keypair", csr.getSubjectPrivateKey(),
                instanceOf(PrivateKey.class));
        assertThat("CSR failed to auto-generate RSA keypair", csr.getSubjectPublicKey(),
                instanceOf(PublicKey.class));
        PublicKey pubKey = mock(PublicKey.class);
        PrivateKey privKey = mock(PrivateKey.class);
        KeyPair kp = new KeyPair(pubKey, privKey);
        csr.setSubjectKeyPair(kp);
        assertThat("Unable to get mock private key", csr.getSubjectPrivateKey(),
                sameInstance(privKey));
        assertThat("Unable to get mock public key", csr.getSubjectPublicKey(),
                sameInstance(pubKey));
    }

    @Test
    public void assertDates() {
        boolean outcome = csr.getNotAfter()
                .isAfter(csr.getNotBefore());
        assertThat("'Not after' date should never be chronologically before the 'Not before' date'",
                outcome, equalTo(true));
    }

    @Test(expected = IllegalArgumentException.class)
    public void badNotBeforeDate() {
        csr.setNotBefore(csr.getNotAfter()
                .plusDays(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void badNotAfterDate() {
        csr.setNotAfter(csr.getNotBefore()
                .minusDays(1));
    }

    @Test
    public void setValidDates() throws Exception {
        //Test default dates
        assertDates();

        //Try new dates
        DateTime effectiveDate = DateTime.now();
        csr.setNotBefore(effectiveDate);
        assertDates();
        csr.setNotAfter(effectiveDate.plusDays(1));
        assertDates();
    }

    @Test
    public void testSetSerialNumber() throws Exception {
        csr.setSerialNumber(1);
        assertThat("The serial number should be one", 1L, equalTo(csr.getSerialNumber()));
    }

    @Test
    public void subjectName() throws Exception {

        assertThat("Subject name should never be null", true,
                equalTo(csr.getSubjectName() != null));
        csr.setCommonName("test");
        String cn = csr.getSubjectName()
                .toString();
        assertThat("Subject name should be 'test'", cn, endsWith("test"));
    }

    @Test
    public void subjectFromDN() throws Exception {
        assertThat("Subject name should never be null", true,
                equalTo(csr.getSubjectName() != null));
        csr.setDistinguishedName("CN=john.smith", "O=Tardis", "o=police box", "L=London", "C=UK");
        String subjectName = csr.getSubjectName()
                .toString();

        assertThat("Subject name should contain 'cn=john.smith'", subjectName,
                containsString("cn=john.smith"));
        assertThat("Subject name should contain 'o=Tardis'", subjectName,
                containsString("o=Tardis"));
        assertThat("Subject name should contain 'o=police box'", subjectName,
                containsString("o=police box"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void badSerialNumber() {
        csr.setSerialNumber(-1);
    }
}