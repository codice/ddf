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
package ddf.security.pdp.realm.xacml.processor;

import com.connexta.arbitro.PDP;
import com.connexta.arbitro.PDPConfig;
import com.connexta.arbitro.finder.AttributeFinder;
import com.connexta.arbitro.finder.AttributeFinderModule;
import com.connexta.arbitro.finder.PolicyFinder;
import com.connexta.arbitro.finder.PolicyFinderModule;
import com.connexta.arbitro.finder.impl.CurrentEnvModule;
import com.connexta.arbitro.finder.impl.SelectorModule;
import com.google.common.collect.ImmutableList;
import ddf.security.audit.SecurityLogger;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RequestType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ResponseType;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.platform.util.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLFilterImpl;

/**
 * Implementation of a XACML Policy Decision Point (PDP). This class acts as a proxy to the real
 * XACML PDP.
 */
public class XacmlClient {
  private static final Logger LOGGER = LoggerFactory.getLogger(XacmlClient.class);

  private static final String XACML30_NAMESPACE = "urn:oasis:names:tc:xacml:3.0:core:schema:wd-17";

  private static final String XACML_PREFIX = "xacml";

  private static JAXBContext jaxbContext;

  private static final long DEFAULT_POLLING_INTERVAL_IN_SECONDS = 60;

  private static final String NULL_DIRECTORY_EXCEPTION_MSG =
      "Cannot read from null XACML Policy Directory";

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  static long defaultPollingIntervalInSeconds = 60;

  private PDP pdp;

  private Set<String> xacmlPolicyDirectories;

  private final Parser parser;

  private SecurityLogger securityLogger;

  /**
   * Creates the proxy to the real XACML PDP.
   *
   * @param relativeXacmlPoliciesDirectoryPath Relative directory path to the root of the DDF
   *     installation.
   * @param parser for marshal and unmarshal
   * @throws PdpException
   */
  public XacmlClient(
      String relativeXacmlPoliciesDirectoryPath, Parser parser, SecurityLogger securityLogger)
      throws PdpException {
    this.parser = parser;
    this.securityLogger = securityLogger;
    if (StringUtils.isEmpty(relativeXacmlPoliciesDirectoryPath)) {
      throw new PdpException(NULL_DIRECTORY_EXCEPTION_MSG);
    }

    File xacmlPoliciesDirectory;

    try {
      xacmlPoliciesDirectory = new File(relativeXacmlPoliciesDirectoryPath).getCanonicalFile();
    } catch (IOException e) {
      throw new PdpException(e.getMessage(), e);
    }
    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Object>)
              () -> {
                initialize(xacmlPoliciesDirectory);
                return null;
              });
    } catch (PrivilegedActionException e) {
      LOGGER.warn("Failed to initialize XACML PDP", e);
    }
  }

  private void initialize(File xacmlPoliciesDirectory) throws PdpException {
    try {
      // Only a single default directory is supported
      // If the directory path becomes customizable this
      // functionality should be re-evaluated
      FileUtils.forceMkdir(xacmlPoliciesDirectory);
    } catch (IOException e) {
      LOGGER.warn("Unable to create directory: {}", xacmlPoliciesDirectory.getAbsolutePath());
    }
    checkXacmlPoliciesDirectory(xacmlPoliciesDirectory);

    /**
     * We currently only support one XACML policies directory, but we may support multiple
     * directories in the future.
     */
    xacmlPolicyDirectories = new HashSet<>(1);
    xacmlPolicyDirectories.add(xacmlPoliciesDirectory.getPath());
    createPdp(createPdpConfig());
  }

  /**
   * Evaluates the XACML request and returns a XACML response.
   *
   * @param xacmlRequestType XACML request
   * @return XACML response
   * @throws PdpException
   */
  public ResponseType evaluate(RequestType xacmlRequestType) throws PdpException {
    String xacmlRequest = this.marshal(xacmlRequestType);

    String xacmlResponse = this.callPdp(xacmlRequest);

    LOGGER.debug("\nXACML 3.0 Response from XACML PDP:\n {}", xacmlResponse);

    DOMResult domResult = addNamespaceAndPrefixes(xacmlResponse);

    return unmarshal(domResult);
  }

  /** Creates the XACML PDP. */
  private void createPdp(PDPConfig pdpConfig) {
    LOGGER.debug("Creating PDP of type: {}", PDP.class.getName());
    pdp = new PDP(pdpConfig);
    LOGGER.debug("PDP creation successful.");
  }

  /**
   * Creates the XACML PDP configuration.
   *
   * @return PDPConfig
   */
  private PDPConfig createPdpConfig() {
    LOGGER.debug("Creating PDP Config.");
    AttributeFinder attributeFinder = new AttributeFinder();
    List<AttributeFinderModule> attributeFinderModules = new ArrayList<AttributeFinderModule>();
    SelectorModule selectorModule = new SelectorModule();
    CurrentEnvModule currentEnvModule = new CurrentEnvModule();
    attributeFinderModules.add(selectorModule);
    attributeFinderModules.add(currentEnvModule);
    attributeFinder.setModules(attributeFinderModules);
    return new PDPConfig(attributeFinder, createPolicyFinder(), null, false);
  }

  /**
   * Creates a policy finder to find XACML polices.
   *
   * @return PolicyFinder
   */
  private PolicyFinder createPolicyFinder() {
    LOGGER.debug(
        "XACML policies will be looked for in the following location(s): {}",
        xacmlPolicyDirectories);
    PolicyFinder policyFinder = new PolicyFinder();
    PollingPolicyFinderModule policyFinderModule =
        new PollingPolicyFinderModule(
            xacmlPolicyDirectories, defaultPollingIntervalInSeconds, securityLogger);
    policyFinderModule.start();
    Set<PolicyFinderModule> policyFinderModules = new HashSet<>(1);
    policyFinderModules.add(policyFinderModule);
    policyFinder.setModules(policyFinderModules);

    return policyFinder;
  }

  /**
   * Performs basic checks on the XACML policy directory.
   *
   * @param xacmlPoliciesDirectory The directory containing the XACML policy.
   * @throws PdpException
   */
  private void checkXacmlPoliciesDirectory(File xacmlPoliciesDirectory) throws PdpException {
    StringBuilder message = new StringBuilder();
    boolean errors = false;

    if (!xacmlPoliciesDirectory.isDirectory()) {
      message
          .append("The XACML policies directory ")
          .append(xacmlPoliciesDirectory.getPath())
          .append(" does not exist or is not a directory.  ");
      errors = true;
    }

    if (!xacmlPoliciesDirectory.canRead()) {
      message
          .append("The XACML policies directory ")
          .append(xacmlPoliciesDirectory.getPath())
          .append(" is not readable.  ");
      errors = true;
    }

    if (errors) {
      throw new PdpException(message.toString());
    }
  }

  /**
   * Calls the real XACML PDP to evaluate the XACML request.
   *
   * @param xacmlRequest The XACML request as a string.
   * @return The XACML response as a string.
   */
  private String callPdp(String xacmlRequest) {

    return pdp.evaluate(xacmlRequest);
  }

  /**
   * Adds namespaces and namespace prefixes to the XACML response returned by the XACML PDP. The
   * XACML PDP returns a response with no namespaces, so we need to add them to unmarshal the
   * response.
   *
   * @param xacmlResponse The XACML response as a string.
   * @return DOM representation of the XACML response with namespaces and namespace prefixes.
   * @throws PdpException
   */
  private DOMResult addNamespaceAndPrefixes(String xacmlResponse) throws PdpException {
    XMLReader xmlReader = null;

    try {
      XMLReader xmlParser = XML_UTILS.getSecureXmlParser();
      xmlReader =
          new XMLFilterImpl(xmlParser) {
            @Override
            public void startElement(
                String uri, String localName, String qName, Attributes attributes)
                throws SAXException {
              super.startElement(
                  XACML30_NAMESPACE, localName, XACML_PREFIX + ":" + qName, attributes);
            }

            @Override
            public void endElement(String uri, String localName, String qName) throws SAXException {
              super.endElement(XACML30_NAMESPACE, localName, XACML_PREFIX + ":" + qName);
            }
          };
    } catch (SAXException e) {
      String message = "Unable to read XACML response:\n" + xacmlResponse;
      LOGGER.info(message);
      throw new PdpException(message, e);
    }

    DOMResult domResult;
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(XacmlClient.class.getClassLoader());
    try {
      TransformerFactory transformerFactory = XML_UTILS.getSecureXmlTransformerFactory();

      domResult = new DOMResult();

      Transformer transformer = transformerFactory.newTransformer();
      transformer.transform(
          new SAXSource(xmlReader, new InputSource(new StringReader(xacmlResponse))), domResult);
    } catch (TransformerException e) {
      String message = "Unable to transform XACML response:\n" + xacmlResponse;
      LOGGER.info(message);
      throw new PdpException(message, e);
    } finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }

    return domResult;
  }

  /**
   * Marshalls the XACML request to a string.
   *
   * @param xacmlRequestType The XACML request to marshal.
   * @return A string representation of the XACML request.
   */
  private String marshal(RequestType xacmlRequestType) throws PdpException {
    if (null == parser) {
      throw new IllegalStateException("XMLParser must be configured.");
    }
    String xacmlRequest = null;
    try {
      List<String> ctxPath = new ArrayList<>(1);
      ctxPath.add(ResponseType.class.getPackage().getName());
      ParserConfigurator configurator =
          parser.configureParser(ctxPath, XacmlClient.class.getClassLoader());
      configurator.addProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      ObjectFactory objectFactory = new ObjectFactory();
      parser.marshal(configurator, objectFactory.createRequest(xacmlRequestType), os);
      xacmlRequest = os.toString("UTF-8");
    } catch (ParserException | UnsupportedEncodingException e) {
      String message = "Unable to marshal XACML request.";
      LOGGER.info(message, e);
      throw new PdpException(message, e);
    }

    LOGGER.debug("\nXACML 3.0 Request:\n{}", xacmlRequest);

    return xacmlRequest;
  }

  /**
   * Unmarshalls the XACML response.
   *
   * @param xacmlResponse The XACML response with all namespaces and namespace prefixes added.
   * @return The XACML response.
   * @throws PdpException
   */
  @SuppressWarnings("unchecked")
  private ResponseType unmarshal(DOMResult xacmlResponse) throws PdpException {
    List<String> ctxPath = ImmutableList.of(ResponseType.class.getPackage().getName());

    if (null == parser) {
      throw new IllegalStateException("XMLParser must be configured.");
    }

    ParserConfigurator configurator =
        parser.configureParser(ctxPath, XacmlClient.class.getClassLoader());

    try {
      JAXBElement<ResponseType> xacmlResponseTypeElement =
          parser.unmarshal(configurator, JAXBElement.class, xacmlResponse.getNode());
      return xacmlResponseTypeElement.getValue();

    } catch (ParserException e) {
      String message = "Unable to unmarshal XACML response.";
      LOGGER.info(message);
      throw new PdpException(message, e);
    }
  }
}
