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
package org.apache.wss4j.common.crypto;

import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.apache.wss4j.common.ext.WSSecurityException;

/**
 * This extension of Merlin simply handles name constraints properly. Once Merlin's code has been
 * updated, this can be removed from the baseline.
 */
public class MerlinExtension extends Merlin {
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(Merlin.class);

    public MerlinExtension() {
        super();
    }

    public MerlinExtension(boolean loadCACerts, String cacertsPasswd) {
        super(loadCACerts, cacertsPasswd);
    }

    public MerlinExtension(Properties properties, ClassLoader loader, PasswordEncryptor passwordEncryptor)
            throws WSSecurityException, IOException {
        super(properties, loader, passwordEncryptor);
    }

    /**
     * Evaluate whether a given certificate chain should be trusted.
     *
     * @param certs Certificate chain to validate
     * @param enableRevocation whether to enable CRL verification or not
     * @param subjectCertConstraints A set of constraints on the Subject DN of the certificates
     *
     * @throws WSSecurityException if the certificate chain is invalid
     */
    public void verifyTrust(
            X509Certificate[] certs,
            boolean enableRevocation,
            Collection<Pattern> subjectCertConstraints
    ) throws WSSecurityException {
        //
        // FIRST step - Search the keystore for the transmitted certificate
        //
        if (certs.length == 1 && !enableRevocation) {
            String issuerString = certs[0].getIssuerX500Principal().getName();
            BigInteger issuerSerial = certs[0].getSerialNumber();

            CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ISSUER_SERIAL);
            cryptoType.setIssuerSerial(issuerString, issuerSerial);
            X509Certificate[] foundCerts = getX509Certificates(cryptoType);

            //
            // If a certificate has been found, the certificates must be compared
            // to ensure against phony DNs (compare encoded form including signature)
            //
            if (foundCerts != null && foundCerts[0] != null && foundCerts[0].equals(certs[0])) {
                try {
                    certs[0].checkValidity();
                } catch (CertificateExpiredException | CertificateNotYetValidException e) {
                    throw new WSSecurityException(
                            WSSecurityException.ErrorCode.FAILED_CHECK, e, "invalidCert"
                    );
                }
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            "Direct trust for certificate with " + certs[0].getSubjectX500Principal().getName()
                    );
                }
                return;
            }
        }

        //
        // SECOND step - Search for the issuer cert (chain) of the transmitted certificate in the
        // keystore or the truststore
        //
        List<Certificate[]> foundIssuingCertChains = null;
        String issuerString = certs[0].getIssuerX500Principal().getName();
        if (certs.length == 1) {

            Object subject = convertSubjectToPrincipal(issuerString);

            if (keystore != null) {
                foundIssuingCertChains = getCertificates(subject, keystore);
            }

            //If we can't find the issuer in the keystore then look at the truststore
            if ((foundIssuingCertChains == null || foundIssuingCertChains.isEmpty()) && truststore != null) {
                foundIssuingCertChains = getCertificates(subject, truststore);
            }

            if (foundIssuingCertChains == null || foundIssuingCertChains.isEmpty()
                    || foundIssuingCertChains.get(0).length < 1) {
                String subjectString = certs[0].getSubjectX500Principal().getName();
                if (LOG.isDebugEnabled()) {
                    LOG.debug(
                            "No certs found in keystore for issuer " + issuerString
                                    + " of certificate for " + subjectString
                    );
                }
                throw new WSSecurityException(
                        WSSecurityException.ErrorCode.FAILURE, "certpath", new Object[] {"No trusted certs found"}
                );
            }
        }

        //
        // THIRD step
        // Check the certificate trust path for the issuer cert chain
        //
        if (LOG.isDebugEnabled()) {
            LOG.debug(
                    "Preparing to validate certificate path for issuer " + issuerString
            );
        }

        try {
            Set<TrustAnchor> set = new HashSet<>();
            if (truststore != null) {
                Enumeration<String> truststoreAliases = truststore.aliases();
                buildTrustAnchors(set, truststoreAliases, truststore);
            }

            //
            // Add certificates from the keystore - only if there is no TrustStore, apart from
            // the case that the truststore is the JDK CA certs. This behaviour is preserved
            // for backwards compatibility reasons
            //
            if (keystore != null && (truststore == null || loadCACerts)) {
                Enumeration<String> aliases = keystore.aliases();
                buildTrustAnchors(set, aliases, keystore);
            }

            // Verify the trust path using the above settings
            String provider = getCryptoProvider();
            CertPathValidator validator = null;
            if (provider == null || provider.length() == 0) {
                validator = CertPathValidator.getInstance("PKIX");
            } else {
                validator = CertPathValidator.getInstance("PKIX", provider);
            }

            PKIXParameters param = createPKIXParameters(set, enableRevocation);

            // Generate cert path
            if (foundIssuingCertChains != null && !foundIssuingCertChains.isEmpty()) {
                java.security.cert.CertPathValidatorException validatorException = null;
                // Try each potential issuing cert path for a match
                for (Certificate[] foundCertChain : foundIssuingCertChains) {
                    X509Certificate[] x509certs = new X509Certificate[foundCertChain.length + 1];
                    x509certs[0] = certs[0];
                    System.arraycopy(foundCertChain, 0, x509certs, 1, foundCertChain.length);

                    List<X509Certificate> certList = Arrays.asList(x509certs);
                    CertPath path = getCertificateFactory().generateCertPath(certList);

                    try {
                        validator.validate(path, param);
                        // We have a valid cert path at this point so break
                        validatorException = null;
                        break;
                    } catch (java.security.cert.CertPathValidatorException e) {
                        validatorException = e;
                    }
                }

                if (validatorException != null) {
                    throw validatorException;
                }
            } else {
                List<X509Certificate> certList = Arrays.asList(certs);
                CertPath path = getCertificateFactory().generateCertPath(certList);

                validator.validate(path, param);
            }
        } catch (NoSuchProviderException | NoSuchAlgorithmException
                | CertificateException | InvalidAlgorithmParameterException
                | java.security.cert.CertPathValidatorException
                | KeyStoreException e) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, e, "certpath"
            );
        }

        // Finally check Cert Constraints
        if (!matchesSubjectDnPattern(certs[0], subjectCertConstraints)) {
            throw new WSSecurityException(WSSecurityException.ErrorCode.FAILED_AUTHENTICATION);
        }
    }

    private void buildTrustAnchors(Set<TrustAnchor> trustAnchorSet,
            Enumeration<String> truststoreAliases, KeyStore keyStore) throws KeyStoreException {
        while (truststoreAliases.hasMoreElements()) {
            String alias = truststoreAliases.nextElement();
            X509Certificate cert = (X509Certificate) keyStore.getCertificate(alias);
            if (cert != null) {
                //Sun cannot validate if the name constraints are anything other than null
                TrustAnchor trustAnchor = new TrustAnchor(cert, null);
                trustAnchorSet.add(trustAnchor);
            }
        }
    }

    private Object convertSubjectToPrincipal(String subjectDN) {
        //
        // Convert the subject DN to a java X500Principal object first. This is to ensure
        // interop with a DN constructed from .NET, where e.g. it uses "S" instead of "ST".
        // Then convert it to a BouncyCastle X509Name, which will order the attributes of
        // the DN in a particular way (see WSS-168). If the conversion to an X500Principal
        // object fails (e.g. if the DN contains "E" instead of "EMAILADDRESS"), then fall
        // back on a direct conversion to a BC X509Name
        //
        Object subject;
        try {
            X500Principal subjectRDN = new X500Principal(subjectDN);
            subject = createBCX509Name(subjectRDN.getName());
        } catch (java.lang.IllegalArgumentException ex) {
            subject = createBCX509Name(subjectDN);
        }

        return subject;
    }

    /**
     * Get an X509 Certificate (chain) of the X500Principal argument in the supplied KeyStore. If multiple
     * certs match the Subject DN, then multiple cert chains are returned.
     * @param subjectRDN either an X500Principal or a BouncyCastle X509Name instance.
     * @param store The KeyStore
     * @return an X509 Certificate (chain)
     * @throws WSSecurityException
     */
    private List<Certificate[]> getCertificates(Object subjectRDN, KeyStore store)
            throws WSSecurityException {
        LOG.debug("Searching keystore for cert with Subject {}", subjectRDN);
        List<Certificate[]> foundCerts = new ArrayList<>();
        try {
            for (Enumeration<String> e = store.aliases(); e.hasMoreElements();) {
                String alias = e.nextElement();
                Certificate[] certs = store.getCertificateChain(alias);
                if (certs == null || certs.length == 0) {
                    // no cert chain, so lets check if getCertificate gives us a result.
                    Certificate cert = store.getCertificate(alias);
                    if (cert != null) {
                        certs = new Certificate[]{cert};
                    }
                }
                if (certs != null && certs.length > 0 && certs[0] instanceof X509Certificate) {
                    X500Principal foundRDN = ((X509Certificate)certs[0]).getSubjectX500Principal();
                    Object certName = createBCX509Name(foundRDN.getName());

                    if (subjectRDN.equals(certName)) {
                        LOG.debug("Subject certificate match found using keystore alias {}", alias);
                        foundCerts.add(certs);
                    }
                }
            }
        } catch (KeyStoreException e) {
            throw new WSSecurityException(
                    WSSecurityException.ErrorCode.FAILURE, e, "keystore"
            );
        }

        if (foundCerts.isEmpty()) {
            LOG.debug("No Subject match found in keystore");
        }
        return foundCerts;
    }
}
