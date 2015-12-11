package ddf.security.samlp.impl;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;

import javax.validation.constraints.NotNull;

import org.opensaml.common.SAMLVersion;
import org.opensaml.common.SignableSAMLObject;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.xml.XMLObject;
import org.opensaml.xml.validation.ValidationException;
import org.opensaml.xml.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;

public abstract class SamlValidator implements Validator {
    private static final Logger LOGGER = LoggerFactory.getLogger(SamlValidator.class);

    final Builder builder;

    private final XMLObject xmlObject;

    private SamlValidator(Builder builder) {
        this.builder = builder;
        xmlObject = builder.xmlObject;
    }

    @Override
    final public void validate(XMLObject xmlObject) throws ValidationException {
        // This is intentionally an instance equality check
        if (xmlObject != this.xmlObject) {
            throw new ValidationException("Cannot validate a different target.");
        }

        checkTimestamp();
        checkSamlVersion();
        checkRequiredFields();
        checkDestination();
        additionalValidation();
        checkId();
    }

    protected abstract void checkTimestamp() throws ValidationException;

    protected abstract void checkSamlVersion() throws ValidationException;

    protected abstract void checkRequiredFields() throws ValidationException;

    protected abstract void checkDestination() throws ValidationException;

    protected abstract void additionalValidation() throws ValidationException;

    protected void checkId() throws ValidationException {
        // pass, default method
    }

    protected void checkPostSignature() throws ValidationException {
        // pass, default method
    }

    protected void checkRedirectSignature() throws ValidationException {
        // pass, default method
    }

    void checkTimestamp(Instant issueInstant) throws ValidationException {
        Instant now = Instant.now();
        if (issueInstant.minus(builder.jitter)
                .isAfter(now)) {
            throw new ValidationException("Issue Instant cannot be in the future");
        }

        if (issueInstant.plus(builder.jitter)
                .isBefore(now.minus(builder.issueTimeout))) {
            throw new ValidationException("Issue Instant was outside valid time range");
        }
    }

    void checkSamlVersion(SAMLVersion samlVersion) throws ValidationException {
        if (samlVersion == null) {
            throw new ValidationException("SAML Version cannot be null!");
        }
        if (!SAMLVersion.VERSION_20.equals(samlVersion)) {
            throw new ValidationException("Invalid SAML Version!");
        }
    }

    void checkDestination(URL destination) throws ValidationException {
        // TODO (RCZ) - url validation logic, scrap the query params, yada yada yada
    }

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

    void checkRedirectSignature(Object dummy) throws ValidationException {
        // TODO (RCZ) - do this
    }

    public static class Builder {
        protected SimpleSign simpleSign;

        protected SamlProtocol.Binding binding;

        protected boolean isRequest;

        protected XMLObject xmlObject;

        protected Duration issueTimeout = Duration.ofMinutes(10);

        /*Should we be handling jitter differently?*/
        protected Duration jitter = Duration.ofSeconds(30);

        protected String inResponse;

        protected URL destination;

        protected String relayState;

        protected String signature;

        protected String sigAlgo;

        protected String samlString;

        public Builder(SimpleSign simpleSign) {
            this.simpleSign = simpleSign;
        }

        public SamlValidator build() throws IllegalStateException {
            if (binding == null) {
                throw new IllegalStateException("Binding cannot be null!");
            }

            if (destination == null) {
                throw new IllegalStateException("The service endpoint destination cannot be null");
            }

            if (binding == SamlProtocol.Binding.HTTP_POST) {
                return isRequest ? new PostRequest(this) : new PostResponse(this);
            }
            if (binding == SamlProtocol.Binding.HTTP_REDIRECT) {
                if (isBlank(signature) || isBlank(sigAlgo) || isBlank(samlString)) {
                    throw new UnsupportedOperationException("Cannot validate object with blank data");
                }
                return isRequest ? new RedirectRequest(this) : new RedirectResponse(this);
            }

            throw new UnsupportedOperationException("Binding not supported.");
        }

        public Builder setRedirectParams(String relayState, String signature, String sigAlgo,
                String samlString) {
            this.relayState = relayState;
            this.signature = signature;
            this.sigAlgo = sigAlgo;
            this.samlString = samlString;
            return this;
        }

        public Builder setInResponse(@NotNull String inResponse) {
            if (isBlank(inResponse)) {
                throw new IllegalArgumentException("InResponseTo Id cannot be blank!");
            }
            this.inResponse = inResponse;
            return this;
        }

        public Builder setDestination(URL destination) {
            this.destination = destination;
            return this;
        }

        public Builder setIssueTimeout(@NotNull Duration issueTimeout) {
            if (issueTimeout == null) {
                throw new IllegalArgumentException("Issue Timeout cannot be null!");
            }
            this.issueTimeout = issueTimeout;
            return this;
        }

        public Builder setJitter(@NotNull Duration jitter) {
            if (jitter == null) {
                throw new IllegalArgumentException("jitter cannot be null!");
            }
            this.jitter = jitter;
            return this;
        }

        public Builder setBinding(@NotNull SamlProtocol.Binding binding) {
            if (binding == null) {
                throw new IllegalArgumentException("binding cannot be null!");
            }
            this.binding = binding;
            return this;
        }

        /**
         * @param xmlObject target object to validate
         * @return this builder
         * @throws IllegalArgumentException If not passed a
         *                                  {@link org.opensaml.saml2.core.LogoutResponse} or a
         *                                  {@link org.opensaml.saml2.core.LogoutRequest}
         */
        public Builder setXmlObject(XMLObject xmlObject) throws IllegalArgumentException {
            this.xmlObject = xmlObject;

            if (this.xmlObject instanceof LogoutRequest) {
                isRequest = true;
                LOGGER.trace("xmlObject is a LogoutRequest [{}]", xmlObject);
            } else if (this.xmlObject instanceof LogoutResponse) {
                isRequest = false;
                LOGGER.trace("xmlObject is a LogoutResponse [{}]", xmlObject);
            } else {
                throw new IllegalArgumentException("Could not determine type of xmlObject");
            }
            return this;
        }
    }

    public static abstract class Request extends SamlValidator {
        protected final LogoutRequest logoutRequest;

        private Request(Builder builder) {
            super(builder);
            logoutRequest = (LogoutRequest) builder.xmlObject;
        }

        @Override
        protected void checkSamlVersion() throws ValidationException {
            checkSamlVersion(logoutRequest.getVersion());
        }

        @Override
        protected void checkTimestamp() throws ValidationException {
            if (logoutRequest.getIssueInstant() == null) {
                throw new ValidationException("Issue Instant cannot be null!");
            }

            checkTimestamp(Instant.ofEpochMilli(logoutRequest.getIssueInstant()
                    .getMillis()));
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
                checkDestination(builder.destination);
            }
        }
    }

    public static abstract class Response extends SamlValidator {
        protected final LogoutResponse logoutResponse;

        private Response(Builder builder) {
            super(builder);
            logoutResponse = (LogoutResponse) builder.xmlObject;
        }

        @Override
        protected void checkSamlVersion() throws ValidationException {
            checkSamlVersion(logoutResponse.getVersion());
        }

        @Override
        protected void checkTimestamp() throws ValidationException {
            if (logoutResponse.getIssueInstant() == null) {
                throw new ValidationException("Issue Instant cannot be null!");
            }

            checkTimestamp(Instant.ofEpochMilli(logoutResponse.getIssueInstant()
                    .getMillis()));
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
                checkDestination(builder.destination);
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
            checkRedirectSignature(0);
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
            checkRedirectSignature(0);
        }
    }
}
