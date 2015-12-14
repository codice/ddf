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
package ddf.security.samlp.impl;

import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.time.Duration;
import java.time.Instant;

import javax.validation.constraints.NotNull;

import org.codice.ddf.security.common.HttpUtils;
import org.joda.time.DateTime;
import org.opensaml.common.SAMLVersion;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.validation.ValidatingXMLObject;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;

public abstract class SamlValidator implements Validator<ValidatingXMLObject> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SamlValidator.class);

    protected final Builder builder;

    private SamlValidator(Builder builder) {
        this.builder = builder;
    }

    @Override
    public final void validate(ValidatingXMLObject xmlObject) throws ValidationException {
        // This is intentionally an instance equality check
        if (xmlObject != builder.xmlObject) {
            throw new ValidationException("Cannot validate a different target.");
        }

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
        if (instant.minus(builder.clockSkew)
                .isAfter(now)) {
            throw new ValidationException("Issue Instant cannot be in the future");
        }

        if (instant.plus(builder.clockSkew)
                .isBefore(now.minus(builder.issueTimeout))) {
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
        if (samlObject.getSignature() != null) {
            try {
                builder.simpleSign.validateSignature(samlObject.getSignature(),
                        samlObject.getDOM()
                                .getOwnerDocument());
            } catch (SimpleSign.SignatureException e) {
                throw new ValidationException("Invalid or untrusted signature.");
            }
        }
    }

    void checkRedirectSignature(String reqres) throws ValidationException {
        try {
            String signedParts = String.format("%s=%s&RelayState=%s&SigAlg=%s",
                    reqres,
                    URLEncoder.encode(builder.samlString, "UTF-8"),
                    builder.relayState,
                    URLEncoder.encode(builder.sigAlgo, "UTF-8"));

            if (!builder.simpleSign.validateSignature(signedParts,
                    builder.signature,
                    builder.signingCertificate)) {
                throw new ValidationException("Signature verification failed for redirect binding.");
            }
        } catch (SimpleSign.SignatureException | UnsupportedEncodingException e) {
            throw new ValidationException("Signature validation failed.", e);
        }
    }

    public static class Builder {
        protected SimpleSign simpleSign;

        protected SamlProtocol.Binding binding;

        protected boolean isRequest;

        protected XMLObject xmlObject;

        protected Duration issueTimeout = Duration.ofMinutes(10);

        protected Duration clockSkew = Duration.ofSeconds(30);

        protected String inResponse;

        protected String endpoint;

        protected String relayState;

        protected String signature;

        protected String sigAlgo;

        protected String samlString;

        protected String signingCertificate;

        public Builder(SimpleSign simpleSign) {
            this.simpleSign = simpleSign;
        }

        public void buildAndValidate(@NotNull String destination,
                @NotNull SamlProtocol.Binding binding, @NotNull ValidatingXMLObject xmlObject)
                throws IllegalStateException, ValidationException {
            Validator<ValidatingXMLObject> validator = build(destination, binding, xmlObject);
            xmlObject.registerValidator(validator);
            xmlObject.validate(false);
        }

        /**
         * @param endpoint
         * @param binding
         * @param xmlObject target object to validate
         * @return
         * @throws IllegalStateException
         */
        public SamlValidator build(@NotNull String endpoint, @NotNull SamlProtocol.Binding binding,
                @NotNull ValidatingXMLObject xmlObject)
                throws IllegalStateException, ValidationException {
            if (binding == null) {
                throw new IllegalArgumentException("Binding cannot be null!");
            }
            this.binding = binding;

            if (isBlank(endpoint)) {
                throw new IllegalArgumentException("The service endpoint destination cannot be null");
            }
            this.endpoint = endpoint;

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
                if (isBlank(signature) || isBlank(sigAlgo) || isBlank(samlString) || isBlank(
                        signingCertificate)) {
                    throw new UnsupportedOperationException("Cannot validate object with blank data");
                }
                return isRequest ? new RedirectRequest(this) : new RedirectResponse(this);
            }

            throw new UnsupportedOperationException("Binding not supported.");
        }

        public Builder setRedirectParams(String relayState, String signature, String sigAlgo,
                String samlString, String signingCertificate) {
            this.relayState = relayState;
            this.signature = signature;
            this.sigAlgo = sigAlgo;
            this.samlString = samlString;
            this.signingCertificate = signingCertificate;
            return this;
        }

        public Builder setInResponse(@NotNull String inResponse) {
            if (isBlank(inResponse)) {
                throw new IllegalArgumentException("InResponseTo Id cannot be blank!");
            }
            this.inResponse = inResponse;
            return this;
        }

        public Builder setIssueTimeout(@NotNull Duration issueTimeout) {
            if (issueTimeout == null) {
                throw new IllegalArgumentException("Issue Timeout cannot be null!");
            }
            this.issueTimeout = issueTimeout;
            return this;
        }

        public Builder setClockSkew(@NotNull Duration clockSkew) {
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
                            .equals(builder.endpoint)) {
                        throw new ValidationException("Destination validation failed");
                    }
                } catch (MalformedURLException e) {
                    throw new ValidationException(String.format("Destination [%s]is not a valid URL",
                            logoutRequest.getDestination()), e);
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
                    if (!builder.endpoint.equals(HttpUtils.validateAndStripQueryString(
                            logoutResponse.getDestination()))) {
                        throw new ValidationException("Destination validation failed");
                    }
                } catch (MalformedURLException e) {
                    throw new ValidationException("Invalid Destination URL", e);
                }
            }
        }

        @Override
        protected void checkId() throws ValidationException {
            if (isNotBlank(builder.inResponse)) {
                if (!builder.inResponse.equals(logoutResponse.getInResponseTo())) {
                    throw new ValidationException("The InResponseTo value was incorrect");
                }
            }
        }
    }

    public static class PostRequest extends Request {
        protected final LogoutRequest logoutRequest;

        private PostRequest(Builder builder) {
            super(builder);
            logoutRequest = (LogoutRequest) builder.xmlObject;
        }

        @Override
        protected void additionalValidation() throws ValidationException {
            checkPostSignature(logoutRequest);
        }
    }

    public static class PostResponse extends Response {
        protected final LogoutResponse logoutResponse;

        private PostResponse(Builder builder) {
            super(builder);
            logoutResponse = (LogoutResponse) builder.xmlObject;
        }

        @Override
        protected void additionalValidation() throws ValidationException {
            checkPostSignature(logoutResponse);
        }
    }

    public static class RedirectRequest extends Request {
        protected final LogoutRequest logoutRequest;

        private RedirectRequest(Builder builder) {
            super(builder);
            logoutRequest = (LogoutRequest) builder.xmlObject;
        }

        @Override
        protected void additionalValidation() throws ValidationException {
            checkRedirectSignature("SAMLRequest");
        }
    }

    public static class RedirectResponse extends Response {
        protected final LogoutResponse logoutResponse;

        private RedirectResponse(Builder builder) {
            super(builder);
            logoutResponse = (LogoutResponse) builder.xmlObject;
        }

        @Override
        protected void additionalValidation() throws ValidationException {
            checkRedirectSignature("SAMLResponse");
        }
    }
}
