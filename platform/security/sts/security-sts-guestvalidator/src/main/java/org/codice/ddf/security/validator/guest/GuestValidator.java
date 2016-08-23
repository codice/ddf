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
package org.codice.ddf.security.validator.guest;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.principal.GuestPrincipal;

public class GuestValidator implements TokenValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GuestValidator.class);

    private List<String> supportedRealm;

    private GuestAuthenticationToken getGuestTokenFromTarget(ReceivedToken validateTarget) {
        Object token = validateTarget.getToken();
        if ((token instanceof BinarySecurityTokenType)
                && GuestAuthenticationToken.GUEST_TOKEN_VALUE_TYPE.equals(((BinarySecurityTokenType) token).getValueType())) {
            String credential = ((BinarySecurityTokenType) token).getValue();
            try {
                BaseAuthenticationToken base = GuestAuthenticationToken.parse(credential, true);
                return new GuestAuthenticationToken(base.getRealm(),
                        GuestPrincipal.parseAddressFromName(base.getPrincipal()
                                .toString()));
            } catch (WSSecurityException e) {
                LOGGER.warn("Unable to parse {} from encodedToken.",
                        GuestAuthenticationToken.class.getSimpleName(),
                        e);
                return null;
            }
        }
        return null;
    }

    private boolean validIpAddress(String address) {
        try {
            return InetAddress.getByName(address) != null;
        } catch (UnknownHostException e) {
            return false;
        }
    }

    @Override
    public boolean canHandleToken(ReceivedToken validateTarget) {
        return canHandleToken(validateTarget, null);
    }

    @Override
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        GuestAuthenticationToken guestToken = getGuestTokenFromTarget(validateTarget);
        // currently realm is not being passed through (no RealmParser that determines the realm
        // based on the web context. So this just looks at the realm passed in the credentials.
        // This generic instance just looks for the default realms (DDF and Karaf)
        if (guestToken != null) {
            if (guestToken.getRealm() == null) {
                LOGGER.trace("No realm specified in request, canHandletoken = true");
                return true;
            } else {
                if (supportedRealm.contains(guestToken.getRealm())
                        || "*".equals(guestToken.getRealm())) {
                    LOGGER.trace("Realm '{}' recognized - canHandleToken = true",
                            guestToken.getRealm());
                    return true;
                } else {
                    LOGGER.trace("Realm '{}' unrecognized - canHandleToken = false",
                            guestToken.getRealm());
                }
            }
        }
        return false;
    }

    @Override
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(ReceivedToken.STATE.INVALID);

        GuestAuthenticationToken guestToken = getGuestTokenFromTarget(validateTarget);

        response.setToken(validateTarget);

        if (guestToken != null) {
            response.setPrincipal(new GuestPrincipal(guestToken.getIpAddress()));

            if (guestToken.getRealm() != null) {
                if ((supportedRealm.contains(guestToken.getRealm())
                        || "*".equals(guestToken.getRealm())) && guestToken.getCredentials()
                        .equals(GuestAuthenticationToken.GUEST_CREDENTIALS) && validIpAddress(
                        guestToken.getIpAddress())) {
                    validateTarget.setState(ReceivedToken.STATE.VALID);
                    validateTarget.setPrincipal(new GuestPrincipal(guestToken.getIpAddress()));
                }
            } else if (guestToken.getCredentials()
                    .equals(GuestAuthenticationToken.GUEST_CREDENTIALS)
                    && validIpAddress(guestToken.getIpAddress())) {
                validateTarget.setState(ReceivedToken.STATE.VALID);
                validateTarget.setPrincipal(new GuestPrincipal(guestToken.getIpAddress()));
            }
        }
        return response;
    }

    /**
     * Set the realm that this validator supports. This can be used to differentiate between
     * two instances of this validator where each contains a differnent token validator.
     *
     * @param supportedRealm string representing the realm supported by this validator
     */
    public void setSupportedRealm(List<String> supportedRealm) {
        this.supportedRealm = supportedRealm;
    }
}
