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

import ddf.security.sts.client.configuration.StsAddressProvider;
import ddf.security.ws.policy.PolicyLoader;
import java.util.Map;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.endpoint.Endpoint;
import org.apache.cxf.frontend.WSDLGetInterceptor;
import org.apache.cxf.frontend.WSDLGetOutInterceptor;
import org.apache.cxf.frontend.WSDLGetUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.OutgoingChainInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.service.model.EndpointInfo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Extension of the WSDLGetInterceptor that adds in the policy to the WSDL for clients to see. */
public class PolicyWSDLGetInterceptor extends WSDLGetInterceptor {

  private static final String XML_ENC = "utf-8";

  /*This is used to match a substring in an xpath but also used as a regex, so if changed
  take care not to put characters that could make the regex match more than it should*/
  private static final String DEFAULT_ADDRESS = "https://localhost:8993";

  private static final Logger LOGGER = LoggerFactory.getLogger(PolicyWSDLGetInterceptor.class);

  private StsAddressProvider addressProvider;

  private PolicyLoader loader;

  public PolicyWSDLGetInterceptor(PolicyLoader loader) {
    super();
    getBefore().add(WSDLGetInterceptor.class.getName());
    this.loader = loader;
    BundleContext bundleCtx =
        FrameworkUtil.getBundle(PolicyWSDLGetInterceptor.class).getBundleContext();
    addressProvider = bundleCtx.getService(bundleCtx.getServiceReference(StsAddressProvider.class));
  }

  // Majority of this method is from the WSDLGetInterceptor, in-line comments
  // specify which areas were added
  @Override
  public void handleMessage(Message message) throws Fault {
    String method = (String) message.get(Message.HTTP_REQUEST_METHOD);
    String query = (String) message.get(Message.QUERY_STRING);

    if (!"GET".equals(method) || StringUtils.isEmpty(query)) {
      return;
    }

    String baseUri = (String) message.get(Message.REQUEST_URL);
    String ctx = (String) message.get(Message.PATH_INFO);

    Map<String, String> map = UrlUtils.parseQueryString(query);
    if (isRecognizedQuery(
        map, baseUri, ctx, message.getExchange().getEndpoint().getEndpointInfo())) {
      Document doc = getDocument(message, baseUri, map, ctx);

      // DDF- start ADDED code
      if (doc != null && map.containsKey("wsdl")) {
        doc = addPolicyToWSDL(doc, loader.getPolicy());
        doc = configureAddress(doc);
      }
      // DDF- end of ADDED code

      Endpoint e = message.getExchange().get(Endpoint.class);
      Message mout = new MessageImpl();
      mout.setExchange(message.getExchange());
      mout = e.getBinding().createMessage(mout);
      mout.setInterceptorChain(
          OutgoingChainInterceptor.getOutInterceptorChain(message.getExchange()));
      message.getExchange().setOutMessage(mout);

      mout.put(WSDLGetInterceptor.DOCUMENT_HOLDER, doc);
      mout.put(Message.CONTENT_TYPE, "text/xml");

      // just remove the interceptor which should not be used
      cleanUpOutInterceptors(mout);

      // notice this is being added after the purge above, don't swap the order!
      mout.getInterceptorChain().add(WSDLGetOutInterceptor.INSTANCE);

      // DDF- CHANGED TRANSFORM_SKIP to transform.skip because private field
      message.getExchange().put("transform.skip", Boolean.TRUE);
      // skip the service executor and goto the end of the chain.
      message
          .getInterceptorChain()
          .doInterceptStartingAt(message, OutgoingChainInterceptor.class.getName());
    }
  }

  private Document configureAddress(Document doc) {
    String fullAddress = getFullAddress();
    if (fullAddress.equalsIgnoreCase(DEFAULT_ADDRESS)) {
      return doc;
    }
    Element de = doc.getDocumentElement();
    XPath xPath = XPathFactory.newInstance().newXPath();
    String xPathString =
        String.format(
            "//@*[starts-with(.,\"%s\")] | //text()[starts-with(.,\"%s\")]",
            DEFAULT_ADDRESS, DEFAULT_ADDRESS);
    NodeList nodes;
    try {
      nodes = (NodeList) xPath.evaluate(xPathString, de, XPathConstants.NODESET);
    } catch (XPathExpressionException e) {
      LOGGER.info("Could not evaluate the xPath expression: {}", xPathString, e);
      return doc;
    }
    for (int i = 0; i < nodes.getLength(); i++) {
      Node node = nodes.item(i);
      node.setNodeValue(node.getNodeValue().replaceAll(DEFAULT_ADDRESS, fullAddress));
    }
    return de.getOwnerDocument();
  }

  private String getFullAddress() {
    String proto = addressProvider.getProtocol();
    String host = addressProvider.getHost();
    String port = "".equals(addressProvider.getPort()) ? "" : ":" + addressProvider.getPort();
    return String.format("%s://%s%s", proto, host, port);
  }

  /**
   * Adds the specified policy into the WSDL document. The policy should be added as a child to the
   * main definitions element of the WSDL and a PolicyReference pointing to it should be added to
   * the main soap binding.
   *
   * @param wsdlDoc WDSL file without the policy added
   * @param policyDoc Policy to add to the WSDL.
   * @return A combined Node containing the WSDL with the policy added.
   */
  public static Document addPolicyToWSDL(Document wsdlDoc, Document policyDoc) {
    Element newElement = wsdlDoc.getDocumentElement();
    Node policyNode = policyDoc.getDocumentElement();
    newElement.appendChild(wsdlDoc.importNode(policyNode, true));
    NodeList bindingElements = newElement.getElementsByTagName("binding");
    if (null != bindingElements) {
      Element bindingElement = (Element) bindingElements.item(0);
      Element policyReferenceElement =
          wsdlDoc.createElementNS("http://www.w3.org/ns/ws-policy", "wsp:PolicyReference");
      policyReferenceElement.setAttribute("URI", "#TransportSAML2Policy");
      bindingElement.appendChild(policyReferenceElement);
    }

    return newElement.getOwnerDocument();
  }

  private Document getDocument(
      Message message, String base, Map<String, String> params, String ctxUri) {
    // cannot have two wsdl's being generated for the same endpoint at the same
    // time as the addresses may get mixed up
    // For WSDL's the WSDLWriter does not share any state between documents.
    // For XSD's, the WSDLGetUtils makes a copy of any XSD schema documents before updating
    // any addresses and returning them, so for both WSDL and XSD this is the only part that needs
    // to be synchronized.
    synchronized (message.getExchange().getEndpoint()) {
      return new WSDLGetUtils()
          .getDocument(
              message, base, params, ctxUri, message.getExchange().getEndpoint().getEndpointInfo());
    }
  }

  private boolean isRecognizedQuery(
      Map<String, String> map, String baseUri, String ctx, EndpointInfo endpointInfo) {

    if (map.containsKey("wsdl") || map.containsKey("xsd")) {
      return true;
    }
    return false;
  }
}
