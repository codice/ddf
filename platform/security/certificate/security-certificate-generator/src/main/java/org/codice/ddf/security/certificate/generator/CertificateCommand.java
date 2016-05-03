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

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.apache.commons.lang.Validate;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.style.BCStyle;

import ddf.security.SecurityConstants;

public class CertificateCommand {

    /**
     * Pass in a string to use as the common name of the certificate to be generated.
     * Exception thrown if 0 arguments or more than 1 argument.
     *
     * @param args
     */
    public static void main(String args[]) {
        if (args.length != 2) {
            String canonicalName = CertificateCommand.class.getCanonicalName();
            String exCn = String.format("java %s -cn \"John Smith\"", canonicalName);

            String exDn = String.format(
                    "java %s -dn \"cn=John Whorfin, o=Yoyodyne, l=San Narciso, st=California, c=US\"",
                    canonicalName);
            String usage = String.format(
                    "%nUsage: java %s [-cn <common name>] | [-dn <distinguished name>]%n Examples: %n%s%n%s",
                    canonicalName, exCn, exDn);
            throw new RuntimeException(usage);
        }

        if (args[0].trim()
                .equalsIgnoreCase("-cn")) {
            configureDemoCert(args[1].trim());
        } else {
            String[] dn = Arrays.stream(args[1].split("[,]"))
                    .map(String::trim)
                    .toArray(String[]::new);
            configureDemoCertWithDN(dn);
        }
    }

    /**
     * Generates new signed certificate. The input parameter is used as the certificate's common name.
     * <p>
     * Postcondition is the server keystore is updated to include a private entry. The private
     * entry has the new certificate chain that connects the server to the Demo CA. The matching
     * private key is also stored in the entry.
     *
     * @param commonName string to use as the common name in the new certificate.
     * @return the string used as the common name in the new certificate
     */
    public static String configureDemoCert(String commonName) {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setCommonName(commonName);
        return configureCert(commonName, csr);
    }

    /**
     * Generates new signed certificate. The input parameter is the full set of attributes of the
     * distinguished name for the cert. It must include a single {@code CN} value for the common name.
     * <p>
     * Postcondition is the server keystore is updated to include a private entry. The private
     * entry has the new certificate chain that connects the server to the Demo CA. The matching
     * private key is also stored in the entry.
     *
     * @param dn String params in the form {@code attrKey=attrVal} composing a distinguished name.
     *           e.g. {@code configureDemoCertWithDN("cn=John Whorfin", "o=Yoyodyne", "l=San Narciso", "st=California", "c=US")}
     * @return the string used as the common name in the new certificate
     */
    public static String configureDemoCertWithDN(String... dn) {
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setDistinguishedName(dn);
        RDN[] rdns = csr.getSubjectName()
                .getRDNs(BCStyle.CN);

        Validate.isTrue(rdns != null && rdns.length == 1,
                "CN attribute must be included in distinguished name");
        assert rdns != null && rdns.length == 1;

        return configureCert(rdns[0].getFirst()
                .getValue()
                .toString(), csr);
    }

    protected static KeyStoreFile getKeyStoreFile() {
        return KeyStoreFile.openFile(SecurityConstants.getKeystorePath(),
                SecurityConstants.getKeystorePassword()
                        .toCharArray());
    }

    private static String configureCert(String commonName, CertificateSigningRequest csr) {
        CertificateAuthority demoCa = new DemoCertificateAuthority();
        KeyStore.PrivateKeyEntry pkEntry = demoCa.sign(csr);
        KeyStoreFile ksFile = getKeyStoreFile();
        for (String alias : ksFile.aliases()) {
            if (ksFile.isKey(alias)) {
                ksFile.deleteEntry(alias);
            }
        }
        ksFile.setEntry(commonName, pkEntry);
        ksFile.save();
        return ((X509Certificate) pkEntry.getCertificate()).getSubjectDN()
                .getName();
    }
}
