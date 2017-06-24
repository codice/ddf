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
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.activation.MimeTypeParseException;
import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.Component;
import org.codice.ddf.catalog.content.monitor.configurators.KeystoreTruststoreConfigurator;
import org.codice.ddf.catalog.content.monitor.features.CamelFeatures;
import org.codice.ddf.catalog.content.monitor.features.CxfFeatures;
import org.codice.ddf.catalog.content.monitor.features.KarafSpringFeatures;
import org.codice.ddf.catalog.content.monitor.util.BundleInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.stubbing.Answer;
import org.ops4j.io.FileUtils;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ddf.catalog.CatalogFramework;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.security.service.SecurityManager;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class ContentDirectoryMonitorIT extends AbstractComponentTest {

    @Inject
    private CamelContext camelContext;

    @Inject
    private ContentDirectoryMonitor contentDirectoryMonitor;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private String directoryPath;

    private CatalogFramework catalogFramework;

    @Before
    public void setup() throws IOException, SourceUnavailableException {
        directoryPath = temporaryFolder.getRoot()
                .getCanonicalPath();

        SecurityManager securityManager = mock(SecurityManager.class);
        registerService(securityManager, SecurityManager.class);

        catalogFramework = mockCatalogFramework();
        registerService(catalogFramework, CatalogFramework.class);
    }

    @Test
    public void testCamelContext() {
        Component component = camelContext.getComponent("content");
        assertThat(component, notNullValue());
    }

    @Test
    public void testInPlaceMonitoring()
            throws IOException, InterruptedException, SourceUnavailableException, IngestException,
            MimeTypeParseException {
        updateContentDirectoryMonitor(directoryPath, ContentDirectoryMonitor.IN_PLACE);

        File file = createTestFile(directoryPath);
        ContentItem result = getContentItem();

        assertContentItem(result, file);
        assertThat(file.exists(), is(true));
    }

    @Test
    public void testMoveMonitoring()
            throws IOException, SourceUnavailableException, IngestException,
            MimeTypeParseException {
        updateContentDirectoryMonitor(directoryPath, ContentDirectoryMonitor.MOVE);
        File file = createTestFile(directoryPath);
        File movedFile = Paths.get(directoryPath, ".ingested", file.getName())
                .toFile();

        ContentItem result = getContentItem();

        assertContentItem(result, movedFile);
        assertThat(file.exists(), is(false));
        assertThat(movedFile.exists(), is(true));
    }

    @Test
    public void testDeleteMonitoring()
            throws IOException, SourceUnavailableException, IngestException,
            MimeTypeParseException {
        updateContentDirectoryMonitor(directoryPath, ContentDirectoryMonitor.DELETE);

        File file = createTestFile(directoryPath);
        File directory = Paths.get(directoryPath)
                .toFile();

        await("file deleted").atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .until(() -> !file.exists());
        assertThat(directory.list().length, is(0));
    }

    @Override
    protected Option setupDistribution() {
        return composite(KarafSpringFeatures.start("spring"),
                CamelFeatures.start("camel"),
                CxfFeatures.start("cxf-rt-security-saml",
                        "cxf-bindings-soap",
                        "cxf-ws-policy",
                        "cxf-ws-security",
                        "cxf-frontend-javascript",
                        "cxf-jaxrs"),
                keystoreAndTruststoreConfig(),
                initContentDirectoryMonitorConfig());
    }

    @Override
    protected List<BundleInfo> bundlesToStart() {
        List<BundleInfo> bundles = new ArrayList<>();
        bundles.addAll(testDependencies());
        bundles.addAll(contentDirectoryMonitorDependencies());
        return bundles;
    }

    private List<BundleInfo> testDependencies() {
        return Arrays.asList(new BundleInfo("org.awaitility", "awaitility"),
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
        return Arrays.asList(new BundleInfo("org.apache.commons", "commons-lang3"),
                new BundleInfo("commons-lang", "commons-lang"),
                new BundleInfo("commons-io", "commons-io"),
                new BundleInfo("commons-collections", "commons-collections"),
                new BundleInfo("commons-configuration", "commons-configuration"),

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
                new BundleInfo("ddf.distribution", "branding-api"),
                new BundleInfo("ddf.distribution", "ddf-branding-plugin"),
                new BundleInfo("ddf.platform", "platform-configuration"),
                new BundleInfo("ddf.security.handler", "security-handler-api"),

                new BundleInfo("ddf.mime.core", "mime-core-api"),
                new BundleInfo("ddf.mime.core", "mime-core-impl"),
                new BundleInfo("ddf.catalog.core", "catalog-core-camelcomponent"));
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
        Files.write(file.toPath(), Collections.singletonList("Hello, World"));

        await("create storage request created").atMost(TIMEOUT_IN_SECONDS, TimeUnit.SECONDS)
                .until(created::get);
        return file;
    }

    private void updateContentDirectoryMonitor(String directoryPath, String processingMechanism) {
        Map<String, Object> properties = new HashMap<>();

        properties.put("monitoredDirectoryPath", directoryPath);
        properties.put("numThreads", 1);
        properties.put("readLockIntervalMilliseconds", 500);
        properties.put("processingMechanism", processingMechanism);

        contentDirectoryMonitor.updateCallback(properties);
    }

    private ContentItem getContentItem() throws SourceUnavailableException, IngestException {
        ArgumentCaptor<CreateStorageRequest> createStorageRequest = ArgumentCaptor.forClass(
                CreateStorageRequest.class);
        verify(catalogFramework).create(createStorageRequest.capture());
        return createStorageRequest.getValue()
                .getContentItems()
                .get(0);
    }

    private void assertContentItem(ContentItem item, File file)
            throws IOException, MimeTypeParseException {
        assertThat(item.getFilename(), is(file.getName()));
        assertThat(item.getUri(), is("content:" + item.getId()));
        assertThat(item.getSize(), is(file.length()));
    }

    private CatalogFramework mockCatalogFramework() throws SourceUnavailableException {
        CatalogFramework catalogFramework = mock(CatalogFramework.class);

        SourceInfoResponse sourceInfoResponse = mockSourceInfoResponse();
        when(catalogFramework.getSourceInfo(anyObject())).thenReturn(sourceInfoResponse);

        return catalogFramework;
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
}

