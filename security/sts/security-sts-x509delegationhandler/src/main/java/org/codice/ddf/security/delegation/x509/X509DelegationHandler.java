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
package org.codice.ddf.security.delegation.x509;

import org.apache.cxf.common.logging.LogUtils;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.delegation.TokenDelegationHandler;
import org.apache.cxf.sts.token.delegation.TokenDelegationParameters;
import org.apache.cxf.sts.token.delegation.TokenDelegationResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.wss4j.dom.WSConstants;
import org.slf4j.LoggerFactory;

import java.util.logging.Logger;

/**
 * The SAML TokenDelegationHandler implementation. It disallows ActAs or OnBehalfOf for
 * all cases apart from the case of a Bearer SAML Token. In addition, the AppliesTo
 * address (if supplied) must match an AudienceRestriction address (if in token), if the
 * "checkAudienceRestriction" property is set to "true".
 */
public class X509DelegationHandler implements TokenDelegationHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory
      .getLogger(X509DelegationHandler.class);
    private static final Logger LOG =
        LogUtils.getL7dLogger(X509DelegationHandler.class);
    public static final String X509_PKI_PATH = WSConstants.X509TOKEN_NS + "#X509PKIPathv1";
    public static final String X509_V3 = WSConstants.X509TOKEN_NS + "#X509v3";

    public static final String BASE64_ENCODING = WSConstants.SOAPMESSAGE_NS + "#Base64Binary";

    private boolean checkAudienceRestriction;

    public boolean canHandleToken(ReceivedToken delegateTarget) {
        Object token = delegateTarget.getToken();
        if (token instanceof BinarySecurityTokenType) {
            return true;
/*
            // removed until we can find a way to make these pluggable - currently hard-coded in the
            // blueprint for the cxf server. Since the isDelegationAllowed only checks for an instance
            // BinarySecurityTokenType, then this is doing the job.
            BinarySecurityTokenType bstt = (BinarySecurityTokenType) token;
            if ((X509_PKI_PATH.equals(bstt.getValueType()) || X509_V3.equals(bstt.getValueType())) &&
                BASE64_ENCODING.equals(bstt.getEncodingType())) {
                return true;
            }
*/
        }
        return false;
    }

    public TokenDelegationResponse isDelegationAllowed(TokenDelegationParameters tokenParameters) {
        TokenDelegationResponse response = new TokenDelegationResponse();
        ReceivedToken delegateTarget = tokenParameters.getToken();
        response.setToken(delegateTarget);

        Object token = delegateTarget.getToken();
        if (token instanceof BinarySecurityTokenType) {
            response.setDelegationAllowed(true);
        }

/*
        if (isDelegationAllowed(delegateTarget, tokenParameters.getAppliesToAddress())) {
            response.setDelegationAllowed(true);
        }
*/

        return response;
    }

    /**
     * Is Delegation allowed for a particular token
     */
/*
    protected boolean isDelegationAllowed(
        ReceivedToken receivedToken, String appliesToAddress
    ) {
        Element validateTargetElement = (Element)receivedToken.getToken();
        try {
            AssertionWrapper assertion = new AssertionWrapper(validateTargetElement);

            for (String confirmationMethod : assertion.getConfirmationMethods()) {
                if (!(SAML1Constants.CONF_BEARER.equals(confirmationMethod)
                    || SAML2Constants.CONF_BEARER.equals(confirmationMethod))) {
                    LOG.fine("An unsupported Confirmation Method was used: " + confirmationMethod);
                    return false;
                }
            }

            if (checkAudienceRestriction && appliesToAddress != null) {
                List<String> addresses = getAudienceRestrictions(assertion);
                if (!(addresses.isEmpty() || addresses.contains(appliesToAddress))) {
                    LOG.fine("The AppliesTo address " + appliesToAddress + " is not contained"
                             + " in the Audience Restriction addresses in the assertion");
                    return false;
                }
            }
        } catch (WSSecurityException ex) {
            LOG.log(Level.WARNING, "Error in ascertaining whether delegation is allowed", ex);
            return false;
        }

        return true;
    }

    protected List<String> getAudienceRestrictions(AssertionWrapper assertion) {
        List<String> addresses = new ArrayList<String>();
        if (assertion.getSaml1() != null) {
            for (AudienceRestrictionCondition restriction
                : assertion.getSaml1().getConditions().getAudienceRestrictionConditions()) {
                for (org.opensaml.saml1.core.Audience audience : restriction.getAudiences()) {
                    addresses.add(audience.getUri());
                }
            }
        } else if (assertion.getSaml2() != null) {
            for (org.opensaml.saml2.core.AudienceRestriction restriction
                : assertion.getSaml2().getConditions().getAudienceRestrictions()) {
                for (org.opensaml.saml2.core.Audience audience : restriction.getAudiences()) {
                    addresses.add(audience.getAudienceURI());
                }
            }
        }

        return addresses;
    }

    public boolean isCheckAudienceRestriction() {
        return checkAudienceRestriction;
    }

    */
/**
     * Set whether to perform a check that the received AppliesTo address is contained in the
     * token as one of the AudienceRestriction URIs. The default is false.
     * @param checkAudienceRestriction whether to perform an audience restriction check or not
     *//*

    public void setCheckAudienceRestriction(boolean checkAudienceRestriction) {
        this.checkAudienceRestriction = checkAudienceRestriction;
    }
*/
}

