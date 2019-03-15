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
package ddf.security.realm.sts;

import com.google.common.base.Splitter;
import ddf.security.PropertiesLoader;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.saml.impl.SecurityAssertionSaml;
import ddf.security.sts.client.configuration.STSClientConfiguration;
import java.security.AccessController;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import javax.xml.stream.XMLStreamException;
import org.apache.cxf.Bus;
import org.apache.cxf.BusFactory;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.bus.spring.SpringBusFactory;
import org.apache.cxf.staxutils.W3CDOMStreamWriter;
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
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ddf.platform.filter.AuthenticationFailureException;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.codice.ddf.security.handler.api.STSAuthenticationToken;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.codice.ddf.security.policy.context.ContextPolicyManager;
import org.codice.ddf.security.saml.assertion.validator.SamlAssertionValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

public class StsRealm extends AuthenticatingRealm implements STSClientConfiguration {
  private static final Logger LOGGER = (LoggerFactory.getLogger(StsRealm.class));

  private static final String ADDRESSING_NAMESPACE = "http://www.w3.org/2005/08/addressing";

  private static final Splitter SPLITTER = Splitter.on(',').trimResults().omitEmptyStrings();

  protected Bus bus;

  private PropertyResolver address = null;

  private String endpointName = null;

  private String serviceName = null;

  private String username = null;

  private String password = null;

  private String signatureUsername = null;

  private String signatureProperties = null;

  private String encryptionUsername = null;

  private String encryptionProperties = null;

  private String tokenUsername = null;

  private String tokenProperties = null;

  private List<String> claims = new ArrayList<>();

  private ContextPolicyManager contextPolicyManager;

  private SamlAssertionValidator samlAssertionValidator;

  private String assertionType = null;

  private String keyType = null;

  private String keySize = null;

  private Boolean useKey = null;

  private List<String> usernameAttributeList;

  public StsRealm() {
    this.bus = getBus();
    setCredentialsMatcher(new STSCredentialsMatcher());
  }

  public void setContextPolicyManager(ContextPolicyManager contextPolicyManager) {
    this.contextPolicyManager = contextPolicyManager;
  }

  /** Determine if the supplied token is supported by this realm. */
  @Override
  public boolean supports(AuthenticationToken token) {
    boolean supported =
        token != null && token.getCredentials() != null && token instanceof STSAuthenticationToken;

    if (supported) {
      LOGGER.debug("Token {} is supported by {}.", token.getClass(), StsRealm.class.getName());
    } else if (token != null) {
      LOGGER.debug("Token {} is not supported by {}.", token.getClass(), StsRealm.class.getName());
    } else {
      LOGGER.debug("The supplied authentication token is null. Sending back not supported.");
    }

    return supported;
  }

  /** Perform authentication based on the supplied token. */
  @Override
  protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) {
    Object credential;

    // perform validation
    if (token instanceof SAMLAuthenticationToken) {
      try {
        samlAssertionValidator.validate((SAMLAuthenticationToken) token);
        credential = token.getCredentials();
      } catch (AuthenticationFailureException e) {
        String msg = "Unable to validate request's authentication.";
        LOGGER.info(msg);
        throw new AuthenticationException(msg, e);
      }
    } else if (token instanceof STSAuthenticationToken) {
      credential = ((STSAuthenticationToken) token).getCredentialsAsString();
    } else {
      credential = token.getCredentials().toString();
    }

    if (credential == null) {
      String msg =
          "Unable to authenticate credential.  A NULL credential was provided in the supplied authentication token. This may be due to an error with the SSO server that created the token.";
      LOGGER.info(msg);
      throw new AuthenticationException(msg);
    } else {
      // removed the credentials from the log message for now, I don't think we should be dumping
      // user/pass into log
      LOGGER.debug("Received credentials.");
    }

    SecurityToken securityToken;
    if (token instanceof SAMLAuthenticationToken) {

      securityToken =
          AccessController.doPrivileged(
              (PrivilegedAction<SecurityToken>) () -> checkRenewSecurityToken(credential));
    } else {
      securityToken =
          AccessController.doPrivileged(
              (PrivilegedAction<SecurityToken>) () -> requestSecurityToken(credential));
    }

    LOGGER.debug("Creating token authentication information with SAML.");
    SimpleAuthenticationInfo simpleAuthenticationInfo = new SimpleAuthenticationInfo();
    SimplePrincipalCollection principals = createPrincipalFromToken(securityToken);
    simpleAuthenticationInfo.setPrincipals(principals);
    simpleAuthenticationInfo.setCredentials(credential);

    return simpleAuthenticationInfo;
  }

  /**
   * Creates a new principal object from an incoming security token.
   *
   * @param token SecurityToken that contains the principals.
   * @return new SimplePrincipalCollection
   */
  private SimplePrincipalCollection createPrincipalFromToken(SecurityToken token) {
    SimplePrincipalCollection principals = new SimplePrincipalCollection();
    SecurityAssertion securityAssertion = null;
    try {
      securityAssertion = new SecurityAssertionSaml(token, usernameAttributeList);
      Principal principal = securityAssertion.getPrincipal();
      if (principal != null) {
        principals.add(principal.getName(), getName());
      }
    } catch (Exception e) {
      LOGGER.warn(
          "Encountered error while trying to get the Principal for the SecurityToken. Security functions may not work properly.",
          e);
    }
    if (securityAssertion != null) {
      principals.add(securityAssertion, getName());
    }
    return principals;
  }

  /**
   * Request a security token (SAML assertion) from the STS.
   *
   * @param authToken The subject the security token is being request for.
   * @return security token (SAML assertion)
   */
  protected SecurityToken requestSecurityToken(Object authToken) {
    SecurityToken token = null;
    String stsAddress = getAddress();

    try {
      LOGGER.debug("Requesting security token from STS at: {}.", stsAddress);

      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(StsRealm.class.getClassLoader());
      try {
        if (authToken != null) {
          LOGGER.debug("Telling the STS to request a security token on behalf of the auth token");
          STSClient stsClient = configureStsClient();

          stsClient.setWsdlLocation(stsAddress);
          stsClient.setOnBehalfOf(authToken);
          stsClient.setTokenType(getAssertionType());
          stsClient.setKeyType(getKeyType());
          stsClient.setKeySize(Integer.parseInt(getKeySize()));
          stsClient.setAllowRenewing(true);
          stsClient.setAllowRenewingAfterExpiry(true);
          token = stsClient.requestSecurityToken();
          LOGGER.debug("Finished requesting security token.");
        }
      } finally {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
    } catch (Exception e) {
      String msg = "Error requesting the security token from STS at: " + stsAddress + ".";
      LOGGER.debug(msg, e);
      throw new AuthenticationException(msg, e);
    }

    return token;
  }

  /**
   * Renew a security token (SAML assertion) from the STS.
   *
   * @param credential The token being renewed.
   * @return security token (SAML assertion)
   */
  protected SecurityToken checkRenewSecurityToken(final Object credential) {
    try {
      if (credential instanceof PrincipalCollection) {
        Optional<SecurityAssertionSaml> assertionSamlOptional =
            ((PrincipalCollection) credential)
                .byType(SecurityAssertionSaml.class)
                .stream()
                .filter(sa -> sa.getToken() instanceof SecurityToken)
                .findFirst();
        if (assertionSamlOptional.isPresent()) {
          SecurityAssertionSaml assertion = assertionSamlOptional.get();
          return (Instant.now().plusSeconds(60).isAfter(assertion.getNotOnOrAfter().toInstant()))
              ? renewSecurityToken((SecurityToken) assertion.getToken())
              : (SecurityToken) assertion.getToken();
        }
      }
    } catch (Exception e) {
      String msg = "Error renewing the security token from STS.";
      LOGGER.debug(msg, e);
      throw new AuthenticationException(msg, e);
    }

    return null;
  }

  private SecurityToken renewSecurityToken(final SecurityToken securityToken) throws Exception {
    LOGGER.debug("Telling the STS to renew a security token on behalf of the auth token");
    STSClient stsClient = configureStsClient();
    String stsAddress = getAddress();
    LOGGER.debug("Renewing security token from STS at: {}.", stsAddress);
    stsClient.setWsdlLocation(stsAddress);
    stsClient.setTokenType(getAssertionType());
    stsClient.setKeyType(getKeyType());
    stsClient.setKeySize(Integer.parseInt(getKeySize()));
    stsClient.setAllowRenewing(true);
    stsClient.setAllowRenewingAfterExpiry(true);
    SecurityToken token = stsClient.renewSecurityToken(securityToken);
    LOGGER.debug("Finished renewing security token.");
    return token;
  }

  /**
   * Logs the current STS client configuration.
   *
   * @param stsClient
   */
  private void logStsClientConfiguration(STSClient stsClient) {
    StringBuilder builder = new StringBuilder();

    builder.append("\nSTS Client configuration:\n");
    builder.append("STS WSDL location: " + stsClient.getWsdlLocation() + "\n");
    builder.append("STS service name: " + stsClient.getServiceQName() + "\n");
    builder.append("STS endpoint name: " + stsClient.getEndpointQName() + "\n");

    Map<String, Object> map = stsClient.getProperties();
    Set<Map.Entry<String, Object>> entries = map.entrySet();
    builder.append("\nSTS Client properties:\n");
    for (Map.Entry<String, Object> entry : entries) {
      builder.append("key: " + entry.getKey() + "; value: " + entry.getValue() + "\n");
    }

    LOGGER.debug("builder: {}", builder);
  }

  /** Helper method to setup STS Client. */
  protected Bus getBus() {
    BusFactory bf = new CXFBusFactory();
    Bus setBus = bf.createBus();
    SpringBusFactory.setDefaultBus(setBus);
    SpringBusFactory.setThreadDefaultBus(setBus);

    return setBus;
  }

  /**
   * Helper method to setup STS Client.
   *
   * @param stsClient
   */
  private void addStsProperties(STSClient stsClient) {
    Map<String, Object> map = new HashMap<>();

    String signaturePropertiesPath = getSignatureProperties();
    if (signaturePropertiesPath != null && !signaturePropertiesPath.isEmpty()) {
      LOGGER.debug("Setting signature properties on STSClient: {}", signaturePropertiesPath);
      Properties signatureProperties = PropertiesLoader.loadProperties(signaturePropertiesPath);
      map.put(SecurityConstants.SIGNATURE_PROPERTIES, signatureProperties);
    }

    String encryptionPropertiesPath = getEncryptionProperties();
    if (encryptionPropertiesPath != null && !encryptionPropertiesPath.isEmpty()) {
      LOGGER.debug("Setting encryption properties on STSClient: {}", encryptionPropertiesPath);
      Properties encryptionProperties = PropertiesLoader.loadProperties(encryptionPropertiesPath);
      map.put(SecurityConstants.ENCRYPT_PROPERTIES, encryptionProperties);
    }

    String stsPropertiesPath = getTokenProperties();
    if (stsPropertiesPath != null && !stsPropertiesPath.isEmpty()) {
      LOGGER.debug("Setting sts properties on STSClient: {}", stsPropertiesPath);
      Properties stsProperties = PropertiesLoader.loadProperties(stsPropertiesPath);
      map.put(SecurityConstants.STS_TOKEN_PROPERTIES, stsProperties);
    }

    LOGGER.debug("Setting callback handler on STSClient");
    // DDF-733 map.put(SecurityConstants.CALLBACK_HANDLER, new CommonCallbackHandler());

    LOGGER.debug("Setting STS TOKEN USE CERT FOR KEY INFO to \"true\"");
    map.put(SecurityConstants.STS_TOKEN_USE_CERT_FOR_KEYINFO, String.valueOf(getUseKey()));

    LOGGER.debug("Adding in realm information to the STSClient");
    map.put("CLIENT_REALM", "DDF");

    stsClient.setProperties(map);
  }

  /** Helper method to setup STS Client. */
  private STSClient configureBaseStsClient() {
    STSClient stsClient = new STSClient(bus);
    String stsAddress = getAddress();
    String stsServiceName = getServiceName();
    String stsEndpointName = getEndpointName();

    if (stsAddress != null) {
      LOGGER.debug("Setting WSDL location on STSClient: {}", stsAddress);
      stsClient.setWsdlLocation(stsAddress);
    }

    if (stsServiceName != null) {
      LOGGER.debug("Setting service name on STSClient: {}", stsServiceName);
      stsClient.setServiceName(stsServiceName);
    }

    if (stsEndpointName != null) {
      LOGGER.debug("Setting endpoint name on STSClient: {}", stsEndpointName);
      stsClient.setEndpointName(stsEndpointName);
    }

    LOGGER.debug("Setting addressing namespace on STSClient: {}", ADDRESSING_NAMESPACE);
    stsClient.setAddressingNamespace(ADDRESSING_NAMESPACE);

    return stsClient;
  }

  /** Helper method to setup STS Client. */
  protected STSClient configureStsClient() {
    LOGGER.debug("Configuring the STS client.");

    STSClient stsClient = configureBaseStsClient();

    addStsProperties(stsClient);

    setClaimsOnStsClient(stsClient, createClaimsElement());

    if (LOGGER.isDebugEnabled()) {
      logStsClientConfiguration(stsClient);
    }

    return stsClient;
  }

  /** Set the claims on the sts client. */
  private void setClaimsOnStsClient(STSClient stsClient, Element claimsElement) {
    if (claimsElement != null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Setting STS claims to: {}", this.getFormattedXml(claimsElement));
      }

      stsClient.setClaims(claimsElement);
    }
  }

  /**
   * Create the claims element with the claims provided in the STS client configuration in the admin
   * console.
   */
  protected Element createClaimsElement() {
    Element claimsElement = null;
    Set<String> claims = new LinkedHashSet<>();
    claims.addAll(getClaims());

    if (contextPolicyManager != null) {
      Collection<ContextPolicy> contextPolicies = contextPolicyManager.getAllContextPolicies();
      if (contextPolicies != null && !contextPolicies.isEmpty()) {
        for (ContextPolicy contextPolicy : contextPolicies) {
          claims.addAll(contextPolicy.getAllowedAttributeNames());
        }
      }
    }

    if (!claims.isEmpty()) {
      W3CDOMStreamWriter writer = null;

      try {
        writer = new W3CDOMStreamWriter();

        writer.writeStartElement("wst", "Claims", STSUtils.WST_NS_05_12);
        writer.writeNamespace("wst", STSUtils.WST_NS_05_12);
        writer.writeNamespace("ic", "http://schemas.xmlsoap.org/ws/2005/05/identity");
        writer.writeAttribute("Dialect", "http://schemas.xmlsoap.org/ws/2005/05/identity");

        for (String claim : claims) {
          LOGGER.trace("Claim: {}", claim);
          writer.writeStartElement(
              "ic", "ClaimType", "http://schemas.xmlsoap.org/ws/2005/05/identity");
          writer.writeAttribute("Uri", claim);
          writer.writeAttribute("Optional", "true");
          writer.writeEndElement();
        }

        writer.writeEndElement();

        claimsElement = writer.getDocument().getDocumentElement();
      } catch (XMLStreamException e) {
        String msg =
            "Unable to create claims. Subjects will not have any attributes. Check STS Client configuration.";
        LOGGER.warn(msg, e);
        claimsElement = null;
      } finally {
        if (writer != null) {
          try {
            writer.close();
          } catch (XMLStreamException ignore) {
            // ignore
          }
        }
      }

      if (LOGGER.isDebugEnabled()) {
        if (claimsElement != null) {
          LOGGER.debug("Claims: {}", getFormattedXml(claimsElement));
        }
      }
    } else {
      LOGGER.debug("There are no claims to process.");
      claimsElement = null;
    }

    return claimsElement;
  }

  /** Transform into formatted XML. */
  private String getFormattedXml(Node node) {
    Document document =
        node.getOwnerDocument().getImplementation().createDocument("", "fake", null);
    Element copy = (Element) document.importNode(node, true);
    document.importNode(node, false);
    document.removeChild(document.getDocumentElement());
    document.appendChild(copy);
    DOMImplementation domImpl = document.getImplementation();
    DOMImplementationLS domImplLs = (DOMImplementationLS) domImpl.getFeature("LS", "3.0");
    if (null != domImplLs) {
      LSSerializer serializer = domImplLs.createLSSerializer();
      serializer.getDomConfig().setParameter("format-pretty-print", true);
      return serializer.writeToString(document);
    } else {
      return "";
    }
  }

  public List<String> getUsernameAttributeList() {
    return usernameAttributeList;
  }

  public void setUsernameAttributeList(List<String> usernameAttributeList) {
    this.usernameAttributeList = usernameAttributeList;
  }

  @Override
  public String getAddress() {
    return address.getResolvedString();
  }

  @Override
  public void setAddress(String address) {
    this.address = new PropertyResolver(address);
  }

  @Override
  public String getEndpointName() {
    return endpointName;
  }

  @Override
  public void setEndpointName(String endpointName) {
    this.endpointName = endpointName;
  }

  @Override
  public String getServiceName() {
    return serviceName;
  }

  @Override
  public void setServiceName(String serviceName) {
    this.serviceName = serviceName;
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public void setUsername(String username) {
    this.username = username;
  }

  @Override
  public String getPassword() {
    return password;
  }

  @Override
  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public String getSignatureUsername() {
    return signatureUsername;
  }

  @Override
  public void setSignatureUsername(String signatureUsername) {
    this.signatureUsername = signatureUsername;
  }

  @Override
  public String getSignatureProperties() {
    return signatureProperties;
  }

  @Override
  public void setSignatureProperties(String signatureProperties) {
    this.signatureProperties = signatureProperties;
  }

  @Override
  public String getEncryptionUsername() {
    return encryptionUsername;
  }

  @Override
  public void setEncryptionUsername(String encryptionUsername) {
    this.encryptionUsername = encryptionUsername;
  }

  @Override
  public String getEncryptionProperties() {
    return encryptionProperties;
  }

  @Override
  public void setEncryptionProperties(String encryptionProperties) {
    this.encryptionProperties = encryptionProperties;
  }

  @Override
  public String getTokenUsername() {
    return tokenUsername;
  }

  @Override
  public void setTokenUsername(String tokenUsername) {
    this.tokenUsername = tokenUsername;
  }

  @Override
  public String getTokenProperties() {
    return tokenProperties;
  }

  @Override
  public void setTokenProperties(String tokenProperties) {
    this.tokenProperties = tokenProperties;
  }

  @Override
  public List<String> getClaims() {
    return claims;
  }

  @Override
  public void setClaims(List<String> claims) {
    this.claims = Collections.unmodifiableList(claims);
  }

  @Override
  public void setClaims(String claimsListAsString) {

    setClaims(SPLITTER.splitToList(claimsListAsString));
  }

  @Override
  public String getAssertionType() {
    return assertionType;
  }

  @Override
  public void setAssertionType(String assertionType) {
    this.assertionType = assertionType;
  }

  @Override
  public String getKeyType() {
    return keyType;
  }

  @Override
  public void setKeyType(String keyType) {
    this.keyType = keyType;
  }

  @Override
  public String getKeySize() {
    return keySize;
  }

  @Override
  public void setKeySize(String keySize) {
    this.keySize = keySize;
  }

  @Override
  public Boolean getUseKey() {
    return useKey;
  }

  @Override
  public void setUseKey(Boolean useKey) {
    this.useKey = useKey;
  }

  public SamlAssertionValidator getSamlAssertionValidator() {
    return samlAssertionValidator;
  }

  public void setSamlAssertionValidator(SamlAssertionValidator samlAssertionValidator) {
    this.samlAssertionValidator = samlAssertionValidator;
  }

  /**
   * Credentials matcher class that ensures the AuthInfo received from the STS matches the AuthToken
   */
  protected static class STSCredentialsMatcher implements CredentialsMatcher {

    @Override
    public boolean doCredentialsMatch(AuthenticationToken token, AuthenticationInfo info) {
      if (token instanceof SAMLAuthenticationToken) {
        Object oldToken = token.getCredentials();
        Object newToken = info.getCredentials();
        return oldToken.equals(newToken);
      } else if (token instanceof STSAuthenticationToken) {
        String xmlCreds = ((STSAuthenticationToken) token).getCredentialsAsString();
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
