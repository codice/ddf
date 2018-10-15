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
package org.codice.ddf.catalog.content.monitor;

import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.builder.verify.VerifyHttp.verifyHttp;
import static com.xebialabs.restito.semantics.Condition.endsWithUri;
import static com.xebialabs.restito.semantics.Condition.method;
import static com.xebialabs.restito.semantics.Condition.uri;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.streamBundle;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.tinybundles.core.TinyBundles.withBnd;

import aQute.bnd.osgi.Constants;
import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Condition;
import com.xebialabs.restito.server.StubServer;
import ddf.catalog.CatalogFramework;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.data.impl.ContentItemValidator;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.content.operation.impl.CreateStorageResponseImpl;
import ddf.catalog.content.operation.impl.DeleteStorageResponseImpl;
import ddf.catalog.content.operation.impl.ReadStorageRequestImpl;
import ddf.catalog.content.operation.impl.ReadStorageResponseImpl;
import ddf.catalog.content.operation.impl.UpdateStorageResponseImpl;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.OperationImpl;
import ddf.catalog.operation.impl.ResponseImpl;
import ddf.catalog.source.SourceDescriptor;
import ddf.security.service.SecurityManager;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.catalog.content.impl.FileSystemStorageProvider;
import org.codice.ddf.catalog.content.monitor.configurators.KeystoreTruststoreConfigurator;
import org.codice.ddf.catalog.content.monitor.features.CamelFeatures;
import org.codice.ddf.catalog.content.monitor.features.CxfFeatures;
import org.codice.ddf.catalog.content.monitor.features.KarafSpringFeatures;
import org.codice.ddf.catalog.content.monitor.util.BundleInfo;
import org.codice.ddf.test.common.ComponentTestRunner;
import org.codice.ddf.test.common.annotations.AfterExam;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.codice.ddf.test.common.annotations.PaxExamRule;
import org.codice.ddf.test.common.configurators.PortFinder;
import org.glassfish.grizzly.http.Method;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.ops4j.io.FileUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.ops4j.pax.tinybundles.core.TinyBundles;

@RunWith(ComponentTestRunner.class)
@ExamReactorStrategy(PerClass.class)
public class ContentDirectoryMonitorIT extends AbstractComponentTest {

  private static final String WEBDAV_FILE_CONTENT = "test";

  @Rule public PaxExamRule paxExamRule = new PaxExamRule(this);

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private static PortFinder portFinder;

  private static StubServer stubServer;

  private static final String WEBDAV_STUB_SERVER = "WebDavStubServer";

  @Inject private CamelContext camelContext;

  private ContentDirectoryMonitor contentDirectoryMonitor;

  private String directoryPath;

  private CatalogFramework catalogFramework;

  private ArgumentCaptor<CreateStorageRequest> createStorageRequest;

  private ArgumentCaptor<CreateRequest> createRequest;

  private CountDownLatch catalogFrameworkCreateStorage = new CountDownLatch(1);

  private CountDownLatch catalogFrameworkCreate = new CountDownLatch(1);

  private final Condition isPropFindMethod =
      Condition.custom(c -> c.getMethod().getMethodString().equals("PROPFIND"));

  @BeforeExam
  public void setupClass() throws Exception {
    portFinder = new PortFinder();
    stubServer = new StubServer(portFinder.getPort(WEBDAV_STUB_SERVER));
    whenHttp(stubServer)
        .match(endsWithUri("/webdavtest"))
        .then(
            Action.stringContent(
                IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("proplist.xml"), "UTF-8")));
    whenHttp(stubServer)
        .match(endsWithUri("/webdavtest/file1"))
        .then(Action.contentType("text/plain"), Action.stringContent(WEBDAV_FILE_CONTENT));
    stubServer.start();
  }

  @AfterExam
  public void tearDownClass() throws Exception {
    portFinder.close();
  }

  @Before
  public void setup() throws Exception {
    directoryPath = temporaryFolder.getRoot().getCanonicalPath();

    SecurityManager securityManager = mock(SecurityManager.class);
    registerService(securityManager, SecurityManager.class);

    catalogFramework = mockCatalogFramework();
    registerService(catalogFramework, CatalogFramework.class);

    registerService(mock(AttributeRegistry.class), AttributeRegistry.class);

    createStorageRequest = ArgumentCaptor.forClass(CreateStorageRequest.class);
    createRequest = ArgumentCaptor.forClass(CreateRequest.class);

    await("Content Directory Monitor service registration")
        .atMost(60, TimeUnit.SECONDS)
        .until(() -> bundleContext.getServiceReference(ContentDirectoryMonitor.class) != null);

    contentDirectoryMonitor =
        bundleContext.getService(bundleContext.getServiceReference(ContentDirectoryMonitor.class));
  }

  @Test
  public void testInPlaceMonitoring() throws Exception {
    updateContentDirectoryMonitor(directoryPath, ContentDirectoryMonitor.IN_PLACE);

    File file = createTestFile(directoryPath);
    long fileSize = file.length();

    waitForCreate();

    verifyCreateRequest("test", "txt", file.toURI().toASCIIString(), fileSize);
    assertThat("File should still exist", file.exists(), is(true));
  }

  @Test
  public void testInPlaceDavMonitoring() throws Exception {
    String webDavPath = "http://localhost:" + stubServer.getPort() + "/webdavtest";
    updateContentDirectoryMonitor(webDavPath, ContentDirectoryMonitor.IN_PLACE);

    waitForCreate();

    verifyCreateRequest("file1", "", webDavPath + "/file1", WEBDAV_FILE_CONTENT.length());
    verifyHttp(stubServer).atLeast(1, isPropFindMethod, uri("/webdavtest"));
    verifyHttp(stubServer).atLeast(1, isPropFindMethod, uri("/webdavtest/file1"));
    verifyHttp(stubServer).once(method(Method.GET), uri("/webdavtest/file1"));
    assertThat("WebDav cached file should not exist", isDavFileCached(), is(false));
  }

  @Test
  public void testMoveMonitoring() throws Exception {
    updateContentDirectoryMonitor(directoryPath, ContentDirectoryMonitor.MOVE);

    File file = createTestFile(directoryPath);
    long fileLength = file.length();
    File movedFile = Paths.get(directoryPath, ".ingested", file.getName()).toFile();

    waitForCreateStorage();

    verifyCreateStorageRequest("test", ".txt", fileLength);
    await("File deleted").atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS).until(() -> !file.exists());
    await("File moved").atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS).until(movedFile::exists);
  }

  @Test
  public void testDeleteMonitoring() throws Exception {
    updateContentDirectoryMonitor(directoryPath, ContentDirectoryMonitor.DELETE);

    File file = createTestFile(directoryPath);
    long fileLength = file.length();
    File directory = Paths.get(directoryPath).toFile();
    waitForCreateStorage();

    await("File deleted")
        .atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
        .until(() -> directory.list().length == 0);
    verifyCreateStorageRequest("test", "txt", fileLength);
  }

  @Override
  protected Option setupDistribution() {
    return composite(
        KarafSpringFeatures.start("spring"),
        CamelFeatures.start("camel"),
        CxfFeatures.start(
            "cxf-rt-security-saml",
            "cxf-bindings-soap",
            "cxf-ws-policy",
            "cxf-ws-security",
            "cxf-frontend-javascript",
            "cxf-jaxrs"),
        keystoreAndTruststoreConfig(),
        initContentDirectoryMonitorConfig(),
        streamBundle(
            TinyBundles.bundle()
                .add(FileSystemStorageProvider.class)
                .add(ContentItemImpl.class)
                .add(ContentItemValidator.class)
                .add(CreateStorageResponseImpl.class)
                .add(DeleteStorageResponseImpl.class)
                .add(ReadStorageResponseImpl.class)
                .add(UpdateStorageResponseImpl.class)
                .add(AttributeImpl.class)
                .add(ResponseImpl.class)
                .add(OperationImpl.class)
                .add(ReadStorageRequestImpl.class)
                .set(Constants.BUNDLE_SYMBOLICNAME, "tiny")
                .set(Constants.EXPORT_PACKAGE, "*")
                .set(Constants.IMPORT_PACKAGE, "")
                .build(withBnd())));
  }

  @Override
  protected List<BundleInfo> bundlesToStart() {
    List<BundleInfo> bundles = new ArrayList<>();
    bundles.addAll(testDependencies());
    bundles.addAll(contentDirectoryMonitorDependencies());
    return bundles;
  }

  private List<BundleInfo> testDependencies() {
    return Arrays.asList(
        new BundleInfo("ddf.lib", "test-common"),
        new BundleInfo("ddf.lib", "common-system"),
        new BundleInfo("org.awaitility", "awaitility"),
        new BundleInfo("org.mockito", "mockito-core"),
        new BundleInfo("org.objenesis", "objenesis"));
  }

  private Option keystoreAndTruststoreConfig() {
    try {
      File keystore = FileUtils.getFileFromClasspath("serverKeystore.jks");
      File truststore = FileUtils.getFileFromClasspath("serverTruststore.jks");

      return KeystoreTruststoreConfigurator.createKeystoreAndTruststore(keystore, truststore);
    } catch (FileNotFoundException e) {
      return null;
    }
  }

  private Option initContentDirectoryMonitorConfig() {
    return editConfigurationFilePut(
        "etc/org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor-test.cfg",
        "monitoredDirectoryPath",
        "");
  }

  private List<BundleInfo> contentDirectoryMonitorDependencies() {
    return Arrays.asList(
        new BundleInfo("org.apache.commons", "commons-lang3"),
        new BundleInfo("commons-lang", "commons-lang"),
        new BundleInfo("commons-io", "commons-io"),
        new BundleInfo("commons-collections", "commons-collections"),
        new BundleInfo("org.apache.commons", "commons-collections4"),
        new BundleInfo("commons-configuration", "commons-configuration"),
        new BundleInfo("org.apache.commons", "commons-configuration2"),
        new BundleInfo("joda-time", "joda-time"),
        new BundleInfo("com.google.guava", "guava"),
        new BundleInfo("org.apache.tika", "tika-core"),
        new BundleInfo("org.apache.shiro", "shiro-core"),
        new BundleInfo("javax.servlet", "javax.servlet-api"),
        new BundleInfo("javax.validation", "validation-api"),
        new BundleInfo("org.bouncycastle", "bcprov-jdk15on"),
        new BundleInfo("ddf.platform.util", "util-uuidgenerator-api"),
        new BundleInfo("ddf.platform.util", "util-uuidgenerator-impl"),
        new BundleInfo("ddf.catalog.core", "catalog-core-camelcontext"),
        new BundleInfo("ddf.platform.api", "platform-api"),
        new BundleInfo("org.codice.thirdparty", "gt-opengis"),
        new BundleInfo("ddf.catalog.core", "catalog-core-api"),
        new BundleInfo("ddf.security.core", "security-core-api"),
        new BundleInfo("ddf.security.expansion", "security-expansion-api"),
        new BundleInfo("ddf.security.expansion", "security-expansion-impl"),
        new BundleInfo("ddf.platform", "branding-api"),
        new BundleInfo("ddf.platform", "security-filter-api"),
        new BundleInfo("ddf.action.core", "action-core-api"),
        new BundleInfo("ddf.distribution", "ddf-branding-plugin"),
        new BundleInfo("ddf.security.handler", "security-handler-api"),
        new BundleInfo("ddf.mime.core", "mime-core-api"),
        new BundleInfo("ddf.mime.core", "mime-core-impl"),
        new BundleInfo("ddf.mime.tika", "mime-tika-resolver"),
        new BundleInfo("ch.qos.cal10n", "cal10n-api"),
        new BundleInfo("org.slf4j", "slf4j-ext"),
        new BundleInfo("ddf.catalog.core", "catalog-core-camelcomponent"),
        new BundleInfo("ddf.catalog.transformer", "tika-input-transformer"),
        new BundleInfo("ddf.thirdparty", "restito"),
        new BundleInfo("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.xalan"));
  }

  private CatalogFramework mockCatalogFramework() throws Exception {
    CatalogFramework catalogFramework = mock(CatalogFramework.class);

    SourceInfoResponse sourceInfoResponse = mockSourceInfoResponse();
    when(catalogFramework.getSourceInfo(anyObject())).thenReturn(sourceInfoResponse);

    when(catalogFramework.create(any(CreateStorageRequest.class)))
        .thenAnswer(
            (Answer<CreateResponse>)
                invocation -> {
                  catalogFrameworkCreateStorage.countDown();
                  return null;
                });

    when(catalogFramework.create(any(CreateRequest.class)))
        .thenAnswer(
            (Answer<CreateResponse>)
                invocation -> {
                  catalogFrameworkCreate.countDown();
                  return null;
                });

    return catalogFramework;
  }

  private boolean isDavFileCached() {
    File tmpFile = Paths.get(System.getProperty("java.io.tmpdir")).toFile();
    File[] cachedDavFiles = tmpFile.listFiles(file -> file.getName().startsWith("dav"));

    return cachedDavFiles.length == 0 ? false : true;
  }

  private SourceInfoResponse mockSourceInfoResponse() {
    SourceInfoResponse sourceInfoResponse = mock(SourceInfoResponse.class);

    SourceDescriptor sourceDescriptor = mock(SourceDescriptor.class);
    when(sourceDescriptor.isAvailable()).thenReturn(true);

    Set<SourceDescriptor> sourceInfo = new HashSet<>();
    sourceInfo.add(sourceDescriptor);

    when(sourceInfoResponse.getSourceInfo()).thenReturn(sourceInfo);
    return sourceInfoResponse;
  }

  private void updateContentDirectoryMonitor(String directoryPath, String processingMechanism) {
    Map<String, Object> properties = new HashMap<>();

    properties.put("monitoredDirectoryPath", directoryPath);
    properties.put("numThreads", 1);
    properties.put("readLockIntervalMilliseconds", 500);
    properties.put("processingMechanism", processingMechanism);

    contentDirectoryMonitor.updateCallback(properties);
  }

  private File createTestFile(String directoryPath) throws Exception {
    File file = File.createTempFile("test", ".txt", new File(directoryPath));
    Files.write(file.toPath(), Collections.singletonList("Hello, World"));
    return file;
  }

  private void waitForCreateStorage() throws Exception {
    catalogFrameworkCreateStorage.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
  }

  private void waitForCreate() throws Exception {
    catalogFrameworkCreate.await(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
  }

  private void verifyCreateStorageRequest(
      String fileNamePrefix, String fileNameSuffix, long fileLength) throws Exception {
    verify(catalogFramework).create(createStorageRequest.capture());
    List<ContentItem> contentItems = createStorageRequest.getValue().getContentItems();

    assertThat(contentItems.size(), is(1));
    ContentItem contentItem = contentItems.get(0);
    verifyContentItem(contentItem, fileNamePrefix, fileNameSuffix, fileLength);
  }

  private void verifyCreateRequest(
      String fileNamePrefix, String fileNameSuffix, String uri, long fileLength) throws Exception {
    verify(catalogFramework).create(createRequest.capture());
    List<Metacard> metacards = createRequest.getValue().getMetacards();

    assertThat(metacards.size(), is(1));
    Metacard metacard = metacards.get(0);
    assertThat(metacard.getTitle(), startsWith(fileNamePrefix));
    assertThat(metacard.getTitle(), endsWith(fileNameSuffix));
    assertThat(metacard.getResourceURI().toASCIIString(), is(uri));
    assertThat(Long.parseLong(metacard.getResourceSize()), is(fileLength));
  }

  private void verifyContentItem(
      ContentItem contentItem, String fileNamePrefix, String fileNameSuffix, long fileLength)
      throws Exception {
    assertThat(contentItem.getMimeTypeRawData(), is("text/plain"));
    assertThat(contentItem.getFilename(), startsWith(fileNamePrefix));
    assertThat(contentItem.getFilename(), endsWith(fileNameSuffix));
    assertThat(contentItem.getUri(), is("content:" + contentItem.getId()));
    assertThat(contentItem.getSize(), is(fileLength));
  }
}
