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
package org.codice.ddf.security.validator.anonymous;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.codice.ddf.security.handler.api.AnonymousAuthenticationToken;
import org.codice.ddf.security.handler.api.BSTAuthenticationToken;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.xml.util.Base64;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AnonymousValidatorTest {

    ReceivedToken receivedToken;

    ReceivedToken receivedBadToken;

    AnonymousValidator validator;

    TokenValidatorParameters parameters;

    @Before
    public void setup() {
        validator = new AnonymousValidator();
        validator.setSupportedRealm(Arrays.asList("DDF"));
        AnonymousAuthenticationToken anonymousAuthenticationToken = new AnonymousAuthenticationToken(
                "DDF");

        BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
        binarySecurityTokenType.setValueType(AnonymousAuthenticationToken.ANONYMOUS_TOKEN_VALUE_TYPE);
        binarySecurityTokenType.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
        binarySecurityTokenType.setId(AnonymousAuthenticationToken.BST_ANONYMOUS_LN);
        binarySecurityTokenType.setValue(anonymousAuthenticationToken.getEncodedCredentials());
        JAXBElement<BinarySecurityTokenType> binarySecurityTokenElement = new JAXBElement<BinarySecurityTokenType>(
                new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "BinarySecurityToken"), BinarySecurityTokenType.class,
                binarySecurityTokenType
        );

        BinarySecurityTokenType binarySecurityTokenType2 = new BinarySecurityTokenType();
        binarySecurityTokenType.setValueType(AnonymousAuthenticationToken.ANONYMOUS_TOKEN_VALUE_TYPE);
        binarySecurityTokenType2.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
        binarySecurityTokenType2.setId(AnonymousAuthenticationToken.BST_ANONYMOUS_LN);
        binarySecurityTokenType2.setValue(Base64.encodeBytes("NotAnonymous".getBytes(), Base64.DONT_BREAK_LINES));
        JAXBElement<BinarySecurityTokenType> binarySecurityTokenElement2 = new JAXBElement<BinarySecurityTokenType>(
                new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "BinarySecurityToken"), BinarySecurityTokenType.class,
                binarySecurityTokenType2
        );

        receivedToken = new ReceivedToken(binarySecurityTokenElement);
        receivedBadToken = new ReceivedToken(binarySecurityTokenElement2);
        parameters = new TokenValidatorParameters();
        parameters.setToken(receivedToken);
    }

    @Test
    public void testCanHandleToken() throws JAXBException {
        boolean canHandle = validator.canHandleToken(receivedToken);

        assertTrue(canHandle);
    }

    @Test
    public void testCanValidateToken() {
        TokenValidatorResponse response = validator.validateToken(parameters);

        assertEquals(ReceivedToken.STATE.VALID, response.getToken().getState());
    }

    @Test
    public void testCanValidateBadToken() {
        parameters.setToken(receivedBadToken);
        TokenValidatorResponse response = validator.validateToken(parameters);

        assertEquals(ReceivedToken.STATE.INVALID, response.getToken().getState());
    }
}
