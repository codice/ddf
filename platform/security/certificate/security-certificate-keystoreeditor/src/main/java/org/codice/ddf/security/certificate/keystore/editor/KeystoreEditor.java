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
package org.codice.ddf.security.certificate.keystore.editor;

import ddf.security.SecurityConstants;
import ddf.security.audit.SecurityLogger;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.ssl.PKCS8Key;
import org.apache.http.HttpHost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.bouncycastle.asn1.ASN1Encodable;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.cms.CMSObjectIdentifiers;
import org.bouncycastle.asn1.cms.ContentInfo;
import org.bouncycastle.asn1.cms.OriginatorInfo;
import org.bouncycastle.asn1.cms.SignedData;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x500.style.IETFUtils;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateHolder;
import org.bouncycastle.cms.CMSEnvelopedData;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMEncryptedKeyPair;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeystoreEditor implements KeystoreEditorMBean {

  private static final Logger LOGGER = LoggerFactory.getLogger(KeystoreEditor.class);

  protected static final String CERT_TYPE = "application/x-x509-ca-cert";

  protected static final String PEM_TYPE = "application/x-pem-file";

  protected static final String DER_TYPE = "application/x-x509-ca-cert";

  protected static final String PKCS7_TYPE = "application/x-pkcs7-certificates";

  protected static final String PKCS12_TYPE = "application/x-pkcs12";

  protected static final String JKS_TYPE = "application/x-java-keystore";

  private KeyStore keyStore;

  private KeyStore trustStore;

  private SecurityLogger securityLogger;

  public KeystoreEditor() {
    registerMbean();
    addProvider();
    init();
  }

  public void setSecurityLogger(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }

  private void addProvider() {
    Security.addProvider(new BouncyCastleProvider());
  }

  private void init() {
    try {
      keyStore = SecurityConstants.newKeystore();
      trustStore = SecurityConstants.newTruststore();
    } catch (KeyStoreException e) {
      LOGGER.info(
          "Unable to create keystore instance of type {}",
          System.getProperty(SecurityConstants.KEYSTORE_TYPE),
          e);
    }
    Path keyStoreFile = Paths.get(SecurityConstants.getKeystorePath());
    Path trustStoreFile = Paths.get(SecurityConstants.getTruststorePath());
    Path ddfHomePath = Paths.get(System.getProperty("ddf.home"));
    if (!keyStoreFile.isAbsolute()) {
      keyStoreFile = Paths.get(ddfHomePath.toString(), keyStoreFile.toString());
    }
    if (!trustStoreFile.isAbsolute()) {
      trustStoreFile = Paths.get(ddfHomePath.toString(), trustStoreFile.toString());
    }
    String keyStorePassword = SecurityConstants.getKeystorePassword();
    String trustStorePassword = SecurityConstants.getTruststorePassword();
    if (!Files.isReadable(keyStoreFile) || !Files.isReadable(trustStoreFile)) {
      LOGGER.info(
          "Unable to read system key/trust store files: [ {} ] [ {} ]",
          keyStoreFile,
          trustStoreFile);
      return;
    }
    try (InputStream kfis = Files.newInputStream(keyStoreFile)) {
      keyStore.load(kfis, keyStorePassword.toCharArray());
    } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
      LOGGER.info("Unable to load system key file.", e);
      try {
        keyStore.load(null, null);
      } catch (NoSuchAlgorithmException | CertificateException | IOException ignore) {
      }
    }
    try (InputStream tfis = Files.newInputStream(trustStoreFile)) {
      trustStore.load(tfis, trustStorePassword.toCharArray());
    } catch (NoSuchAlgorithmException | CertificateException | IOException e) {
      LOGGER.info("Unable to load system trust file.", e);
      try {
        trustStore.load(null, null);
      } catch (NoSuchAlgorithmException | CertificateException | IOException ignore) {
      }
    }
  }

  private void registerMbean() {
    ObjectName objectName = null;
    MBeanServer mBeanServer = null;
    try {
      objectName = new ObjectName(KeystoreEditor.class.getName() + ":service=keystore");
      mBeanServer = ManagementFactory.getPlatformMBeanServer();
    } catch (MalformedObjectNameException e) {
      LOGGER.info("Unable to create Keystore Editor MBean.", e);
    }
    if (mBeanServer != null) {
      try {
        try {
          mBeanServer.registerMBean(this, objectName);
          LOGGER.debug("Registered Keystore Editor MBean under object name: {}", objectName);
        } catch (InstanceAlreadyExistsException e) {
          // Try to remove and re-register
          mBeanServer.unregisterMBean(objectName);
          mBeanServer.registerMBean(this, objectName);
          LOGGER.debug("Re-registered Keystore Editor MBean");
        }
      } catch (Exception e) {
        LOGGER.info("Could not register MBean [{}].", objectName, e);
      }
    }
  }

  @Override
  public List<Map<String, Object>> getKeystore() {
    return getKeyStoreInfo(keyStore);
  }

  @Override
  public List<Map<String, Object>> getTruststore() {
    return getKeyStoreInfo(trustStore);
  }

  private List<Map<String, Object>> getKeyStoreInfo(KeyStore store) {
    List<Map<String, Object>> storeEntries = new ArrayList<>();
    try {
      Enumeration<String> aliases = store.aliases();
      while (aliases.hasMoreElements()) {
        String alias = aliases.nextElement();
        Map<String, Object> aliasMap = new HashMap<>();
        Certificate certificate = store.getCertificate(alias);
        boolean isKey = store.isKeyEntry(alias);
        aliasMap.put("alias", alias);
        aliasMap.put("isKey", isKey);
        aliasMap.put("type", certificate.getType());
        aliasMap.put("format", certificate.getPublicKey().getFormat());
        aliasMap.put("algorithm", certificate.getPublicKey().getAlgorithm());
        storeEntries.add(aliasMap);
      }
    } catch (KeyStoreException e) {
      LOGGER.info("Unable to read entries from keystore.", e);
    }
    return storeEntries;
  }

  @Override
  public void addPrivateKey(
      String alias,
      String keyPassword,
      String storePassword,
      String data,
      String type,
      String fileName)
      throws KeystoreEditorException {
    securityLogger.audit("Adding alias {} to private key", alias);
    LOGGER.trace("Received data {}", data);
    Path keyStoreFile = Paths.get(SecurityConstants.getKeystorePath());
    if (!keyStoreFile.isAbsolute()) {
      Path ddfHomePath = Paths.get(System.getProperty("ddf.home"));
      keyStoreFile = Paths.get(ddfHomePath.toString(), keyStoreFile.toString());
    }
    String keyStorePassword = SecurityConstants.getKeystorePassword();
    addToStore(
        alias,
        keyPassword,
        storePassword,
        data,
        type,
        fileName,
        keyStoreFile.toString(),
        keyStorePassword,
        keyStore);
  }

  @Override
  public void addTrustedCertificate(
      String alias,
      String keyPassword,
      String storePassword,
      String data,
      String type,
      String fileName)
      throws KeystoreEditorException {
    securityLogger.audit("Adding alias {} to trust store", alias);
    LOGGER.trace("Received data {}", data);
    Path trustStoreFile = Paths.get(SecurityConstants.getTruststorePath());
    if (!trustStoreFile.isAbsolute()) {
      Path ddfHomePath = Paths.get(System.getProperty("ddf.home"));
      trustStoreFile = Paths.get(ddfHomePath.toString(), trustStoreFile.toString());
    }
    String trustStorePassword = SecurityConstants.getTruststorePassword();
    addToStore(
        alias,
        keyPassword,
        storePassword,
        data,
        type,
        fileName,
        trustStoreFile.toString(),
        trustStorePassword,
        trustStore);
  }

  @Override
  public List<Map<String, Object>> addTrustedCertificateFromUrl(String url) {
    SSLSocket socket = null;
    String decodedUrl = null;
    List<Map<String, Object>> resultList = new ArrayList<>();
    OutputStream fos = null;
    try {
      decodedUrl = new String(Base64.getDecoder().decode(url), "UTF-8");
      socket = createNonVerifyingSslSocket(decodedUrl);
      socket.startHandshake();
      X509Certificate[] peerCertificateChain =
          (X509Certificate[]) socket.getSession().getPeerCertificates();
      for (X509Certificate certificate : peerCertificateChain) {
        try {
          X500Name x500name = new JcaX509CertificateHolder(certificate).getSubject();
          RDN cn = x500name.getRDNs(BCStyle.CN)[0];
          String cnStr = IETFUtils.valueToString(cn.getFirst().getValue());
          trustStore.setCertificateEntry(cnStr, certificate);
          resultList.add(Collections.singletonMap("success", true));
        } catch (CertificateEncodingException e) {
          resultList.add(Collections.singletonMap("success", false));
          LOGGER.info("Unable to store certificate: {}", certificate.toString(), e);
        }
      }
      Path trustStoreFile = Paths.get(SecurityConstants.getTruststorePath());
      if (!trustStoreFile.isAbsolute()) {
        Path ddfHomePath = Paths.get(System.getProperty("ddf.home"));
        trustStoreFile = Paths.get(ddfHomePath.toString(), trustStoreFile.toString());
      }
      String keyStorePassword = SecurityConstants.getTruststorePassword();
      fos = Files.newOutputStream(trustStoreFile);
      trustStore.store(fos, keyStorePassword.toCharArray());
    } catch (IOException | GeneralSecurityException e) {
      LOGGER.info(
          "Unable to add certificate(s) to trust store from URL: {}",
          (decodedUrl != null) ? decodedUrl : url,
          e);
    } finally {
      IOUtils.closeQuietly(socket);
      IOUtils.closeQuietly(fos);
    }
    return resultList;
  }

  @Override
  public List<Map<String, Object>> certificateDetails(String url) {
    List<Map<String, Object>> certificates = new ArrayList<>();
    SSLSocket socket = null;
    String decodedUrl = null;
    try {
      decodedUrl = new String(Base64.getDecoder().decode(url), "UTF-8");
      socket = createNonVerifyingSslSocket(decodedUrl);
      socket.startHandshake();
      X509Certificate[] peerCertificateChain =
          (X509Certificate[]) socket.getSession().getPeerCertificates();
      for (X509Certificate certificate : peerCertificateChain) {
        Map<String, Object> certMap = new HashMap<>();
        certMap.put("type", certificate.getType());
        certMap.put("issuerDn", certificate.getIssuerDN());
        certMap.put("subjectDn", certificate.getSubjectDN());
        certMap.put("extendedKeyUsage", certificate.getExtendedKeyUsage());
        certMap.put("notAfter", certificate.getNotAfter().toString());
        certMap.put("notBefore", certificate.getNotBefore().toString());
        certMap.put("thumbprint", DigestUtils.sha1Hex(certificate.getEncoded()));
        certificates.add(certMap);
      }
    } catch (IOException | GeneralSecurityException e) {
      LOGGER.info(
          "Unable to parse certificate from URL: {}", (decodedUrl != null) ? decodedUrl : url, e);
    } finally {
      IOUtils.closeQuietly(socket);
    }

    return certificates;
  }

  SSLSocket createNonVerifyingSslSocket(String decodedUrl)
      throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException,
          UnrecoverableKeyException, IOException {
    URL httpsUrl = new URL(decodedUrl);
    SSLContext sslContext = SSLContext.getInstance("TLS");
    TrustManager tm = new NonVerifyingTrustManager();
    sslContext.init(null, new TrustManager[] {tm}, null);
    SSLConnectionSocketFactory sslSocketFactory = new NonVerifyingSslSocketFactory(sslContext);
    return (SSLSocket)
        sslSocketFactory.connectSocket(
            30000,
            null,
            new HttpHost(httpsUrl.getHost()),
            new InetSocketAddress(httpsUrl.getHost(), httpsUrl.getPort()),
            null,
            null);
  }

  @Override
  public List<String> replaceSystemStores(
      String fqdn,
      String keyPassword,
      String keystorePassword,
      String keystoreData,
      String keystoreFileName,
      String truststorePassword,
      String truststoreData,
      String truststoreFileName)
      throws KeystoreEditorException {

    List<String> errors = new ArrayList<>();

    if (StringUtils.isEmpty(keystoreFileName)
        || StringUtils.isEmpty(keystoreData)
        || StringUtils.isEmpty(keystorePassword)
        || StringUtils.isEmpty(keyPassword)) {
      errors.add("Some of the required keystore fields are missing");
    }

    if (StringUtils.isEmpty(truststoreFileName)
        || StringUtils.isEmpty(truststoreData)
        || StringUtils.isEmpty(truststorePassword)) {
      errors.add("Some of the required truststore fields are missing");
    }

    if (!errors.isEmpty()) {
      return errors;
    }

    try {
      if (StringUtils.isEmpty(fqdn)
          || !validKeystoreAlias(fqdn, keystorePassword, keystoreData, keystoreFileName)) {
        errors.add("Keystore does not contain the required key for " + fqdn);
        return errors;
      }

      deleteAllSystemStoreEntries();

      addPrivateKey(fqdn, keyPassword, keystorePassword, keystoreData, null, keystoreFileName);

      addTrustedCertificate(
          fqdn, null, truststorePassword, truststoreData, null, truststoreFileName);
    } catch (Exception e) {
      LOGGER.info("Unable to replace system stores.", e);
      errors.add(
          "Unable to replace system stores. Most likely due to invalid password or invalid store type");
    }
    return errors;
  }

  private boolean validKeystoreAlias(
      String alias, String keystorePassword, String keystoreData, String keystoreFileName)
      throws KeystoreEditorException {
    boolean valid = false;
    try (InputStream inputStream =
        new ByteArrayInputStream(Base64.getDecoder().decode(keystoreData))) {
      if (StringUtils.isBlank(alias)) {
        throw new IllegalArgumentException("Alias cannot be null.");
      }
      KeyStore ks = null;
      if (StringUtils.endsWithIgnoreCase(keystoreFileName, ".p12")
          || StringUtils.endsWithIgnoreCase(keystoreFileName, ".pfx")) {
        ks = KeyStore.getInstance("PKCS12");
      } else if (StringUtils.endsWithIgnoreCase(keystoreFileName, ".jks")) {
        ks = KeyStore.getInstance("jks");
      }

      if (ks != null) {
        ks.load(inputStream, keystorePassword.toCharArray());
        valid = ks.containsAlias(alias);
      }
    } catch (Exception e) {
      LOGGER.info("Unable read keystore data.", e);
      throw new KeystoreEditorException("Unable read keystore data.", e);
    }
    return valid;
  }

  private synchronized void addToStore(
      String alias,
      String keyPassword,
      String storePassword,
      String data,
      String type,
      String fileName,
      String path,
      String storepass,
      KeyStore store)
      throws KeystoreEditorException {
    OutputStream fos = null;
    try (InputStream inputStream = new ByteArrayInputStream(Base64.getDecoder().decode(data))) {
      if (StringUtils.isBlank(alias)) {
        throw new IllegalArgumentException("Alias cannot be null.");
      }
      Path storeFile = Paths.get(path);
      // check the two most common key/cert stores first (pkcs12 and jks)
      if (PKCS12_TYPE.equals(type) || StringUtils.endsWithIgnoreCase(fileName, ".p12")) {
        // priv key + cert chain
        KeyStore pkcs12Store = KeyStore.getInstance("PKCS12");
        pkcs12Store.load(inputStream, storePassword.toCharArray());
        Certificate[] chain = pkcs12Store.getCertificateChain(alias);
        Key key = pkcs12Store.getKey(alias, keyPassword.toCharArray());
        if (key != null) {
          store.setKeyEntry(alias, key, keyPassword.toCharArray(), chain);
          fos = Files.newOutputStream(storeFile);
          store.store(fos, storepass.toCharArray());
        }
      } else if (JKS_TYPE.equals(type) || StringUtils.endsWithIgnoreCase(fileName, ".jks")) {
        // java keystore file
        KeyStore jks = KeyStore.getInstance("jks");
        jks.load(inputStream, storePassword.toCharArray());
        Enumeration<String> aliases = jks.aliases();

        // we are going to store all entries from the jks regardless of the passed in alias
        while (aliases.hasMoreElements()) {
          String jksAlias = aliases.nextElement();

          if (jks.isKeyEntry(jksAlias)) {
            Key key = jks.getKey(jksAlias, keyPassword.toCharArray());
            Certificate[] certificateChain = jks.getCertificateChain(jksAlias);
            store.setKeyEntry(jksAlias, key, keyPassword.toCharArray(), certificateChain);
          } else {
            Certificate certificate = jks.getCertificate(jksAlias);
            store.setCertificateEntry(jksAlias, certificate);
          }
        }

        fos = Files.newOutputStream(storeFile);
        store.store(fos, storepass.toCharArray());
        // need to parse der separately from pem, der has the same mime type but is binary hence
        // checking both
      } else if (DER_TYPE.equals(type) && StringUtils.endsWithIgnoreCase(fileName, ".der")) {
        ASN1InputStream asn1InputStream = new ASN1InputStream(inputStream);
        ASN1Primitive asn1Primitive = asn1InputStream.readObject();
        X509CertificateHolder x509CertificateHolder =
            new X509CertificateHolder(asn1Primitive.getEncoded());
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "BC");
        Certificate certificate =
            certificateFactory.generateCertificate(
                new ByteArrayInputStream(x509CertificateHolder.getEncoded()));
        X500Name x500name =
            new JcaX509CertificateHolder((X509Certificate) certificate).getSubject();
        RDN cn = x500name.getRDNs(BCStyle.CN)[0];
        String cnStr = IETFUtils.valueToString(cn.getFirst().getValue());
        if (!store.isCertificateEntry(cnStr) && !store.isKeyEntry(cnStr)) {
          store.setCertificateEntry(cnStr, certificate);
        }
        store.setCertificateEntry(alias, certificate);
        fos = Files.newOutputStream(storeFile);
        store.store(fos, storepass.toCharArray());
        // if it isn't one of the stores we support, it might be a key or cert by itself
      } else if (isPemParsable(type, fileName)) {
        // This is the catch all case for PEM, P7B, etc. with common file extensions if the mime
        // type isn't read correctly in the browser
        Reader reader =
            new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        PEMParser pemParser = new PEMParser(reader);
        Object object;
        boolean setEntry = false;
        while ((object = pemParser.readObject()) != null) {
          if (object instanceof PEMEncryptedKeyPair || object instanceof PEMKeyPair) {
            PEMKeyPair pemKeyPair;
            if (object instanceof PEMEncryptedKeyPair) {
              PEMEncryptedKeyPair pemEncryptedKeyPairKeyPair = (PEMEncryptedKeyPair) object;
              JcePEMDecryptorProviderBuilder jcePEMDecryptorProviderBuilder =
                  new JcePEMDecryptorProviderBuilder();
              pemKeyPair =
                  pemEncryptedKeyPairKeyPair.decryptKeyPair(
                      jcePEMDecryptorProviderBuilder.build(keyPassword.toCharArray()));
            } else {
              pemKeyPair = (PEMKeyPair) object;
            }

            KeyPair keyPair = new JcaPEMKeyConverter().setProvider("BC").getKeyPair(pemKeyPair);
            PrivateKey privateKey = keyPair.getPrivate();
            Certificate[] chain = store.getCertificateChain(alias);
            if (chain == null) {
              chain = buildCertChain(alias, store);
            }
            store.setKeyEntry(alias, privateKey, keyPassword.toCharArray(), chain);
            setEntry = true;
          } else if (object instanceof X509CertificateHolder) {
            X509CertificateHolder x509CertificateHolder = (X509CertificateHolder) object;
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "BC");
            Certificate certificate =
                certificateFactory.generateCertificate(
                    new ByteArrayInputStream(x509CertificateHolder.getEncoded()));
            X500Name x500name =
                new JcaX509CertificateHolder((X509Certificate) certificate).getSubject();
            RDN cn = x500name.getRDNs(BCStyle.CN)[0];
            String cnStr = IETFUtils.valueToString(cn.getFirst().getValue());
            if (!store.isCertificateEntry(cnStr) && !store.isKeyEntry(cnStr)) {
              store.setCertificateEntry(cnStr, certificate);
            }
            store.setCertificateEntry(alias, certificate);
            setEntry = true;
          } else if (object instanceof ContentInfo) {
            ContentInfo contentInfo = (ContentInfo) object;
            if (contentInfo.getContentType().equals(CMSObjectIdentifiers.envelopedData)) {
              CMSEnvelopedData cmsEnvelopedData = new CMSEnvelopedData(contentInfo);
              OriginatorInfo originatorInfo =
                  cmsEnvelopedData.getOriginatorInfo().toASN1Structure();
              ASN1Set certificates = originatorInfo.getCertificates();
              setEntry = importASN1CertificatesToStore(store, setEntry, certificates);
            } else if (contentInfo.getContentType().equals(CMSObjectIdentifiers.signedData)) {
              SignedData signedData = SignedData.getInstance(contentInfo.getContent());
              ASN1Set certificates = signedData.getCertificates();
              setEntry = importASN1CertificatesToStore(store, setEntry, certificates);
            }
          } else if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
            PKCS8EncryptedPrivateKeyInfo pkcs8EncryptedPrivateKeyInfo =
                (PKCS8EncryptedPrivateKeyInfo) object;
            Certificate[] chain = store.getCertificateChain(alias);
            if (chain == null) {
              chain = buildCertChain(alias, store);
            }
            try {
              store.setKeyEntry(alias, pkcs8EncryptedPrivateKeyInfo.getEncoded(), chain);
              setEntry = true;
            } catch (KeyStoreException keyEx) {
              try {
                PKCS8Key pkcs8Key =
                    new PKCS8Key(
                        pkcs8EncryptedPrivateKeyInfo.getEncoded(), keyPassword.toCharArray());
                store.setKeyEntry(
                    alias, pkcs8Key.getPrivateKey(), keyPassword.toCharArray(), chain);
                setEntry = true;
              } catch (GeneralSecurityException e) {
                LOGGER.info(
                    "Unable to add PKCS8 key to keystore with secondary method. Throwing original exception.",
                    e);
                throw keyEx;
              }
            }
          }
        }
        if (setEntry) {
          fos = Files.newOutputStream(storeFile);
          store.store(fos, storepass.toCharArray());
        }
      }
    } catch (Exception e) {
      LOGGER.info("Unable to add entry {} to store", alias, e);
      throw new KeystoreEditorException("Unable to add entry " + alias + " to store", e);
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException ignore) {
        }
      }
    }
    init();
  }

  private boolean isPemParsable(String type, String fileName) {
    // check mime types
    if (PKCS7_TYPE.equals(type) || CERT_TYPE.equals(type) || PEM_TYPE.equals(type)) {
      return true;
    }
    // check file extensions
    return (StringUtils.endsWithIgnoreCase(fileName, ".crt")
        || StringUtils.endsWithIgnoreCase(fileName, ".key")
        || StringUtils.endsWithIgnoreCase(fileName, ".pem")
        || StringUtils.endsWithIgnoreCase(fileName, ".p7b"));
  }

  private boolean importASN1CertificatesToStore(
      KeyStore store, boolean setEntry, ASN1Set certificates) throws KeystoreEditorException {
    Enumeration certificateEnumeration = certificates.getObjects();
    try {
      while (certificateEnumeration.hasMoreElements()) {
        ASN1Primitive asn1Primitive =
            ((ASN1Encodable) certificateEnumeration.nextElement()).toASN1Primitive();
        org.bouncycastle.asn1.x509.Certificate instance =
            org.bouncycastle.asn1.x509.Certificate.getInstance(asn1Primitive);
        CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509", "BC");
        Certificate certificate =
            certificateFactory.generateCertificate(new ByteArrayInputStream(instance.getEncoded()));
        X500Name x500name =
            new JcaX509CertificateHolder((X509Certificate) certificate).getSubject();
        RDN cn = x500name.getRDNs(BCStyle.CN)[0];
        store.setCertificateEntry(IETFUtils.valueToString(cn.getFirst().getValue()), certificate);
        setEntry = true;
      }
    } catch (CertificateException | NoSuchProviderException | KeyStoreException | IOException e) {
      throw new KeystoreEditorException("Unable to import ASN1 certificates to store", e);
    }
    return setEntry;
  }

  private Certificate[] buildCertChain(String alias, KeyStore store)
      throws KeystoreEditorException {
    List<Certificate> certificates = buildCertChainList(alias, store);
    Certificate[] certificateArr = new Certificate[certificates.size()];
    for (int i = 0; i < certificates.size(); i++) {
      certificateArr[i] = certificates.get(i);
    }
    return certificateArr;
  }

  private List<Certificate> buildCertChainList(String alias, KeyStore store)
      throws KeystoreEditorException {
    try {
      Certificate certificate = store.getCertificate(alias);
      if (certificate != null) {
        X500Name x500nameSubject =
            new JcaX509CertificateHolder((X509Certificate) certificate).getSubject();
        RDN subjectCn = x500nameSubject.getRDNs(BCStyle.CN)[0];
        X500Name x500nameIssuer =
            new JcaX509CertificateHolder((X509Certificate) certificate).getIssuer();
        RDN issuerCn = x500nameIssuer.getRDNs(BCStyle.CN)[0];
        String issuer = IETFUtils.valueToString(issuerCn.getFirst().getValue());
        String subject = IETFUtils.valueToString(subjectCn.getFirst().getValue());
        if (StringUtils.isBlank(issuer) || issuer.equals(subject)) {
          List<Certificate> certificates = new ArrayList<>();
          certificates.add(certificate);
          return certificates;
        } else {
          List<Certificate> certificates = buildCertChainList(issuer, store);
          certificates.add(certificate);
          return certificates;
        }
      } else {
        return new ArrayList<>();
      }
    } catch (CertificateEncodingException | KeyStoreException e) {
      throw new KeystoreEditorException("Unable to build cert chain list.", e);
    }
  }

  @Override
  public void deletePrivateKey(String alias) {
    securityLogger.audit("Removing {} from System keystore.", alias);
    Path keyStoreFile = Paths.get(SecurityConstants.getKeystorePath());
    if (!keyStoreFile.isAbsolute()) {
      Path ddfHomePath = Paths.get(System.getProperty("ddf.home"));
      keyStoreFile = Paths.get(ddfHomePath.toString(), keyStoreFile.toString());
    }
    String keyStorePassword = SecurityConstants.getKeystorePassword();
    deleteFromStore(alias, keyStoreFile.toString(), keyStorePassword, keyStore);
  }

  @Override
  public void deleteTrustedCertificate(String alias) {
    securityLogger.audit("Removing {} from System truststore.", alias);
    Path trustStoreFile = Paths.get(SecurityConstants.getTruststorePath());
    if (!trustStoreFile.isAbsolute()) {
      Path ddfHomePath = Paths.get(System.getProperty("ddf.home"));
      trustStoreFile = Paths.get(ddfHomePath.toString(), trustStoreFile.toString());
    }
    String trustStorePassword = SecurityConstants.getTruststorePassword();
    deleteFromStore(alias, trustStoreFile.toString(), trustStorePassword, trustStore);
  }

  private synchronized void deleteFromStore(
      String alias, String path, String pass, KeyStore store) {
    if (alias == null) {
      throw new IllegalArgumentException("Alias cannot be null.");
    }
    File storeFile = new File(path);
    try (FileOutputStream fos = new FileOutputStream(storeFile)) {
      store.deleteEntry(alias);
      store.store(fos, pass.toCharArray());
    } catch (KeyStoreException | IOException | CertificateException | NoSuchAlgorithmException e) {
      LOGGER.info("Unable to remove entry {} from store", alias, e);
    }
  }

  private void deleteAllSystemStoreEntries() throws KeyStoreException {
    Enumeration<String> aliases = keyStore.aliases();
    while (aliases.hasMoreElements()) {
      deletePrivateKey(aliases.nextElement());
    }
    aliases = trustStore.aliases();
    while (aliases.hasMoreElements()) {
      deleteTrustedCertificate(aliases.nextElement());
    }
  }

  private static class NonVerifyingSslSocketFactory extends SSLConnectionSocketFactory {

    NonVerifyingSslSocketFactory(SSLContext sslContext)
        throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException,
            UnrecoverableKeyException {
      super(sslContext, new NoopHostnameVerifier());
    }
  }

  private static class NonVerifyingTrustManager implements X509TrustManager {
    public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {}

    public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException {}

    public X509Certificate[] getAcceptedIssuers() {
      return null;
    }
  }

  public static class KeystoreEditorException extends Exception {
    public KeystoreEditorException() {}

    public KeystoreEditorException(String message) {
      super(message);
    }

    public KeystoreEditorException(String message, Throwable cause) {
      super(message, cause);
    }

    public KeystoreEditorException(Throwable cause) {
      super(cause);
    }

    public KeystoreEditorException(
        String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
      super(message, cause, enableSuppression, writableStackTrace);
    }
  }
}
