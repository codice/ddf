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
package org.codice.ddf.catalog.content.monitor;

import static org.awaitility.Awaitility.await;
import static org.codice.ddf.catalog.content.monitor.configurators.KarafConfigurator.karafConfiguration;
import static org.codice.ddf.catalog.content.monitor.features.CamelFeatures.CamelFeature.CAMEL;
import static org.codice.ddf.catalog.content.monitor.features.CamelFeatures.camelFeatures;
import static org.codice.ddf.catalog.content.monitor.features.CxfFeatures.CxfFeature.CXF_BINDINGS_SOAP;
import static org.codice.ddf.catalog.content.monitor.features.CxfFeatures.CxfFeature.CXF_FRONTEND_JAVASCRIPT;
import static org.codice.ddf.catalog.content.monitor.features.CxfFeatures.CxfFeature.CXF_JAXRS;
import static org.codice.ddf.catalog.content.monitor.features.CxfFeatures.CxfFeature.CXF_RT_SECURITY_SAML;
import static org.codice.ddf.catalog.content.monitor.features.CxfFeatures.CxfFeature.CXF_WS_POLICY;
import static org.codice.ddf.catalog.content.monitor.features.CxfFeatures.CxfFeature.CXF_WS_SECURITY;
import static org.codice.ddf.catalog.content.monitor.features.CxfFeatures.cxfFeatures;
import static org.codice.ddf.catalog.content.monitor.features.KarafSpringFeatures.SpringFeature.SPRING;
import static org.codice.ddf.catalog.content.monitor.features.KarafSpringFeatures.karafSpringFeatures;
import static org.codice.ddf.catalog.content.monitor.features.KarafStandardFeatures.StandardFeature.STANDARD;
import static org.codice.ddf.catalog.content.monitor.features.KarafStandardFeatures.karafStandardFeatures;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.apache.karaf.features.BootFinished;
import org.codice.ddf.catalog.content.monitor.configurators.KeystoreTruststoreConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.security.service.SecurityManager;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ContentDirectoryMonitorComponentTest {
    private static final Logger LOG =
            LoggerFactory.getLogger(ContentDirectoryMonitorComponentTest.class);

    private static final int TIMEOUT_IN_SECONDS = 10;

    @Inject
    private BundleContext bundleContext;

    private List<ServiceRegistration> serviceRegistrations = new ArrayList<>();

    @Inject
    private BootFinished bootFinished;

    @Inject
    private CamelContext camelContext;

    @Inject
    private ContentDirectoryMonitor contentDirectoryMonitor;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String directoryPath;

    private CatalogFramework catalogFramework;

    @Configuration
    public Option[] config() throws IOException {

        return options(testDependencies(),
                karafConfiguration(),
                karafStandardFeatures(STANDARD),
                karafSpringFeatures(SPRING),
                camelFeatures(CAMEL),
                cxfFeatures(CXF_RT_SECURITY_SAML,
                        CXF_BINDINGS_SOAP,
                        CXF_WS_POLICY,
                        CXF_WS_SECURITY,
                        CXF_FRONTEND_JAVASCRIPT,
                        CXF_JAXRS),
                keystoreAndTruststoreConfig(),
                contentDirectoryMonitorDependencies(),
                initContentDirectoryMonitorConfig());
    }

    private Option testDependencies() {
        return composite(startBundle("org.awaitility", "awaitility"),
                startBundle("org.mockito", "mockito-core"),
                startBundle("org.objenesis", "objenesis"));
    }

    private Option keystoreAndTruststoreConfig() {
        InputStream keystore = ContentDirectoryMonitorComponentTest.class.getResourceAsStream(
                "/serverKeystore.jks");
        InputStream truststore = ContentDirectoryMonitorComponentTest.class.getResourceAsStream(
                "/serverTruststore.jks");

        return KeystoreTruststoreConfigurator.createKeystoreAndTruststore(keystore, truststore);
    }

    private Option initContentDirectoryMonitorConfig() throws IOException {
        return editConfigurationFilePut(
                "etc/org.codice.ddf.catalog.content.monitor.ContentDirectoryMonitor-test.cfg",
                "monitoredDirectoryPath",
                "");
    }

    private Option contentDirectoryMonitorDependencies() {
        return composite(startBundle("org.apache.commons", "commons-lang3"),
                startBundle("commons-lang", "commons-lang"),
                startBundle("commons-io", "commons-io"),
                startBundle("commons-collections", "commons-collections"),
                startBundle("commons-configuration", "commons-configuration"),

                startBundle("com.google.guava", "guava"),
                startBundle("org.apache.tika", "tika-core"),

                startBundle("org.apache.shiro", "shiro-core"),
                startBundle("javax.servlet", "javax.servlet-api"),
                startBundle("javax.validation", "validation-api"),
                startBundle("org.bouncycastle", "bcprov-jdk15on"),

                startBundle("ddf.platform.util", "util-uuidgenerator-api"),
                startBundle("ddf.platform.util", "util-uuidgenerator-impl"),
                startBundle("ddf.catalog.core", "catalog-core-camelcontext"),
                startBundle("ddf.platform.api", "platform-api"),
                startBundle("org.codice.thirdparty", "gt-opengis"),
                startBundle("ddf.catalog.core", "catalog-core-api"),
                startBundle("ddf.security.core", "security-core-api"),
                startBundle("ddf.security.expansion", "security-expansion-api"),
                startBundle("ddf.security.expansion", "security-expansion-impl"),
                startBundle("ddf.distribution", "branding-api"),
                startBundle("ddf.distribution", "ddf-branding-plugin"),
                startBundle("ddf.platform", "platform-configuration"),
                startBundle("ddf.security.handler", "security-handler-api"),

                startBundle("ddf.mime.core", "mime-core-api"),
                startBundle("ddf.mime.core", "mime-core-impl"),
                startBundle("ddf.catalog.core", "catalog-core-camelcomponent"),

                startBundle("ddf.catalog.core", "catalog-core-directorymonitor"));
    }

    private Option startBundle(String groupId, String artifactId) {
        return mavenBundle(groupId, artifactId).versionAsInProject()
                .start();
    }

    @Before
    public void setup() throws IOException {
        directoryPath = temporaryFolder.getRoot()
                .getCanonicalPath();

        SecurityManager securityManager = mock(SecurityManager.class);
        registerService(securityManager, SecurityManager.class);

        catalogFramework = mock(CatalogFramework.class);
        registerService(catalogFramework, CatalogFramework.class);
    }

    @After
    public void teardown() {
        for (ServiceRegistration registration : serviceRegistrations) {
            registration.unregister();
        }
    }

    private <T> void registerService(T service, Class<T> registerClass) {
        ServiceRegistration<T> registration = bundleContext.registerService(registerClass,
                service,
                null);
        serviceRegistrations.add(registration);
        await("service registration").atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .until(() -> bundleContext.getServiceReference(registerClass) != null);
    }

    @Test
    public void testCamelContext() {
        Component component = camelContext.getComponent("content");
        assertThat(component, notNullValue());
    }

    @Test
    public void testInPlaceMonitoring()
            throws IOException, InterruptedException, SourceUnavailableException, IngestException {
        updateContentDirectoryMonitor(directoryPath, ContentDirectoryMonitor.IN_PLACE);

        File file = createTestFile(directoryPath);

        ArgumentCaptor<CreateStorageRequest> createStorageRequest = ArgumentCaptor.forClass(
                CreateStorageRequest.class);
        verify(catalogFramework).create(createStorageRequest.capture());
        ContentItem result = createStorageRequest.getValue()
                .getContentItems()
                .get(0);

        assertThat(result.getFilename(), is(file.getName()));
        assertThat(file.exists(), is(true));
    }

    @Test
    public void testMoveMonitoring()
            throws IOException, SourceUnavailableException, IngestException {
        updateContentDirectoryMonitor(directoryPath, ContentDirectoryMonitor.MOVE);
        File file = createTestFile(directoryPath);
        File movedFile = Paths.get(directoryPath, ".ingested", file.getName())
                .toFile();
        assertThat(file.exists(), is(false));
        assertThat(movedFile.exists(), is(true));
    }

    @Test
    public void testDeleteMonitoring()
            throws IOException, SourceUnavailableException, IngestException {
        updateContentDirectoryMonitor(directoryPath, ContentDirectoryMonitor.DELETE);

        File file = createTestFile(directoryPath);
        File directory = Paths.get(directoryPath)
                .toFile();

        await("file deleted").atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .until(() -> !file.exists());

        assertThat(directory.list().length, is(0));
    }

    private AtomicBoolean createStorageRequestNotification()
            throws SourceUnavailableException, IngestException {
        AtomicBoolean created = new AtomicBoolean(false);
        Answer<CreateResponse> response = invocation -> {
            created.set(true);
            return null;
        };
        when(catalogFramework.create(any(CreateStorageRequest.class))).thenAnswer(response);
        return created;
    }

    private File createTestFile(String directoryPath)
            throws IOException, SourceUnavailableException, IngestException {
        AtomicBoolean created = createStorageRequestNotification();

        File file = File.createTempFile("test", ".txt", new File(directoryPath));
        Files.write("Hello, World", file, Charset.forName("UTF-8"));

        await("create storage request created").atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .until(created::get);
        return file;
    }

    private void updateContentDirectoryMonitor(String directoryPath, String processingMechanism) {
        updateContentDirectoryMonitor(directoryPath, 1, 500, processingMechanism);
    }

    private void updateContentDirectoryMonitor(String directoryPath, int numThreads,
            int readLockInterval, String processingMechanism) {
        Map<String, Object> properties = new HashMap<>();

        properties.put("monitoredDirectoryPath", directoryPath);
        properties.put("numThreads", numThreads);
        properties.put("readLockIntervalMilliseconds", readLockInterval);
        properties.put("processingMechanism", processingMechanism);

        contentDirectoryMonitor.updateCallback(properties);
    }
}

