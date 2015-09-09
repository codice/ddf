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

import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PrivateKeyEntryTest {

    @Mock
    X509Certificate subjectCert;

    @Mock
    PrivateKey subjectPrivateKey;

    @Mock
    X509Certificate issuerCert;

    @Test
    public void createAndAccess() throws Exception {
        PrivateKeyEntry pkEntry = new PrivateKeyEntry(subjectCert, subjectPrivateKey, issuerCert);
        assertThat(pkEntry.getIssuerCertificate(), sameInstance(issuerCert));
        assertThat(pkEntry.getSubjectCertificate(), sameInstance(subjectCert));
        assertThat(pkEntry.getSubjectPrivateKey(), sameInstance(subjectPrivateKey));
        X509Certificate[] chain = pkEntry.getCertificateChain();
        assertThat(chain[0], sameInstance(subjectCert));
        assertThat(chain[1], sameInstance(issuerCert));
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidSubjectCert() {
        new PrivateKeyEntry(null, subjectPrivateKey, subjectCert);
    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidIssuerCert() {
        new PrivateKeyEntry(subjectCert, subjectPrivateKey, null);

    }

    @Test(expected = IllegalArgumentException.class)
    public void invalidKeyCert() {
        new PrivateKeyEntry(subjectCert, null, issuerCert);
    }

}