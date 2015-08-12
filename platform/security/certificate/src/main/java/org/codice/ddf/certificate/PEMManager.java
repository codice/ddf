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
package org.codice.ddf.certificate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.openssl.PEMDecryptorProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



        /*+----------------------------------------------------------------------
         ||
         ||  Class: PEMManager
         ||
         ||        Purpose:  Collection of static methods that extracts objects from PEM files. Currently PEMKeyPair, PrivateKeys, and X509Certificate's.
         ||
         ||  Inherits From:  None
         ||
         ||     Interfaces:  None
         ||
         |+-----------------------------------------------------------------------
         ||
         ||      Constants:  None
         ||
         |+-----------------------------------------------------------------------
         ||
         ||  Class Methods:     X509Certificate     loadX509FromPEM(String)
         ||                     PrivateKey          getPrivateKeyFromPEMFile(String, String)
         ||
         ||
         ++-----------------------------------------------------------------------*/

public class PEMManager {

    /**
     * ---------------------------------------------------------------------
     * Method: getPrivateKeyFromPEMFile
     * <p/>
     * Purpose:  Given a PEM file that contains a PEMEncryptedKeyPair or PrivateKeyInfo object, this method extracts and returns the private key contained within.
     * <p/>
     * Pre-condition:  None
     * <p/>
     * Post-condition: The privateKey found in the PEM file is returned
     *
     * @param fileName the name/path of the PEM file to use
     * @param password password used to decrypt the privateKey
     * @return PrivateKey that was extracted from the PEM file
     * -------------------------------------------------------------------
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(PEMManager.class);

    public static PrivateKey getPrivateKeyFromPEMFile(String fileName, String password) {
        KeyPair kp;
        PrivateKey privKey = null;
        CertificateManager.registerBCSecurityProvider();
        try {
            File privateKeyFile = new File(fileName);
            PEMParser pemParser = new PEMParser(new FileReader(privateKeyFile));
            Object objectFromPEM = pemParser.readObject();
            PEMDecryptorProvider decProv = new JcePEMDecryptorProviderBuilder()
                    .build(password.toCharArray());
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            if (objectFromPEM instanceof PEMEncryptedKeyPair) {
                kp = converter
                        .getKeyPair(((PEMEncryptedKeyPair) objectFromPEM).decryptKeyPair(decProv));
                privKey = kp.getPrivate();
            } else if (objectFromPEM instanceof PrivateKeyInfo) {
                PrivateKeyInfo pki = (PrivateKeyInfo) objectFromPEM;
                privKey = converter.getPrivateKey(pki);
            } else if (objectFromPEM instanceof PEMKeyPair) {
                PrivateKeyInfo pki = ((PEMKeyPair) objectFromPEM).getPrivateKeyInfo();
                privKey = converter.getPrivateKey(pki);
            }
        } catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        return privKey;
    }

    /**
     * ---------------------------------------------------------------------
     * Method: loadX509FromPEM
     * <p/>
     * Purpose:  Loads a X509 certificate from a valid PEM file that contains one
     * <p/>
     * Pre-condition:  BouncyCastle has been set as a security provider
     * <p/>
     * Post-condition: The X509Certificate contained in the PEM file is returned
     *
     * @param filePath path to the PEM file that contains the X509
     * @return X509Certificate extracted from PEM file
     * -------------------------------------------------------------------
     */

    public static X509Certificate loadX509FromPEM(String filePath) {
        X509Certificate cert = null;
        try {
            JcaX509CertificateConverter certConverter = new JcaX509CertificateConverter()
                    .setProvider("BC");
            PEMParser pp = new PEMParser(new InputStreamReader(new FileInputStream(filePath)));
            Object o = pp.readObject();
            if (o instanceof X509CertificateHolder) {
                X509CertificateHolder ch = (X509CertificateHolder) o;
                cert = certConverter.getCertificate(ch);
            } else {
                LOGGER.error("File does not contain valid x509!");
                throw new IOException();
            }
        } catch (CertificateException | IOException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return cert;
    }
}
