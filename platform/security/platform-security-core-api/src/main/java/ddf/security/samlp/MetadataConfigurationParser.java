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
package ddf.security.samlp;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.codice.ddf.configuration.PropertyResolver;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpBackOffIOExceptionHandler;
import com.google.api.client.http.HttpBackOffUnsuccessfulResponseHandler;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.util.ExponentialBackOff;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

public class MetadataConfigurationParser {

    public static final String METADATA_ROOT_FOLDER = "metadata";

    public static final String ETC_FOLDER = "etc";

    private static final Logger LOGGER = LoggerFactory.getLogger(MetadataConfigurationParser.class);

    private static final String HTTPS = "https://";

    private static final String HTTP = "http://";

    private static final String FILE = "file:";

    private final Map<String, EntityDescriptor> entityDescriptorMap = new ConcurrentHashMap<>();

    private final Consumer<EntityDescriptor> updateCallback;

    static {
        OpenSAMLUtil.initSamlEngine();
    }

    public MetadataConfigurationParser(List<String> entityDescriptions) throws IOException {
        this(entityDescriptions, null);
    }

    public MetadataConfigurationParser(List<String> entityDescriptions,
            Consumer<EntityDescriptor> updateCallback) throws IOException {
        this.updateCallback = updateCallback;
        parseEntityDescriptions(entityDescriptions);
    }

    public Map<String, EntityDescriptor> getEntryDescriptions() {
        return entityDescriptorMap;
    }

    private void parseEntityDescriptions(List<String> entityDescriptions) throws IOException {
        String ddfHome = System.getProperty("ddf.home");
        for (String entityDescription : entityDescriptions) {
            buildEntityDescriptor(entityDescription);
        }
        Path metadataFolder = Paths.get(ddfHome, ETC_FOLDER, METADATA_ROOT_FOLDER);
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(metadataFolder)) {
            for (Path path : directoryStream) {
                if (Files.isReadable(path)) {
                    try (InputStream fileInputStream = Files.newInputStream(path)) {
                        EntityDescriptor entityDescriptor =
                                readEntityDescriptor(new InputStreamReader(fileInputStream,
                                        "UTF-8"));

                        LOGGER.info("entityId = {}", entityDescriptor.getEntityID());
                        entityDescriptorMap.put(entityDescriptor.getEntityID(), entityDescriptor);
                        if (updateCallback != null) {
                            updateCallback.accept(entityDescriptor);
                        }
                    }
                }
            }
        } catch (NoSuchFileException e) {
            LOGGER.debug("IDP metadata directory is not configured.", e);
        }
    }

    private void buildEntityDescriptor(String entityDescription) throws IOException {
        EntityDescriptor entityDescriptor = null;
        entityDescription = entityDescription.trim();
        if (entityDescription.startsWith(HTTPS) || entityDescription.startsWith(HTTP)) {
            if (entityDescription.startsWith(HTTP)) {
                LOGGER.warn(
                        "Retrieving metadata via HTTP instead of HTTPS. The metadata configuration is unsafe!!!");
            }
            PropertyResolver propertyResolver = new PropertyResolver(entityDescription);
            HttpTransport httpTransport = new NetHttpTransport();

            ExecutorService executor = Executors.newSingleThreadExecutor();
            ListeningExecutorService service = MoreExecutors.listeningDecorator(executor);

            addHttpCallback(propertyResolver, httpTransport, service, executor);
            try {
                if (!service.isShutdown() && !service.awaitTermination(30, TimeUnit.SECONDS)) {
                    LOGGER.debug("Executor service shutdown timed out");
                }
            } catch (InterruptedException e) {
                LOGGER.debug("Problem shutting down executor", e);
                service.shutdown();
                Thread.currentThread()
                        .interrupt();
            }
        } else if (entityDescription.startsWith(FILE + System.getProperty("ddf.home"))) {
            String pathStr = StringUtils.substringAfter(entityDescription, FILE);
            Path path = Paths.get(pathStr);
            if (Files.isReadable(path)) {
                try (InputStream fileInputStream = Files.newInputStream(path)) {
                    entityDescriptor = readEntityDescriptor(new InputStreamReader(fileInputStream,
                            "UTF-8"));
                }
            }
        } else if (entityDescription.startsWith("<") && entityDescription.endsWith(">")) {
            entityDescriptor = readEntityDescriptor(new StringReader(entityDescription));
        } else {
            LOGGER.info("Skipping unknown metadata configuration value: {}", entityDescription);
        }

        if (entityDescriptor != null) {
            entityDescriptorMap.put(entityDescriptor.getEntityID(), entityDescriptor);
            if (updateCallback != null) {
                updateCallback.accept(entityDescriptor);
            }
        }
    }

    private void addHttpCallback(PropertyResolver propertyResolver, HttpTransport httpTransport,
            ListeningExecutorService service, ExecutorService executor) throws IOException {
        HttpRequest httpRequest = generateHttpRequest(propertyResolver, httpTransport);
        ListenableFuture<HttpResponse> httpResponseFuture = service.submit(httpRequest::execute);

        FutureCallback<HttpResponse> callback = getHttpResponseFutureCallback(service);
        Futures.addCallback(httpResponseFuture, callback, executor);
    }

    private HttpRequest generateHttpRequest(PropertyResolver propertyResolver,
            HttpTransport httpTransport) throws IOException {
        HttpRequest httpRequest = httpTransport.createRequestFactory()
                .buildGetRequest(new GenericUrl(propertyResolver.getResolvedString()));

        httpRequest.setUnsuccessfulResponseHandler(new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff()).setBackOffRequired(
                HttpBackOffUnsuccessfulResponseHandler.BackOffRequired.ALWAYS));
        httpRequest.setIOExceptionHandler(new HttpBackOffIOExceptionHandler(new ExponentialBackOff()));

        return httpRequest;
    }

    private FutureCallback<HttpResponse> getHttpResponseFutureCallback(
            ListeningExecutorService service) {
        return new FutureCallback<HttpResponse>() {
            @Override
            public void onSuccess(HttpResponse httpResponse) {
                if (httpResponse != null
                        && httpResponse.getStatusCode() == HttpStatusCodes.STATUS_CODE_OK) {
                    try {
                        String parsedResponse = httpResponse.parseAsString();
                        buildEntityDescriptor(parsedResponse);
                        service.shutdown();
                    } catch (IOException e) {
                        LOGGER.info("Unable to parse metadata from: {}",
                                httpResponse.getRequest()
                                        .getUrl()
                                        .toString(),
                                e);
                    }
                } else {
                    LOGGER.warn("No/bad response; re-submitting request");
                }
            }

            @Override
            public void onFailure(Throwable throwable) {
                LOGGER.info("Unable to retrieve metadata.", throwable);
            }
        };
    }

    private EntityDescriptor readEntityDescriptor(Reader reader) {
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