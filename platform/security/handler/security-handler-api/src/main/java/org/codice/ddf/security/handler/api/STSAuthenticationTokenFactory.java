/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.handler.api;

import static org.apache.wss4j.common.WSS4JConstants.X509TOKEN_NS;

import ddf.security.PropertiesLoader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.xml.bind.JAXBElement;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.ws.security.sts.provider.model.ObjectFactory;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.token.BinarySecurity;
import org.apache.wss4j.common.token.X509Security;
import org.apache.wss4j.dom.WSConstants;
import org.apache.xml.security.Init;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.parser.xml.XmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class STSAuthenticationTokenFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(STSAuthenticationTokenFactory.class);

  public static final String BASE64_ENCODING = WSConstants.SOAPMESSAGE_NS + "#Base64Binary";

  public static final String TOKEN_VALUE_SEPARATOR = "#";

  public static final String PKI_TOKEN_ID = "X509PKIPathv1";

  private static final String PKI_TOKEN_VALUE = X509TOKEN_NS + TOKEN_VALUE_SEPARATOR + PKI_TOKEN_ID;

  private Parser parser = new XmlParser();

  private Merlin merlin;

  private String signaturePropertiesPath;

  /** Initializes Merlin crypto object. */
  public void init() {
    try {
      merlin =
          new Merlin(
              PropertiesLoader.loadProperties(signaturePropertiesPath),
              STSAuthenticationTokenFactory.class.getClassLoader(),
              null);
    } catch (WSSecurityException | IOException e) {
      LOGGER.warn("Unable to read merlin properties file. Unable to validate certificates.", e);
    }
    Init.init();
  }

  /**
   * Creates a {@link STSAuthenticationToken} from a given username and password. Uses a {@link
   * UsernameTokenType} internally to store the username and password.
   *
   * @param username - user's username
   * @param password - user's password
   * @return a BaseAuthenticationToken containing the given username and password
   */
  public STSAuthenticationToken fromUsernamePassword(String username, String password, String ip) {
    UsernameTokenType usernameTokenType = new UsernameTokenType();
    AttributedString user = new AttributedString();
    user.setValue(username);
    usernameTokenType.setUsername(user);

    // Add a password
    PasswordString pass = new PasswordString();
    pass.setValue(password);
    pass.setType(WSConstants.PASSWORD_TEXT);
    JAXBElement<PasswordString> passwordType =
        new JAXBElement<>(QNameConstants.PASSWORD, PasswordString.class, pass);
    usernameTokenType.getAny().add(passwordType);

    // Marshall the received JAXB object into a DOM Element
    List<String> ctxPath = new ArrayList<>(2);
    ctxPath.add(ObjectFactory.class.getPackage().getName());
    ctxPath.add(
        org.apache.cxf.ws.security.sts.provider.model.wstrust14.ObjectFactory.class
            .getPackage()
            .getName());

    ParserConfigurator configurator =
        parser.configureParser(ctxPath, BaseAuthenticationToken.class.getClassLoader());
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    JAXBElement<UsernameTokenType> tokenType =
        new JAXBElement<>(
            QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameTokenType);

    String usernameToken = null;
    try {
      parser.marshal(configurator, tokenType, os);
      usernameToken = os.toString("UTF-8");
    } catch (ParserException | UnsupportedEncodingException ex) {
      LOGGER.info("Unable to parse username token.", ex);
    }

    return new STSAuthenticationToken(
        null, new SimplePrincipalCollection(usernameToken, "IDP"), ip);
  }

  /**
   * Creates a {@link STSAuthenticationToken} from a given list of certificates. Uses a {@link
   * BinarySecurityTokenType} internally to store the certificates.
   *
   * @param certs - the user's certificates
   * @return a BaseAuthenticationToken containing the given certificates
   */
  public STSAuthenticationToken fromCertificates(X509Certificate[] certs, String ip) {
    if (certs == null || certs.length == 0) {
      return null;
    }

    byte[] certBytes = null;
    try {
      certBytes = getCertBytes(certs);
    } catch (WSSecurityException e) {
      LOGGER.debug("Unable to convert PKI certs to byte array.", e);
    }

    if (certBytes == null) {
      return null;
    }

    BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
    binarySecurityTokenType.setValueType(PKI_TOKEN_VALUE);
    binarySecurityTokenType.setEncodingType(BASE64_ENCODING);
    binarySecurityTokenType.setId(PKI_TOKEN_ID);
    binarySecurityTokenType.setValue(Base64.getEncoder().encodeToString(certBytes));

    // Turn the received JAXB object into a DOM element
    Document doc = DOMUtils.createDocument();
    BinarySecurity binarySecurity = new X509Security(doc);
    binarySecurity.setEncodingType(binarySecurityTokenType.getEncodingType());
    binarySecurity.setValueType(PKI_TOKEN_VALUE);
    String data = binarySecurityTokenType.getValue();
    Node textNode = doc.createTextNode(data);
    binarySecurity.getElement().appendChild(textNode);

    return new STSAuthenticationToken(certs[0].getSubjectDN(), binarySecurity.toString(), ip);
  }

  /**
   * Returns a byte array representing a certificate chain.
   *
   * @param certs
   * @return byte[]
   * @throws WSSecurityException
   */
  private byte[] getCertBytes(X509Certificate[] certs) throws WSSecurityException {
    byte[] certBytes = null;

    if (merlin != null) {
      certBytes = merlin.getBytesFromCertificates(certs);
    }
    return certBytes;
  }

  public void setSignaturePropertiesPath(String path) {
    this.signaturePropertiesPath = path;
  }
}
