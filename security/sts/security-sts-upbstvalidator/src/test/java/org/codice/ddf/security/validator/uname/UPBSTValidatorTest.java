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
package org.codice.ddf.security.validator.uname;

import junit.framework.Assert;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.JAASUsernameTokenValidator;
import org.codice.ddf.security.handler.api.BSTAuthenticationToken;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Collection;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class UPBSTValidatorTest {

    final JAASUsernameTokenValidator niceValidator = new JAASUsernameTokenValidator() {
        public Credential validate(Credential credential, RequestData data) {

            return null;
        }
    };
    final JAASUsernameTokenValidator meanValidator = new JAASUsernameTokenValidator();
    JAXBElement<BinarySecurityTokenType> upbstToken;

    STSPropertiesMBean stsPropertiesMBean;

    @Before
    public void setup() {
        niceValidator.setContextName("realm");
        meanValidator.setContextName("realm");
        stsPropertiesMBean = mock(STSPropertiesMBean.class);
        when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(new Merlin());
        when(stsPropertiesMBean.getCallbackHandler()).thenReturn(new CallbackHandler() {
            @Override
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {

            }
        });
        UPAuthenticationToken upAuthenticationToken = new UPAuthenticationToken("good", "password", "realm");
        BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
        binarySecurityTokenType.setValueType(UPAuthenticationToken.UP_TOKEN_VALUE_TYPE);
        binarySecurityTokenType.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
        binarySecurityTokenType.setId(UPAuthenticationToken.BST_USERNAME_LN);
        binarySecurityTokenType.setValue(upAuthenticationToken.getEncodedCredentials());
        upbstToken = new JAXBElement<>(
                new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "BinarySecurityToken"), BinarySecurityTokenType.class,
                binarySecurityTokenType
        );
    }

    @Test
    public void testValidateGoodTokenNoCache() {
        UPBSTValidator upbstValidator = new UPBSTValidator() {
            public void addRealm(ServiceReference<JaasRealm> serviceReference) {
                validators.put("realm", niceValidator);
            }
        };
        upbstValidator.addRealm(null);
        TokenValidatorParameters tokenParameters = new TokenValidatorParameters();
        ReceivedToken validateTarget = new ReceivedToken(upbstToken);
        tokenParameters.setToken(validateTarget);
        tokenParameters.setStsProperties(stsPropertiesMBean);
        TokenValidatorResponse response = upbstValidator.validateToken(tokenParameters);

        Assert.assertEquals(ReceivedToken.STATE.VALID, response.getToken().getState());
    }

    @Test
    public void testValidateGoodTokenCache() {
        UPBSTValidator upbstValidator = new UPBSTValidator() {
            public void addRealm(ServiceReference<JaasRealm> serviceReference) {
                validators.put("realm", meanValidator);
            }
        };
        upbstValidator.addRealm(null);
        TokenValidatorParameters tokenParameters = new TokenValidatorParameters();
        tokenParameters.setTokenStore(new TokenStore() {
            @Override
            public void add(SecurityToken token) {

            }

            @Override
            public void add(String identifier, SecurityToken token) {

            }

            @Override
            public void remove(String identifier) {

            }

            @Override
            public Collection<String> getTokenIdentifiers() {
                return null;
            }

            @Override
            public SecurityToken getToken(String identifier) {
                SecurityToken securityToken = new SecurityToken();
                securityToken.setTokenHash(-1432225335);
                return securityToken;
            }
        });
        ReceivedToken validateTarget = new ReceivedToken(upbstToken);
        tokenParameters.setToken(validateTarget);
        tokenParameters.setStsProperties(stsPropertiesMBean);
        TokenValidatorResponse response = upbstValidator.validateToken(tokenParameters);

        Assert.assertEquals(ReceivedToken.STATE.VALID, response.getToken().getState());
    }

    @Test
    public void testValidateBadTokenNoCache() {
        UPBSTValidator upbstValidator = new UPBSTValidator() {
            public void addRealm(ServiceReference<JaasRealm> serviceReference) {
                validators.put("realm", meanValidator);
            }
        };
        upbstValidator.addRealm(null);
        TokenValidatorParameters tokenParameters = new TokenValidatorParameters();
        ReceivedToken validateTarget = new ReceivedToken(upbstToken);
        tokenParameters.setToken(validateTarget);
        tokenParameters.setStsProperties(stsPropertiesMBean);
        TokenValidatorResponse response = upbstValidator.validateToken(tokenParameters);

        Assert.assertEquals(ReceivedToken.STATE.INVALID, response.getToken().getState());
    }

    @Test
    public void testValidateBadTokenCache() {
        UPBSTValidator upbstValidator = new UPBSTValidator() {
            public void addRealm(ServiceReference<JaasRealm> serviceReference) {
                validators.put("realm", meanValidator);
            }
        };
        upbstValidator.addRealm(null);
        TokenValidatorParameters tokenParameters = new TokenValidatorParameters();
        tokenParameters.setTokenStore(new TokenStore() {
            @Override
            public void add(SecurityToken token) {

            }

            @Override
            public void add(String identifier, SecurityToken token) {

            }

            @Override
            public void remove(String identifier) {

            }

            @Override
            public Collection<String> getTokenIdentifiers() {
                return null;
            }

            @Override
            public SecurityToken getToken(String identifier) {
                SecurityToken securityToken = new SecurityToken();
                securityToken.setTokenHash(-1432225336);
                return securityToken;
            }
        });
        ReceivedToken validateTarget = new ReceivedToken(upbstToken);
        tokenParameters.setToken(validateTarget);
        tokenParameters.setStsProperties(stsPropertiesMBean);
        TokenValidatorResponse response = upbstValidator.validateToken(tokenParameters);

        Assert.assertEquals(ReceivedToken.STATE.INVALID, response.getToken().getState());
    }
}
