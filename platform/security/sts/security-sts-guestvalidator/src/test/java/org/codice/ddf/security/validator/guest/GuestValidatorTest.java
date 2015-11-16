/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.validator.guest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.codice.ddf.security.handler.api.BSTAuthenticationToken;
import org.codice.ddf.security.handler.api.GuestAuthenticationToken;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.xml.util.Base64;

import ddf.security.principal.GuestPrincipal;

public class GuestValidatorTest {

    ReceivedToken receivedToken;

    ReceivedToken receivedBadToken;

    ReceivedToken receivedTokenIpv6;

    ReceivedToken receivedTokenBadIp;

    GuestValidator validator;

    TokenValidatorParameters parameters;

    ReceivedToken receivedAnyRealmToken;

    @Before
    public void setup() {
        validator = new GuestValidator();
        validator.setSupportedRealm(Arrays.asList("DDF"));
        GuestAuthenticationToken guestAuthenticationToken = new GuestAuthenticationToken(
                "DDF", "127.0.0.1");

        GuestAuthenticationToken guestAuthenticationTokenAnyRealm = new GuestAuthenticationToken(
                "*", "127.0.0.1");

        GuestAuthenticationToken guestAuthenticationTokenIpv6 = new GuestAuthenticationToken(
                "*", "0:0:0:0:0:0:0:1");

        GuestAuthenticationToken guestAuthenticationTokenBadIp = new GuestAuthenticationToken(
                "*", "123.abc.45.def");

        BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
        binarySecurityTokenType
                .setValueType(GuestAuthenticationToken.GUEST_TOKEN_VALUE_TYPE);
        binarySecurityTokenType.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
        binarySecurityTokenType.setId(GuestAuthenticationToken.BST_GUEST_LN);
        binarySecurityTokenType.setValue(guestAuthenticationToken.getEncodedCredentials());
        JAXBElement<BinarySecurityTokenType> binarySecurityTokenElement = new JAXBElement<BinarySecurityTokenType>(
                new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "BinarySecurityToken"), BinarySecurityTokenType.class,
                binarySecurityTokenType);

        BinarySecurityTokenType binarySecurityTokenType2 = new BinarySecurityTokenType();
        binarySecurityTokenType2
                .setValueType(GuestAuthenticationToken.GUEST_TOKEN_VALUE_TYPE);
        binarySecurityTokenType2.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
        binarySecurityTokenType2.setId(GuestAuthenticationToken.BST_GUEST_LN);
        binarySecurityTokenType2
                .setValue(Base64.encodeBytes("NotGuest".getBytes(), Base64.DONT_BREAK_LINES));
        JAXBElement<BinarySecurityTokenType> binarySecurityTokenElement2 = new JAXBElement<BinarySecurityTokenType>(
                new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "BinarySecurityToken"), BinarySecurityTokenType.class,
                binarySecurityTokenType2);

        BinarySecurityTokenType binarySecurityTokenType3 = new BinarySecurityTokenType();
        binarySecurityTokenType3
                .setValueType(GuestAuthenticationToken.GUEST_TOKEN_VALUE_TYPE);
        binarySecurityTokenType3.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
        binarySecurityTokenType3.setId(GuestAuthenticationToken.BST_GUEST_LN);
        binarySecurityTokenType3
                .setValue(guestAuthenticationTokenAnyRealm.getEncodedCredentials());
        JAXBElement<BinarySecurityTokenType> binarySecurityTokenElement3 = new JAXBElement<BinarySecurityTokenType>(
                new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "BinarySecurityToken"), BinarySecurityTokenType.class,
                binarySecurityTokenType3);

        BinarySecurityTokenType binarySecurityTokenType4 = new BinarySecurityTokenType();
        binarySecurityTokenType4
                .setValueType(GuestAuthenticationToken.GUEST_TOKEN_VALUE_TYPE);
        binarySecurityTokenType4.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
        binarySecurityTokenType4.setId(GuestAuthenticationToken.BST_GUEST_LN);
        binarySecurityTokenType4
                .setValue(guestAuthenticationTokenIpv6.getEncodedCredentials());
        JAXBElement<BinarySecurityTokenType> binarySecurityTokenElement4 = new JAXBElement<BinarySecurityTokenType>(
                new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "BinarySecurityToken"), BinarySecurityTokenType.class,
                binarySecurityTokenType4);

        BinarySecurityTokenType binarySecurityTokenType5 = new BinarySecurityTokenType();
        binarySecurityTokenType5
                .setValueType(GuestAuthenticationToken.GUEST_TOKEN_VALUE_TYPE);
        binarySecurityTokenType5.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
        binarySecurityTokenType5.setId(GuestAuthenticationToken.BST_GUEST_LN);
        binarySecurityTokenType5
                .setValue(guestAuthenticationTokenBadIp.getEncodedCredentials());
        JAXBElement<BinarySecurityTokenType> binarySecurityTokenElement5 = new JAXBElement<BinarySecurityTokenType>(
                new QName(
                        "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                        "BinarySecurityToken"), BinarySecurityTokenType.class,
                binarySecurityTokenType5);

        receivedToken = new ReceivedToken(binarySecurityTokenElement);
        receivedAnyRealmToken = new ReceivedToken(binarySecurityTokenElement3);
        receivedBadToken = new ReceivedToken(binarySecurityTokenElement2);
        receivedTokenIpv6 = new ReceivedToken(binarySecurityTokenElement4);
        receivedTokenBadIp = new ReceivedToken(binarySecurityTokenElement5);
        parameters = new TokenValidatorParameters();
        parameters.setToken(receivedToken);
    }

    @Test
    public void testCanHandleToken() throws JAXBException {
        boolean canHandle = validator.canHandleToken(receivedToken);

        assertTrue(canHandle);
    }

    @Test
    public void testCanHandleAnyRealmToken() throws JAXBException {
        boolean canHandle = validator.canHandleToken(receivedAnyRealmToken);

        assertTrue(canHandle);
    }

    @Test
    public void testCanValidateToken() {
        TokenValidatorResponse response = validator.validateToken(parameters);

        assertEquals(ReceivedToken.STATE.VALID, response.getToken().getState());

        assertThat(response.getToken().getPrincipal(), instanceOf(GuestPrincipal.class));
    }

    @Test
    public void testCanValidateAnyRealmToken() {
        TokenValidatorParameters params = new TokenValidatorParameters();
        params.setToken(receivedAnyRealmToken);
        TokenValidatorResponse response = validator.validateToken(params);

        assertEquals(ReceivedToken.STATE.VALID, response.getToken().getState());
    }

    @Test
    public void testCanValidateIpv6Token() {
        TokenValidatorParameters params = new TokenValidatorParameters();
        params.setToken(receivedTokenIpv6);
        TokenValidatorResponse response = validator.validateToken(params);

        assertEquals(ReceivedToken.STATE.VALID, response.getToken().getState());
    }

    @Test
    public void testCanValidateBadIpToken() {
        TokenValidatorParameters params = new TokenValidatorParameters();
        params.setToken(receivedTokenBadIp);
        TokenValidatorResponse response = validator.validateToken(params);

        assertEquals(ReceivedToken.STATE.INVALID, response.getToken().getState());
    }

    @Test
    public void testCanValidateBadToken() {
        parameters.setToken(receivedBadToken);
        TokenValidatorResponse response = validator.validateToken(parameters);

        assertEquals(ReceivedToken.STATE.INVALID, response.getToken().getState());
    }
}
