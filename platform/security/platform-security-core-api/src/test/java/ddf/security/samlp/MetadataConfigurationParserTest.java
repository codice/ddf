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
package ddf.security.samlp;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.config.SocketConfig;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.bootstrap.HttpServer;
import org.apache.http.impl.bootstrap.ServerBootstrap;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.wss4j.common.saml.OpenSAMLUtil;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.opensaml.saml.saml2.metadata.EntityDescriptor;

public class MetadataConfigurationParserTest {

  private Path entityDescriptorPath;
  private Path entitiesDescriptorPath;

  private HttpServer server;

  private String serverAddress;
  public static final Long SEVEN_DAYS = Duration.ofDays(7).toMillis();

  @Mock HttpRequestHandler handler;
  public static final String CACHE_DURATION_REGEX = "cacheDuration=\"\\w*\"";

  @Before
  public void before() throws Exception {
    System.setProperty("org.ops4j.pax.logging.DefaultServiceLog.level", "INFO");
    MockitoAnnotations.initMocks(this);
    OpenSAMLUtil.initSamlEngine();

    entityDescriptorPath =
        Paths.get(getClass().getResource("/etc/metadata/entityDescriptor.xml").toURI());
    entitiesDescriptorPath =
        Paths.get(getClass().getResource("/etc/metadata/entitiesDescriptor.xml").toURI());
    System.setProperty("ddf.home", "");

    this.server =
        ServerBootstrap.bootstrap()
            .setSocketConfig(SocketConfig.custom().setSoTimeout(5000).build())
            .registerHandler("/*", handler)
            .create();
    this.server.start();
    serverAddress = "localhost:" + server.getLocalPort();
  }

  @After
  public void after() throws Exception {
    System.clearProperty("org.ops4j.pax.logging.DefaultServiceLog.level");
    server.stop();
  }

  @Test
  public void testMetadataFolder() throws Exception {
    metadataFolderEntities(null);
  }

  @Test
  public void testMetadataFolderCallback() throws Exception {
    AtomicBoolean invoked = new AtomicBoolean(false);
    metadataFolderEntities(ed -> invoked.set(true));
    assertThat("Callback was not invoked", invoked.get());
  }

  private void metadataFolderEntities(Consumer<EntityDescriptor> updateCallback) throws Exception {
    System.setProperty(
        "ddf.home", entityDescriptorPath.getParent().getParent().getParent().toString());
    MetadataConfigurationParser metadataConfigurationParser =
        new MetadataConfigurationParser(Collections.emptyList(), updateCallback);
    Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntityDescriptors();

    assertThat(entities.size(), is(3));
  }

  @Test
  public void testMetadataFileForEntityDescriptor() throws Exception {
    metadataFile(entityDescriptorPath, null, 1);
  }

  @Test
  public void testMetadataFileCallbackForEntityDescriptor() throws Exception {
    AtomicBoolean invoked = new AtomicBoolean(false);
    metadataFile(entityDescriptorPath, ed -> invoked.set(true), 1);
    assertThat("Callback was not invoked", invoked.get());
  }

  @Test
  public void testMetadataFileForEntitiesDescriptor() throws Exception {
    metadataFile(entitiesDescriptorPath, null, 2);
  }

  @Test
  public void testMetadataFileCallbackForEntitieDescriptor() throws Exception {
    AtomicBoolean invoked = new AtomicBoolean(false);
    metadataFile(entitiesDescriptorPath, ed -> invoked.set(true), 2);
    assertThat("Callback was not invoked", invoked.get());
  }

  private void metadataFile(
      Path filePath, Consumer<EntityDescriptor> updateCallback, int expectedSize)
      throws IOException {
    MetadataConfigurationParser metadataConfigurationParser =
        new MetadataConfigurationParser(
            Collections.singletonList("file:" + filePath.toString()), updateCallback);
    Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntityDescriptors();

    assertThat(entities.size(), is(expectedSize));
  }

  @Test
  public void testMetadataString() throws Exception {
    metadataString(null);
  }

  @Test
  public void testMetadataStringCallback() throws Exception {
    AtomicBoolean invoked = new AtomicBoolean(false);
    metadataString(ed -> invoked.set(true));
    assertThat("Callback was not invoked", invoked.get());
  }

  private void metadataString(Consumer<EntityDescriptor> updateCallback) throws IOException {
    MetadataConfigurationParser metadataConfigurationParser =
        new MetadataConfigurationParser(
            Collections.singletonList(IOUtils.toString(entityDescriptorPath.toUri())),
            updateCallback);
    Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntityDescriptors();

    assertThat(entities.size(), is(1));
  }

  @Test
  public void testMetadataHttp() throws Exception {
    serverRespondsWith(IOUtils.toString(entityDescriptorPath.toUri(), StandardCharsets.UTF_8));

    MetadataConfigurationParser metadataConfigurationParser =
        new MetadataConfigurationParser(Collections.singletonList("http://" + serverAddress));

    Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntityDescriptors();

    while (entities.size() != 1) {
      TimeUnit.MILLISECONDS.sleep(10);
    }

    assertThat(entities.size(), is(1));
  }

  @Test
  public void testMetadataBadString() throws Exception {
    MetadataConfigurationParser metadataConfigurationParser =
        new MetadataConfigurationParser(Collections.singletonList("bad xml"));
    Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntityDescriptors();

    assertThat(entities.size(), is(0));
  }

  @Test
  public void testMetadataBadFile() throws Exception {
    MetadataConfigurationParser metadataConfigurationParser =
        new MetadataConfigurationParser(Collections.singletonList("file:bad path"));
    Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntityDescriptors();

    assertThat(entities.size(), is(0));
  }

  @Test
  public void testRootElementNoCacheDuration() throws Exception {
    String xml = IOUtils.toString(entityDescriptorPath.toUri(), StandardCharsets.UTF_8);
    String xmlNoCacheDuration = xml.replaceFirst(CACHE_DURATION_REGEX, "");
    EntityDescriptor entity = getEntityDescriptor(xmlNoCacheDuration);
    assertThat(
        String.format("Expected default cache duration %s milliseconds", SEVEN_DAYS),
        entity.getCacheDuration(),
        is(SEVEN_DAYS));
  }

  @Test
  public void testRootElementValidUntil() throws Exception {
    String xml = IOUtils.toString(entityDescriptorPath.toUri(), StandardCharsets.UTF_8);
    DateTime validUntil = DateTime.now().plusYears(1);
    String validUntilXmlString = String.format("validUntil=\"%tF\"", validUntil.toDate());
    String xmlNoCacheDuration = xml.replaceFirst(CACHE_DURATION_REGEX, validUntilXmlString);
    EntityDescriptor entity = getEntityDescriptor(xmlNoCacheDuration);
    boolean isSameDate = entity.getValidUntil().toLocalDate().isEqual(validUntil.toLocalDate());
    assertThat("Expected different valid-until date", isSameDate, is(true));
  }

  private EntityDescriptor getEntityDescriptor(String xml) throws Exception {
    serverRespondsWith(xml);
    MetadataConfigurationParser metadataConfigurationParser =
        new MetadataConfigurationParser(Collections.singletonList("http://" + serverAddress));
    Map<String, EntityDescriptor> entities = metadataConfigurationParser.getEntityDescriptors();
    String key = "https://localhost:8993/services/idp/login";
    assertThat("Missing SAML entity. Test cannot proceed.", entities, hasKey(key));
    return entities.get(key);
  }

  private void serverRespondsWith(String message) throws HttpException, IOException {
    doAnswer(
            invocationOnMock -> {
              HttpResponse response = (HttpResponse) invocationOnMock.getArguments()[1];
              response.setEntity(new StringEntity(message));
              response.setStatusCode(200);
              return null;
            })
        .when(handler)
        .handle(any(), any(), any());
  }
}
