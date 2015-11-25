/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.claims.attributequery;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.codice.ddf.platform.util.TransformerProperties;
import org.codice.ddf.platform.util.XMLUtils;
import org.opensaml.Configuration;
import org.opensaml.saml2.core.Assertion;
import org.opensaml.saml2.core.AttributeQuery;
import org.opensaml.saml2.core.Response;
import org.opensaml.saml2.core.Status;
import org.opensaml.ws.soap.soap11.Body;
import org.opensaml.ws.soap.soap11.Envelope;
import org.opensaml.ws.soap.soap11.Header;
import org.opensaml.ws.soap.soap11.impl.BodyBuilder;
import org.opensaml.ws.soap.soap11.impl.EnvelopeBuilder;
import org.opensaml.ws.soap.soap11.impl.EnvelopeMarshaller;
import org.opensaml.ws.soap.soap11.impl.HeaderBuilder;
import org.opensaml.xml.XMLObjectBuilderFactory;
import org.opensaml.xml.io.MarshallingException;
import org.opensaml.xml.io.Unmarshaller;
import org.opensaml.xml.io.UnmarshallingException;
import org.opensaml.xml.signature.SignatureException;
import org.opensaml.xml.signature.Signer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import ddf.security.samlp.SamlProtocol;
import ddf.security.samlp.SimpleSign;

public class AttributeQueryClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AttributeQueryClient.class);

    private static final String SAML2_PROTOCOL = "urn:oasis:names:tc:SAML:2.0:protocol";

    private static final String SAML2_SUCCESS = "urn:oasis:names:tc:SAML:2.0:status:Success";

    private static final String SAML2_UNKNOWN_ATTR_PROFILE = "urn:oasis:names:tc:SAML:2.0:status:UnknownAttrProfile";

    private static final String SAML2_INVALID_ATTR_NAME_VALUE = "urn:oasis:names:tc:SAML:2.0:status:InvalidAttrNameOrValue";

    private static final String SAML2_UNKNOWN_PRINCIPAL = "urn:oasis:names:tc:SAML:2.0:status:UnknownPrincipal";

    private SimpleSign simpleSign;

    private String externalAttributeStoreUrl;

    private String issuer;

    private String destination;

    public AttributeQueryClient(SimpleSign simpleSign, String externalAttributeStoreUrl,
            String issuer, String destination) {
        LOGGER.debug("Initializing AttributeQueryClient.");

        this.simpleSign = simpleSign;
        this.externalAttributeStoreUrl = externalAttributeStoreUrl;
        this.issuer = issuer;
        this.destination = destination;
    }

    /**
     * Creates the AttributeQuery request, the Https client to send the request, and returns
     * the Response from the external attribute store.
     *
     * @return Assertion of the response.
     */
    public Assertion retrieveResponse(String username) {
        return sendRequest(signSoapRequest(createRequest(username)));
    }

    private AttributeQuery createRequest(String username) {
        LOGGER.debug("Creating SAML Protocol AttributeQuery.");

        AttributeQuery attributeQuery = SamlProtocol
                .createAttributeQuery(SamlProtocol.createIssuer(issuer),
                        SamlProtocol.createSubject(SamlProtocol.createNameID(username)),
                        destination);

        LOGGER.debug("SAML Protocol AttributeQuery created.");

        return attributeQuery;
    }

    /**
     * Signs SOAP message of an AttributeQuery.
     *
     * @param attributeQuery Required for creating the SOAP message and for marshalling into an element for signing
     * @return Document of the AttributeQuery.
     */
    public Document signSoapRequest(AttributeQuery attributeQuery) throws AttributeQueryException {
        try {
            // Create and set signature for request.
            simpleSign.signSamlObject(attributeQuery);
        } catch (SimpleSign.SignatureException e) {
            throw new AttributeQueryException("Error creating signature for request", e);
        }

        // Create Soap message for request.
        Element soapElement = createSOAPPMessage(attributeQuery);

        try {
            // Sign soap message.
            Signer.signObject(attributeQuery.getSignature());
        } catch (SignatureException e) {
            throw new AttributeQueryException("SignatureException occurred when signing request.",
                    e);
        }

        // Print AttributeQuery Request
        if (LOGGER.isTraceEnabled()) {
            printXML("AttributeQuery Request:\n{}", soapElement);
        }

        return soapElement.getOwnerDocument();
    }

    /**
     * Creates a SOAP message of the AttributeQuery request.
     *
     * @param attributeQuery is added to the SOAP message
     * @return soapElement is the Element of the SOAP message
     */
    @SuppressWarnings("unchecked")
    private Element createSOAPPMessage(AttributeQuery attributeQuery)
            throws AttributeQueryException {
        LOGGER.debug("Creating SAML SOAP object.");

        XMLObjectBuilderFactory builderFactory = Configuration.getBuilderFactory();
        BodyBuilder soapBodyBuilder = (BodyBuilder) builderFactory
                .getBuilder(Body.DEFAULT_ELEMENT_NAME);

        EnvelopeBuilder soapEnvelopeBuilder = (EnvelopeBuilder) builderFactory
                .getBuilder(Envelope.DEFAULT_ELEMENT_NAME);

        HeaderBuilder soapHeaderBuilder = (HeaderBuilder) builderFactory
                .getBuilder(Header.DEFAULT_ELEMENT_NAME);

        Body body = soapBodyBuilder.buildObject();
        body.getUnknownXMLObjects().add(attributeQuery);

        Envelope envelope = soapEnvelopeBuilder.buildObject();
        envelope.setBody(body);

        Header header = soapHeaderBuilder.buildObject();
        envelope.setHeader(header);

        LOGGER.debug("SAML SOAP object created.");

        try {
            return new EnvelopeMarshaller().marshall(envelope);
        } catch (MarshallingException e) {
            throw new AttributeQueryException("Cannot marshall SOAP object to an Element.", e);
        }
    }

    /**
     * Sends the request to the external attribute store via an HTTPS Client.
     *
     * @param requestDocument of the request
     * @return Assertion of the response
     * @throws AttributeQueryException
     */
    public Assertion sendRequest(Document requestDocument) throws AttributeQueryException {
        URL url;
        HttpsURLConnection connection;
        DataOutputStream write;
        try {
            url = new URL(externalAttributeStoreUrl);

            connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/xml");
            connection.setRequestProperty("SOAPAction",
                    "http://www.oasis-open.org/committees/security");
            connection.setRequestProperty("Cache-Control", "no-cache, no-store");
            connection.setRequestProperty("Pragma", "no-cache");
            connection.setDoOutput(true);

            StringWriter buffer = new StringWriter();
            TransformerFactory transFactory = TransformerFactory.newInstance();
            Transformer transformer = transFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.transform(new DOMSource(requestDocument), new StreamResult(buffer));

            write = new DataOutputStream(connection.getOutputStream());
            write.writeBytes(buffer.toString());
            write.flush();
            write.close();

            int statusCode = connection.getResponseCode();
            if (!reportConnectionStatus(statusCode)) {
                // If connection is unsuccessful, do not continue and return null.
                return null;
            }

        } catch (MalformedURLException e) {
            LOGGER.error("Invalid external attribute store: {}", externalAttributeStoreUrl, e);
            throw new AttributeQueryException("Could send request to specified URL.", e);
        } catch (IOException e) {
            LOGGER.error("Invalid connection, unable to send request to {}",
                    externalAttributeStoreUrl);
            throw new AttributeQueryException("Could not create connection.", e);
        } catch (TransformerException e) {
            throw new AttributeQueryException("Unable to transform SOAP element into a string.", e);
        }

        try {
            Assertion assertion = null;
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(true);
            Document responseDocument = documentBuilderFactory.newDocumentBuilder()
                    .parse(connection.getInputStream());

            // Print Response
            if (LOGGER.isTraceEnabled()) {
                printXML("SAML Response:\n {}", responseDocument);
            }

            Node responseNode = responseDocument.getElementsByTagNameNS(SAML2_PROTOCOL, "Response")
                    .item(0);
            Element responseElement = (Element) responseNode;

            Unmarshaller unmarshaller = Configuration.getUnmarshallerFactory()
                    .getUnmarshaller(responseElement);

            Response response = (Response) unmarshaller.unmarshall(responseElement);
            LOGGER.debug("Successfully marshalled Element to SAML Response.");
            if (response.getStatus().getStatusCode().getValue().equals(SAML2_SUCCESS)) {
                LOGGER.debug("Successful response, retrieved attributes.");
                // Should only have one assertion.
                assertion = response.getAssertions().get(0);

            } else {
                reportError(response.getStatus());
            }
            return assertion;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new AttributeQueryException(
                    "Unable to parse request string into an XML document.", e);
        } catch (UnmarshallingException e) {
            throw new AttributeQueryException("Unable to marshall Element to SAML Response.", e);
        }
    }

    /**
     * If response is unsuccessful, report the cause of the error.
     *
     * @param status Status of response object.
     */
    private void reportError(Status status) throws AttributeQueryException {
        if (status.getStatusCode().getValue().equals(SAML2_UNKNOWN_ATTR_PROFILE)) {
            LOGGER.error(
                    "Unsuccessful response: {}. Incorrect version number or incorrect parsing error.",
                    status.getStatusMessage().getMessage());
        } else if (status.getStatusCode().getValue().equals(SAML2_INVALID_ATTR_NAME_VALUE)) {
            LOGGER.error("Unsuccessful response: {}. Request attribute name is unknown.",
                    status.getStatusMessage().getMessage());
        } else if (status.getStatusCode().getValue().equals(SAML2_UNKNOWN_PRINCIPAL)) {
            LOGGER.error(
                    "Unsuccessful response: {}. Unknown principal name, user is not recognized.",
                    status.getStatusMessage().getMessage());
        } else {
            LOGGER.error("Unsuccessful response: {}.", status.getStatusMessage().getMessage());
        }
        // Allow bad response to go through.
    }

    /**
     * If the connection was successful, continue on, else do not.
     *
     * @param statusCode of the connection.
     */
    private boolean reportConnectionStatus(int statusCode) {
        switch (statusCode) {
        case HttpsURLConnection.HTTP_OK:
            LOGGER.debug("OK {}. Successful connection to: {}", statusCode,
                    externalAttributeStoreUrl);
            return true;
        case HttpsURLConnection.HTTP_UNAUTHORIZED:
            LOGGER.warn("Unauthorized {}. Could not connect to: {}", statusCode,
                    externalAttributeStoreUrl);
            return false;
        case HttpURLConnection.HTTP_NOT_FOUND:
            LOGGER.warn("URL not found {} . Could not connect to: {}", statusCode,
                    externalAttributeStoreUrl);
            return false;
        default:
            LOGGER.warn("Status code not supported {}.", statusCode);
            return false;
        }
    }

    /**
     * Prints the given XML.
     *
     * @param xmlNode Node to transform.
     * @param message Message to display.
     */
    private void printXML(String message, Node xmlNode) {
        TransformerProperties transformerProperties = new TransformerProperties();
        transformerProperties.addOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        LOGGER.trace(message, XMLUtils.format(xmlNode, transformerProperties));
    }

    public void setSimpleSign(SimpleSign simpleSign) {
        this.simpleSign = simpleSign;
    }

    public void setExternalAttributeStoreUrl(String externalAttributeStoreUrl) {
        this.externalAttributeStoreUrl = externalAttributeStoreUrl;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }
}
