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
package org.codice.ddf.security.handler.basic;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBElement;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.ws.security.sts.provider.model.ObjectFactory;
import org.apache.cxf.ws.security.sts.provider.model.secext.AttributedString;
import org.apache.cxf.ws.security.sts.provider.model.secext.PasswordString;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.wss4j.dom.WSConstants;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.security.handler.api.BaseAuthenticationToken;

public class WssBasicAuthenticationHandler extends AbstractBasicAuthenticationHandler {
  /** WS-Security compliant basic type to use when configuring context policy. */
  private static final String AUTH_TYPE = "WSSBASIC";

  private final Parser parser;

  public WssBasicAuthenticationHandler(Parser parser) {
    this.parser = parser;
  }

  protected BaseAuthenticationToken getBaseAuthenticationToken(String username, String password) {
    if (null == parser) {
      throw new IllegalStateException("XMLParser must be configured.");
    }

    UsernameTokenType usernameTokenType = new UsernameTokenType();
    AttributedString user = new AttributedString();
    user.setValue(username);
    usernameTokenType.setUsername(user);
    String usernameToken = null;

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
        parser.configureParser(ctxPath, WssBasicAuthenticationHandler.class.getClassLoader());
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    JAXBElement<UsernameTokenType> tokenType =
        new JAXBElement<>(
            QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameTokenType);

    try {
      parser.marshal(configurator, tokenType, os);
      usernameToken = os.toString("UTF-8");
    } catch (ParserException | UnsupportedEncodingException ex) {
      LOGGER.info("Unable to parse username token.", ex);
    }

    return new BaseAuthenticationToken(null, usernameToken);
  }

  @Override
  public String getAuthenticationType() {
    return AUTH_TYPE;
  }
}
