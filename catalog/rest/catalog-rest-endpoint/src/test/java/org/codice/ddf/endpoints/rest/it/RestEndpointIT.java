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
package org.codice.ddf.endpoints.rest.it;

import static io.restassured.RestAssured.when;
import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.TEN_SECONDS;
import static org.awaitility.Duration.TWO_HUNDRED_MILLISECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;

import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolver;
import ddf.mime.MimeTypeToTransformerMapper;
import io.restassured.RestAssured;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import javax.inject.Inject;
import org.codice.ddf.attachment.AttachmentParser;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.codice.ddf.test.common.AbstractComponentTest;
import org.codice.ddf.test.common.UrlBuilder;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.codice.ddf.test.common.annotations.MockOsgiService;
import org.codice.ddf.test.common.annotations.MockOsgiService.Property;
import org.codice.ddf.test.common.annotations.PaxExamRule;
import org.codice.ddf.test.common.configurators.ApplicationOptions;
import org.codice.ddf.test.common.configurators.BundleOptionBuilder.BundleOption;
import org.codice.ddf.test.common.configurators.DdfComponentOptions;
import org.codice.ddf.test.common.configurators.FeatureOptionBuilder.FeatureOption;
import org.codice.ddf.test.common.configurators.PortFinder;
import org.codice.ddf.test.common.options.TestResourcesOptions;
import org.codice.ddf.test.common.rules.ServiceRegistrationRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.opengis.filter.Filter;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.KarafDistributionOption;
import org.ops4j.pax.exam.options.BootClasspathLibraryOption;
import org.ops4j.pax.exam.options.ModifiableCompositeOption;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class RestEndpointIT extends AbstractComponentTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(RestEndpointIT.class);

  private static UrlBuilder restEndpointUrlBuilder;

  @Rule public PaxExamRule paxExamRule = new PaxExamRule(this);

  @Rule
  public final ServiceRegistrationRule serviceRegistrationRule = new ServiceRegistrationRule();

  @Inject ConfigurationAdmin configAdmin;

  @Inject private BundleContext bundleContext;

  @MockOsgiService private CatalogFramework catalogFramework;

  @MockOsgiService private AttachmentParser attachmentParser;

  @MockOsgiService private MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

  @MockOsgiService private AttributeRegistry attributeRegistry;

  @MockOsgiService(answer = Answers.RETURNS_DEEP_STUBS)
  private FilterBuilder filterBuilder;

  @MockOsgiService(properties = {@Property(key = "name", value = "tikaMimeTypeResolver")})
  private MimeTypeResolver mimeTypeResolver;

  @MockOsgiService private MimeTypeMapper mimeTypeMapper;

  @MockOsgiService(properties = {@Property(key = "id", value = "uuidGenerator")})
  private UuidGenerator uuidGenerator;

  @BeforeExam
  public void setupClass() {
    LOGGER.trace("setupClass");

    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

    restEndpointUrlBuilder =
        UrlBuilder.fromSystemProperties(
                SystemBaseUrl.EXTERNAL_HOST,
                SystemBaseUrl.EXTERNAL_HTTP_PORT,
                SystemBaseUrl.EXTERNAL_HTTPS_PORT,
                SystemBaseUrl.INTERNAL_ROOT_CONTEXT)
            .add("/catalog");
  }

  @Before
  public void setup() {
    LOGGER.trace("setup");

    waitForAllBundlesActive();
    waitForRestEndpoint();
  }

  @Test
  public void testGetWithMetacardIdOnly() throws Exception {
    String metacardId = "123";
    String contentType = "text/plain";
    String contentText = "Content";
    byte[] contentBytes = contentText.getBytes();

    Filter filter = mock(Filter.class);
    QueryResponse queryResponse = mock(QueryResponse.class);
    Result result = mock(Result.class);
    Metacard metacard = mock(Metacard.class);
    BinaryContent content = mock(BinaryContent.class);

    given(filterBuilder.attribute(Metacard.ID).is().equalTo().text(metacardId)).willReturn(filter);
    given(catalogFramework.query(any(QueryRequest.class), any())).willReturn(queryResponse);
    given(queryResponse.getResults()).willReturn(ImmutableList.of(result));
    given(result.getMetacard()).willReturn(metacard);
    given(catalogFramework.transform(same(metacard), eq("xml"), any(Map.class)))
        .willReturn(content);
    given(content.getInputStream()).willReturn(new ByteArrayInputStream(contentBytes));
    given(content.getMimeTypeValue()).willReturn(contentType);
    given(content.getSize()).willReturn((long) contentBytes.length);

    when()
        .get(restEndpointUrlBuilder.add(metacardId).build())
        .then()
        .assertThat()
        .statusCode(200)
        .header(HttpHeaders.CONTENT_LENGTH, is(String.valueOf(contentBytes.length)))
        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
        .contentType(contentType)
        .body(is(contentText));
  }

  @Test
  public void testGetInvalidMetacardId() throws Exception {
    String metacardId = "123";

    Filter filter = mock(Filter.class);
    QueryResponse queryResponse = mock(QueryResponse.class);

    given(filterBuilder.attribute(Metacard.ID).is().equalTo().text(metacardId)).willReturn(filter);
    given(catalogFramework.query(any(QueryRequest.class), any())).willReturn(queryResponse);
    given(queryResponse.getResults()).willReturn(ImmutableList.of());

    when()
        .get(restEndpointUrlBuilder.add(metacardId).build())
        .then()
        .assertThat()
        .statusCode(404)
        .body(containsString("Unable to retrieve requested metacard."));
  }

  @Override
  protected ApplicationOptions getApplicationOptions(PortFinder portFinder) {
    return new DdfComponentOptions(portFinder) {

      @Override
      protected BundleOption getBundleOptions() {
        return super.getBundleOptions()
            .add("org.bouncycastle", "bcprov-jdk15on")
            .add("ddf.catalog.transformer", "catalog-transformer-attribute")
            .add("ddf.catalog.core", "catalog-core-attachment")
            .add("ddf.catalog.rest", "catalog-rest-api")
            .add("ddf.catalog.rest", "catalog-rest-service-impl")
            .add("ddf.catalog.rest", "catalog-rest-impl");
      }

      @Override
      protected FeatureOption getFeatureOptions() {
        final String[] cxfFeatures = {"cxf", "cxf-commands"};
        final String[] utilitiesFeatures = {"action-core-impl", "platform-util"};
        final String[] kernelFeatures = {"kernel", "apache-commons", "guava"};

        return super.getFeatureOptions()
            .addFeatures("org.apache.cxf.karaf", "apache-cxf", cxfFeatures)
            .addFeatures("ddf.features", "utilities", utilitiesFeatures)
            .addFeatures("ddf.features", "kernel", kernelFeatures)
            .addFeatureFrom("ddf.features", "test-utilities", "features", "rest-assured")
            .addFeatureFrom(
                "ddf.platform.util", "util-uuidgenerator-api", "feature", "uuidgenerator-api")
            .addFeatureFrom("ddf.mime.core", "mime-core-api", "feature", "mime-core-api-only")
            .addFeatureFrom(
                "ddf.catalog.core", "catalog-core-api", "feature", "catalog-core-api-only");
      }

      @Override
      public Option get() {
        // Add activation and annotation bundles and expose them via the system bundle. This is
        // the same thing that the DDF kernel does, but this test runs on the base Karaf distro, so
        // we don't have those changes
        ModifiableCompositeOption options =
            CoreOptions.composite(
                super.get(),
                CoreOptions.bootClasspathLibraries(
                    new BootClasspathLibraryOption(
                        CoreOptions.maven("jakarta.activation", "jakarta.activation-api")
                            .versionAsInProject()),
                    new BootClasspathLibraryOption(
                        CoreOptions.maven("jakarta.annotation", "jakarta.annotation-api")
                            .versionAsInProject()),
                    new BootClasspathLibraryOption(
                        CoreOptions.maven("com.google.code.findbugs", "jsr305")
                            .versionAsInProject())));
        options.add(
            KarafDistributionOption.editConfigurationFilePut(
                "etc/custom.properties",
                new File(TestResourcesOptions.getTestResource("/custom.properties"))));
        // Add JAXB bundles. They must be installed before Karaf's org.apache.karaf.features.core
        // bundle (which gets installed via startup.properties), hence the low start level.
        options.add(
            mavenBundle("org.apache.servicemix.specs", "org.apache.servicemix.specs.jaxb-api-2.3")
                .versionAsInProject()
                .startLevel(13),
            mavenBundle(
                    "org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxb-runtime")
                .versionAsInProject()
                .startLevel(14));
        return options;
      }
    };
  }

  private void waitForRestEndpoint() {
    LOGGER.trace("waitForRestEndpoint");
    with()
        .pollInterval(FIVE_HUNDRED_MILLISECONDS)
        .await("REST Endpoint Available")
        .atMost(TEN_SECONDS)
        .until(
            () -> {
              try {
                when().head(restEndpointUrlBuilder.build()).then().assertThat().statusCode(200);
              } catch (Throwable e) {
                return false;
              }

              return true;
            });
  }

  // TODO (DDF-3547) - Change to make sure all the bundles of the feature under test are active
  // and move to common class.
  private void waitForAllBundlesActive() {
    LOGGER.trace("waitForAllBundlesActive");
    with()
        .pollInterval(TWO_HUNDRED_MILLISECONDS)
        .await("All bundles active")
        .atMost(TEN_SECONDS)
        .until(
            () ->
                Arrays.stream(bundleContext.getBundles())
                    .allMatch(
                        b -> (b.getState() == Bundle.ACTIVE) || (b.getState() == Bundle.RESOLVED)));
  }
}
