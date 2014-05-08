/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.security.realm.sts;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.configuration.security.FiltersType;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.endpoint.EndpointException;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.cxf.ws.security.trust.STSUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAuthenticationInfo;
import org.apache.shiro.authc.credential.CredentialsMatcher;
import org.apache.shiro.realm.AuthenticatingRealm;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.util.Base64;
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import ddf.security.common.audit.SecurityLogger;
import ddf.security.common.callback.CommonCallbackHandler;
import ddf.security.common.util.CommonSSLFactory;
import ddf.security.common.util.PropertiesLoader;
import ddf.security.encryption.EncryptionService;
import ddf.security.sts.client.configuration.STSClientConfiguration;

/**
 * The STS Realm is the main piece of the security framework responsible for exchanging a binary
 * security token for a SAML assertion.
 */
public class StsRealm extends AuthenticatingRealm implements ConfigurationWatcher {
    private static final XLogger LOGGER = new XLogger(LoggerFactory.getLogger(StsRealm.class));

    private static final String NAME = StsRealm.class.getSimpleName();

    private static final String HTTPS = "https";

    private static final String ADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing";

    // AES is the best encryption but isn't always supported, 3DES is widely
    // supported and is very difficult to crack
    private static final String[] SSL_ALLOWED_ALGORITHMS = {".*_EXPORT_.*", ".*_WITH_AES_.*",
        ".*_WITH_3DES_.*"};

    private static final String[] SSL_DISALLOWED_ALGORITHMS = {".*_WITH_NULL_.*", ".*_DH_anon_.*"};

    private STSClient stsClient;

    private Bus bus;

    private String trustStorePath;

    private String trustStorePassword;

    private String keyStorePath;

    private String keyStorePassword;

    private boolean settingsConfigured;

    private EncryptionService encryptionService;

    private STSClientConfiguration stsClientConfig;

    public StsRealm() {
        this.bus = getBus();
        setCredentialsMatcher(new STSCredentialsMatcher());
    }

    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public void setStsClientConfig(STSClientConfiguration stsClientConfig) {
        this.stsClientConfig = stsClientConfig;
    }

    /**
     * Watch for changes in System Settings and STS Client Settings from the configuration page in
     * the DDF console.
     */
    @Override
    public void configurationUpdateCallback(Map<String, String> properties) {
        settingsConfigured = false;

        if (properties != null && !properties.isEmpty()) {

            if (LOGGER.isDebugEnabled()) {
                logIncomingProperties(properties);
            }

            if (isDdfConfigurationUpdate(properties)) {
                LOGGER.debug("Got system configuration update.");
                setDdfPropertiesFromConfigAdmin(properties);
                try {
                    configureStsClient();
                    settingsConfigured = true;
                } catch (Exception e) {
                    LOGGER.debug("STS was not available during configuration update, will try again when realm is called. Full stack trace is available at the TRACE level.");
                    LOGGER.trace("Could not create STS client", e);
                }
            }
        } else {
            LOGGER.debug("properties are NULL or empty");
        }
    }

    /**
     * Sets the configuration properties with an incoming property map. Users of this method should use {@link #configurationUpdateCallback(Map)} instead.
     * @param properties
     * 
     * @deprecated Since version 2.3.0.
     */
    @Deprecated
    public void setDefaultConfiguration(@SuppressWarnings("rawtypes")
    Map properties) {
        String value;
        value = (String) properties.get(ConfigurationManager.TRUST_STORE);
        if (value != null) {
            trustStorePath = value;
        }

        value = (String) properties.get(ConfigurationManager.TRUST_STORE_PASSWORD);
        if (value != null) {
            trustStorePassword = value;
        }

        value = (String) properties.get(ConfigurationManager.KEY_STORE);
        if (value != null) {
            keyStorePath = value;
        }

        value = (String) properties.get(ConfigurationManager.KEY_STORE_PASSWORD);
        if (value != null) {
            keyStorePassword = value;
        }

    }

    /**
     * Determine if the supplied token is supported by this realm.
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        boolean supported = token != null
                && AuthenticationToken.class.isAssignableFrom(token.getClass()) ? true : false;

        if (supported) {
            LOGGER.debug("Token {} is supported by {}.", token.getClass(), StsRealm.class.getName());
        } else if (token != null) {
            LOGGER.debug("Token {} is not supported by {}.", token.getClass(),
                    StsRealm.class.getName());
        } else {
            LOGGER.debug("The supplied authentication token is null. Sending back not supported.");
        }

        return supported;
    }

    /**
     * Perform authentication based on the supplied token.
     */
    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
        String method = "doGetAuthenticationInfo( AuthenticationToken token )";
        LOGGER.entry(method);

        String credential;

        if (token.getCredentials() != null) {
            credential = token.getCredentials().toString();
            //removed the credentials from the log message for now, I don't think we should be dumping user/pass into log
            LOGGER.debug("Received credentials.");
        } else {
            String msg = "Unable to authenticate credential.  A NULL credential was provided in the supplied authentication token. This may be due to an error with the SSO server that created the token.";
            LOGGER.error(msg);
            throw new AuthenticationException(msg);
        }

        if (!settingsConfigured) {
            configureStsClient();
            settingsConfigured = true;
        }

        SecurityToken securityToken = requestSecurityToken(credential);

        LOGGER.debug("Creating token authentication information with SAML.");
        SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo();
        SimplePrincipalCollection principals = new SimplePrincipalCollection();
        principals.add(token.getPrincipal(), NAME);
        principals.add(securityToken, NAME);
        simpleAuthenticationInfo.setPrincipals(principals);
        simpleAuthenticationInfo.setCredentials(credential);

        LOGGER.exit(method);
        return simpleAuthenticationInfo;
    }

    /**
     * Request a security token (SAML assertion) from the STS.
     * 
     * @param binarySecurityToken
     *            The subject the security token is being request for.
     * @return security token (SAML assertion)
     */
    private SecurityToken requestSecurityToken(String binarySecurityToken) {
        SecurityToken token = null;
        String stsAddress = stsClientConfig.getAddress();

        try {
            LOGGER.debug("Requesting security token from STS at: " + stsAddress + ".");

            if (binarySecurityToken != null) {
                LOGGER.debug("Telling the STS to request a security token on behalf of the binary security token:\n"
                        + binarySecurityToken);
                SecurityLogger
                        .logInfo("Telling the STS to request a security token on behalf of the binary security token:\n"
                                + binarySecurityToken);
                stsClient.setWsdlLocation(stsAddress);
                stsClient.setOnBehalfOf(binarySecurityToken);
                stsClient
                        .setTokenType("http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0");
                stsClient.setKeyType("http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey");
                token = stsClient.requestSecurityToken(stsAddress);
                LOGGER.debug("Finished requesting security token.");
                SecurityLogger.logInfo("Finished requesting security token.");

                SecurityLogger.logSecurityAssertionInfo(token);
            }
        } catch (Exception e) {
            String msg = "Error requesting the security token from STS at: " + stsAddress + ".";
            LOGGER.error(msg, e);
            SecurityLogger.logError(msg);
            throw new AuthenticationException(msg, e);
        }

        return token;
    }

    /**
     * Log properties from DDF configuration updates.
     */
    private void logIncomingProperties(@SuppressWarnings("rawtypes")
    Map properties) {
        @SuppressWarnings("unchecked")
        Set<String> keys = properties.keySet();
        StringBuilder builder = new StringBuilder();
        builder.append("\nIncoming properties:\n");
        for (String key : keys) {
            builder.append("key: " + key + "; value: " + properties.get(key) + "\n");
        }

        LOGGER.debug(builder.toString());
    }

    /**
     * Logs the current STS client configuration.
     */
    private void logStsClientConfiguration() {
        StringBuilder builder = new StringBuilder();

        builder.append("\nSTS Client configuration:\n");
        builder.append("STS WSDL location: " + stsClient.getWsdlLocation() + "\n");
        builder.append("STS service name: " + stsClient.getServiceQName() + "\n");
        builder.append("STS endpoint name: " + stsClient.getEndpointQName() + "\n");
        builder.append("STS claims: " + getFormattedXml(stsClient.getClaims()) + "\n");

        Map<String, Object> map = stsClient.getProperties();
        Set<String> keys = map.keySet();
        builder.append("\nSTS Client properties:\n");
        for (String key : keys) {
            builder.append("key: " + key + "; value: " + map.get(key) + "\n");
        }

        LOGGER.debug(builder.toString());
    }

    /**
     * Determines if the received update is a DDF System Settings update.
     */
    private boolean isDdfConfigurationUpdate(@SuppressWarnings("rawtypes")
    Map properties) {
        boolean updated = false;

        if (properties.containsKey(ConfigurationManager.TRUST_STORE)
                && properties.containsKey(ConfigurationManager.TRUST_STORE_PASSWORD)
                && properties.containsKey(ConfigurationManager.KEY_STORE)
                && properties.containsKey(ConfigurationManager.KEY_STORE_PASSWORD)) {
            updated = true;
        } else {
            updated = false;
        }

        return updated;
    }

    /**
     * Setup trust store for SSL client.
     */
    private void setupTrustStore(TLSClientParameters tlsParams, String trustStorePath,
            String trustStorePassword) {
        File trustStoreFile = new File(trustStorePath);
        if (trustStoreFile.exists() && trustStorePassword != null) {
            KeyStore trustStore = null;
            FileInputStream fis = null;

            try {
                trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                fis = new FileInputStream(trustStoreFile);
                LOGGER.debug("Loading trustStore");
                trustStore.load(fis, trustStorePassword.toCharArray());
                TrustManagerFactory trustFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustFactory.init(trustStore);
                LOGGER.debug("trust manager factory initialized");
                TrustManager[] tm = trustFactory.getTrustManagers();
                tlsParams.setTrustManagers(tm);

            } catch (FileNotFoundException e) {
                LOGGER.error("Unable to find SSL store: " + trustStorePath, e);
            } catch (IOException e) {
                LOGGER.error("Unable to load trust store. " + trustStore, e);
            } catch (CertificateException e) {
                LOGGER.error("Unable to load certificates from trust store. " + trustStore, e);
            } catch (KeyStoreException e) {
                LOGGER.error("Unable to read trust store: ", e);
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error(
                        "Problems creating SSL socket. Usually this is "
                                + "referring to the certificate sent by the server not being trusted by the client.",
                        e);
            } finally {
                IOUtils.closeQuietly(fis);
            }
        }
    }

    /**
     * Setup key store for SSL client.
     */
    private void setupKeyStore(TLSClientParameters tlsParams, String keyStorePath,
            String keyStorePassword) {
        File keyStoreFile = new File(keyStorePath);

        if (keyStoreFile.exists() && keyStorePassword != null) {
            FileInputStream fis = null;
            KeyStore keyStore = null;

            try {
                keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
                fis = new FileInputStream(keyStoreFile);

                LOGGER.debug("Loading keyStore");
                keyStore.load(fis, keyStorePassword.toCharArray());

                KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory
                        .getDefaultAlgorithm());
                keyFactory.init(keyStore, keyStorePassword.toCharArray());
                LOGGER.debug("key manager factory initialized");
                KeyManager[] km = keyFactory.getKeyManagers();
                tlsParams.setKeyManagers(km);
            } catch (FileNotFoundException e) {
                LOGGER.error("Unable to find SSL store: " + keyStorePath, e);
            } catch (IOException e) {
                LOGGER.error("Unable to load key store. " + keyStoreFile, e);
            } catch (CertificateException e) {
                LOGGER.error("Unable to load certificates from key store. " + keyStoreFile, e);
            } catch (KeyStoreException e) {
                LOGGER.error("Unable to read key store: ", e);
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error(
                        "Problems creating SSL socket. Usually this is "
                                + "referring to the certificate sent by the server not being trusted by the client.",
                        e);
            } catch (UnrecoverableKeyException e) {
                LOGGER.error("Unable to read key store: ", e);
            } finally {
                IOUtils.closeQuietly(fis);
            }

        }
    }

    /**
     * Setup cipher suites filter for SSL client.
     */
    private void setupCipherSuiteFilters(TLSClientParameters tlsParams) {
        // this sets the algorithms that we accept for SSL
        FiltersType filter = new FiltersType();
        filter.getInclude().addAll(Arrays.asList(SSL_ALLOWED_ALGORITHMS));
        filter.getExclude().addAll(Arrays.asList(SSL_DISALLOWED_ALGORITHMS));
        tlsParams.setCipherSuitesFilter(filter);
    }

    /**
     * Configure SSL on the client.
     */
    private void configureSslOnClient(Client client) {
        HTTPConduit httpConduit = (HTTPConduit) client.getConduit();

        TLSClientParameters tlsParams = new TLSClientParameters();
        tlsParams.setDisableCNCheck(true);

        setupTrustStore(tlsParams, trustStorePath, trustStorePassword);

        setupKeyStore(tlsParams, keyStorePath, keyStorePassword);

        setupCipherSuiteFilters(tlsParams);

        httpConduit.setTlsClientParameters(tlsParams);
    }

    /**
     * Set properties based on DDF System Setting updates.
     */
    private void setDdfPropertiesFromConfigAdmin(Map<String, String> properties) {
        String setTrustStorePath = properties.get(ConfigurationManager.TRUST_STORE);
        if (StringUtils.isNotBlank(setTrustStorePath)) {
            LOGGER.debug("Setting trust store path: " + setTrustStorePath);
            this.trustStorePath = setTrustStorePath;
        }

        String setTrustStorePassword = properties
                .get(ConfigurationManager.TRUST_STORE_PASSWORD);
        if (StringUtils.isNotBlank(setTrustStorePassword)) {
            if (encryptionService == null) {
                LOGGER.error("The StsRealm has a null Encryption Service. Unable to decrypt the encrypted "
                        + "trustStore password. Setting decrypted password to null.");
                this.trustStorePassword = null;
            } else {
                setTrustStorePassword = encryptionService.decryptValue(setTrustStorePassword);
                LOGGER.debug("Setting trust store password.");
                this.trustStorePassword = setTrustStorePassword;
            }
        }

        String setKeyStorePath = properties.get(ConfigurationManager.KEY_STORE);
        if (StringUtils.isNotBlank(setKeyStorePath)) {
            LOGGER.debug("Setting key store path: " + setKeyStorePath);
            this.keyStorePath = setKeyStorePath;
        }

        String setKeyStorePassword = properties
                .get(ConfigurationManager.KEY_STORE_PASSWORD);
        if (StringUtils.isNotBlank(setKeyStorePassword)) {
            if (encryptionService == null) {
                LOGGER.error("The StsRealm has a null Encryption Service. Unable to decrypt the encrypted "
                        + "keyStore password. Setting decrypted password to null.");
                this.keyStorePassword = null;
            } else {
                setKeyStorePassword = encryptionService.decryptValue(setKeyStorePassword);
                LOGGER.debug("Setting key store password.");
                this.keyStorePassword = setKeyStorePassword;
            }
        }
    }

    /**
     * Helper method to setup STS Client.
     */
    private Bus getBus() {
        BusFactory bf = new CXFBusFactory();
        Bus setBus = bf.createBus();
        SpringBusFactory.setDefaultBus(setBus);
        SpringBusFactory.setThreadDefaultBus(setBus);

        return setBus;
    }

    /**
     * Helper method to setup STS Client.
     */
    private void addSignatureProperties(Map<String, Object> map) {
        String signaturePropertiesPath = stsClientConfig.getSignatureProperties();
        if (signaturePropertiesPath != null && !signaturePropertiesPath.isEmpty()) {
            LOGGER.debug("Setting signature properties on STSClient: " + signaturePropertiesPath);
            Properties signatureProperties = PropertiesLoader
                    .loadProperties(signaturePropertiesPath);
            map.put(SecurityConstants.SIGNATURE_PROPERTIES, signatureProperties);
        }
    }

    /**
     * Helper method to setup STS Client.
     */
    private void addEncryptionProperties(Map<String, Object> map) {
        String encryptionPropertiesPath = stsClientConfig.getEncryptionProperties();
        if (encryptionPropertiesPath != null && !encryptionPropertiesPath.isEmpty()) {
            LOGGER.debug("Setting encryption properties on STSClient: " + encryptionPropertiesPath);
            Properties encryptionProperties = PropertiesLoader
                    .loadProperties(encryptionPropertiesPath);
            map.put(SecurityConstants.ENCRYPT_PROPERTIES, encryptionProperties);
        }
    }

    /**
     * Helper method to setup STS Client.
     */
    private void addStsCryptoProperties(Map<String, Object> map) {
        String stsPropertiesPath = stsClientConfig.getTokenProperties();
        if (stsPropertiesPath != null && !stsPropertiesPath.isEmpty()) {
            LOGGER.debug("Setting sts properties on STSClient: " + stsPropertiesPath);
            Properties stsProperties = PropertiesLoader.loadProperties(stsPropertiesPath);
            map.put(SecurityConstants.STS_TOKEN_PROPERTIES, stsProperties);
        }
    }

    /**
     * Helper method to setup STS Client.
     */
    private void addCallbackHandler(Map<String, Object> map) {
        LOGGER.debug("Setting callback handler on STSClient");
        map.put(SecurityConstants.CALLBACK_HANDLER, new CommonCallbackHandler());
    }

    /**
     * Helper method to setup STS Client.
     */
    private void addUseCertForKeyInfo(Map<String, Object> map) {
        LOGGER.debug("Setting STS TOKEN USE CERT FOR KEY INFO to \"true\"");
        map.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, Boolean.TRUE.toString());
    }

    /**
     * Helper method to setup STS Client.
     */
    private void addStsProperties() {
        Map<String, Object> map = new HashMap<String, Object>();

        addSignatureProperties(map);

        addEncryptionProperties(map);

        addStsCryptoProperties(map);

        addCallbackHandler(map);

        addUseCertForKeyInfo(map);

        stsClient.setProperties(map);
    }

    /**
     * Helper method to setup STS Client.
     */
    private void configureBaseStsClient() {
        stsClient = new STSClient(bus);
        String stsAddress = stsClientConfig.getAddress();
        String stsServiceName = stsClientConfig.getServiceName();
        String stsEndpointName = stsClientConfig.getEndpointName();

        if (stsAddress != null) {
            LOGGER.debug("Setting WSDL location on STSClient: " + stsAddress);
            stsClient.setWsdlLocation(stsAddress);
        }

        if (stsServiceName != null) {
            LOGGER.debug("Setting service name on STSClient: " + stsServiceName);
            stsClient.setServiceName(stsServiceName);
        }

        if (stsEndpointName != null) {
            LOGGER.debug("Setting endpoint name on STSClient: " + stsEndpointName);
            stsClient.setEndpointName(stsEndpointName);
        }

        LOGGER.debug("Setting addressing namespace on STSClient: " + ADDRESSING_NAMESPACE);
        stsClient.setAddressingNamespace(ADDRESSING_NAMESPACE);
    }

    /**
     * Helper method to setup STS Client.
     */
    private void configureStsClient() {
        LOGGER.debug("Configuring the STS client.");

        try {
            HttpsURLConnection.setDefaultSSLSocketFactory(CommonSSLFactory.createSocket(
                    trustStorePath, trustStorePassword, keyStorePath, keyStorePassword));
        } catch (IOException ioe) {
            throw new RuntimeException(
                    "Could not create SSL connection with given trust/key stores.", ioe);
        }

        configureBaseStsClient();

        addStsProperties();

        setClaimsOnStsClient(createClaimsElement());

        if (stsClient.getWsdlLocation() != null && stsClient.getWsdlLocation().startsWith(HTTPS)) {
            setupSslOnStsClientHttpConduit();
        } else {
            LOGGER.debug("STS address is null, unable to create STS Client");
        }

        if (LOGGER.isDebugEnabled()) {
            logStsClientConfiguration();
        }
    }

    /**
     * Helper method to setup STS Client.
     */
    private void setupSslOnStsClientHttpConduit() {
        LOGGER.debug("Setting up SSL on the STSClient HTTP conduit");

        try {
            Client client = stsClient.getClient();

            if (client != null) {
                configureSslOnClient(client);
            } else {
                LOGGER.debug("CXF STS endpoint client is null.  Unable to setup SSL on the STSClient HTTP conduit.");
            }
        } catch (BusException e) {
            LOGGER.error("Unable to create STS client.", e);
        } catch (EndpointException e) {
            LOGGER.error("Unable to create STS client endpoint.", e);
        }
    }

    /**
     * Creates a binary security token based on the provided credential.
     */
    private String getBinarySecurityToken(String credential) {
        BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
        binarySecurityTokenType.setValueType("#CAS");
        binarySecurityTokenType.setEncodingType(WSConstants.SOAPMESSAGE_NS + "#Base64Binary");
        binarySecurityTokenType.setId("CAS");
        binarySecurityTokenType.setValue(Base64.encode(credential.getBytes()));
        JAXBElement<BinarySecurityTokenType> binarySecurityTokenElement = new JAXBElement<BinarySecurityTokenType>(
                new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "BinarySecurityToken"), BinarySecurityTokenType.class,
                binarySecurityTokenType);
        Writer writer = new StringWriter();
        JAXB.marshal(binarySecurityTokenElement, writer);

        String binarySecurityToken = writer.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Binary Security Token: " + binarySecurityToken);
        }

        return binarySecurityToken;
    }

    /**
     * Set the claims on the sts client.
     */
    private void setClaimsOnStsClient(Element claimsElement) {
        if (claimsElement != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(" Setting STS claims to:\n" + this.getFormattedXml(claimsElement));
            }

            stsClient.setClaims(claimsElement);
        }
    }

    /**
     * Create the claims element with the claims provided in the STS client configuration in the
     * admin console.
     */
    private Element createClaimsElement() {
        Element claimsElement = null;
        List<String> claims = stsClientConfig.getClaims();

        if (claims != null && claims.size() != 0) {
            W3CDOMStreamWriter writer = null;

            try {
                writer = new W3CDOMStreamWriter();

                writer.writeStartElement("wst", "Claims", STSUtils.WST_NS_05_12);
                writer.writeNamespace("wst", STSUtils.WST_NS_05_12);
                writer.writeNamespace("ic", "http://schemas.xmlsoap.org/ws/2005/05/identity");
                writer.writeAttribute("Dialect", "http://schemas.xmlsoap.org/ws/2005/05/identity");

                for (String claim : claims) {
                    LOGGER.trace("Claim: " + claim);
                    writer.writeStartElement("ic", "ClaimType",
                            "http://schemas.xmlsoap.org/ws/2005/05/identity");
                    writer.writeAttribute("Uri", claim);
                    writer.writeAttribute("Optional", "true");
                    writer.writeEndElement();
                }

                writer.writeEndElement();

                claimsElement = writer.getDocument().getDocumentElement();
            } catch (ParserConfigurationException e) {
                String msg = "Unable to create claims.";
                LOGGER.error(msg, e);
                claimsElement = null;
            } catch (XMLStreamException e) {
                String msg = "Unable to create claims.";
                LOGGER.error(msg, e);
                claimsElement = null;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("\nClaims:\n" + getFormattedXml(claimsElement));
            }
        } else {
            LOGGER.debug("There are no claims to process.");
            claimsElement = null;
        }

        return claimsElement;
    }

    /**
     * Transform into formatted XML.
     */
    private String getFormattedXml(Node node) {
        Document document = node.getOwnerDocument().getImplementation()
                .createDocument("", "fake", null);
        Element copy = (Element) document.importNode(node, true);
        document.importNode(node, false);
        document.removeChild(document.getDocumentElement());
        document.appendChild(copy);
        DOMImplementation domImpl = document.getImplementation();
        DOMImplementationLS domImplLs = (DOMImplementationLS) domImpl.getFeature("LS", "3.0");
        LSSerializer serializer = domImplLs.createLSSerializer();
        serializer.getDomConfig().setParameter("format-pretty-print", true);
        return serializer.writeToString(document);
    }

    /**
     * Credentials matcher class that ensures the AuthInfo received from the STS matches the
     * AuthToken
     */
    private static class STSCredentialsMatcher implements CredentialsMatcher {

        @Override
        public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
            if (token.getCredentials() != null && info.getCredentials() != null) {
                return token.getCredentials().equals(info.getCredentials());
            }
            return false;
        }
    }

}
