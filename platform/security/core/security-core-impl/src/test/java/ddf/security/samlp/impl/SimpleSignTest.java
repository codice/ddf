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
package ddf.security.samlp.impl;

import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import ddf.security.samlp.SignatureException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Base64;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import javax.ws.rs.core.UriBuilder;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.jaxrs.impl.UriBuilderImpl;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.crypto.PasswordEncryptor;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.apache.wss4j.common.util.DOM2Writer;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.SubjectConfirmationData;
import org.opensaml.saml.saml2.core.impl.SubjectConfirmationDataBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class SimpleSignTest {

  private String cannedResponse;

  private PasswordEncryptor encryptionService;

  private SystemCrypto systemCrypto;

  private SimpleSign simpleSign;

  private static final String SAML_RESPONSE = "SAMLResponse";

  private static final String RELAY_STATE = "RelayState";

  private static final String SIG_ALG = "SigAlg";

  private static final String RELAY_STATE_VAL = "b0b4e449-7f69-413f-a844-61fe2256de19";

  private static final String SINGLE_SIGN_ON_LOCATION = "https://localhost:8993/services/idp/login";

  private String dsaCert;

  @BeforeClass
  public static void init() {
    OpenSAMLUtil.initSamlEngine();
    Security.addProvider(new BouncyCastleProvider());
  }

  @Before
  public void setUp() throws Exception {

    encryptionService = mock(PasswordEncryptor.class);
    systemCrypto =
        new SystemCrypto(
            "sign/encryption.properties", "sign/signature.properties", encryptionService);
    simpleSign = new SimpleSign(systemCrypto);

    cannedResponse =
        Resources.toString(Resources.getResource(getClass(), "/SAMLResponse.xml"), Charsets.UTF_8);

    // Normally you would have the cert in a string already but for this test we will have to pull
    // it out of the jks file
    Certificate cert =
        ((Merlin) systemCrypto.getSignatureCrypto()).getKeyStore().getCertificate("dsa");
    StringWriter writer = new StringWriter();
    PemWriter pemWriter = new PemWriter(writer);
    pemWriter.writeObject(new PemObject("CERTIFICATE", cert.getEncoded()));
    pemWriter.flush();
    dsaCert =
        writer
            .toString()
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "");
  }

  @Test
  public void testSignSamlObject() throws Exception {

    Document responseDoc = StaxUtils.read(new ByteArrayInputStream(cannedResponse.getBytes()));
    XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
    org.opensaml.saml.saml2.core.Response response =
        (org.opensaml.saml.saml2.core.Response) responseXmlObject;
    simpleSign.signSamlObject(response);

    Document doc = DOMUtils.createDocument();
    Element requestElement = OpenSAMLUtil.toDom(response, doc);
    String responseMessage = DOM2Writer.nodeToString(requestElement);
    responseDoc = StaxUtils.read(new ByteArrayInputStream(responseMessage.getBytes()));
    responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
    response = (org.opensaml.saml.saml2.core.Response) responseXmlObject;
    simpleSign.validateSignature(response.getSignature(), response.getDOM().getOwnerDocument());
  }

  @Test(expected = SignatureException.class)
  public void testSignSamlObjectThenModify() throws Exception {

    Document responseDoc = StaxUtils.read(new ByteArrayInputStream(cannedResponse.getBytes()));
    XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
    org.opensaml.saml.saml2.core.Response response =
        (org.opensaml.saml.saml2.core.Response) responseXmlObject;
    simpleSign.signSamlObject(response);

    Document doc = DOMUtils.createDocument();
    Element requestElement = OpenSAMLUtil.toDom(response, doc);
    requestElement.setAttribute("oops", "changedit");
    String responseMessage = DOM2Writer.nodeToString(requestElement);
    responseDoc = StaxUtils.read(new ByteArrayInputStream(responseMessage.getBytes()));
    responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
    response = (org.opensaml.saml.saml2.core.Response) responseXmlObject;
    simpleSign.validateSignature(response.getSignature(), response.getDOM().getOwnerDocument());
  }

  @Test
  public void testSignSamlObjectModifyAndResign() throws Exception {

    Document responseDoc = StaxUtils.read(new ByteArrayInputStream(cannedResponse.getBytes()));
    XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
    org.opensaml.saml.saml2.core.Response response =
        (org.opensaml.saml.saml2.core.Response) responseXmlObject;
    simpleSign.signSamlObject(response);

    final SubjectConfirmationData scd = new SubjectConfirmationDataBuilder().buildObject();
    scd.setNotOnOrAfter(DateTime.now().plusMinutes(30));
    for (Assertion assertion : response.getAssertions()) {
      assertion
          .getSubject()
          .getSubjectConfirmations()
          .forEach(sc -> sc.setSubjectConfirmationData(scd));
    }

    Document doc = DOMUtils.createDocument();
    Element requestElement = OpenSAMLUtil.toDom(response, doc);
    String responseMessage = DOM2Writer.nodeToString(requestElement);
    responseDoc = StaxUtils.read(new ByteArrayInputStream(responseMessage.getBytes()));
    responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
    response = (org.opensaml.saml.saml2.core.Response) responseXmlObject;
    simpleSign.validateSignature(response.getSignature(), response.getDOM().getOwnerDocument());
  }

  @Test
  public void testForceSign() throws Exception {
    Document responseDoc = StaxUtils.read(new ByteArrayInputStream(cannedResponse.getBytes()));
    XMLObject responseXmlObject = OpenSAMLUtil.fromDom(responseDoc.getDocumentElement());
    org.opensaml.saml.saml2.core.Response response =
        (org.opensaml.saml.saml2.core.Response) responseXmlObject;
    response = simpleSign.forceSignSamlObject(response);

    simpleSign.validateSignature(response.getSignature(), response.getDOM().getOwnerDocument());
  }

  @Test
  public void testSignUriStringWithDsa() throws Exception {

    systemCrypto =
        new SystemCrypto(
            "dsa-encryption.properties", "dsa-signature.properties", encryptionService);
    simpleSign = new SimpleSign(systemCrypto);
    String deflatedSamlResponse = deflateAndBase64Encode(cannedResponse);

    String queryParams =
        String.format(
            "SAMLResponse=%s&RelayState=%s",
            URLEncoder.encode(deflatedSamlResponse, "UTF-8"),
            URLEncoder.encode(RELAY_STATE_VAL, "UTF-8"));
    String idpRequest = SINGLE_SIGN_ON_LOCATION + "?" + queryParams;
    UriBuilder idpUri = new UriBuilderImpl(new URI(idpRequest));
    simpleSign.signUriString(queryParams, idpUri);

    String signatureAlgorithm = URLEncodedUtils.parse(idpUri.build(), "UTF-8").get(2).getValue();
    String signatureString = URLEncodedUtils.parse(idpUri.build(), "UTF-8").get(3).getValue();

    String signedMessage =
        String.format(
            "%s=%s&%s=%s&%s=%s",
            SAML_RESPONSE,
            URLEncoder.encode(deflatedSamlResponse, "UTF-8"),
            RELAY_STATE,
            URLEncoder.encode(RELAY_STATE_VAL, "UTF-8"),
            SIG_ALG,
            URLEncoder.encode(signatureAlgorithm, "UTF-8"));
    boolean valid =
        simpleSign.validateSignature(signatureAlgorithm, signedMessage, signatureString, dsaCert);
    assertTrue("Signature was expected to be valid", valid);
  }

  @Test(expected = SignatureException.class)
  public void testSignUriStringAndModifyWithDsa() throws Exception {

    systemCrypto =
        new SystemCrypto(
            "dsa-encryption.properties", "dsa-signature.properties", encryptionService);
    simpleSign = new SimpleSign(systemCrypto);

    String deflatedSamlResponse = deflateAndBase64Encode(cannedResponse);

    String queryParams =
        String.format(
            "SAMLResponse=%s&RelayState=%s",
            URLEncoder.encode(deflatedSamlResponse, "UTF-8"),
            URLEncoder.encode(RELAY_STATE_VAL, "UTF-8"));
    String idpRequest = SINGLE_SIGN_ON_LOCATION + "?" + queryParams;
    UriBuilder idpUri = new UriBuilderImpl(new URI(idpRequest));
    simpleSign.signUriString(queryParams, idpUri);
    idpUri.queryParam("RelayState", "changedit");

    String signatureAlgorithm = URLEncodedUtils.parse(idpUri.build(), "UTF-8").get(2).getValue();
    String signatureString = URLEncodedUtils.parse(idpUri.build(), "UTF-8").get(3).getValue();

    String signedMessage =
        String.format(
            "%s=%s&%s=%s&%s=%s",
            SAML_RESPONSE,
            URLEncoder.encode(deflatedSamlResponse, "UTF-8"),
            RELAY_STATE,
            URLEncoder.encode(RELAY_STATE_VAL, "UTF-8"),
            SIG_ALG,
            URLEncoder.encode(signatureAlgorithm, "UTF-8"));
    simpleSign.validateSignature(signatureAlgorithm, signedMessage, signatureString, dsaCert);
  }

  /**
   * Deflates a value and Base64 encodes the result. This code is copied from RestSecurity because
   * it would cause a circular dependency to use it directly..
   *
   * @param value value to deflate and Base64 encode
   * @return String
   * @throws IOException if the value cannot be converted
   */
  public static String deflateAndBase64Encode(String value) throws IOException {
    ByteArrayOutputStream valueBytes = new ByteArrayOutputStream();
    try (OutputStream tokenStream =
        new DeflaterOutputStream(valueBytes, new Deflater(Deflater.DEFLATED, true))) {
      tokenStream.write(value.getBytes(StandardCharsets.UTF_8));
      tokenStream.close();

      return Base64.getEncoder().encodeToString(valueBytes.toByteArray());
    }
  }
}
