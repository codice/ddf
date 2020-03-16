/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.samlp.impl;

import static org.apache.commons.lang.CharEncoding.UTF_8;
import static org.opensaml.xmlsec.signature.support.SignatureConstants.ALGO_ID_SIGNATURE_DSA_SHA256;

import ddf.security.samlp.SignatureException;
import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import javax.annotation.Nullable;
import javax.ws.rs.core.UriBuilder;
import javax.xml.stream.XMLStreamException;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.rs.security.saml.sso.SSOConstants;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.WSS4JConstants;
import org.apache.wss4j.common.crypto.CryptoType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SAMLKeyInfo;
import org.apache.wss4j.common.saml.SAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.saml.WSSSAMLKeyInfoProcessor;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SignatureTrustValidator;
import org.apache.wss4j.dom.validate.Validator;
import org.apache.xml.security.algorithms.JCEMapper;
import org.opensaml.saml.common.SAMLObjectContentReference;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.keyinfo.impl.X509KeyInfoGeneratorFactory;
import org.opensaml.xmlsec.signature.KeyInfo;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.opensaml.xmlsec.signature.support.provider.ApacheSantuarioSignatureValidationProviderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SimpleSign {

  private static final Logger LOGGER = LoggerFactory.getLogger(SimpleSign.class);

  static {
    OpenSAMLUtil.initSamlEngine();
  }

  private static final String RSA_ALGO_URI = WSS4JConstants.RSA;
  private static final String RSA_ALGO_JCE = JCEMapper.translateURItoJCEID(WSS4JConstants.RSA);
  private static final String DSA_ALGO_URI = ALGO_ID_SIGNATURE_DSA_SHA256;
  private static final String DSA_ALGO_JCE =
      JCEMapper.translateURItoJCEID(ALGO_ID_SIGNATURE_DSA_SHA256);

  private final SystemCrypto crypto;

  public SimpleSign(SystemCrypto systemCrypto) {
    crypto = systemCrypto;
  }

  public void resignAssertion(Assertion assertion) throws SignatureException {
    final Signature signature = assertion.getSignature();

    if (signature == null) {
      signSamlObject(assertion);
      return;
    }

    final String digestAlgorithm =
        ((SAMLObjectContentReference) signature.getContentReferences().get(0)).getDigestAlgorithm();

    signSamlObject(
        assertion,
        signature.getSignatureAlgorithm(),
        signature.getCanonicalizationAlgorithm(),
        digestAlgorithm);
  }

  public void signSamlObject(SignableSAMLObject samlObject) throws SignatureException {
    X509Certificate[] certificates = getSignatureCertificates();
    String sigAlgo = getSignatureAlgorithmURI(certificates[0]);
    signSamlObject(
        samlObject,
        sigAlgo,
        SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS,
        SignatureConstants.ALGO_ID_DIGEST_SHA1);
  }

  /**
   * Forces a signature value to be added to a {@code SignableSAMLObject}.
   *
   * <p>This is needed in the case of a message wrapped in a SOAP Envelope where the {@code
   * org.opensaml.xmlsec.signature.support.Signer} will not be triggered.
   *
   * @param samlObject signable object to be signed
   * @param <T> instance of a class that extends {@code SignableSAMLObject}
   * @return copy of the input object with a valid signature value
   * @throws SignatureException
   * @throws WSSecurityException
   * @throws XMLStreamException
   */
  public <T extends SignableSAMLObject> T forceSignSamlObject(T samlObject)
      throws SignatureException, WSSecurityException, XMLStreamException {
    signSamlObject(samlObject);

    Document doc = DOMUtils.createDocument();
    doc.appendChild(doc.createElement("root"));
    Element reqElem = OpenSAMLUtil.toDom(samlObject, doc);

    String nodeToString = DOM2Writer.nodeToString(reqElem);

    final Document responseDoc =
        StaxUtils.read(new ByteArrayInputStream(nodeToString.getBytes(StandardCharsets.UTF_8)));
    return (T) OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
  }

  public void signUriString(String queryParams, UriBuilder uriBuilder) throws SignatureException {
    X509Certificate[] certificates = getSignatureCertificates();
    String sigAlgo = getSignatureAlgorithmURI(certificates[0]);
    PrivateKey privateKey = getSignaturePrivateKey();
    java.security.Signature signature = initSign(certificates[0], privateKey);

    String requestToSign;
    try {
      requestToSign =
          queryParams + "&" + SSOConstants.SIG_ALG + "=" + URLEncoder.encode(sigAlgo, UTF_8);
    } catch (UnsupportedEncodingException e) {
      throw new SignatureException(e);
    }

    try {
      signature.update(requestToSign.getBytes(UTF_8));
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
      uriBuilder.queryParam(SSOConstants.SIG_ALG, URLEncoder.encode(sigAlgo, UTF_8));
      uriBuilder.queryParam(
          SSOConstants.SIGNATURE,
          URLEncoder.encode(Base64.getEncoder().encodeToString(signatureBytes), UTF_8));
    } catch (UnsupportedEncodingException e) {
      throw new SignatureException(e);
    }
  }

  private java.security.Signature initSign(X509Certificate certificate, PrivateKey privateKey)
      throws SignatureException {
    java.security.Signature signature = getSignature(certificate);
    try {
      signature.initSign(privateKey);
    } catch (InvalidKeyException e) {
      throw new SignatureException(e);
    }
    return signature;
  }

  private java.security.Signature getSignature(X509Certificate certificate)
      throws SignatureException {
    java.security.Signature signature;
    try {
      if ("DSA".equalsIgnoreCase(certificate.getPublicKey().getAlgorithm())) {
        signature = java.security.Signature.getInstance(DSA_ALGO_JCE, "BC");
      } else {
        signature = java.security.Signature.getInstance(RSA_ALGO_JCE);
      }
    } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
      throw new SignatureException(e);
    }
    return signature;
  }

  private String getSignatureAlgorithmURI(X509Certificate certificate) {
    String sigAlgoUri =
        ("DSA".equalsIgnoreCase(certificate.getPublicKey().getAlgorithm()))
            ? DSA_ALGO_URI
            : RSA_ALGO_URI;

    LOGGER.debug("Using Signature algorithm {}", sigAlgoUri);
    return sigAlgoUri;
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
      throw new SignatureException(
          "No certs were found to sign the request using name: " + crypto.getSignatureAlias());
    }

    return issuerCerts;
  }

  private PrivateKey getSignaturePrivateKey() throws SignatureException {
    PrivateKey privateKey;
    try {
      privateKey =
          crypto
              .getSignatureCrypto()
              .getPrivateKey(crypto.getSignatureAlias(), crypto.getSignaturePassword());
    } catch (WSSecurityException e) {
      throw new SignatureException(e);
    }
    return privateKey;
  }

  public boolean validateSignature(
      String sigAlg,
      String queryParamsToValidate,
      String encodedSignature,
      @Nullable String encodedPublicKey)
      throws SignatureException {
    if (encodedPublicKey == null) {
      LOGGER.warn(
          "Could not verify the signature of request because there was no signing certificate. Ensure that the IdP Metadata includes a signing certificate.");
      return false;
    }

    try {
      CertificateFactory certificateFactory = CertificateFactory.getInstance("X509");
      Certificate certificate =
          certificateFactory.generateCertificate(
              new ByteArrayInputStream(Base64.getMimeDecoder().decode(encodedPublicKey)));

      java.security.Signature sig;
      String jceSigAlg = JCEMapper.translateURItoJCEID(sigAlg);

      if (jceSigAlg == null) {
        throw new SignatureException(
            new NoSuchAlgorithmException(
                String.format("The Signature Algorithm %s is not supported.", sigAlg)));
      }

      try {
        sig = java.security.Signature.getInstance(jceSigAlg);
      } catch (NoSuchAlgorithmException e) {
        throw new SignatureException(e);
      }

      sig.initVerify(certificate.getPublicKey());
      sig.update(queryParamsToValidate.getBytes(StandardCharsets.UTF_8));
      return sig.verify(Base64.getMimeDecoder().decode(encodedSignature));
    } catch (InvalidKeyException
        | CertificateException
        | java.security.SignatureException
        | IllegalArgumentException e) {
      throw new SignatureException(e);
    }
  }

  public void validateSignature(Signature signature, Document doc) throws SignatureException {
    RequestData requestData = new RequestData();
    requestData.setWsDocInfo(new WSDocInfo(doc));
    requestData.setSigVerCrypto(crypto.getSignatureCrypto());
    WSSConfig wssConfig = WSSConfig.getNewInstance();
    requestData.setWssConfig(wssConfig);

    SAMLKeyInfo samlKeyInfo = null;

    KeyInfo keyInfo = signature.getKeyInfo();
    if (keyInfo != null) {
      try {
        samlKeyInfo =
            SAMLUtil.getCredentialFromKeyInfo(
                keyInfo.getDOM(),
                new WSSSAMLKeyInfoProcessor(requestData),
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

  private void validateSignatureAndSamlKey(Signature signature, SAMLKeyInfo samlKeyInfo)
      throws SignatureException {
    SAMLSignatureProfileValidator validator = new SAMLSignatureProfileValidator();
    try {
      validator.validate(signature);
    } catch (org.opensaml.xmlsec.signature.support.SignatureException e) {
      throw new SignatureException("Error validating the SAMLKey signature", e);
    }

    BasicX509Credential credential = null;
    if (samlKeyInfo.getCerts() != null) {
      credential = new BasicX509Credential(samlKeyInfo.getCerts()[0]);
    } else {
      throw new SignatureException("Can't get X509Certificate or PublicKey to verify signature.");
    }

    ClassLoader threadLoader = null;
    try {
      threadLoader = Thread.currentThread().getContextClassLoader();
      Thread.currentThread()
          .setContextClassLoader(
              ApacheSantuarioSignatureValidationProviderImpl.class.getClassLoader());
      SignatureValidator.validate(signature, credential);
    } catch (org.opensaml.xmlsec.signature.support.SignatureException e) {
      throw new SignatureException("Error validating the XML signature", e);
    } finally {
      if (threadLoader != null) {
        Thread.currentThread().setContextClassLoader(threadLoader);
      }
    }
  }

  private void signSamlObject(
      SignableSAMLObject samlObject, String sigAlgo, String canonAlgo, String digestAlgo)
      throws SignatureException {
    X509Certificate[] certificates = getSignatureCertificates();
    PrivateKey privateKey = getSignaturePrivateKey();

    // Create the signature
    Signature signature = OpenSAMLUtil.buildSignature();
    if (signature == null) {
      throw new SignatureException("Unable to build signature.");
    }

    signature.setCanonicalizationAlgorithm(canonAlgo);
    signature.setSignatureAlgorithm(sigAlgo);

    BasicX509Credential signingCredential = new BasicX509Credential(certificates[0]);
    signingCredential.setPrivateKey(privateKey);

    signature.setSigningCredential(signingCredential);

    X509KeyInfoGeneratorFactory x509KeyInfoGeneratorFactory = new X509KeyInfoGeneratorFactory();
    x509KeyInfoGeneratorFactory.setEmitEntityCertificate(true);

    try {
      KeyInfo keyInfo = x509KeyInfoGeneratorFactory.newInstance().generate(signingCredential);
      signature.setKeyInfo(keyInfo);
    } catch (org.opensaml.security.SecurityException e) {
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
        (SAMLObjectContentReference) signature.getContentReferences().get(0);
    contentRef.setDigestAlgorithm(digestAlgo);

    samlObject.releaseDOM();
    samlObject.releaseChildrenDOM(true);
  }
}
