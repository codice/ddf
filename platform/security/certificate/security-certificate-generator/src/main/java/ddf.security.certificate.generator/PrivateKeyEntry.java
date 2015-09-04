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

import org.apache.commons.lang3.Validate;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public class PrivateKeyEntry {
    X509Certificate issuerCertificate;
    X509Certificate subjectCertificate;
    PrivateKey subjectPrivateKey;

    public PrivateKeyEntry(X509Certificate subjectCertificate, PrivateKey subjectPrivateKey, X509Certificate issuerCertificate) {
        Validate.isTrue(subjectCertificate != null, "Subject certificate cannot be null");
        Validate.isTrue(subjectPrivateKey != null, "Subject private key cannot be null");
        Validate.isTrue(issuerCertificate != null, "Issuer certificate cannot be null");
        this.subjectCertificate = subjectCertificate;
        this.issuerCertificate = issuerCertificate;
        this.subjectPrivateKey = subjectPrivateKey;
    }

    public X509Certificate getIssuerCertificate() {
        return issuerCertificate;
    }

    public PrivateKey getSubjectPrivateKey() {
        return subjectPrivateKey;
    }

    public X509Certificate getSubjectCertificate() {
        return subjectCertificate;
    }

    public X509Certificate[] getCertificateChain() {
        X509Certificate[] chain = new X509Certificate[2];
        chain[0] = getSubjectCertificate();
        chain[1] = getIssuerCertificate();
        return chain;
    }
}
