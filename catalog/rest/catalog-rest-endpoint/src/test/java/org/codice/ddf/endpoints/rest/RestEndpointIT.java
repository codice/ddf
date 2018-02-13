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
package org.codice.ddf.endpoints.rest;

import static com.jayway.restassured.RestAssured.when;
import static org.awaitility.Awaitility.with;
import static org.awaitility.Duration.FIVE_HUNDRED_MILLISECONDS;
import static org.awaitility.Duration.TEN_SECONDS;
import static org.awaitility.Duration.TWO_HUNDRED_MILLISECONDS;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;
import com.google.common.net.HttpHeaders;
import com.jayway.restassured.RestAssured;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolver;
import ddf.mime.MimeTypeToTransformerMapper;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Map;
import javax.inject.Inject;
import org.codice.ddf.catalog.transform.Transform;
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
import org.codice.ddf.test.common.rules.ServiceRegistrationRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.opengis.filter.Filter;
import org.ops4j.pax.exam.junit.PaxExam;
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

  @MockOsgiService private Transform transform;

  @MockOsgiService private CatalogFramework catalogFramework;

  @MockOsgiService private MimeTypeToTransformerMapper mimeTypeToTransformerMapper;

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
                SystemBaseUrl.HOST,
                SystemBaseUrl.HTTP_PORT,
                SystemBaseUrl.HTTPS_PORT,
                SystemBaseUrl.ROOT_CONTEXT)
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
    given(catalogFramework.query(any(QueryRequest.class), any(FederationStrategy.class)))
        .willReturn(queryResponse);
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
    given(catalogFramework.query(any(QueryRequest.class), any(FederationStrategy.class)))
        .willReturn(queryResponse);
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
            .add("ddf.catalog.transformer", "catalog-transformer-attribute");
      }

      @Override
      protected FeatureOption getFeatureOptions() {
        final String[] springFeatures = {
          "spring-instrument", "spring-jms", "spring-test", "spring-web"
        };

        final String[] cxfFeatures = {"cxf", "cxf-commands"};
        final String[] platformFeatures = {"action-core-impl", "commons"};

        return super.getFeatureOptions()
            .addFeatures("org.apache.karaf.features", "spring", springFeatures)
            .addFeatures("org.apache.cxf.karaf", "apache-cxf", cxfFeatures)
            .addFeatures("ddf.platform", "platform-app", platformFeatures)
            .addFeatureFrom("ddf.thirdparty", "rest-assured", "feature", "rest-assured")
            .addFeatureFrom(
                "ddf.platform.util", "util-uuidgenerator-api", "feature", "uuidgenerator-api")
            .addFeatureFrom("ddf.mime.core", "mime-core-api", "feature", "mime-core-api-only")
            .addFeatureFrom(
                "ddf.catalog.core", "catalog-core-api", "feature", "catalog-core-api-only")
            .addFeatureFrom(
                "ddf.catalog.core",
                "catalog-core-transform-api",
                "feature",
                "catalog-core-transform-api-only");
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
                LOGGER.info("endpoint not up", e);
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
