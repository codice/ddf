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

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.security.cert.CertificateException;
import java.util.Hashtable;
import java.util.Map;

public class CertificateGeneratorBean implements CertificateGeneratorInterface {
    private static final Logger LOGGER = LoggerFactory.getLogger(CertificateGeneratorBean.class);

    private PkiTools pkiTools = new PkiTools();

    public Map<String, byte[]> getCertificate() throws CertificateException {

        Map<String, byte[]> table = new Hashtable<>();
        CertificateAuthority ca = new CertificateAuthority();
        CertificateSigningRequest csr = new CertificateSigningRequest();
        DateTime notAfter = DateTime.now().plusYears(100);
        csr.setNotAfter(notAfter);
        csr.setCertificateAuthority(ca);
        try {
            csr.setCommonNameToHostname();
        } catch (UnknownHostException e) {
            csr.setCommonName("UNKNOWN HOST");
            LOGGER.warn("Could not determine host name when creating X509 certificate");
        }
        csr.build();
        table.put("endEntityCertificate", csr.getSignedCertificate().getEncoded());
        table.put("endEntityPublicKey", pkiTools.keyToDer(csr.getPublicKey()));
        table.put("endEntityPrivateKey", pkiTools.keyToDer(csr.getPrivateKey()));
        table.put("rootCertificate", ca.getCertificate().getEncoded());
        return table;
    }
}
