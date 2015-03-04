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
package org.codice.ddf.security.handler.api;

import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.ws.security.WSConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.StringWriter;
import java.io.Writer;

public abstract class BSTAuthenticationToken extends BaseAuthenticationToken {

    public static final String DDF_BST_NS = "urn:ddf:security:sso";

    public static final String DDF_BST_LN = "DDFToken";

    public static final String DDF_BST_SAML_LN = "DDFSAML";

    public static final String DDF_BST_X509_LN = "DDFX509";

    public static final String DDF_BST_USERNAME_LN = "DDFUsername";

    public static final String DDF_BST_ANONYMOUS_LN = "DDFAnonymous";

    private static final Logger LOGGER = LoggerFactory.getLogger(BSTAuthenticationToken.class);

    // values to be included in the binary security token - specific to each subclass
    protected String tokenValueType = DDF_BST_NS + '#' + DDF_BST_LN;

    protected String tokenId = DDF_BST_LN;

    private static final JAXBContext binaryTokenContext = initContext();

    private static JAXBContext initContext() {
        try {
            return JAXBContext.newInstance(BinarySecurityTokenType.class);
        } catch (JAXBException e) {
            LOGGER.error("Unable to create BinarySecurityToken JAXB context.", e);
        }
        return null;
    }

    public BSTAuthenticationToken(Object principal, Object credentials) {
        this(principal, credentials, DEFAULT_REALM);
    }

    public BSTAuthenticationToken(Object principal, Object credentials, String realm) {
        super(principal, realm, credentials);
    }

    @Override
    public String getCredentialsAsXMLString() {
        return getBinarySecurityToken();
    }

    public abstract String getEncodedCredentials();

    public String getBinarySecurityToken() {
        return getBinarySecurityToken(getEncodedCredentials());
    }

    /**
     * Creates a binary security token based on the provided credential.
     */
    private synchronized String getBinarySecurityToken(String credential) {
        Writer writer = new StringWriter();

        Marshaller marshaller = null;

        BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
        binarySecurityTokenType.setValueType(tokenValueType);
        binarySecurityTokenType.setEncodingType(WSConstants.SOAPMESSAGE_NS + "#Base64Binary");
        binarySecurityTokenType.setId(tokenId);
        binarySecurityTokenType.setValue(credential);
        JAXBElement<BinarySecurityTokenType> binarySecurityTokenElement = new JAXBElement<BinarySecurityTokenType>(
          new QName(
            "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
            "BinarySecurityToken"), BinarySecurityTokenType.class,
          binarySecurityTokenType
        );

        if (binaryTokenContext != null) {
            try {
                marshaller = binaryTokenContext.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            } catch (JAXBException e) {
                LOGGER.error("Exception while creating UsernameToken marshaller.", e);
            }

            if (marshaller != null) {
                try {
                    marshaller.marshal(binarySecurityTokenElement, writer);
                } catch (JAXBException e) {
                    LOGGER.error("Exception while writing username token.", e);
                }
            }
        }

        String binarySecurityToken = writer.toString();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Binary Security Token: " + binarySecurityToken);
        }

        return binarySecurityToken;
    }

    public void setTokenValueType(String ns, String ln) {
        this.tokenValueType = ns + '#' + ln;
    }

    public void setTokenId(String tid) {
        this.tokenId = tid;
    }
}
