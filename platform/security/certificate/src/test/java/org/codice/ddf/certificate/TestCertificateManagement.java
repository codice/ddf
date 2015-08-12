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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Enumeration;

import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCertificateManagement {
    private static final Logger LOGGER = LoggerFactory.getLogger(TestCertificateManagement.class);

    @Test
    public void testMultipleCertsAddedToKeyStore() {
        try {
            CertificateManager.registerBCSecurityProvider();
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            PrivateKey privKey = keyPair.getPrivate();
            PublicKey pubKey = keyPair.getPublic();

            //Create an X509 signing request
            X509v3CertificateBuilder signingRequest = CertificateManager
                    .createDefaultX509SigningRequest(pubKey);

            //Sign the certificate using the signing request and private key
            X509Certificate signedCert = CertificateManager
                    .signX509CertificateSHA256RSA(signingRequest, privKey);

            X509v3CertificateBuilder otherSigningReq = CertificateManager
                    .createCustomX509SigningRequest("CN=" + CertificateManager.getHostName()
                                    + ", OU=someOu, O=someO, L=someLocal, C=someC",
                            "CN=subjectCN , OU=subjectOU, O=subjectO, L=subjectL, C=subjectC", 365,
                            pubKey);
            //Create an X509 signing request
            X509Certificate otherSignedCert = CertificateManager
                    .signX509CertificateSHA256RSA(otherSigningReq, privKey);
            //Sign the certificate using the signing request and private key
            X509v3CertificateBuilder anotherSigningReq = CertificateManager
                    .createCustomX509SigningRequest("CN=" + CertificateManager.getHostName()
                                    + ", OU=someOu, O=someO, L=someLocal, C=someC",
                            "CN=kkkkkkHBH , OU=subjectOU, O=subjectO, L=subjectL, C=subjectC", 365,
                            pubKey);

            X509Certificate anotherSignedCert = CertificateManager
                    .signX509CertificateSHA256RSA(otherSigningReq, privKey);

            JKSManager.createNewKeyStore("someKeyStoreName", "changeit");
            //Create certificate chain with the created certificates
            X509Certificate[] certChain = {signedCert, otherSignedCert, anotherSignedCert};
            //Add the cert to the KeyStore
            JKSManager.addCertToKeyStore("someKeyStoreName", "changeit", "alias!", "changeit",
                    certChain, privKey);

            //Retrieve the aliases from the KeyStore
            Enumeration<String> aliases = JKSManager.getAliases("someKeyStoreName", "changeit");
            assertTrue(aliases.nextElement().equals("alias!"));

            //Check the validity of the signed cert (these methods will throw exceptions if the signed certificate is not valid
            signedCert.checkValidity(new Date());
            signedCert.verify(pubKey);
            otherSignedCert.checkValidity(new Date());
            otherSignedCert.verify(pubKey);
            //Test cleanup
            new File("someKeyStoreName").delete();
        } catch (CertificateException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Test
    public void testDeleteFromKeyStore() {
        try {
            CertificateManager.registerBCSecurityProvider();
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            PrivateKey privKey = keyPair.getPrivate();
            PublicKey pubKey = keyPair.getPublic();

            //Create an X509 signing request
            X509v3CertificateBuilder signingRequest = CertificateManager
                    .createDefaultX509SigningRequest(pubKey);

            //Sign the certificate using the signing request and private key
            X509Certificate signedCert = CertificateManager
                    .signX509CertificateSHA256RSA(signingRequest, privKey);

            //Create an X509 signing request
            X509v3CertificateBuilder otherSigningReq = CertificateManager
                    .createCustomX509SigningRequest("CN=" + CertificateManager.getHostName()
                                    + ", OU=someOu, O=someO, L=someLocal, C=someC",
                            "CN=subjectCN , OU=subjectOU, O=subjectO, L=subjectL, C=subjectC", 365,
                            pubKey);
            //Sign the certificate using the signing request and private key
            X509Certificate otherSignedCert = CertificateManager
                    .signX509CertificateSHA256RSA(otherSigningReq, privKey);

            //Create an X509 signing request
            X509v3CertificateBuilder anotherSigningReq = CertificateManager
                    .createCustomX509SigningRequest("CN=" + CertificateManager.getHostName()
                                    + ", OU=someOu, O=someO, L=someLocal, C=someC",
                            "CN=kkkkkkHBH , OU=subjectOU, O=subjectO, L=subjectL, C=subjectC", 365,
                            pubKey);
            //Sign the certificate using the signing request and private key
            X509Certificate anotherSignedCert = CertificateManager
                    .signX509CertificateSHA256RSA(otherSigningReq, privKey);

            //Create the KeyStore
            JKSManager.createNewKeyStore("someDifferentKeyStore", "changeit");
            //Create certificate chain with the created certificates
            X509Certificate[] certChain = {signedCert, otherSignedCert, anotherSignedCert};
            //Add the cert to the KeyStore
            JKSManager.addCertToKeyStore("someDifferentKeyStore", "changeit", "analias", "changeit",
                    certChain, privKey);
            //Retrieve the aliases from the KeyStore
            Enumeration<String> aliases = JKSManager
                    .getAliases("someDifferentKeyStore", "changeit");
            assertTrue(aliases.nextElement().equals("analias"));
            //Remove the Cert from the KeyStore
            JKSManager.removeCertFromKeyStore("someDifferentKeyStore", "changeit", "analias");
            aliases = JKSManager.getAliases("someDifferentKeyStore", "changeit");
            assertFalse(aliases.hasMoreElements());

            //Check the validity of the signed certs (these methods will throw exceptions if the signed certificate is not valid
            signedCert.checkValidity(new Date());
            signedCert.verify(pubKey);
            otherSignedCert.checkValidity(new Date());
            otherSignedCert.verify(pubKey);
            anotherSignedCert.checkValidity(new Date());
            anotherSignedCert.verify(pubKey);
            //Test cleanup
            new File("someDifferentKeyStore").delete();
        } catch (CertificateException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Test
    public void testCreatingNewKeyStoreWithNewX509() {
        try {
            CertificateManager.registerBCSecurityProvider();
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024, new SecureRandom());
            KeyPair keyPair = keyPairGenerator.genKeyPair();
            PrivateKey privKey = keyPair.getPrivate();
            PublicKey pubKey = keyPair.getPublic();

            //Create an X509 signing request
            X509v3CertificateBuilder signingRequest = CertificateManager
                    .createDefaultX509SigningRequest(pubKey);

            //Sign the certificate using the signing request and private key
            X509Certificate signedCert = CertificateManager
                    .signX509CertificateSHA256RSA(signingRequest, privKey);

            X509v3CertificateBuilder otherSigningReq = CertificateManager
                    .createCustomX509SigningRequest("CN=" + CertificateManager.getHostName()
                                    + ", OU=someOu, O=someO, L=someLocal, C=someC",
                            "CN=subjectCN , OU=subjectOU, O=subjectO, L=subjectL, C=subjectC", 365,
                            pubKey);

            X509Certificate otherSignedCert = CertificateManager
                    .signX509CertificateSHA256RSA(otherSigningReq, privKey);

            //Certificate chain with the created certificates
            X509Certificate[] certChain = {signedCert, otherSignedCert};

            //save the certificates to a new KeyStore
            JKSManager.saveX509toNewKeystore(signedCert, privKey, "testKeyStoreName", "aliasname",
                    "changeit");
            Enumeration<String> aliases = JKSManager.getAliases("testKeyStoreName", "changeit");
            assertTrue(aliases.nextElement().equals("aliasname"));

            //Check the validity of the signed cert (these methods will throw exceptions if the signed certificate is not valid
            signedCert.checkValidity(new Date());
            signedCert.verify(pubKey);
            otherSignedCert.checkValidity(new Date());
            otherSignedCert.verify(pubKey);
            //Test cleanup
            new File("testKeyStoreName").delete();

        } catch (CertificateException | SignatureException | NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Test
    public void testGetKeyStore() {
        try {
            JKSManager.createNewKeyStore("nameForKeyStore", "changeit");
            KeyStore jks = JKSManager.getKeyStore("nameForKeyStore", "changeit");
            assertEquals(jks.size(), 0);
            new File("nameForKeyStore").delete();
        } catch (KeyStoreException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    @Test
    public void testLoadX509FromPEM() {
        URL pemURL = getClass().getResource("/TestLoadX509.pem");
        Object o = PEMManager.loadX509FromPEM(pemURL.getPath());
        assertTrue(o instanceof X509Certificate);
    }

    @Test
    public void testLoadPrivateKeyInfoFromPEM() {
        URL pemURL = getClass().getResource("/TestPrivateKeyInfo.pem");
        Object o = PEMManager.getPrivateKeyFromPEMFile(pemURL.getPath(), "changeit");
        assertTrue(o instanceof PrivateKey);
    }

    @Test
    public void testLoadPEMKeyPairFromPEM() {
        URL pemURL = getClass().getResource("/TestEncryptedKeyPair.pem");
        Object o = PEMManager.getPrivateKeyFromPEMFile(pemURL.getPath(), "changeit");
        assertTrue(o instanceof PrivateKey);
    }

    @Test
    public void testLoadPEMEncryptedKeyPairFromPEM() {
        URL pemURL = getClass().getResource("/TestPEMEncryptedKeyPair.pem");
        Object o = PEMManager.getPrivateKeyFromPEMFile(pemURL.getPath(), "changeit");
        assertTrue(o instanceof PrivateKey);
    }

}
