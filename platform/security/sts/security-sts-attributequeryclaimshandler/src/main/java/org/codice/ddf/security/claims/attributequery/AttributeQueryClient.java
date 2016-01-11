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
import java.net.HttpURLConnection;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;

import org.codice.ddf.platform.util.TransformerProperties;
import org.codice.ddf.platform.util.XMLUtils;
import org.opensaml.core.xml.XMLObjectBuilderFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.AttributeQuery;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Status;
import org.opensaml.soap.soap11.Envelope;
import org.opensaml.soap.soap11.impl.EnvelopeMarshaller;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
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

    private static final String SAML2_UNKNOWN_ATTR_PROFILE =
            "urn:oasis:names:tc:SAML:2.0:status:UnknownAttrProfile";

    private static final String SAML2_INVALID_ATTR_NAME_VALUE =
            "urn:oasis:names:tc:SAML:2.0:status:InvalidAttrNameOrValue";

    private static final String SAML2_UNKNOWN_PRINCIPAL =
            "urn:oasis:names:tc:SAML:2.0:status:UnknownPrincipal";

    private static final String SOAP_ACTION = "http://www.oasis-open.org/committees/security";

    private XMLObjectBuilderFactory builderFactory =
            XMLObjectProviderRegistrySupport.getBuilderFactory();

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
        LOGGER.debug("Creating SAML Protocol AttributeQuery for user: {}.", username);

        AttributeQuery attributeQuery = SamlProtocol.createAttributeQuery(SamlProtocol.createIssuer(
                issuer),
                SamlProtocol.createSubject(SamlProtocol.createNameID(username)),
                destination);

        LOGGER.debug("SAML Protocol AttributeQuery created for user: {}.", username);

        return attributeQuery;
    }

    /**
     * Signs SOAP message of an AttributeQuery.
     *
     * @param attributeQuery request to be signed.
     * @return Document of the AttributeQuery.
     */
    private Document signSoapRequest(AttributeQuery attributeQuery) throws AttributeQueryException {
        Element soapElement;
        try {
            // Create and set signature for request.
            simpleSign.signSamlObject(attributeQuery);

            // Create soap message for request.
            soapElement = createSoapMessage(attributeQuery);

            // Sign soap message.
            Signer.signObject(attributeQuery.getSignature());
        } catch (SignatureException | SimpleSign.SignatureException e) {
            throw new AttributeQueryException("Error occurred during signing of the request.", e);
        }

        // Print AttributeQuery Request.
        if (LOGGER.isTraceEnabled()) {
            printXML("SAML Protocol AttributeQuery Request:\n{}", soapElement);
        }
        return soapElement.getOwnerDocument();
    }

    /**
     * Creates a SOAP message of the AttributeQuery request.
     *
     * @param attributeQuery is added to the SOAP message
     * @return soapElement is the Element of the SOAP message
     */
    private Element createSoapMessage(AttributeQuery attributeQuery)
            throws AttributeQueryException {
        LOGGER.debug("Creating SOAP message from the SAML AttributeQuery.");

        Envelope envelope = SamlProtocol.createSoapMessage(attributeQuery);

        LOGGER.debug("SOAP message from the SAML AttributeQuery created.");

        try {
            return new EnvelopeMarshaller().marshall(envelope);
        } catch (MarshallingException e) {
            throw new AttributeQueryException("Cannot marshall SOAP object to an Element.", e);
        }
    }

    /**
     * Sends the request to the external attribute store via an HTTPS Client.
     *
     * @param requestDocument of the request.
     * @return Assertion of the response or null if the response contains a bad status code.
     * @throws AttributeQueryException
     */
    private Assertion sendRequest(Document requestDocument) throws AttributeQueryException {
        URL url;
        HttpURLConnection connection;
        try {
            url = new URL(externalAttributeStoreUrl);

            connection = createHttpsUrlConnection(url);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "text/xml");
            connection.setRequestProperty("SOAPAction", SOAP_ACTION);
            connection.setRequestProperty("Cache-Control", "no-cache, no-store");
            connection.setRequestProperty("Pragma", "no-cache");
            connection.setDoOutput(true);

            TransformerProperties transformerProperties = new TransformerProperties();
            transformerProperties.addOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");

            try (DataOutputStream write = new DataOutputStream(connection.getOutputStream())) {
                write.writeBytes(XMLUtils.format(requestDocument, transformerProperties));
                write.flush();

                if (!reportStatusCode(connection.getResponseCode())) {
                    // If connection is unsuccessful, do not continue and return null.
                    return null;
                }
            }
        } catch (IOException e) {
            LOGGER.error("Error when creating connection, unable to send request to {}",
                    externalAttributeStoreUrl);
            throw new AttributeQueryException("Could not create connection.", e);
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

            // Extract Response from Soap message.
            Node responseNode = responseDocument.getElementsByTagNameNS(SAML2_PROTOCOL, "Response")
                    .item(0);
            Element responseElement = (Element) responseNode;

            Unmarshaller unmarshaller = XMLObjectProviderRegistrySupport.getUnmarshallerFactory()
                    .getUnmarshaller(responseElement);

            Response response = (Response) unmarshaller.unmarshall(responseElement);
            LOGGER.debug("Successfully marshalled Element to SAML Response.");
            if (response.getStatus()
                    .getStatusCode()
                    .getValue()
                    .equals(SAML2_SUCCESS)) {
                LOGGER.debug("Successful response, retrieved attributes.");
                // Should only have one assertion.
                assertion = response.getAssertions()
                        .get(0);

            } else {
                reportError(response.getStatus());
            }
            return assertion;
        } catch (IOException | ParserConfigurationException | SAXException e) {
            throw new AttributeQueryException("Unable to parse request string into an XML document.",
                    e);
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
        switch (status.getStatusCode()
                .getValue()) {
        case SAML2_UNKNOWN_ATTR_PROFILE:
            LOGGER.error(
                    "Unsuccessful response: {}. Incorrect version number or incorrect parsing error.");
            break;
        case SAML2_INVALID_ATTR_NAME_VALUE:
            LOGGER.error("Unsuccessful response: {}. Request attribute name is unknown.");
            break;
        case SAML2_UNKNOWN_PRINCIPAL:
            LOGGER.error(
                    "Unsuccessful response: {}. Unknown principal name, user is not recognized.");
            break;
        default:
            LOGGER.error(status.getStatusMessage()
                    .getMessage());
            break;
        }
        if (status.getStatusMessage() != null && status.getStatusMessage()
                .getMessage() != null) {
            LOGGER.error(status.getStatusMessage()
                    .getMessage());
        }
        // Allow bad response to go through.
    }

    /**
     * If the connection was successful, continue on, else do not.
     *
     * @param statusCode of the connection.
     */
    private boolean reportStatusCode(int statusCode) {
        switch (statusCode) {
        case HttpURLConnection.HTTP_OK:
            LOGGER.debug("OK {}. Successful connection to: {}",
                    statusCode,
                    externalAttributeStoreUrl);
            return true;
        case HttpURLConnection.HTTP_UNAUTHORIZED:
            LOGGER.warn("Unauthorized {}. Could not connect to: {}",
                    statusCode,
                    externalAttributeStoreUrl);
            return false;
        case HttpURLConnection.HTTP_NOT_FOUND:
            LOGGER.warn("URL not found {} . Could not connect to: {}",
                    statusCode,
                    externalAttributeStoreUrl);
            return false;
        default:
            LOGGER.warn("Status code not supported {}.", statusCode);
            return false;
        }
    }

    /**
     * Creates an Http client for sending the request.
     *
     * @param url of the external attribute store.
     * @return HttpsUrlConnection if protocol is https,
     * else return HttpUrlConnection.
     */
    protected HttpURLConnection createHttpsUrlConnection(URL url) {
        try {
            if (url.getProtocol()
                    .equalsIgnoreCase("https")) {
                return (HttpsURLConnection) url.openConnection();
            } else {
                return (HttpURLConnection) url.openConnection();
            }
        } catch (IOException e) {
            LOGGER.error("Unable to create HttpsUrlConnection", e);
        }
        return null;
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
