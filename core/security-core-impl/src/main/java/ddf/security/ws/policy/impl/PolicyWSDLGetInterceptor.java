/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.ws.policy.impl;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.common.util.StringUtils;
import org.apache.cxf.common.util.UrlUtils;
import org.apache.cxf.frontend.WSDLGetInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageImpl;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.transport.Conduit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import ddf.security.ws.policy.PolicyLoader;


/**
 * Extension of the WSDLGetInterceptor that adds in the policy to the WSDL for
 * clients to see.
 * 
 */
public class PolicyWSDLGetInterceptor extends WSDLGetInterceptor
{

    private Logger logger = LoggerFactory.getLogger(PolicyWSDLGetInterceptor.class);
    private PolicyLoader loader;
    private static final String XML_ENC = "utf-8";

    public PolicyWSDLGetInterceptor( PolicyLoader loader )
    {
        super();
        getBefore().add(WSDLGetInterceptor.class.getName());
        this.loader = loader;
    }

    // Majority of this method is from the WSDLGetInterceptor, in-line comments
    // specify which areas were added
    @Override
    public void handleMessage( Message message ) throws Fault
    {
        String method = (String) message.get(Message.HTTP_REQUEST_METHOD);
        String query = (String) message.get(Message.QUERY_STRING);
        if (!"GET".equals(method) || StringUtils.isEmpty(query))
        {
            return;
        }
        String baseUri = (String) message.get(Message.REQUEST_URL);
        String ctx = (String) message.get(Message.PATH_INFO);

        synchronized (message.getExchange().getEndpoint())
        {
            Map<String, String> map = UrlUtils.parseQueryString(query);
            if (isRecognizedQuery(map, baseUri, ctx, message.getExchange().getEndpoint().getEndpointInfo()))
            {

                try
                {
                    Conduit c = message.getExchange().getDestination().getBackChannel(message, null, null);
                    Message mout = new MessageImpl();
                    mout.setExchange(message.getExchange());
                    message.getExchange().setOutMessage(mout);
                    mout.put(Message.CONTENT_TYPE, "text/xml");
                    c.prepare(mout);
                    OutputStream os = mout.getContent(OutputStream.class);
                    Document doc = getDocument(message, baseUri, map, ctx, message.getExchange().getEndpoint()
                        .getEndpointInfo());

                    // start added code
                    Node combinedDoc = doc;
                    if (map.containsKey("wsdl"))
                    {
                        combinedDoc = addPolicyToWSDL(doc, loader.getPolicy());
                    }
                    // end of added code

                    XMLStreamWriter writer = StaxUtils.createXMLStreamWriter(os, XML_ENC);
                    StaxUtils.writeNode(combinedDoc, writer, true);
                    message.getInterceptorChain().abort();
                    try
                    {
                        writer.flush();
                        writer.close();
                        os.flush();
                        os.close();
                    }
                    catch (IOException ex)
                    {
                        logger.debug("Failure writing full wsdl to the stream", ex);
                    }
                }
                catch (IOException e)
                {
                    throw new Fault(e);
                }
                catch (XMLStreamException e)
                {
                    throw new Fault(e);
                }
                finally
                {
                    message.getExchange().setOutMessage(null);
                }
            }
        }
    }

    /**
     * Adds the specified policy into the WSDL document. The policy should be
     * added as a child to the main definitions element of the WSDL and a
     * PolicyReference pointing to it should be added to the main soap binding.
     * 
     * @param wsdlDoc WDSL file without the policy added
     * @param policyDoc Policy to add to the WSDL.
     * @return A combined Node containing the WSDL with the policy added.
     */
    protected Node addPolicyToWSDL( Document wsdlDoc, Document policyDoc )
    {
        Element newElement = wsdlDoc.getDocumentElement();
        Node policyNode = policyDoc.getDocumentElement();
        newElement.appendChild(wsdlDoc.importNode(policyNode, true));
        Element bindingElement = (Element) newElement.getElementsByTagName("binding").item(0);
        Element policyReferenceElement = wsdlDoc.createElementNS("http://www.w3.org/ns/ws-policy",
            "wsp:PolicyReference");
        policyReferenceElement.setAttribute("URI", "#TransportSAML2Policy");
        bindingElement.appendChild(policyReferenceElement);

        return newElement;
    }

}
