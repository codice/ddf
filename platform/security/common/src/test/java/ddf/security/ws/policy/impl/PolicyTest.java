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
package ddf.security.ws.policy.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.cxf.helpers.DOMUtils;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/** Tests the policy adding logic (loading policies and converting the WSDLs). */
public class PolicyTest {

  private static final String POLICY_LOCATION = "/policies/ddf_sample_policy.xml";

  private static final String BAD_POLICY_LOCATION = "/policies/bad_policy.xml";

  private static final String TXT_POLICY_LOCATION = "/policies/notXmlPolicy.txt";

  private static final String WSDL_LOCATION = "/wsdl/w3c_example.wsdl";

  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyTest.class);

  private static BundleContext mockContext;

  private static Bundle mockBundle;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void setup() {
    mockContext = mock(BundleContext.class);
    mockBundle = mock(Bundle.class);
    when(mockBundle.getResource(anyString()))
        .thenAnswer(
            new Answer<URL>() {
              @Override
              public URL answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return this.getClass().getResource((String) args[0]);
              }
            });
    when(mockContext.getBundle()).thenReturn(mockBundle);
  }

  public static Document readXml(InputStream is)
      throws SAXException, IOException, ParserConfigurationException {
    DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

    dbf.setValidating(false);
    dbf.setIgnoringComments(false);
    dbf.setIgnoringElementContentWhitespace(true);
    dbf.setNamespaceAware(true);
    // dbf.setCoalescing(true);
    // dbf.setExpandEntityReferences(true);

    DocumentBuilder db = null;
    db = dbf.newDocumentBuilder();
    db.setEntityResolver(new DOMUtils.NullResolver());

    // db.setErrorHandler( new MyErrorHandler());

    return db.parse(is);
  }

  @Test
  public void filePolicyLoaderTest() {
    try {
      FilePolicyLoader loader = new FilePolicyLoader(mockContext, POLICY_LOCATION);
      assertNotNull(loader.getPolicy());
    } catch (Exception e) {
      LOGGER.error("Exception while loading policy: ", e);
      fail("Exception while loading policy: " + e.getMessage());
    }
  }

  @Test
  public void combinePolicyTest() {
    try {
      FilePolicyLoader policyLoader = new FilePolicyLoader(mockContext, POLICY_LOCATION);
      Document wsdlDoc = readXml(getClass().getResourceAsStream(WSDL_LOCATION));

      assertNotNull(wsdlDoc);
      assertNotNull(policyLoader.getPolicy());

      Document doc = PolicyWSDLGetInterceptor.addPolicyToWSDL(wsdlDoc, policyLoader.getPolicy());
      assertNotNull(doc);
      assertFalse(wsdlDoc.isEqualNode(policyLoader.getPolicy()));
      assertFalse(doc.isEqualNode(policyLoader.getPolicy()));

    } catch (Exception e) {
      LOGGER.error("Exception while combining policy: ", e);
      fail("Exception while combining policy " + e.getMessage());
    }
  }

  @Test(expected = IOException.class)
  public void badFileLocationTest() throws IOException {
    new FilePolicyLoader(mockContext, BAD_POLICY_LOCATION);
    fail("Should have thrown an exception when passed in a bad file location.");
  }

  @Test(expected = IOException.class)
  public void notXmlFile() throws IOException {
    new FilePolicyLoader(mockContext, TXT_POLICY_LOCATION);
    fail("Should have thrown an exception when passed in a non-xml file.");
  }
}
