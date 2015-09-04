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
package ddf.security.certificate.generator;

import org.bouncycastle.asn1.x500.X500Name;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

@RunWith(MockitoJUnitRunner.class)
public class PkiToolsTest {

    PkiTools tools;

    @Before
    public void setup() {
        tools = new PkiTools();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nameIsNull() throws IllegalArgumentException {
        tools.makeDistinguishedName(null);
    }

    @Test
    public void nameIsEmptyString() throws CertificateEncodingException {

        X500Name name = tools.makeDistinguishedName("");
        assertThat(name.toString(), equalTo("cn="));
    }

    @Test
    public void nameIsNotEmpty() throws CertificateEncodingException {
        String host = "host.domain.tld";
        X500Name name = tools.makeDistinguishedName(host);
        assertThat(name.toString(), equalTo("cn=" + host));
    }

    @Test
    public void convertCertificate() throws CertificateException {
        String originalCert = DemoCertificateAuthority.pemDemoCaCertificate;
        assertThat(originalCert, not(equalTo("")));
        assertThat(tools.certificateToPem(tools.pemToCertificate(originalCert)), equalTo(originalCert));
    }

    @Test
    public void hostName() {
        assertThat(tools.getHostName(), not(equalTo("")));
    }


}