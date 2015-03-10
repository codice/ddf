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

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.ws.security.WSConstants;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.util.Base64;
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

    public static final String BASE64_ENCODING = WSConstants.SOAPMESSAGE_NS + "#Base64Binary";

    public static final String BST_NS = "urn:org:codice:security:sso";

    public static final String BST_LN = "Token";

    public static final String TOKEN_VALUE_SEPARATOR = "#";

    protected static final String BST_PRINCIPAL = "Principal:";

    protected static final String BST_CREDENTIALS = "Credentials:";

    protected static final String BST_REALM = "Realm:";

    protected static final String NEWLINE = "\n";

    private static final Logger LOGGER = LoggerFactory.getLogger(BSTAuthenticationToken.class);

    // values to be included in the binary security token - specific to each subclass
    protected String tokenValueType = BST_NS + TOKEN_VALUE_SEPARATOR + BST_LN;

    protected String tokenId = BST_LN;

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

    /**
     * Creates an instance of BaseAuthenticationToken by parsing the given credential string. The
     * passed boolean indicates if the provided credentials are encoded or not.
     * If the string contains the necessary components (username, password, realm), a new instance of
     * BaseAuthenticationToken is created and initialized with the credentials. If not, a null value
     * is returned.
     *
     * @param stringBST unencoded credentials string
     * @return initialized username/password token if parsed successfully, null otherwise
     */
    public static BaseAuthenticationToken parse(String stringBST, boolean isEncoded)
            throws WSSecurityException {
        BaseAuthenticationToken baseAuthenticationToken = null;

        String unencodedCreds = isEncoded ? new String(Base64.decode(stringBST)) : stringBST;
        if (!StringUtils.isEmpty(unencodedCreds) && unencodedCreds.startsWith(BST_PRINCIPAL)) {
            String[] components = unencodedCreds.split(NEWLINE);
            if (components.length == 3) {
                String p = parseComponent(components[0], BST_PRINCIPAL);
                String c = parseComponent(components[1], BST_CREDENTIALS);
                String r = parseComponent(components[2], BST_REALM);

                baseAuthenticationToken = new BaseAuthenticationToken(p, r, c);
            }
        }

        if (baseAuthenticationToken == null) {
            throw new WSSecurityException(
                    "Exception decoding specified credentials. Unable to find required components.");
        }

        return baseAuthenticationToken;
    }

    protected static String parseComponent(String s, String expectedStartsWith) {
        String value = "";
        int minLength = expectedStartsWith == null ? 1 : expectedStartsWith.length() + 1;
        if ((s != null) && (s.length() > minLength)) {
            value = s.substring(minLength - 1);
        }
        return value;
    }

    @Override
    public String getCredentialsAsXMLString() {
        return getBinarySecurityToken();
    }

    public String getBinarySecurityToken() {
        return getBinarySecurityToken(getEncodedCredentials());
    }

    public String getEncodedCredentials() {
        StringBuilder builder = new StringBuilder();
        builder.append(BST_PRINCIPAL);
        builder.append(getPrincipal());
        builder.append(NEWLINE);
        builder.append(BST_CREDENTIALS);
        builder.append(getCredentials());
        builder.append(NEWLINE);
        builder.append(BST_REALM);
        builder.append(getRealm());
        String retVal = builder.toString();
        if (LOGGER.isTraceEnabled()) {
            String[] lines = retVal.split(NEWLINE);
            if (lines.length >= 3) {
                LOGGER.trace("Credentials String: {}\n{}\n{}", lines[0], BST_CREDENTIALS + "******",
                        lines[2]);
            }
        }
        LOGGER.trace("Credential String: {}", retVal);
        String encodedCreds = Base64.encode(builder.toString().getBytes());
        LOGGER.trace("BST: {}", encodedCreds);
        return encodedCreds;
    }

    /**
     * Creates a binary security token based on the provided credential.
     */
    private synchronized String getBinarySecurityToken(String credential) {
        Writer writer = new StringWriter();

        Marshaller marshaller = null;

        BinarySecurityTokenType binarySecurityTokenType = createBinarySecurityTokenType(credential);
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


    public BinarySecurityTokenType createBinarySecurityTokenType(String credentials) {
        BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
        binarySecurityTokenType.setValueType(tokenValueType);
        binarySecurityTokenType.setEncodingType(BASE64_ENCODING);
        binarySecurityTokenType.setId(tokenId);
        binarySecurityTokenType.setValue(credentials);
        return binarySecurityTokenType;
    }
    public void setTokenValueType(String ns, String ln) {
        this.tokenValueType = ns + TOKEN_VALUE_SEPARATOR + ln;
    }

    public void setTokenId(String tid) {
        this.tokenId = tid;
    }

}
