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

import ddf.security.PropertiesLoader;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.impl.SecurityAssertionImpl;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.common.util.CommonSSLFactory;
import ddf.security.encryption.EncryptionService;
import ddf.security.sts.client.configuration.STSClientConfiguration;
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
import org.codice.ddf.configuration.ConfigurationManager;
import org.codice.ddf.configuration.ConfigurationWatcher;
import org.codice.ddf.security.handler.api.BSTAuthenticationToken;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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

    private ContextPolicyManager contextPolicyManager;

    public StsRealm() {
        this.bus = getBus();
        setCredentialsMatcher(new STSCredentialsMatcher());
    }

    public void setEncryptionService(EncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    public void setStsClientConfig(STSClientConfiguration stsClientConfig) {
        this.stsClientConfig = stsClientConfig;
        configureStsClient();
    }

    public ContextPolicyManager getContextPolicyManager() {
        return contextPolicyManager;
    }

    public void setContextPolicyManager(ContextPolicyManager contextPolicyManager) {
        this.contextPolicyManager = contextPolicyManager;
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
                    LOGGER.debug(
                            "STS was not available during configuration update, will try again when realm is called. Full stack trace is available at the TRACE level.");
                    LOGGER.trace("Could not create STS client", e);
                }
            }
        } else {
            LOGGER.debug("properties are NULL or empty");
        }
    }

    /**
     * Determine if the supplied token is supported by this realm.
     */
    @Override
    public boolean supports(AuthenticationToken token) {
        boolean supported = token instanceof SAMLAuthenticationToken ||
                token instanceof BSTAuthenticationToken;

        if (supported) {
            LOGGER.debug("Token {} is supported by {}.", token.getClass(),
                    StsRealm.class.getName());
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

        Object credential;

        if (token instanceof SAMLAuthenticationToken) {
            credential = token.getCredentials();
        } else if (token instanceof BaseAuthenticationToken) {
            credential = ((BaseAuthenticationToken) token).getCredentialsAsXMLString();
        } else {
            credential = token.getCredentials().toString();
        }
        if (credential == null) {
            String msg = "Unable to authenticate credential.  A NULL credential was provided in the supplied authentication token. This may be due to an error with the SSO server that created the token.";
            LOGGER.error(msg);
            throw new AuthenticationException(msg);
        } else {
            //removed the credentials from the log message for now, I don't think we should be dumping user/pass into log
            LOGGER.debug("Received credentials.");
        }

        if (!settingsConfigured) {
            configureStsClient();
            settingsConfigured = true;
        } else {
            setClaimsOnStsClient(createClaimsElement());
        }

        SecurityToken securityToken;
        if (token instanceof SAMLAuthenticationToken && credential instanceof SecurityToken) {
            securityToken = renewSecurityToken((SecurityToken) credential);
        } else {
            securityToken = requestSecurityToken(credential);
        }

        LOGGER.debug("Creating token authentication information with SAML.");
        SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo();
        SimplePrincipalCollection principals = new SimplePrincipalCollection();
        SecurityAssertion assertion = new SecurityAssertionImpl(securityToken);
        principals.add(assertion.getPrincipal(), NAME);
        principals.add(assertion, NAME);
        simpleAuthenticationInfo.setPrincipals(principals);
        simpleAuthenticationInfo.setCredentials(credential);

        LOGGER.exit(method);
        return simpleAuthenticationInfo;
    }

    /**
     * Request a security token (SAML assertion) from the STS.
     *
     * @param authToken The subject the security token is being request for.
     * @return security token (SAML assertion)
     */
    private SecurityToken requestSecurityToken(Object authToken) {
        SecurityToken token = null;
        String stsAddress = stsClientConfig.getAddress();

        try {
            LOGGER.debug("Requesting security token from STS at: " + stsAddress + ".");

            if (authToken != null) {
                LOGGER.debug(
                        "Telling the STS to request a security token on behalf of the auth token"
                );
                SecurityLogger
                        .logInfo(
                                "Telling the STS to request a security token on behalf of the auth token"
                        );
                stsClient.setWsdlLocation(stsAddress);
                stsClient.setOnBehalfOf(authToken);
                stsClient.setTokenType(stsClientConfig.getAssertionType());
                stsClient.setKeyType(stsClientConfig.getKeyType());
                stsClient.setKeySize(Integer.valueOf(stsClientConfig.getKeySize()));
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
     * Renew a security token (SAML assertion) from the STS.
     *
     * @param securityToken The token being renewed.
     * @return security token (SAML assertion)
     */
    private SecurityToken renewSecurityToken(SecurityToken securityToken) {
        SecurityToken token = null;
        String stsAddress = stsClientConfig.getAddress();

        try {
            LOGGER.debug("Renewing security token from STS at: " + stsAddress + ".");

            if (securityToken != null) {
                LOGGER.debug(
                        "Telling the STS to renew a security token on behalf of the auth token"
                );
                SecurityLogger
                        .logInfo(
                                "Telling the STS to renew a security token on behalf of the auth token"
                        );
                stsClient.setWsdlLocation(stsAddress);
                stsClient.setTokenType(stsClientConfig.getAssertionType());
                stsClient.setKeyType(stsClientConfig.getKeyType());
                stsClient.setKeySize(Integer.valueOf(stsClientConfig.getKeySize()));
                stsClient.setAllowRenewing(true);
                token = stsClient.renewSecurityToken(securityToken);
                LOGGER.debug("Finished renewing security token.");
                SecurityLogger.logInfo("Finished renewing security token.");

                SecurityLogger.logSecurityAssertionInfo(token);
            }
        } catch (Exception e) {
            String msg = "Error renewing the security token from STS at: " + stsAddress + ".";
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
                        e
                );
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
                        e
                );
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
                LOGGER.error(
                        "The StsRealm has a null Encryption Service. Unable to decrypt the encrypted "
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
                LOGGER.error(
                        "The StsRealm has a null Encryption Service. Unable to decrypt the encrypted "
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
    private void addStsProperties() {
        Map<String, Object> map = new HashMap<String, Object>();

        String signaturePropertiesPath = stsClientConfig.getSignatureProperties();
        if (signaturePropertiesPath != null && !signaturePropertiesPath.isEmpty()) {
            LOGGER.debug("Setting signature properties on STSClient: " + signaturePropertiesPath);
            Properties signatureProperties = PropertiesLoader
                    .loadProperties(signaturePropertiesPath);
            map.put(SecurityConstants.SIGNATURE_PROPERTIES, signatureProperties);
        }

        String encryptionPropertiesPath = stsClientConfig.getEncryptionProperties();
        if (encryptionPropertiesPath != null && !encryptionPropertiesPath.isEmpty()) {
            LOGGER.debug("Setting encryption properties on STSClient: " + encryptionPropertiesPath);
            Properties encryptionProperties = PropertiesLoader
                    .loadProperties(encryptionPropertiesPath);
            map.put(SecurityConstants.ENCRYPT_PROPERTIES, encryptionProperties);
        }

        String stsPropertiesPath = stsClientConfig.getTokenProperties();
        if (stsPropertiesPath != null && !stsPropertiesPath.isEmpty()) {
            LOGGER.debug("Setting sts properties on STSClient: " + stsPropertiesPath);
            Properties stsProperties = PropertiesLoader.loadProperties(stsPropertiesPath);
            map.put(SecurityConstants.STS_TOKEN_PROPERTIES, stsProperties);
        }

        LOGGER.debug("Setting callback handler on STSClient");
        //DDF-733 map.put(SecurityConstants.CALLBACK_HANDLER, new CommonCallbackHandler());

        LOGGER.debug("Setting STS TOKEN USE CERT FOR KEY INFO to \"true\"");
        map.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, String.valueOf(stsClientConfig.getUseKey()));

        LOGGER.debug("Adding in realm information to the STSClient");
        map.put("CLIENT_REALM", "DDF");

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
            if(trustStorePath != null && trustStorePassword != null && keyStorePath != null && keyStorePassword != null) {
                HttpsURLConnection.setDefaultSSLSocketFactory(CommonSSLFactory.createSocket(
                        trustStorePath, trustStorePassword, keyStorePath, keyStorePassword));
            }
        } catch (IOException ioe) {
            throw new RuntimeException(
                    "Could not create SSL connection with given trust/key stores.", ioe);
        }

        configureBaseStsClient();

        addStsProperties();

        setClaimsOnStsClient(createClaimsElement());

        if (stsClient.getWsdlLocation() != null && stsClient.getWsdlLocation().startsWith(HTTPS)) {
            if(trustStorePath != null && trustStorePassword != null && keyStorePath != null && keyStorePassword != null) {
                setupSslOnStsClientHttpConduit();
            }
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
                LOGGER.debug(
                        "CXF STS endpoint client is null.  Unable to setup SSL on the STSClient HTTP conduit.");
            }
        } catch (BusException e) {
            LOGGER.error("Unable to create STS client.", e);
        } catch (EndpointException e) {
            LOGGER.error("Unable to create STS client endpoint.", e);
        }
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
        List<String> claims = new ArrayList<String>();
        claims.addAll(stsClientConfig.getClaims());

        if(contextPolicyManager != null) {
            Collection<ContextPolicy> contextPolicies = contextPolicyManager.getAllContextPolicies();
            Set<String> attributes = new HashSet<String>();
            if(contextPolicies != null && contextPolicies.size() > 0) {
                for(ContextPolicy contextPolicy : contextPolicies) {
                    attributes.addAll(contextPolicy.getAllowedAttributeNames());
                }
            }

            if(attributes.size() > 0) {
                claims.addAll(attributes);
            }
        }

        if (claims.size() != 0) {
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
            } catch (XMLStreamException e) {
                String msg = "Unable to create claims.";
                LOGGER.error(msg, e);
                claimsElement = null;
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (XMLStreamException ignore) {
                        //ignore
                    }
                }
            }

            if (LOGGER.isDebugEnabled()) {
                if (claimsElement != null) {
                    LOGGER.debug("\nClaims:\n" + getFormattedXml(claimsElement));
                }
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
            if (token instanceof SAMLAuthenticationToken) {
                SecurityToken oldToken = (SecurityToken) token.getCredentials();
                SecurityToken newToken = (SecurityToken) info.getCredentials();
                return oldToken.getId().equals(newToken.getId());
            } else if (token instanceof BaseAuthenticationToken) {
                String xmlCreds = ((BaseAuthenticationToken) token).getCredentialsAsXMLString();
                if (xmlCreds != null && info.getCredentials() != null) {
                    return xmlCreds.equals(info.getCredentials());
                }
            } else {
                if (token.getCredentials() != null && info.getCredentials() != null) {
                    return token.getCredentials().equals(info.getCredentials());
                }
            }
            return false;
        }
    }

}
