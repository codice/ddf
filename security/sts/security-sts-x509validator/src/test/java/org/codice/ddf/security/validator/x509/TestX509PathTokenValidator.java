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
package org.codice.ddf.security.validator.x509;

import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.Validator;
import org.junit.Test;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestX509PathTokenValidator {
    @Test
    public void testValidateGoodToken() {
        X509PathTokenValidator x509PathTokenValidator = new X509PathTokenValidator();
        x509PathTokenValidator.merlin = mock(Merlin.class);
        try {
            X509Certificate[] x509Certificates = new X509Certificate[] {mock(X509Certificate.class)};
            when(x509PathTokenValidator.merlin.getCertificatesFromBytes(any(byte[].class))).thenReturn(x509Certificates);
        } catch (WSSecurityException e) {
            //ignore
        }
        Validator validator = mock(Validator.class);
        try {
            Credential credential = mock(Credential.class);
            X509Certificate x509Certificate = mock(X509Certificate.class);
            X500Principal x500Principal = new X500Principal("cn=myxman,ou=someunit,o=someorg");
            when(x509Certificate.getSubjectX500Principal()).thenReturn(x500Principal);
            X509Certificate[] x509Certificates = new X509Certificate[] {x509Certificate};
            when(credential.getCertificates()).thenReturn(x509Certificates);
            when(validator.validate(any(Credential.class), any(RequestData.class))).thenReturn(credential);
        } catch (WSSecurityException e) {
            //ignore
        }
        x509PathTokenValidator.setValidator(validator);

        TokenValidatorParameters tokenParameters = mock(TokenValidatorParameters.class);
        STSPropertiesMBean stsPropertiesMBean = mock(STSPropertiesMBean.class);
        when(tokenParameters.getStsProperties()).thenReturn(stsPropertiesMBean);
        Crypto crypto = mock(Crypto.class);
        when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(crypto);
        ReceivedToken receivedToken = mock(ReceivedToken.class);
        doCallRealMethod().when(receivedToken).setState(any(ReceivedToken.STATE.class));
        doCallRealMethod().when(receivedToken).getState();
        when(tokenParameters.getToken()).thenReturn(receivedToken);
        when(receivedToken.isBinarySecurityToken()).thenReturn(true);
        BinarySecurityTokenType binarySecurityTokenType = mock(BinarySecurityTokenType.class);
        when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
        when(binarySecurityTokenType.getEncodingType()).thenReturn(X509PathTokenValidator.BASE64_ENCODING);
        when(binarySecurityTokenType.getValueType()).thenReturn("valuetype");
        when(binarySecurityTokenType.getValue()).thenReturn("data");

        TokenValidatorResponse tokenValidatorResponse = x509PathTokenValidator.validateToken(tokenParameters);

        assertEquals(ReceivedToken.STATE.VALID, tokenValidatorResponse.getToken().getState());
    }

    @Test
    public void testValidateBadToken() {
        X509PathTokenValidator x509PathTokenValidator = new X509PathTokenValidator();
        x509PathTokenValidator.merlin = mock(Merlin.class);
        try {
            X509Certificate[] x509Certificates = new X509Certificate[] {mock(X509Certificate.class)};
            when(x509PathTokenValidator.merlin.getCertificatesFromBytes(any(byte[].class))).thenReturn(x509Certificates);
        } catch (WSSecurityException e) {
            //ignore
        }
        Validator validator = mock(Validator.class);
        try {
            Credential credential = mock(Credential.class);
            X509Certificate x509Certificate = mock(X509Certificate.class);
            X500Principal x500Principal = new X500Principal("cn=myxman,ou=someunit,o=someorg");
            when(x509Certificate.getSubjectX500Principal()).thenReturn(x500Principal);
            X509Certificate[] x509Certificates = new X509Certificate[] {x509Certificate};
            when(credential.getCertificates()).thenReturn(x509Certificates);
            when(validator.validate(any(Credential.class), any(RequestData.class))).thenThrow(new WSSecurityException(WSSecurityException.ErrorCode.SECURITY_ERROR));
        } catch (WSSecurityException e) {
            //ignore
        }
        x509PathTokenValidator.setValidator(validator);

        TokenValidatorParameters tokenParameters = mock(TokenValidatorParameters.class);
        STSPropertiesMBean stsPropertiesMBean = mock(STSPropertiesMBean.class);
        when(tokenParameters.getStsProperties()).thenReturn(stsPropertiesMBean);
        Crypto crypto = mock(Crypto.class);
        when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(crypto);
        ReceivedToken receivedToken = mock(ReceivedToken.class);
        doCallRealMethod().when(receivedToken).setState(any(ReceivedToken.STATE.class));
        doCallRealMethod().when(receivedToken).getState();
        when(tokenParameters.getToken()).thenReturn(receivedToken);
        when(receivedToken.isBinarySecurityToken()).thenReturn(true);
        BinarySecurityTokenType binarySecurityTokenType = mock(BinarySecurityTokenType.class);
        when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
        when(binarySecurityTokenType.getEncodingType()).thenReturn(X509PathTokenValidator.BASE64_ENCODING);
        when(binarySecurityTokenType.getValueType()).thenReturn("valuetype");
        when(binarySecurityTokenType.getValue()).thenReturn("data");

        TokenValidatorResponse tokenValidatorResponse = x509PathTokenValidator.validateToken(tokenParameters);

        assertEquals(ReceivedToken.STATE.INVALID, tokenValidatorResponse.getToken().getState());
    }
}
