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
package ddf.security.assertion.impl;

import ddf.security.SecurityConstants;
import ddf.security.assertion.SecurityAssertion;

import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.saml.ext.AssertionWrapper;
import org.opensaml.saml2.core.Attribute;
import org.opensaml.saml2.core.AttributeStatement;
import org.opensaml.saml2.core.AuthnStatement;
import org.opensaml.saml2.core.AuthzDecisionStatement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the SecurityAssertion interface. This class wraps a SecurityToken.
 * 
 * @author tustisos
 * 
 */
public class SecurityAssertionImpl implements SecurityAssertion {
    /**
     * Log4j Logger
     */
    private Logger LOGGER = LoggerFactory.getLogger(SecurityConstants.SECURITY_LOGGER);

    /**
     * Serial Version UID
     */
    private static final long serialVersionUID = 1L;

    /**
     * Default Hash Value
     */
    private static final int DEFAULT_HASH = 127;

    /**
     * Wrapped SecurityToken.
     */
    private SecurityToken securityToken;

    /**
     * Wrapper class that pulls apart SecurityToken.
     */
    private AssertionWrapper assertionWrapper;

    /**
     * Principal associated with the security token
     */
    private Principal principal;

    /**
     * Uninitialized Constructor
     */
    public SecurityAssertionImpl() {

    }

    /**
     * Default Constructor
     * 
     * @param securityToken
     *            - token to wrap
     */
    public SecurityAssertionImpl(SecurityToken securityToken) {
        this.securityToken = securityToken;
        parseToken(securityToken);
    }

    /**
     * Parses the SecurityToken by wrapping within an AssertionWrapper.
     * 
     * @param securityToken
     *            SecurityToken
     */
    private void parseToken(SecurityToken securityToken) {
        try {
            assertionWrapper = new AssertionWrapper(securityToken.getToken());
        } catch (WSSecurityException e) {
            LOGGER.error("Unable to parse security token.", e);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.security.assertion.SecurityAssertion#getPrincipal()
     */
    @Override
    public Principal getPrincipal() {
        if (securityToken != null) {
            if (principal == null) {
                principal = new AssertionPrincipal();
            }
            return principal;
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.security.assertion.SecurityAssertion#getIssuer()
     */
    @Override
    public String getIssuer() {
        if (assertionWrapper != null) {
            return assertionWrapper.getIssuerString();
        }
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.security.assertion.SecurityAssertion#getAttibuteStatements()
     */
    @Override
    public List<AttributeStatement> getAttibuteStatements() {
        if (assertionWrapper != null && assertionWrapper.getSaml2() != null) {
            return assertionWrapper.getSaml2().getAttributeStatements();
        }
        return new ArrayList<AttributeStatement>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.security.assertion.SecurityAssertion#getAuthnStatements()
     */
    @Override
    public List<AuthnStatement> getAuthnStatements() {
        if (assertionWrapper != null && assertionWrapper.getSaml2() != null) {
            return assertionWrapper.getSaml2().getAuthnStatements();
        }
        return new ArrayList<AuthnStatement>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.security.assertion.SecurityAssertion#getAuthzDecisionStatements ()
     */
    @Override
    public List<AuthzDecisionStatement> getAuthzDecisionStatements() {
        if (assertionWrapper != null && assertionWrapper.getSaml2() != null) {
            return assertionWrapper.getSaml2().getAuthzDecisionStatements();
        }
        return new ArrayList<AuthzDecisionStatement>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.security.assertion.SecurityAssertion#getSecurityToken()
     */
    @Override
    public SecurityToken getSecurityToken() {
        return securityToken;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Principal: " + getPrincipal() + ", Attributes: ");
        for (AttributeStatement attributeStatement : getAttibuteStatements()) {
            for (Attribute attr : attributeStatement.getAttributes()) {
                result.append("[ ");
                result.append(attr.getName());
                result.append(" : ");
                for (int i = 0; i < attr.getAttributeValues().size(); i++) {
                    result.append(attr.getAttributeValues().get(i).getDOM().getTextContent());
                }
                result.append("] ");
            }
        }
        result.append(", AuthnStatements: ");
        for (AuthnStatement authStatement : getAuthnStatements()) {
            result.append("[ ");
            result.append(authStatement.getAuthnContext() + " : ");
            result.append(authStatement.getAuthnInstant() + " : ");
            result.append(authStatement.getDOM().getTextContent());
            result.append("] ");
        }
        result.append(", AuthzDecisionStatements: ");
        for (AuthzDecisionStatement authDecision : getAuthzDecisionStatements()) {
            result.append("[ ");
            result.append(authDecision.getDecision().toString());
            result.append(" ]");
        }
        return result.toString();
    }

    /**
     * Principal implementation that returns values obtained from the assertion.
     * 
     */
    private class AssertionPrincipal implements Principal {
        @Override
        public String getName() {
            return assertionWrapper.getSaml2().getSubject().getNameID().getValue();
        }

        @Override
        public boolean equals(Object another) {
            if (!(another instanceof Principal)) {
                return false;
            }
            Principal tmpPrin = (Principal) another;
            if (tmpPrin.getName() == null && getName() != null) {
                return false;
            }
            if (tmpPrin.getName() != null && getName() == null) {
                return false;
            }
            if (tmpPrin.getName() == null && getName() == null) {
                return super.equals(another);
            }
            return tmpPrin.getName().equals(getName());
        }

        @Override
        public int hashCode() {
            if (getName() == null) {
                return DEFAULT_HASH;
            }
            return getName().hashCode();
        }

        /**
         * Returns the name of the principal in string format.
         */
        public String toString() {
            return getName();
        }
    }
}
