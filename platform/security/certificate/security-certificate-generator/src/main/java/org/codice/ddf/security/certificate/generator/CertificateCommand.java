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
 */

package org.codice.ddf.security.certificate.generator;

import java.security.KeyStore;
import java.security.cert.X509Certificate;

public class CertificateCommand {

    /**
     * Pass in a string to use as the common name of the certificate to be generated.
     * Exception thrown if 0 arguments or more than 1 argument.
     *
     * @param args
     */
    public static void main(String args[]) {

        if (args.length != 1) {
            throw new RuntimeException(
                    "Expecting exactly one command-line argument. Detected " + args.length
                            + " arguments.");
        }
        String commonname = args[0];
        configureDemoCert(commonname);
        removeKey("localhost");
    }

    /**
     * Generates new signed certificate. The input parameter is used as the certificate's common name.
     * Postcondition is the server keystore is updated to include a private entry. The private
     * entry has the new certificate chain  that connects the server to the Demo CA. The matching
     * private key is also stored in the entry.
     *
     * @param commonName string to use as the common name in the new certificate.
     * @return the string used as the common name in the new certificate
     */

    public static String configureDemoCert(String commonName) {
        CertificateAuthority demoCa = new DemoCertificateAuthority();
        CertificateSigningRequest csr = new CertificateSigningRequest();
        csr.setCommonName(commonName);
        KeyStore.PrivateKeyEntry pkEntry = demoCa.sign(csr);
        KeyStoreFile ksFile = getKeyStoreFile();
        ksFile.setEntry(commonName, pkEntry);
        ksFile.save();
        String distinguishedName = ((X509Certificate) pkEntry.getCertificate()).getSubjectDN()
                .getName();
        return distinguishedName;
    }

    /**
     * Remove key from server keystore. The input is the key's alias in the keystore.
     * The method returns true if the key is no longer in the keystore, or false if the
     * key is in the keystore.
     *
     * @param alias
     * @return true if the key is not in the keystore, otherwise false
     */
    public static Boolean removeKey(String alias) {
        KeyStoreFile ksFile = getKeyStoreFile();
        Boolean success = ksFile.deleteEntry(alias);
        ksFile.save();
        return success;
    }

    protected static KeyStoreFile getKeyStoreFile() {
        return KeyStoreFile.openFile(System.getProperty("javax.net.ssl.keyStore"),
                System.getProperty("javax.net.ssl.keyStorePassword")
                        .toCharArray());
    }

}
