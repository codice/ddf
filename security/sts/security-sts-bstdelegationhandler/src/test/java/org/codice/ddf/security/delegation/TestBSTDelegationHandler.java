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

import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.delegation.TokenDelegationParameters;
import org.apache.cxf.sts.token.delegation.TokenDelegationResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.wss4j.dom.WSConstants;
import org.codice.ddf.security.handler.api.BSTAuthenticationToken;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBSTDelegationHandler {
    @Test
    public void testCanHandle() {
        BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
        binarySecurityTokenType.setEncodingType(WSConstants.SOAPMESSAGE_NS + "#Base64Binary");
        binarySecurityTokenType.setValueType(BSTAuthenticationToken.BST_NS + "#" + BSTAuthenticationToken.BST_LN);
        ReceivedToken receivedToken = mock(ReceivedToken.class);
        when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
        BSTDelegationHandler bstDelegationHandler = new BSTDelegationHandler();
        boolean result = bstDelegationHandler.canHandleToken(receivedToken);
        assertEquals(true, result);
    }

    @Test
    public void testCanNotHandle() {
        BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
        binarySecurityTokenType.setEncodingType(WSConstants.SOAPMESSAGE_NS + "#WrongType");
        binarySecurityTokenType.setValueType(BSTAuthenticationToken.BST_NS + "#" + BSTAuthenticationToken.BST_LN);
        ReceivedToken receivedToken = mock(ReceivedToken.class);
        when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
        BSTDelegationHandler bstDelegationHandler = new BSTDelegationHandler();
        boolean result = bstDelegationHandler.canHandleToken(receivedToken);
        assertEquals(false, result);
    }

    @Test
    public void testDelegationAllowed() {
        BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
        binarySecurityTokenType.setEncodingType(WSConstants.SOAPMESSAGE_NS + "#Base64Binary");
        binarySecurityTokenType.setValueType(BSTAuthenticationToken.BST_NS + "#" + BSTAuthenticationToken.BST_LN);
        ReceivedToken receivedToken = mock(ReceivedToken.class);
        when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
        TokenDelegationParameters tokenDelegationParameters = mock(TokenDelegationParameters.class);
        when(tokenDelegationParameters.getToken()).thenReturn(receivedToken);
        BSTDelegationHandler bstDelegationHandler = new BSTDelegationHandler();
        TokenDelegationResponse response = bstDelegationHandler.isDelegationAllowed(tokenDelegationParameters);
        assertEquals(true, response.isDelegationAllowed());
    }

    @Test
    public void testDelegationNotAllowed() {
        UsernameTokenType binarySecurityTokenType = new UsernameTokenType();
        ReceivedToken receivedToken = mock(ReceivedToken.class);
        when(receivedToken.getToken()).thenReturn(binarySecurityTokenType);
        TokenDelegationParameters tokenDelegationParameters = mock(TokenDelegationParameters.class);
        when(tokenDelegationParameters.getToken()).thenReturn(receivedToken);
        BSTDelegationHandler bstDelegationHandler = new BSTDelegationHandler();
        TokenDelegationResponse response = bstDelegationHandler.isDelegationAllowed(tokenDelegationParameters);
        assertEquals(false, response.isDelegationAllowed());
    }
}
