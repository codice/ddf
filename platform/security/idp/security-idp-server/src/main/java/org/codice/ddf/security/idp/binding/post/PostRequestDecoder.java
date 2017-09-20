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
package org.codice.ddf.security.idp.binding.post;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.security.idp.binding.api.RequestDecoder;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.AuthnRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class PostRequestDecoder implements RequestDecoder {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostRequestDecoder.class);

  @Override
  public AuthnRequest decodeRequest(String samlRequest) {
    LOGGER.debug("Creating AuthnRequest object from SAMLRequest string.");
    if (StringUtils.isEmpty(samlRequest)) {
      throw new IllegalArgumentException("Missing SAMLRequest on IdP request.");
    }
    String decodedRequest =
        new String(Base64.getMimeDecoder().decode(samlRequest), StandardCharsets.UTF_8);
    ByteArrayInputStream tokenStream =
        new ByteArrayInputStream(decodedRequest.getBytes(StandardCharsets.UTF_8));
    Document authnDoc;
    try {
      authnDoc = StaxUtils.read(new InputStreamReader(tokenStream, "UTF-8"));
    } catch (Exception ex) {
      throw new IllegalArgumentException("Unable to read SAMLRequest as XML.");
    }
    XMLObject authnXmlObj;
    try {
      authnXmlObj = OpenSAMLUtil.fromDom(authnDoc.getDocumentElement());
    } catch (WSSecurityException ex) {
      throw new IllegalArgumentException("Unable to convert AuthnRequest document to XMLObject.");
    }
    if (!(authnXmlObj instanceof AuthnRequest)) {
      throw new IllegalArgumentException("SAMLRequest object is not AuthnRequest.");
    }
    LOGGER.debug("Created AuthnRequest object successfully.");
    return (AuthnRequest) authnXmlObj;
  }
}
