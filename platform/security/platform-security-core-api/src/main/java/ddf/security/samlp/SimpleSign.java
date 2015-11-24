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
package ddf.security.samlp;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.List;

import javax.ws.rs.core.UriBuilder;

import org.apache.commons.codec.binary.Base64;
import org.apache.cxf.rs.security.saml.sso.SSOConstants;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.saml.WSSSAMLKeyInfoProcessor;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SignatureTrustValidator;
import org.apache.wss4j.dom.validate.Validator;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.common.impl.SAMLObjectContentReference;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.Response;
import org.opensaml.security.SAMLSignatureProfileValidator;
import org.opensaml.xml.security.x509.BasicX509Credential;
import org.opensaml.xml.security.x509.X509KeyInfoGeneratorFactory;
import org.opensaml.xml.signature.KeyInfo;
import org.opensaml.xml.signature.Signature;
import org.opensaml.xml.signature.SignatureConstants;
import org.opensaml.xml.signature.SignatureValidator;
import org.opensaml.xml.validation.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class SimpleSign {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSign.class);

    private final SystemCrypto crypto;

    public SimpleSign(SystemCrypto systemCrypto) {
        crypto = systemCrypto;
    }

    public void signSamlObject(SignableSAMLObject samlObject) throws SignatureException {

        X509Certificate[] certificates = getSignatureCertificates();
        String sigAlgo = getSignatureAlgorithm(certificates[0]);
        PrivateKey privateKey = getSignaturePrivateKey();

        // Create the signature
        Signature signature = OpenSAMLUtil.buildSignature();
        if (signature == null) {
            throw new SignatureException("Unable to build signature.");
        }
        signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
        signature.setSignatureAlgorithm(sigAlgo);

        BasicX509Credential signingCredential = new BasicX509Credential();
        signingCredential.setEntityCertificate(certificates[0]);
        signingCredential.setPrivateKey(privateKey);

        signature.setSigningCredential(signingCredential);

        X509KeyInfoGeneratorFactory x509KeyInfoGeneratorFactory = new X509KeyInfoGeneratorFactory();
        x509KeyInfoGeneratorFactory.setEmitEntityCertificate(true);

        try {
            KeyInfo keyInfo = x509KeyInfoGeneratorFactory.newInstance().generate(signingCredential);
            signature.setKeyInfo(keyInfo);
        } catch (org.opensaml.xml.security.SecurityException e) {
            throw new SignatureException("Error generating KeyInfo from signing credential", e);
        }

        if (samlObject instanceof Response) {
            List<Assertion> assertions = ((Response) samlObject).getAssertions();
            for (Assertion assertion : assertions) {
                assertion.getSignature().setSigningCredential(signingCredential);
            }
        }

        samlObject.setSignature(signature);
        SAMLObjectContentReference contentRef =
                (SAMLObjectContentReference)signature.getContentReferences().get(0);
        contentRef.setDigestAlgorithm(SignatureConstants.ALGO_ID_DIGEST_SHA1);
        samlObject.releaseDOM();
        samlObject.releaseChildrenDOM(true);
    }

    public void signUriString(String queryParams, UriBuilder uriBuilder)
            throws SignatureException {
        X509Certificate[] certificates = getSignatureCertificates();
        String sigAlgo = getSignatureAlgorithm(certificates[0]);
        PrivateKey privateKey = getSignaturePrivateKey();
        java.security.Signature signature = getSignature(certificates[0], privateKey);

        String requestToSign;
        try {
            requestToSign = queryParams + "&" + SSOConstants.SIG_ALG + "=" + URLEncoder
                    .encode(sigAlgo, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new SignatureException(e);
        }

        try {
            signature.update(requestToSign.getBytes("UTF-8"));
        } catch (java.security.SignatureException | UnsupportedEncodingException e) {
            throw new SignatureException(e);
        }

        byte[] signatureBytes;
        try {
            signatureBytes = signature.sign();
        } catch (java.security.SignatureException e) {
            throw new SignatureException(e);
        }

        try {
            uriBuilder.queryParam(SSOConstants.SIG_ALG, URLEncoder.encode(sigAlgo, "UTF-8"));
            uriBuilder.queryParam(SSOConstants.SIGNATURE,
                    URLEncoder.encode(Base64.encodeBase64String(signatureBytes), "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            throw new SignatureException(e);
        }

    }

    private java.security.Signature getSignature(X509Certificate certificate, PrivateKey privateKey)
            throws SignatureException {
        String jceSigAlgo = "SHA1withRSA";
        if ("DSA".equalsIgnoreCase(certificate.getPublicKey().getAlgorithm())) {
            jceSigAlgo = "SHA1withDSA";
        }

        java.security.Signature signature;
        try {
            signature = java.security.Signature.getInstance(jceSigAlgo);
        } catch (NoSuchAlgorithmException e) {
            throw new SignatureException(e);
        }
        try {
            signature.initSign(privateKey);
        } catch (InvalidKeyException e) {
            throw new SignatureException(e);
        }
        return signature;
    }

    private String getSignatureAlgorithm(X509Certificate certificate) {
        String sigAlgo = SSOConstants.RSA_SHA1;
        String pubKeyAlgo = certificate.getPublicKey().getAlgorithm();

        if (pubKeyAlgo.equalsIgnoreCase("DSA")) {
            sigAlgo = SSOConstants.DSA_SHA1;
        }

        LOGGER.debug("Using Signature algorithm {}", sigAlgo);

        return sigAlgo;
    }

    private X509Certificate[] getSignatureCertificates() throws SignatureException {
        CryptoType cryptoType = new CryptoType(CryptoType.TYPE.ALIAS);
        cryptoType.setAlias(crypto.getSignatureAlias());
        X509Certificate[] issuerCerts;

        try {
            issuerCerts = crypto.getSignatureCrypto().getX509Certificates(cryptoType);
        } catch (WSSecurityException e) {
            throw new SignatureException(e);
        }

        if (issuerCerts == null) {
            throw new SignatureException("No certs were found to sign the request using name: " + crypto.getSignatureAlias());
        }

        return issuerCerts;
    }

    private PrivateKey getSignaturePrivateKey() throws SignatureException {
        PrivateKey privateKey;
        try {
            privateKey = crypto.getSignatureCrypto().getPrivateKey(crypto.getSignatureAlias(),
                    crypto.getSignaturePassword());
        } catch (WSSecurityException e) {
            throw new SignatureException(e);
        }
        return privateKey;
    }

    public boolean validateSignature(String queryParamsToValidate, String encodedSignature,
            String encodedPublicKey) throws SignatureException {
        try {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
            Certificate certificate = certificateFactory.generateCertificate(
                    new ByteArrayInputStream(Base64.decodeBase64(encodedPublicKey)));

            String jceSigAlgo = "SHA1withRSA";
            if ("DSA".equalsIgnoreCase(certificate.getPublicKey().getAlgorithm())) {
                jceSigAlgo = "SHA1withDSA";
            }

            java.security.Signature sig = java.security.Signature.getInstance(jceSigAlgo);
            sig.initVerify(certificate.getPublicKey());
            sig.update(queryParamsToValidate.getBytes("UTF-8"));
            return sig.verify(Base64.decodeBase64(encodedSignature));
        } catch (NoSuchAlgorithmException | InvalidKeyException | CertificateException | UnsupportedEncodingException
                | java.security.SignatureException e) {
            throw new SignatureException(e);
        }
    }

    public void validateSignature(Signature signature, Document doc) throws SignatureException {
        RequestData requestData = new RequestData();
        requestData.setSigVerCrypto(crypto.getSignatureCrypto());
        WSSConfig wssConfig = WSSConfig.getNewInstance();
        requestData.setWssConfig(wssConfig);

        SAMLKeyInfo samlKeyInfo = null;

        KeyInfo keyInfo = signature.getKeyInfo();
        if (keyInfo != null) {
            try {
                samlKeyInfo = SAMLUtil.getCredentialFromKeyInfo(keyInfo.getDOM(),
                        new WSSSAMLKeyInfoProcessor(requestData, new WSDocInfo(doc)),
                        crypto.getSignatureCrypto());
            } catch (WSSecurityException e) {
                throw new SignatureException("Unable to get KeyInfo.", e);
            }
        }
        if (samlKeyInfo == null) {
            throw new SignatureException("No KeyInfo supplied in the signature");
        }

        validateSignatureAndSamlKey(signature, samlKeyInfo);

        Credential trustCredential = new Credential();
        trustCredential.setPublicKey(samlKeyInfo.getPublicKey());
        trustCredential.setCertificates(samlKeyInfo.getCerts());
        Validator signatureValidator = new SignatureTrustValidator();

        try {
            signatureValidator.validate(trustCredential, requestData);
        } catch (WSSecurityException e) {
            throw new SignatureException("Error validating signature", e);
        }
    }

    private void validateSignatureAndSamlKey(Signature signature, SAMLKeyInfo samlKeyInfo) throws SignatureException {
        SAMLSignatureProfileValidator validator = new SAMLSignatureProfileValidator();
        try {
            validator.validate(signature);
        } catch (ValidationException e) {
            throw new SignatureException("Error validating the SAMLKey signature", e);
        }

        BasicX509Credential credential = new BasicX509Credential();
        if (samlKeyInfo.getCerts() != null) {
            credential.setEntityCertificate(samlKeyInfo.getCerts()[0]);
        } else if (samlKeyInfo.getPublicKey() != null) {
            credential.setPublicKey(samlKeyInfo.getPublicKey());
        } else {
            throw new SignatureException("Can't get X509Certificate or PublicKey to verify signature.");
        }
        SignatureValidator sigValidator = new SignatureValidator(credential);
        try {
            sigValidator.validate(signature);
        } catch (ValidationException e) {
            throw new SignatureException("Error validating the XML signature", e);
        }
    }

    public static class SignatureException extends Exception {
        public SignatureException() {
        }

        public SignatureException(Throwable cause) {
            super(cause);
        }

        public SignatureException(String message) {
            super(message);
        }

        public SignatureException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
