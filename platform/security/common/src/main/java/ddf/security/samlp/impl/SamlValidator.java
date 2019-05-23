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

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;
import ddf.security.samlp.ValidationException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import javax.validation.constraints.NotNull;
import org.codice.ddf.security.common.HttpUtils;
import org.joda.time.DateTime;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.common.SignableSAMLObject;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.LogoutResponse;
import org.opensaml.xmlsec.signature.SignableXMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class SamlValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SamlValidator.class);

  protected final Builder builder;

  private SamlValidator(Builder builder) {
    this.builder = builder;
  }

  public final void validate() throws ValidationException {
    checkTimestamp();
    checkSamlVersion();
    checkId();
    checkRequiredFields();
    checkDestination();
    additionalValidation();
  }

  protected void checkTimestamp() throws ValidationException {
    DateTime issueInstant = getIssueInstant();
    if (issueInstant == null) {
      throw new ValidationException("Issue Instant cannot be null!");
    }

    Instant instant = Instant.ofEpochMilli(issueInstant.getMillis());
    Instant now = Instant.now();
    if (instant.minus(builder.clockSkew).isAfter(now)) {
      throw new ValidationException("Issue Instant cannot be in the future");
    }

    if (instant.plus(builder.clockSkew).isBefore(now.minus(builder.timeout))) {
      throw new ValidationException("Issue Instant was outside valid time range");
    }
  }

  protected void checkSamlVersion() throws ValidationException {
    SAMLVersion samlVersion = getSamlVersion();
    if (samlVersion == null) {
      throw new ValidationException("SAML Version cannot be null!");
    }
    if (!SAMLVersion.VERSION_20.equals(samlVersion)) {
      throw new ValidationException("Invalid SAML Version!");
    }
  }

  protected abstract void checkRequiredFields() throws ValidationException;

  protected abstract void checkDestination() throws ValidationException;

  protected abstract void additionalValidation() throws ValidationException;

  protected void checkId() throws ValidationException {
    // pass, default method
  }

  protected abstract DateTime getIssueInstant();

  protected abstract SAMLVersion getSamlVersion();

  void checkPostSignature(SignableSAMLObject samlObject) throws ValidationException {
    // Signing is optional, so null is OK
    if (samlObject.getSignature() != null) {
      try {
        builder.simpleSign.validateSignature(
            samlObject.getSignature(), samlObject.getDOM().getOwnerDocument());
      } catch (SimpleSign.SignatureException e) {
        throw new ValidationException("Invalid or untrusted signature.");
      }
    }
  }

  void checkRedirectSignature(String reqres) throws ValidationException {
    try {
      StringBuilder signedParts =
          new StringBuilder(reqres)
              .append("=")
              .append(URLEncoder.encode(builder.samlString, StandardCharsets.UTF_8.name()));
      String relayState = builder.relayState;
      if (relayState != null) {
        signedParts
            .append("&RelayState=")
            .append(URLEncoder.encode(relayState, StandardCharsets.UTF_8.name()));
      }
      signedParts
          .append("&SigAlg=")
          .append(URLEncoder.encode(builder.sigAlgo, StandardCharsets.UTF_8.name()));

      if (!builder.simpleSign.validateSignature(
          builder.sigAlgo, signedParts.toString(), builder.signature, builder.signingCertificate)) {
        throw new ValidationException("Signature verification failed for redirect binding.");
      }
    } catch (SimpleSign.SignatureException | UnsupportedEncodingException e) {
      throw new ValidationException("Signature validation failed.", e);
    }
  }

  /**
   * Builder class for SamlValidator. <br>
   * Default <code>Timeout</code> of 10 minutes and <code>clockSkew</code> of 30 seconds. <br>
   * If validating a redirect saml type, the Signature, Signature Algorithm, Relay State,
   * <b>original</b> Saml string, and signing certificate are required. For Post binding type, only
   * the object is required.
   */
  public static class Builder {
    protected SimpleSign simpleSign;

    protected SamlProtocol.Binding binding;

    protected boolean isRequest;

    protected XMLObject xmlObject;

    protected Duration timeout = Duration.ofMinutes(10);

    protected Duration clockSkew = Duration.ofSeconds(30);

    protected String requestId;

    protected String destination;

    protected String relayState;

    protected String signature;

    protected String sigAlgo;

    protected String samlString;

    protected String signingCertificate;

    /**
     * Creates a new <code>SamlValidator.Builder</code> with the given SimpleSign. <br>
     * Create a new instance, set any optional arguments, and then finish by calling either <code>
     * build()</code> or <code>buildAndValidate</code>.
     *
     * @param simpleSign an instance of {@link SimpleSign}
     */
    public Builder(SimpleSign simpleSign) {
      this.simpleSign = simpleSign;
    }

    /**
     * Utility method that calls the {@link #build(String, SamlProtocol.Binding, SignableXMLObject)}
     * method and then validates the object.
     *
     * @param destination The actual endpoint that the saml object was sent to, not the destination
     *     field on the object
     * @param binding The binding of the object (POST or REDIRECT)
     * @param xmlObject target object to validate
     * @throws IllegalStateException
     * @throws ValidationException
     */
    public void buildAndValidate(
        @NotNull String destination,
        @NotNull SamlProtocol.Binding binding,
        @NotNull SignableXMLObject xmlObject)
        throws IllegalStateException, ValidationException {
      SamlValidator validator = build(destination, binding, xmlObject);
      validator.validate();
    }

    /**
     * @param destination The actual endpoint that the saml object was sent to, not the destination
     *     field on the object
     * @param binding The binding of the object (POST or REDIRECT)
     * @param xmlObject target object to validate
     * @return A {@link SamlValidator} object
     * @throws IllegalStateException
     */
    public SamlValidator build(
        @NotNull String destination,
        SamlProtocol.Binding binding,
        @NotNull SignableXMLObject xmlObject)
        throws IllegalStateException, ValidationException {
      if (binding == null) {
        throw new IllegalArgumentException("Binding cannot be null!");
      }
      this.binding = binding;

      if (isBlank(destination)) {
        throw new IllegalArgumentException("The service destination cannot be null");
      }
      this.destination = destination;

      if (xmlObject instanceof LogoutRequest) {
        isRequest = true;
        LOGGER.trace("xmlObject is a LogoutRequest [{}]", xmlObject);
      } else if (xmlObject instanceof LogoutResponse) {
        isRequest = false;
        LOGGER.trace("xmlObject is a LogoutResponse [{}]", xmlObject);
      } else {
        throw new IllegalArgumentException("Could not determine type of xmlObject");
      }
      this.xmlObject = xmlObject;

      if (binding == SamlProtocol.Binding.HTTP_POST) {
        return isRequest ? new PostRequest(this) : new PostResponse(this);
      }
      if (binding == SamlProtocol.Binding.HTTP_REDIRECT) {
        if (isBlank(signature)
            || isBlank(sigAlgo)
            || isBlank(samlString)
            || isBlank(signingCertificate)) {
          throw new UnsupportedOperationException("Cannot validate object with blank data");
        }
        return isRequest ? new RedirectRequest(this) : new RedirectResponse(this);
      }

      throw new UnsupportedOperationException("Binding not supported.");
    }

    public Builder setRedirectParams(
        String relayState,
        String signature,
        String sigAlgo,
        String samlString,
        String signingCertificate) {
      this.relayState = relayState;
      this.signature = signature;
      this.sigAlgo = sigAlgo;
      this.samlString = samlString;
      this.signingCertificate = signingCertificate;
      return this;
    }

    public Builder setRequestId(@NotNull String requestId) {
      if (isBlank(requestId)) {
        throw new IllegalArgumentException("Logout Request Id cannot be blank!");
      }
      this.requestId = requestId;
      return this;
    }

    public Builder setTimeout(Duration timeout) {
      if (timeout == null) {
        throw new IllegalArgumentException("Timeout cannot be null!");
      }
      this.timeout = timeout;
      return this;
    }

    public Builder setClockSkew(Duration clockSkew) {
      if (clockSkew == null) {
        throw new IllegalArgumentException("clockSkew cannot be null!");
      }
      this.clockSkew = clockSkew;
      return this;
    }
  }

  public abstract static class Request extends SamlValidator {

    protected final LogoutRequest logoutRequest;

    private Request(Builder builder) {
      super(builder);
      logoutRequest = (LogoutRequest) builder.xmlObject;
    }

    /**
     * A SAML LogoutRequest must include at least one SessionIndex, if the logout is initiated by
     * Session Participant. However, if the Session Authority initiates the logout, a SessionIndex
     * is optional. In that case, the absence of a SessionIndex means to logout of all sessions
     * associated with the Principle. Because the SAMLValidator does not have enough information to
     * determine who initiated the logout, this method does not throw a ValidationException.
     * Instead, it logs the condition.
     */
    protected void checkSessionIndexes() {
      if (logoutRequest.getSessionIndexes().isEmpty()) {
        LOGGER.trace(
            "Logout request does not contain a session index for name-id {}",
            logoutRequest.getNameID());
      }
    }

    @Override
    protected SAMLVersion getSamlVersion() {
      return logoutRequest.getVersion();
    }

    @Override
    protected DateTime getIssueInstant() {
      return logoutRequest.getIssueInstant();
    }

    @Override
    protected void checkRequiredFields() throws ValidationException {
      // Version existence covered in #checkSamlVersion
      // IssueInstance existence covered in #checkTimestamp
      if (isBlank(logoutRequest.getID())) {
        throw new ValidationException("ID cannot be blank!");
      }
    }

    @Override
    protected void checkDestination() throws ValidationException {
      if (isNotBlank(logoutRequest.getDestination())) {
        try {
          if (!HttpUtils.validateAndStripQueryString(logoutRequest.getDestination())
              .equals(builder.destination)) {
            throw new ValidationException("Destination validation failed");
          }
        } catch (MalformedURLException e) {
          throw new ValidationException(
              String.format("Destination [%s]is not a valid URL", logoutRequest.getDestination()),
              e);
        }
      }
    }
  }

  public abstract static class Response extends SamlValidator {

    protected final LogoutResponse logoutResponse;

    private Response(Builder builder) {
      super(builder);
      logoutResponse = (LogoutResponse) builder.xmlObject;
    }

    @Override
    protected SAMLVersion getSamlVersion() {
      return logoutResponse.getVersion();
    }

    @Override
    protected DateTime getIssueInstant() {
      return logoutResponse.getIssueInstant();
    }

    @Override
    protected void checkRequiredFields() throws ValidationException {
      // Version existence covered in #checkSamlVersion
      // IssueInstance existence covered in #checkTimestamp
      if (isBlank(logoutResponse.getID())) {
        throw new ValidationException("ID cannot be blank!");
      }
    }

    @Override
    protected void checkDestination() throws ValidationException {
      if (isNotBlank(logoutResponse.getDestination())) {
        try {
          if (!builder.destination.equalsIgnoreCase(
              HttpUtils.validateAndStripQueryString(logoutResponse.getDestination()))) {
            throw new ValidationException("Destination validation failed");
          }
        } catch (MalformedURLException e) {
          throw new ValidationException("Invalid Destination URL", e);
        }
      }
    }

    @Override
    protected void checkId() throws ValidationException {
      if (isNotBlank(builder.requestId)
          && !builder.requestId.equals(logoutResponse.getInResponseTo())) {
        throw new ValidationException("The InResponseTo value did not match the Logout Request Id");
      }
    }
  }

  public static class PostRequest extends Request {

    private PostRequest(Builder builder) {
      super(builder);
    }

    @Override
    protected void additionalValidation() throws ValidationException {
      checkPostSignature(logoutRequest);
      checkSessionIndexes();
    }
  }

  public static class PostResponse extends Response {

    private PostResponse(Builder builder) {
      super(builder);
    }

    @Override
    protected void additionalValidation() throws ValidationException {
      checkPostSignature(logoutResponse);
    }
  }

  public static class RedirectRequest extends Request {

    private RedirectRequest(Builder builder) {
      super(builder);
    }

    @Override
    protected void additionalValidation() throws ValidationException {
      checkRedirectSignature("SAMLRequest");
      checkSessionIndexes();
    }
  }

  public static class RedirectResponse extends Response {

    private RedirectResponse(Builder builder) {
      super(builder);
    }

    @Override
    protected void additionalValidation() throws ValidationException {
      checkRedirectSignature("SAMLResponse");
    }
  }
}
