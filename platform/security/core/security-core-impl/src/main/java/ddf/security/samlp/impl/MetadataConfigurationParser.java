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
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.codice.ddf.platform.util.StandardThreadFactoryBuilder;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.saml.saml2.metadata.EntitiesDescriptor;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;
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

  static {
    OpenSAMLUtil.initSamlEngine();
  }

  private final Map<String, EntityDescriptor> entityDescriptorMap = new ConcurrentHashMap<>();
  private final Consumer<EntityDescriptor> updateCallback;

  public MetadataConfigurationParser(List<String> entityDescriptors) throws IOException {
    this(entityDescriptors, null);
  }

  public MetadataConfigurationParser(
      List<String> entityDescriptors, Consumer<EntityDescriptor> updateCallback)
      throws IOException {
    this.updateCallback = updateCallback;
    parseEntityDescriptors(entityDescriptors);
  }

  public Map<String, EntityDescriptor> getEntityDescriptors() {
    return entityDescriptorMap;
  }

  private void parseEntityDescriptors(List<String> entityDescriptorStrings) throws IOException {
    String ddfHome = System.getProperty("ddf.home");
    for (String entityDescriptor : entityDescriptorStrings) {
      buildEntityDescriptor(entityDescriptor);
    }
    Path metadataFolder = Paths.get(ddfHome, ETC_FOLDER, METADATA_ROOT_FOLDER);

    try {
      AccessController.doPrivileged(
          (PrivilegedExceptionAction<Void>)
              () -> {
                privilegedParseEntityDescriptions(metadataFolder);
                return null;
              });
    } catch (PrivilegedActionException e) {
      if (!(e.getException() instanceof IOException)) {
        LOGGER.warn(
            "Unexpected exception type - {}", e.getException().getClass(), e.getException());
        throw new IOException("Runtime exception occurred reading entity descriptors");
      }
      throw (IOException) e.getException();
    }
  }

  private void privilegedParseEntityDescriptions(Path metadataFolder) throws IOException {
    try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(metadataFolder)) {
      for (Path path : directoryStream) {
        if (Files.isReadable(path)) {
          try (InputStream fileInputStream = Files.newInputStream(path)) {
            List<EntityDescriptor> entityDescriptors =
                readEntityDescriptors(new InputStreamReader(fileInputStream, "UTF-8"));

            entityDescriptors.forEach(this::processEntityDescriptor);
          }
        }
      }
    } catch (NoSuchFileException e) {
      LOGGER.debug("IDP metadata directory is not configured.", e);
    }
  }

  private void buildEntityDescriptor(String entityDescriptorString) throws IOException {
    List<EntityDescriptor> entityDescriptors = new ArrayList<>();
    entityDescriptorString = entityDescriptorString.trim();
    if (entityDescriptorString.startsWith(HTTPS) || entityDescriptorString.startsWith(HTTP)) {
      retrieveEntityDescriptorViaHttp(entityDescriptorString);
    } else if (entityDescriptorString.startsWith(FILE + System.getProperty("ddf.home"))) {
      entityDescriptors =
          retrieveEntityDescriptorViaFile(entityDescriptorString, entityDescriptors);
    } else if (entityDescriptorString.startsWith("<") && entityDescriptorString.endsWith(">")) {
      entityDescriptors = readEntityDescriptors(new StringReader(entityDescriptorString));
    } else {
      LOGGER.info("Skipping unknown metadata configuration value: {}", entityDescriptorString);
    }

    if (!entityDescriptors.isEmpty()) {
      entityDescriptors.forEach(this::processEntityDescriptor);
    }
  }

  private List<EntityDescriptor> retrieveEntityDescriptorViaFile(
      String entityDescriptorString, List<EntityDescriptor> entityDescriptors) throws IOException {
    String pathStr = StringUtils.substringAfter(entityDescriptorString, FILE);
    Path path = Paths.get(pathStr);
    try {
      entityDescriptors =
          AccessController.doPrivileged(
                  (PrivilegedExceptionAction<Optional<List<EntityDescriptor>>>)
                      () -> privilegedRetrieveEntityDescriptorViaFile(path))
              .orElse(entityDescriptors);
    } catch (PrivilegedActionException e) {
      if (!(e.getException() instanceof IOException)) {
        LOGGER.warn(
            "Unexpected exception type - {}", e.getException().getClass(), e.getException());
        throw new IOException("Runtime exception occurred reading entity descriptors");
      }
      throw (IOException) e.getException();
    }

    return entityDescriptors;
  }

  private Optional<List<EntityDescriptor>> privilegedRetrieveEntityDescriptorViaFile(Path path)
      throws IOException {
    if (Files.isReadable(path)) {
      try (InputStream fileInputStream = Files.newInputStream(path)) {
        return Optional.of(readEntityDescriptors(new InputStreamReader(fileInputStream, "UTF-8")));
      }
    }

    return Optional.empty();
  }

  private void retrieveEntityDescriptorViaHttp(String entityDescriptorString) throws IOException {
    if (entityDescriptorString.startsWith(HTTP)) {
      LOGGER.warn(
          "Retrieving metadata via HTTP instead of HTTPS. The metadata configuration is unsafe!!!");
    }
    PropertyResolver propertyResolver = new PropertyResolver(entityDescriptorString);
    HttpTransport httpTransport = new NetHttpTransport();

    ExecutorService executor =
        Executors.newSingleThreadExecutor(
            StandardThreadFactoryBuilder.newThreadFactory("metadataConfigParserThread"));
    ListeningExecutorService service = MoreExecutors.listeningDecorator(executor);

    addHttpCallback(propertyResolver, httpTransport, service, executor);
    try {
      if (!service.isShutdown() && !service.awaitTermination(30, TimeUnit.SECONDS)) {
        LOGGER.debug("Executor service shutdown timed out");
      }
    } catch (InterruptedException e) {
      LOGGER.debug("Problem shutting down executor", e);
      service.shutdown();
      Thread.currentThread().interrupt();
    }
  }

  private void processEntityDescriptor(EntityDescriptor entityDescriptor) {
    LOGGER.info("entityId = {}", entityDescriptor.getEntityID());
    validateMetadata(entityDescriptor);
    entityDescriptorMap.put(entityDescriptor.getEntityID(), entityDescriptor);
    if (updateCallback != null) {
      updateCallback.accept(entityDescriptor);
    }
  }

  private void addHttpCallback(
      PropertyResolver propertyResolver,
      HttpTransport httpTransport,
      ListeningExecutorService service,
      ExecutorService executor)
      throws IOException {
    HttpRequest httpRequest = generateHttpRequest(propertyResolver, httpTransport);
    ListenableFuture<HttpResponse> httpResponseFuture = service.submit(httpRequest::execute);

    FutureCallback<HttpResponse> callback = getHttpResponseFutureCallback(service);
    Futures.addCallback(httpResponseFuture, callback, executor);
  }

  private HttpRequest generateHttpRequest(
      PropertyResolver propertyResolver, HttpTransport httpTransport) throws IOException {
    HttpRequest httpRequest =
        httpTransport
            .createRequestFactory()
            .buildGetRequest(new GenericUrl(propertyResolver.getResolvedString()));

    httpRequest.setUnsuccessfulResponseHandler(
        new HttpBackOffUnsuccessfulResponseHandler(new ExponentialBackOff())
            .setBackOffRequired(HttpBackOffUnsuccessfulResponseHandler.BackOffRequired.ALWAYS));
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
            LOGGER.info(
                "Unable to parse metadata from: {}",
                httpResponse.getRequest().getUrl().toString(),
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

  private List<EntityDescriptor> readEntityDescriptors(Reader reader) {
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
    if (entityXmlObj instanceof EntitiesDescriptor) {
      return ((EntitiesDescriptor) entityXmlObj).getEntityDescriptors();
    } else {
      return Collections.singletonList((EntityDescriptor) entityXmlObj);
    }
  }

  private void validateMetadata(EntityDescriptor root) {
    if (root.getCacheDuration() == null && root.getValidUntil() == null) {
      LOGGER.trace(
          "IDP metadata must either have cache duration or valid-until date."
              + " Defaulting IDP metadata cache duration to {}",
          SamlProtocol.getCacheDuration());
      root.setCacheDuration(SamlProtocol.getCacheDuration().toMillis());
    }
  }
}
