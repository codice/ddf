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
package org.codice.ddf.security.filter.login;

import com.google.common.hash.Hashing;
import ddf.security.PropertiesLoader;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.assertion.impl.SecurityAssertionImpl;
import ddf.security.common.SecurityTokenHolder;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.http.SessionFactory;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivilegedExceptionAction;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.rs.security.saml.sso.SAMLProtocolResponseValidator;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.CryptoFactory;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.saml.SamlAssertionWrapper;
import org.apache.wss4j.dom.WSDocInfo;
import org.apache.wss4j.dom.engine.WSSConfig;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.saml.WSSSAMLKeyInfoProcessor;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.SamlAssertionValidator;
import org.apache.wss4j.dom.validate.Validator;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.platform.filter.AuthenticationChallengeException;
import org.codice.ddf.platform.filter.AuthenticationException;
import org.codice.ddf.platform.filter.AuthenticationFailureException;
import org.codice.ddf.platform.filter.FilterChain;
import org.codice.ddf.platform.filter.SecurityFilter;
import org.codice.ddf.platform.util.XMLUtils;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.HandlerResult;
import org.codice.ddf.security.handler.api.SAMLAuthenticationToken;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.saml.common.SAMLObjectBuilder;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.saml.saml2.core.StatusCode;
import org.opensaml.saml.saml2.core.StatusMessage;
import org.opensaml.saml.saml2.core.SubjectConfirmation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/** Servlet filter that exchanges all incoming tokens for a SAML assertion via an STS. */
public class LoginFilter implements SecurityFilter {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoginFilter.class);

  private static final String DDF_AUTHENTICATION_TOKEN = "ddf.security.token";

  private static final ThreadLocal<DocumentBuilder> BUILDER =
      new ThreadLocal<DocumentBuilder>() {
        @Override
        protected DocumentBuilder initialValue() {
          try {
            return XML_UTILS.getSecureDocumentBuilder(true);
          } catch (ParserConfigurationException ex) {
            // This exception should not happen
            throw new IllegalArgumentException("Unable to create new DocumentBuilder", ex);
          }
        }
      };

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private static SAMLObjectBuilder<Status> statusBuilder;

  private static SAMLObjectBuilder<StatusCode> statusCodeBuilder;

  private static SAMLObjectBuilder<StatusMessage> statusMessageBuilder;

  private static SAMLObjectBuilder<Response> responseBuilder;

  private static SAMLObjectBuilder<Issuer> issuerBuilder;

  private static XMLObjectBuilderFactory builderFactory =
      XMLObjectProviderRegistrySupport.getBuilderFactory();

  private SecurityManager securityManager;

  private String signaturePropertiesFile;

  private Crypto signatureCrypto;

  private Validator assertionValidator = new SamlAssertionValidator();

  private SessionFactory sessionFactory;

  /** Default expiration value is 31 minutes */
  private int expirationTime = 31;

  public LoginFilter() {
    super();
  }

  /**
   * Creates the SAML response that we use for validation against the CXF code.
   *
   * @param inResponseTo
   * @param issuer
   * @param status
   * @return Response
   */
  private static Response createSamlResponse(String inResponseTo, String issuer, Status status) {
    if (responseBuilder == null) {
      responseBuilder =
          (SAMLObjectBuilder<Response>) builderFactory.getBuilder(Response.DEFAULT_ELEMENT_NAME);
    }
    Response response = responseBuilder.buildObject();

    response.setID(UUID.randomUUID().toString());
    response.setIssueInstant(new DateTime());
    response.setInResponseTo(inResponseTo);
    response.setIssuer(createIssuer(issuer));
    response.setStatus(status);
    response.setVersion(SAMLVersion.VERSION_20);

    return response;
  }

  /**
   * Creates the issuer object for the response.
   *
   * @param issuerValue
   * @return Issuer
   */
  private static Issuer createIssuer(String issuerValue) {
    if (issuerBuilder == null) {
      issuerBuilder =
          (SAMLObjectBuilder<Issuer>) builderFactory.getBuilder(Issuer.DEFAULT_ELEMENT_NAME);
    }
    Issuer issuer = issuerBuilder.buildObject();
    issuer.setValue(issuerValue);

    return issuer;
  }

  /**
   * Creates the status object for the response.
   *
   * @param statusCodeValue
   * @param statusMessage
   * @return Status
   */
  private static Status createStatus(String statusCodeValue, String statusMessage) {
    if (statusBuilder == null) {
      statusBuilder =
          (SAMLObjectBuilder<Status>) builderFactory.getBuilder(Status.DEFAULT_ELEMENT_NAME);
    }
    if (statusCodeBuilder == null) {
      statusCodeBuilder =
          (SAMLObjectBuilder<StatusCode>)
              builderFactory.getBuilder(StatusCode.DEFAULT_ELEMENT_NAME);
    }
    if (statusMessageBuilder == null) {
      statusMessageBuilder =
          (SAMLObjectBuilder<StatusMessage>)
              builderFactory.getBuilder(StatusMessage.DEFAULT_ELEMENT_NAME);
    }

    Status status = statusBuilder.buildObject();

    StatusCode statusCode = statusCodeBuilder.buildObject();
    statusCode.setValue(statusCodeValue);
    status.setStatusCode(statusCode);

    if (statusMessage != null) {
      StatusMessage statusMessageObject = statusMessageBuilder.buildObject();
      statusMessageObject.setMessage(statusMessage);
      status.setStatusMessage(statusMessageObject);
    }

    return status;
  }

  public void setSessionFactory(SessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }

  @Override
  public void init() {
    LOGGER.debug("Starting LoginFilter.");
  }

  /**
   * Validates an attached SAML assertion, or exchanges any other incoming token for a SAML
   * assertion via the STS.
   *
   * @param request
   * @param response
   * @param chain
   * @throws IOException
   * @throws ServletException
   */
  @Override
  public void doFilter(
      final ServletRequest request, final ServletResponse response, final FilterChain chain)
      throws IOException, AuthenticationException {
    LOGGER.debug("Performing doFilter() on LoginFilter");
    HttpServletRequest httpRequest = (HttpServletRequest) request;

    if (request.getAttribute(ContextPolicy.NO_AUTH_POLICY) != null) {
      LOGGER.debug("NO_AUTH_POLICY header was found, skipping login filter.");
      chain.doFilter(request, response);
    } else {
      // perform validation
      final Subject subject = validateRequest(httpRequest);
      if (subject != null) {
        httpRequest.setAttribute(SecurityConstants.SECURITY_SUBJECT, subject);
        LOGGER.debug(
            "Now performing request as user {} for {}",
            subject.getPrincipal(),
            StringUtils.isNotBlank(httpRequest.getContextPath())
                ? httpRequest.getContextPath()
                : httpRequest.getServletPath());
        subject.execute(
            () -> {
              PrivilegedExceptionAction<Void> action =
                  () -> {
                    chain.doFilter(request, response);
                    return null;
                  };
              SecurityAssertion securityAssertion =
                  subject.getPrincipals().oneByType(SecurityAssertion.class);
              if (null != securityAssertion) {
                HashSet emptySet = new HashSet();
                javax.security.auth.Subject javaSubject =
                    new javax.security.auth.Subject(
                        true, securityAssertion.getPrincipals(), emptySet, emptySet);
                httpRequest.setAttribute(SecurityConstants.SECURITY_JAVA_SUBJECT, javaSubject);
                javax.security.auth.Subject.doAs(javaSubject, action);
              } else {
                LOGGER.debug("Subject had no security assertion.");
              }
              return null;
            });

      } else {
        LOGGER.debug("Could not attach subject to http request.");
      }
    }
  }

  private Subject validateRequest(final HttpServletRequest httpRequest)
      throws AuthenticationChallengeException, AuthenticationFailureException {

    Subject subject = null;

    Object ddfAuthToken = httpRequest.getAttribute(DDF_AUTHENTICATION_TOKEN);

    if (ddfAuthToken instanceof HandlerResult) {
      HandlerResult result = (HandlerResult) ddfAuthToken;
      BaseAuthenticationToken thisToken = result.getToken();

      /*
       * If the user has already authenticated they will have a valid SAML token. Validate
       * that here and create the subject from the token.
       */
      if (thisToken instanceof SAMLAuthenticationToken) {
        subject = handleAuthenticationToken(httpRequest, (SAMLAuthenticationToken) thisToken);
      } else if (thisToken != null) {
        subject = handleAuthenticationToken(httpRequest, thisToken);
      }
    }

    return subject;
  }

  private Subject handleAuthenticationToken(
      HttpServletRequest httpRequest, SAMLAuthenticationToken token)
      throws AuthenticationFailureException {
    Subject subject;
    try {
      LOGGER.debug("Validating received SAML assertion.");

      boolean wasReference = false;
      boolean firstLogin = true;

      if (token.isReference()) {
        wasReference = true;
        LOGGER.trace("Converting SAML reference to assertion");
        Object sessionToken =
            httpRequest.getSession(false).getAttribute(SecurityConstants.SAML_ASSERTION);
        if (LOGGER.isTraceEnabled()) {
          LOGGER.trace(
              "Http Session assertion - class: {}  loader: {}",
              sessionToken.getClass().getName(),
              sessionToken.getClass().getClassLoader());
          LOGGER.trace(
              "SecurityToken class: {}  loader: {}",
              SecurityToken.class.getName(),
              SecurityToken.class.getClassLoader());
        }
        SecurityToken savedToken = null;
        try {
          savedToken = ((SecurityTokenHolder) sessionToken).getSecurityToken(token.getRealm());
        } catch (ClassCastException e) {
          httpRequest.getSession(false).invalidate();
        }
        if (savedToken != null) {
          firstLogin = false;
          token.replaceReferenece(savedToken);
        }
        if (token.isReference()) {
          String msg = "Missing or invalid SAML assertion for provided reference.";
          LOGGER.debug(msg);
          throw new AuthenticationFailureException(msg);
        }
      }

      SAMLAuthenticationToken newToken = renewSecurityToken(httpRequest.getSession(false), token);

      SecurityToken securityToken;
      if (newToken != null) {
        firstLogin = false;
        securityToken = (SecurityToken) newToken.getCredentials();
      } else {
        securityToken = (SecurityToken) token.getCredentials();
      }
      if (!wasReference) {
        // wrap the token
        SamlAssertionWrapper assertion = new SamlAssertionWrapper(securityToken.getToken());

        // get the crypto junk
        Crypto crypto = getSignatureCrypto();
        Response samlResponse =
            createSamlResponse(
                httpRequest.getRequestURI(),
                assertion.getIssuerString(),
                createStatus(SAMLProtocolResponseValidator.SAML2_STATUSCODE_SUCCESS, null));

        BUILDER.get().reset();
        Document doc = BUILDER.get().newDocument();
        Element policyElement = OpenSAMLUtil.toDom(samlResponse, doc);
        doc.appendChild(policyElement);

        Credential credential = new Credential();
        credential.setSamlAssertion(assertion);

        RequestData requestData = new RequestData();
        requestData.setWsDocInfo(new WSDocInfo(samlResponse.getDOM().getOwnerDocument()));
        requestData.setSigVerCrypto(crypto);
        WSSConfig wssConfig = WSSConfig.getNewInstance();
        requestData.setWssConfig(wssConfig);

        X509Certificate[] x509Certs =
            (X509Certificate[]) httpRequest.getAttribute("javax.servlet.request.X509Certificate");
        requestData.setTlsCerts(x509Certs);

        validateHolderOfKeyConfirmation(assertion, x509Certs);

        if (assertion.isSigned()) {
          // Verify the signature
          WSSSAMLKeyInfoProcessor wsssamlKeyInfoProcessor =
              new WSSSAMLKeyInfoProcessor(requestData);
          assertion.verifySignature(wsssamlKeyInfoProcessor, crypto);

          assertion.parseSubject(
              new WSSSAMLKeyInfoProcessor(requestData),
              requestData.getSigVerCrypto(),
              requestData.getCallbackHandler());
        }

        // Validate the Assertion & verify trust in the signature
        assertionValidator.validate(credential, requestData);
      }

      // if it is all good, then we'll create our subject
      subject = securityManager.getSubject(securityToken);

      if (firstLogin) {
        boolean hasSecurityAuditRole =
            Arrays.stream(System.getProperty("security.audit.roles").split(","))
                .anyMatch(subject::hasRole);
        if (hasSecurityAuditRole) {
          SecurityLogger.audit("Subject has logged in with admin privileges", subject);
        }
      }
      if (!wasReference && firstLogin) {
        addSamlToSession(httpRequest, token.getRealm(), securityToken);
      }
    } catch (SecurityServiceException e) {
      LOGGER.debug("Unable to get subject from SAML request.", e);
      throw new AuthenticationFailureException(e);
    } catch (WSSecurityException e) {
      LOGGER.debug("Unable to read/validate security token from request.", e);
      throw new AuthenticationFailureException(e);
    }
    return subject;
  }

  private void validateHolderOfKeyConfirmation(
      SamlAssertionWrapper assertion, X509Certificate[] x509Certs) throws SecurityServiceException {
    List<String> confirmationMethods = assertion.getConfirmationMethods();
    boolean hasHokMethod = false;
    for (String method : confirmationMethods) {
      if (OpenSAMLUtil.isMethodHolderOfKey(method)) {
        hasHokMethod = true;
      }
    }

    if (hasHokMethod) {
      if (x509Certs != null && x509Certs.length > 0) {
        List<SubjectConfirmation> subjectConfirmations =
            assertion.getSaml2().getSubject().getSubjectConfirmations();
        for (SubjectConfirmation subjectConfirmation : subjectConfirmations) {
          if (OpenSAMLUtil.isMethodHolderOfKey(subjectConfirmation.getMethod())) {
            Element dom = subjectConfirmation.getSubjectConfirmationData().getDOM();
            Node keyInfo = dom.getFirstChild();
            Node x509Data = keyInfo.getFirstChild();
            Node dataNode = x509Data.getFirstChild();
            Node dataText = dataNode.getFirstChild();

            X509Certificate tlsCertificate = x509Certs[0];
            if (dataNode.getLocalName().equals("X509Certificate")) {
              String textContent = dataText.getTextContent();
              byte[] byteValue = Base64.getMimeDecoder().decode(textContent);
              try {
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                X509Certificate cert =
                    (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(byteValue));
                // check that the certificate is still valid
                cert.checkValidity();

                // HoK spec section 2.5:
                // relying party MUST ensure that the certificate bound to the assertion matches the
                // X.509 certificate in its possession.
                // Matching is done by comparing the base64-decoded certificates, or the hash values
                // of the base64-decoded certificates, byte-for-byte.
                // if the certs aren't the same, verify
                if (!tlsCertificate.equals(cert)) {
                  // verify that the cert was signed by the same private key as the TLS cert
                  cert.verify(tlsCertificate.getPublicKey());
                }
              } catch (CertificateException
                  | NoSuchAlgorithmException
                  | InvalidKeyException
                  | SignatureException
                  | NoSuchProviderException e) {
                throw new SecurityServiceException(
                    "Unable to validate Holder of Key assertion with certificate.");
              }

            } else if (dataNode.getLocalName().equals("X509SubjectName")) {
              String textContent = dataText.getTextContent();
              // HoK spec section 2.5:
              // relying party MUST ensure that the subject distinguished name (DN) bound to the
              // assertion matches the DN bound to the X.509 certificate.
              // If, however, the relying party does not trust the certificate issuer to issue such
              // a DN, the attesting entity is not confirmed and the relying party SHOULD disregard
              // the assertion.
              if (!tlsCertificate.getSubjectDN().getName().equals(textContent)) {
                throw new SecurityServiceException(
                    "Unable to validate Holder of Key assertion with subject DN.");
              }

            } else if (dataNode.getLocalName().equals("X509IssuerSerial")) {
              // we have no way to support this confirmation type so we have to throw an error
              throw new SecurityServiceException(
                  "Unable to validate Holder of Key assertion with issuer serial. NOT SUPPORTED");
            } else if (dataNode.getLocalName().equals("X509SKI")) {
              String textContent = dataText.getTextContent();
              byte[] tlsSKI = tlsCertificate.getExtensionValue("2.5.29.14");
              byte[] assertionSKI = Base64.getMimeDecoder().decode(textContent);
              if (tlsSKI != null && tlsSKI.length > 0) {
                ASN1OctetString tlsOs = ASN1OctetString.getInstance(tlsSKI);
                ASN1OctetString assertionOs = ASN1OctetString.getInstance(assertionSKI);
                SubjectKeyIdentifier tlsSubjectKeyIdentifier =
                    SubjectKeyIdentifier.getInstance(tlsOs.getOctets());
                SubjectKeyIdentifier assertSubjectKeyIdentifier =
                    SubjectKeyIdentifier.getInstance(assertionOs.getOctets());
                // HoK spec section 2.5:
                // relying party MUST ensure that the value bound to the assertion matches the
                // Subject Key Identifier (SKI) extension bound to the X.509 certificate.
                // Matching is done by comparing the base64-decoded SKI values byte-for-byte. If the
                // X.509 certificate does not contain an SKI extension,
                // the attesting entity is not confirmed and the relying party SHOULD disregard the
                // assertion.
                if (!Arrays.equals(
                    tlsSubjectKeyIdentifier.getKeyIdentifier(),
                    assertSubjectKeyIdentifier.getKeyIdentifier())) {
                  throw new SecurityServiceException(
                      "Unable to validate Holder of Key assertion with subject key identifier.");
                }
              } else {
                throw new SecurityServiceException(
                    "Unable to validate Holder of Key assertion with subject key identifier.");
              }
            }
          }
        }
      } else {
        throw new SecurityServiceException("Holder of Key assertion, must be used with 2-way TLS.");
      }
    }
  }

  private SAMLAuthenticationToken renewSecurityToken(
      HttpSession session, SAMLAuthenticationToken savedToken)
      throws AuthenticationFailureException {
    if (session != null) {
      SecurityAssertion savedAssertion =
          new SecurityAssertionImpl(((SecurityToken) savedToken.getCredentials()));

      if (savedAssertion.getIssuer() != null
          && !savedAssertion.getIssuer().equals(SystemBaseUrl.INTERNAL.getHost())) {
        return null;
      }

      if (savedAssertion.getNotOnOrAfter() == null) {
        return null;
      }

      long afterMil = savedAssertion.getNotOnOrAfter().getTime();
      long timeoutMillis = (afterMil - System.currentTimeMillis());

      if (timeoutMillis <= 0) {
        throw new AuthenticationFailureException("SAML assertion has expired.");
      }

      if (timeoutMillis <= 60000) { // within 60 seconds
        try {
          LOGGER.debug("Attempting to refresh user's SAML assertion.");

          Subject subject = securityManager.getSubject(savedToken);
          LOGGER.debug("Refresh of user assertion successful");
          for (Object principal : subject.getPrincipals()) {
            if (principal instanceof SecurityAssertion) {
              SecurityToken token = ((SecurityAssertion) principal).getSecurityToken();
              SAMLAuthenticationToken samlAuthenticationToken =
                  new SAMLAuthenticationToken(
                      (java.security.Principal) savedToken.getPrincipal(),
                      token,
                      savedToken.getRealm());
              if (LOGGER.isTraceEnabled()) {
                LOGGER.trace(
                    "Setting session token - class: {}  classloader: {}",
                    token.getClass().getName(),
                    token.getClass().getClassLoader());
              }
              ((SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION))
                  .addSecurityToken(savedToken.getRealm(), token);

              LOGGER.debug("Saved new user assertion to session.");

              return samlAuthenticationToken;
            }
          }

        } catch (SecurityServiceException e) {
          LOGGER.debug(
              "Unable to refresh user's SAML assertion. User will log out prematurely.", e);
          session.invalidate();
        } catch (Exception e) {
          LOGGER.info("Unhandled exception occurred.", e);
          session.invalidate();
        }
      }
    }
    return null;
  }

  private Subject handleAuthenticationToken(
      HttpServletRequest httpRequest, BaseAuthenticationToken token)
      throws AuthenticationFailureException {

    Subject subject;
    HttpSession session = sessionFactory.getOrCreateSession(httpRequest);
    // if we already have an assertion inside the session and it has not expired, then use that
    // instead
    SecurityToken sessionToken = getSecurityToken(session, token.getRealm());

    if (sessionToken == null) {

      /*
       * The user didn't have a SAML token from a previous authentication, but they do have the
       * credentials to log in - perform that action here.
       */
      try {
        // login with the specified authentication credentials (AuthenticationToken)
        subject = securityManager.getSubject(token);

        for (Object principal : subject.getPrincipals().asList()) {
          if (principal instanceof SecurityAssertion) {
            if (LOGGER.isTraceEnabled()) {
              Element samlToken = ((SecurityAssertion) principal).getSecurityToken().getToken();

              LOGGER.trace("SAML Assertion returned: {}", XML_UTILS.prettyFormat(samlToken));
            }
            SecurityToken securityToken = ((SecurityAssertion) principal).getSecurityToken();
            addSamlToSession(httpRequest, token.getRealm(), securityToken);
          }
        }
      } catch (SecurityServiceException e) {
        LOGGER.debug("Unable to get subject from auth request.", e);
        throw new AuthenticationFailureException(e);
      }
    } else {
      LOGGER.trace("Creating SAML authentication token with session.");
      SAMLAuthenticationToken samlToken =
          new SAMLAuthenticationToken(null, session.getId(), token.getRealm());
      return handleAuthenticationToken(httpRequest, samlToken);
    }
    return subject;
  }

  private SecurityToken getSecurityToken(HttpSession session, String realm) {
    if (session.getAttribute(SecurityConstants.SAML_ASSERTION) == null) {
      LOGGER.debug("Security token holder missing from session. New session created improperly.");
      return null;
    }

    SecurityTokenHolder tokenHolder =
        ((SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION));

    SecurityToken token = tokenHolder.getSecurityToken(realm);

    if (token != null) {
      SecurityAssertionImpl assertion = new SecurityAssertionImpl(token);
      if (!assertion.isPresentlyValid()) {
        LOGGER.debug("Session SAML token is invalid.  Removing from session.");
        tokenHolder.remove(realm);
        return null;
      }
    }

    return token;
  }

  private void addSecurityToken(HttpSession session, String realm, SecurityToken token) {
    SecurityTokenHolder holder =
        (SecurityTokenHolder) session.getAttribute(SecurityConstants.SAML_ASSERTION);

    holder.addSecurityToken(realm, token);
  }

  /**
   * Adds SAML assertion to HTTP session.
   *
   * @param httpRequest the http request object for this request
   * @param securityToken the SecurityToken object representing the SAML assertion
   */
  private void addSamlToSession(
      HttpServletRequest httpRequest, String realm, SecurityToken securityToken) {
    if (securityToken == null) {
      LOGGER.debug("Cannot add null security token to session.");
      return;
    }

    HttpSession session = sessionFactory.getOrCreateSession(httpRequest);
    SecurityToken sessionToken = getSecurityToken(session, realm);
    if (sessionToken == null) {
      addSecurityToken(session, realm, securityToken);
    }
    SecurityAssertion securityAssertion = new SecurityAssertionImpl(securityToken);
    SecurityLogger.audit(
        "Added SAML for user [{}] to session [{}]",
        securityAssertion.getPrincipal().getName(),
        Hashing.sha256().hashString(session.getId(), StandardCharsets.UTF_8).toString());
    int minutes = getExpirationTime();
    // we just want to set this to some non-zero value if the configuration is messed up
    int seconds = 60;
    if (minutes > 0) {
      seconds = minutes * 60;
    }
    session.setMaxInactiveInterval(seconds);
  }

  /**
   * Returns a Crypto object initialized against the system signature properties.
   *
   * @return Crypto
   */
  private Crypto getSignatureCrypto() {
    if (signatureCrypto == null && signaturePropertiesFile != null) {
      Properties sigProperties = PropertiesLoader.loadProperties(signaturePropertiesFile);
      if (sigProperties == null) {
        LOGGER.trace("Cannot load signature properties using: {}", signaturePropertiesFile);
        return null;
      }
      ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
      Thread.currentThread().setContextClassLoader(LoginFilter.class.getClassLoader());
      try {
        signatureCrypto = CryptoFactory.getInstance(sigProperties);
      } catch (WSSecurityException ex) {
        LOGGER.trace("Error in loading the signature Crypto object.", ex);
        return null;
      } finally {
        Thread.currentThread().setContextClassLoader(contextClassLoader);
      }
    }
    return signatureCrypto;
  }

  @Override
  public void destroy() {
    LOGGER.debug("Destroying log in filter");
    BUILDER.remove();
  }

  public SecurityManager getSecurityManager() {
    return securityManager;
  }

  public void setSecurityManager(SecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  public void setSignaturePropertiesFile(String signaturePropertiesFile) {
    this.signaturePropertiesFile = signaturePropertiesFile;
  }

  /**
   * Returns session expiration time in minutes.
   *
   * @return minutes for session expiration
   */
  public int getExpirationTime() {
    return expirationTime;
  }

  /**
   * Sets session expiration time in minutes
   *
   * @param expirationTime - time in minutes
   */
  public void setExpirationTime(int expirationTime) {
    this.expirationTime = expirationTime;
  }
}
