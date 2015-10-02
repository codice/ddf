/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.samlp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.opensaml.saml2.metadata.EntityDescriptor;
import org.opensaml.xml.XMLObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

public class MetadataConfigurationParser {

    public static final String METADATA_ROOT_FOLDER = "metadata";

    public static final String ETC_FOLDER = "etc";

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataConfigurationParser.class);

    private static final String HTTPS = "https://";

    private static final String HTTP = "http://";

    private static final String FILE = "file:";

    public static Map<String, EntityDescriptor> buildEntityDescriptors(
            List<String> entityDescriptions) throws IOException {
        String ddfHome = System.getProperty("ddf.home");
        Map<String, EntityDescriptor> entityDescriptors = new HashMap<>();
        for (String entityDescription : entityDescriptions) {
            EntityDescriptor entityDescriptor = buildEntityDescriptor(entityDescription);
            if (entityDescriptor != null) {
                entityDescriptors.put(entityDescriptor.getEntityID(), entityDescriptor);
            }
        }
        Path metadataFolder = Paths.get(ddfHome, ETC_FOLDER, METADATA_ROOT_FOLDER);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(metadataFolder)) {
            for (Path path : directoryStream) {
                if (Files.isReadable(path)) {
                    try (InputStream fileInputStream = Files.newInputStream(path)) {
                        EntityDescriptor entityDescriptor = readEntityDescriptor(
                                new InputStreamReader(fileInputStream, "UTF-8"));

                        entityDescriptors.put(entityDescriptor.getEntityID(), entityDescriptor);
                    }
                }
            }
        } catch (NoSuchFileException e) {
            LOGGER.debug("IDP metadata directory is not configured.", e);
        }
        return entityDescriptors;
    }

    public static EntityDescriptor buildEntityDescriptor(String entityDescription)
            throws IOException {
        EntityDescriptor entityDescriptor = null;
        entityDescription = entityDescription.trim();
        if (entityDescription.startsWith(HTTPS) || entityDescription.startsWith(HTTP)) {
            if (entityDescription.startsWith(HTTP)) {
                LOGGER.warn(
                        "Retrieving metadata via HTTP instead of HTTPS. The metadata configuration is unsafe!!!");
            }
            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                String httpResponse = null;
                HttpGet get = new HttpGet(entityDescription);
                ResponseHandler responseHandler = new BasicResponseHandler();
                try {
                    httpResponse = (String) httpclient.execute(get, responseHandler);
                } catch (IOException e) {
                    LOGGER.warn("Incorrectly configured URL for metadata: {}", entityDescription,
                            e);
                }
                if (httpResponse != null) {
                    ByteArrayInputStream byteStream = new ByteArrayInputStream(
                            httpResponse.getBytes());
                    entityDescriptor = readEntityDescriptor(
                            new InputStreamReader(byteStream, "UTF-8"));
                }
            }
        } else if (entityDescription.startsWith(FILE + System.getProperty("ddf.home"))) {
            String pathStr = StringUtils.substringAfter(entityDescription, FILE);
            Path path = Paths.get(pathStr);
            if (Files.isReadable(path)) {
                try (InputStream fileInputStream = Files.newInputStream(path)) {
                    entityDescriptor = readEntityDescriptor(
                            new InputStreamReader(fileInputStream, "UTF-8"));
                }
            }
        } else if (entityDescription.startsWith("<") && entityDescription.endsWith(">")) {
            entityDescriptor = readEntityDescriptor(new StringReader(entityDescription));
        } else {
            LOGGER.warn("Skipping unknown metadata configuration value: " + entityDescription);
        }

        return entityDescriptor;
    }

    private static EntityDescriptor readEntityDescriptor(Reader reader) {
        Document entityDoc;
        try {
            entityDoc = StaxUtils.read(reader);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to read SAMLRequest as XML.");
        }
        XMLObject entityXmlObj;
        try {
            entityXmlObj = OpenSAMLUtil.fromDom(entityDoc.getDocumentElement());
        } catch (WSSecurityException ex) {
            throw new IllegalArgumentException(
                    "Unable to convert EntityDescriptor document to XMLObject.");
        }

        return (EntityDescriptor) entityXmlObj;
    }
}