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
package org.codice.ddf.security.delegation;

import org.apache.cxf.sts.token.delegation.TokenDelegationHandler;
import org.apache.cxf.sts.token.delegation.TokenDelegationParameters;
import org.apache.cxf.sts.token.delegation.TokenDelegationResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.slf4j.LoggerFactory;

import org.codice.ddf.security.handler.api.BSTAuthenticationToken;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.ws.security.WSConstants;

/**
 * The SAML TokenDelegationHandler implementation. It disallows ActAs or OnBehalfOf for
 * all cases apart from the case of a Bearer SAML Token. In addition, the AppliesTo
 * address (if supplied) must match an AudienceRestriction address (if in token), if the
 * "checkAudienceRestriction" property is set to "true".
 */
public class BSTDelegationHandler implements TokenDelegationHandler {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(BSTDelegationHandler.class);
    public static final String X509_PKI_PATH = WSConstants.X509TOKEN_NS + "#X509PKIPathv1";
    public static final String X509_V3 = WSConstants.X509TOKEN_NS + "#X509v3";
    public static final String BASE64_ENCODING = WSConstants.SOAPMESSAGE_NS + "#Base64Binary";

    public static final String BST_VALUE_TYPE = BSTAuthenticationToken.BST_NS + "#" + BSTAuthenticationToken.BST_LN;

    //private boolean checkAudienceRestriction;

    public boolean canHandleToken(ReceivedToken delegateTarget) {
        Object token = delegateTarget.getToken();
        if (token instanceof BinarySecurityTokenType) {
            BinarySecurityTokenType bstt = (BinarySecurityTokenType) token;
            if (BST_VALUE_TYPE.equals(bstt.getValueType()) &&
                BASE64_ENCODING.equals(bstt.getEncodingType())) {
                return true;
            }
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

        return response;
    }
}

