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
package ddf.catalog.impl;

import static java.time.temporal.ChronoUnit.DAYS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import com.google.common.io.ByteSource;
import ddf.catalog.CatalogFramework;
import ddf.catalog.Constants;
import ddf.catalog.cache.impl.ResourceCacheImpl;
import ddf.catalog.cache.solr.impl.ValidationQueryFactory;
import ddf.catalog.content.StorageProvider;
import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.data.impl.ContentItemImpl;
import ddf.catalog.content.impl.MockMemoryStorageProvider;
import ddf.catalog.content.operation.UpdateStorageRequest;
import ddf.catalog.content.operation.impl.CreateStorageRequestImpl;
import ddf.catalog.content.operation.impl.UpdateStorageRequestImpl;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeInjector;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.DefaultAttributeValueRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.defaultvalues.DefaultAttributeValueRegistryImpl;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeRegistryImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.inject.AttributeInjectorImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.adapter.GeotoolsFilterAdapterImpl;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.history.Historian;
import ddf.catalog.impl.operations.CreateOperations;
import ddf.catalog.impl.operations.DeleteOperations;
import ddf.catalog.impl.operations.OperationsCatalogStoreSupport;
import ddf.catalog.impl.operations.OperationsMetacardSupport;
import ddf.catalog.impl.operations.OperationsSecuritySupport;
import ddf.catalog.impl.operations.OperationsStorageSupport;
import ddf.catalog.impl.operations.QueryOperations;
import ddf.catalog.impl.operations.RemoteDeleteOperations;
import ddf.catalog.impl.operations.ResourceOperations;
import ddf.catalog.impl.operations.SourceOperations;
import ddf.catalog.impl.operations.TransformOperations;
import ddf.catalog.impl.operations.UpdateOperations;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.operation.impl.SourceInfoRequestSources;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostIngestPlugin;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.PostResourcePlugin;
import ddf.catalog.plugin.PreQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.CatalogStore;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.Source;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.util.impl.CachedSource;
import ddf.catalog.util.impl.SourcePoller;
import ddf.catalog.util.impl.SourcePollerRunner;
import ddf.mime.MimeTypeResolver;
import ddf.mime.mapper.MimeTypeMapperImpl;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.permission.KeyValueCollectionPermission;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.activation.MimeType;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.catalog.transform.Transform;
import org.codice.ddf.catalog.transform.TransformResponse;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;
import org.geotools.filter.FilterFactoryImpl;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.rules.TestWatchman;
import org.junit.runners.model.FrameworkMethod;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"unchecked", "JavaDoc"})
public class CatalogFrameworkImplTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(CatalogFrameworkImplTest.class);

  private static final String CUSTOM_METACARD_TYPE_NAME = "custom";

  private static final String DEFAULT_TITLE = "Default Title";

  private static final String DEFAULT_TITLE_CUSTOM = "Custom Title";

  private static final Date DEFAULT_EXPIRATION = Date.from(Instant.now().minus(1, DAYS));

  private static final Date DEFAULT_EXPIRATION_CUSTOM = Date.from(Instant.now().minus(2, DAYS));

  private static final String NO_TRANSFORMER_ID = "NONE";

  CatalogFrameworkImpl framework;

  CatalogFrameworkImpl resourceFramework;

  MockMemoryProvider provider;

  MockMemoryStorageProvider storageProvider;

  MockEventProcessor eventAdmin;

  ResourceRequest mockResourceRequest;

  ResourceResponse mockResourceResponse;

  PostResourcePlugin mockPostResourcePlugin;

  ArgumentCaptor<ResourceResponse> argument;

  List<FederatedSource> federatedSources;

  DefaultAttributeValueRegistry defaultAttributeValueRegistry;

  AttributeInjector attributeInjector;

  FederationStrategy mockFederationStrategy;

  DeleteOperations deleteOperations;

  RemoteDeleteOperations mockRemoteDeleteOperations;

  UuidGenerator uuidGenerator;

  @Rule
  public MethodRule watchman =
      new TestWatchman() {
        public void starting(FrameworkMethod method) {
          LOGGER.debug(
              "***************************  STARTING: {}  **************************\n",
              method.getName());
        }

        public void finished(FrameworkMethod method) {
          LOGGER.debug(
              "***************************  END: {}  **************************\n",
              method.getName());
        }
      };

  @Before
  public void setup()
      throws StopProcessingException, PluginExecutionException, URISyntaxException,
          FederationException, IOException, CatalogTransformerException, InterruptedException,
          MetacardCreationException {
    System.setProperty(
        "bad.files",
        "crossdomain.xml,clientaccesspolicy.xml,.htaccess,.htpasswd,hosts,passwd,group,resolv.conf,nfs.conf,ftpd.conf,ntp.conf,web.config,robots.txt");
    System.setProperty(
        "bad.file.extensions",
        ".exe,.jsp,.html,.js,.php,.phtml,.php3,.php4,.php5,.phps,.shtml,.jhtml,.pl,.py,.cgi,.msi,.com,.scr,.gadget,.application,.pif,.hta,.cpl,.msc,.jar,.kar,.bat,.cmd,.vb,.vbs,.vbe,.jse,.ws,.wsf,.wsc,.wsh,.ps1,.ps1xml,.ps2,.ps2xml,.psc1,.psc2,.msh,.msh1,.msh2,.mshxml,.msh1xml,.msh2xml,.scf,.lnk,.inf,.reg,.dll,.vxd,.cpl,.cfg,.config,.crt,.cert,.pem,.jks,.p12,.p7b,.key,.der,.csr,.jsb,.mhtml,.mht,.xhtml,.xht");
    System.setProperty(
        "bad.mime.types",
        "text/html,text/javascript,text/x-javascript,application/x-shellscript,text/scriptlet,application/x-msdownload,application/x-msmetafile");
    System.setProperty("ignore.files", ".DS_Store,Thumbs.db");

    // Setup
    /*
     * Prepare to capture the ResourceResponse argument passed into
     * PostResourcePlugin.process(). We will verify that it contains a non-null ResourceRequest
     * in the verification section of this test.
     */
    argument = ArgumentCaptor.forClass(ResourceResponse.class);

    Resource mockResource = mock(Resource.class);

    mockResourceRequest = mock(ResourceRequest.class);
    when(mockResourceRequest.getAttributeValue()).thenReturn(new URI("myURI"));
    when(mockResourceRequest.getAttributeName()).thenReturn(new String("myName"));

    mockResourceResponse = mock(ResourceResponse.class);
    when(mockResourceResponse.getRequest()).thenReturn(mockResourceRequest);
    when(mockResourceResponse.getResource()).thenReturn(mockResource);

    mockPostResourcePlugin = mock(PostResourcePlugin.class);
    /*
     * We verify (see verification section of test) that PostResourcePlugin.process() receives a
     * ResourceResponse with a non-null ResourceRequest. We assume that it works correctly and
     * returns a ResourceResponse with a non-null ResourceRequest, so we return our
     * mockResouceResponse that contains a non-null ResourceRequest.
     */
    when(mockPostResourcePlugin.process(isA(ResourceResponse.class)))
        .thenReturn(mockResourceResponse);

    List<PostResourcePlugin> mockPostResourcePlugins = new ArrayList<>();
    mockPostResourcePlugins.add(mockPostResourcePlugin);

    eventAdmin = new MockEventProcessor();
    provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());

    storageProvider = new MockMemoryStorageProvider();

    // Mock register the provider in the container
    // Mock the source poller
    SourcePoller mockPoller = mock(SourcePoller.class);
    when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

    ArrayList<PostIngestPlugin> postIngestPlugins = new ArrayList<>();
    postIngestPlugins.add(eventAdmin);

    mockFederationStrategy = mock(FederationStrategy.class);
    Result mockFederationResult = mock(Result.class);
    when(mockFederationResult.getMetacard()).thenReturn(new MetacardImpl());
    QueryRequest mockQueryRequest = mock(QueryRequest.class);
    Query mockQuery = mock(Query.class);
    when(mockQuery.getTimeoutMillis()).thenReturn(1L);
    when(mockQueryRequest.getQuery()).thenReturn(mockQuery);
    QueryResponseImpl queryResponse =
        new QueryResponseImpl(mockQueryRequest, Collections.singletonList(mockFederationResult), 1);
    when(mockFederationStrategy.federate(anyList(), anyObject())).thenReturn(queryResponse);

    federatedSources = createDefaultFederatedSourceList(true);

    MimeTypeResolver mimeTypeResolver = mock(MimeTypeResolver.class);

    Transform transform = mock(Transform.class);

    TransformResponse transformResponse = mock(TransformResponse.class);

    when(transform.transform(
            any(MimeType.class),
            any(String.class),
            any(Supplier.class),
            any(String.class),
            any(File.class),
            any(String.class),
            any(Map.class)))
        .thenAnswer(
            invocationOnMock -> {
              String id = (String) invocationOnMock.getArguments()[1];
              MetacardImpl metacard = new MetacardImpl();
              metacard.setId(id);
              when(transformResponse.getParentMetacard()).thenReturn(Optional.of(metacard));
              return transformResponse;
            });

    mockRemoteDeleteOperations = mock(RemoteDeleteOperations.class);

    FrameworkProperties frameworkProperties = new FrameworkProperties();
    frameworkProperties.setAccessPlugins(new ArrayList<>());
    frameworkProperties.setPolicyPlugins(new ArrayList<>());
    frameworkProperties.setSourcePoller(mockPoller);
    frameworkProperties.setCatalogProviders(Collections.singletonList((CatalogProvider) provider));
    frameworkProperties.setPostResource(mockPostResourcePlugins);
    frameworkProperties.setFederationStrategy(mockFederationStrategy);
    frameworkProperties.setFilterBuilder(new GeotoolsFilterBuilder());
    frameworkProperties.setPreIngest(new ArrayList<>());
    frameworkProperties.setPostIngest(postIngestPlugins);
    frameworkProperties.setPreQuery(new ArrayList<>());
    frameworkProperties.setPostQuery(new ArrayList<>());
    frameworkProperties.setPreResource(new ArrayList<>());
    frameworkProperties.setPostResource(new ArrayList<>());
    frameworkProperties.setQueryResponsePostProcessor(mock(QueryResponsePostProcessor.class));
    frameworkProperties.setStorageProviders(Collections.singletonList(storageProvider));
    frameworkProperties.setMimeTypeMapper(
        new MimeTypeMapperImpl(Collections.singletonList(mimeTypeResolver)));
    frameworkProperties.setTransform(transform);
    frameworkProperties.setValidationQueryFactory(
        new ValidationQueryFactory(new GeotoolsFilterAdapterImpl(), new GeotoolsFilterBuilder()));

    Map<String, FederatedSource> federatedSourceMap = new HashMap<>();
    if (federatedSources != null) {
      for (FederatedSource source : federatedSources) {
        federatedSourceMap.put(source.getId(), source);
      }
    }
    SourcePollerRunner runner = new SourcePollerRunner();
    SourcePoller poller = new SourcePoller(runner);
    for (FederatedSource source : federatedSources) {
      runner.bind(source);
    }
    runner.bind(provider);
    int wait = 0;
    while (wait < 5) {
      for (FederatedSource source : federatedSources) {
        CachedSource cachedSource = poller.getCachedSource(source);
        if (cachedSource == null || !cachedSource.isAvailable()) {
          Thread.sleep(100);
          wait++;
          break;
        }
      }
      CachedSource cachedProvider = poller.getCachedSource(provider);
      if (cachedProvider == null || !cachedProvider.isAvailable()) {
        Thread.sleep(100);
      }
      wait++;
    }
    frameworkProperties.setSourcePoller(poller);
    frameworkProperties.setFederatedSources(federatedSourceMap);

    defaultAttributeValueRegistry = new DefaultAttributeValueRegistryImpl();
    frameworkProperties.setDefaultAttributeValueRegistry(defaultAttributeValueRegistry);

    attributeInjector = spy(new AttributeInjectorImpl(new AttributeRegistryImpl()));
    frameworkProperties.setAttributeInjectors(Collections.singletonList(attributeInjector));

    uuidGenerator = mock(UuidGenerator.class);
    when(uuidGenerator.generateUuid()).thenReturn(UUID.randomUUID().toString());

    OperationsSecuritySupport opsSecurity = new OperationsSecuritySupport();
    OperationsMetacardSupport opsMetacard = new OperationsMetacardSupport(frameworkProperties);
    SourceOperations sourceOperations = new SourceOperations(frameworkProperties);
    TransformOperations transformOperations = new TransformOperations(frameworkProperties);
    Historian historian = new Historian();
    historian.setHistoryEnabled(false);

    QueryOperations queryOperations =
        new QueryOperations(frameworkProperties, sourceOperations, opsSecurity, opsMetacard);
    OperationsStorageSupport opsStorage =
        new OperationsStorageSupport(sourceOperations, queryOperations);
    opsStorage.setHistorian(historian);

    OperationsCatalogStoreSupport opsCatStore =
        new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations);
    CreateOperations createOperations =
        new CreateOperations(
            frameworkProperties,
            queryOperations,
            sourceOperations,
            opsSecurity,
            opsMetacard,
            opsCatStore,
            opsStorage);
    UpdateOperations updateOperations =
        new UpdateOperations(
            frameworkProperties,
            queryOperations,
            sourceOperations,
            opsSecurity,
            opsMetacard,
            opsCatStore,
            opsStorage);
    deleteOperations =
        new DeleteOperations(
            frameworkProperties, queryOperations, sourceOperations, opsSecurity, opsMetacard);

    deleteOperations.setOpsCatStoreSupport(opsCatStore);

    ResourceOperations resOps =
        new ResourceOperations(frameworkProperties, queryOperations, opsSecurity) {
          @Override
          protected ResourceInfo getResourceInfo(
              ResourceRequest resourceRequest,
              String site,
              boolean isEnterprise,
              StringBuilder federatedSite,
              Map<String, Serializable> requestProperties,
              boolean fanoutEnabled)
              throws ResourceNotSupportedException, ResourceNotFoundException {
            URI uri = null;
            Metacard metacard = new MetacardImpl();

            try {
              uri = new URI("myURI");
            } catch (URISyntaxException e) {
            }

            return new ResourceInfo(metacard, uri);
          }
        };

    updateOperations.setHistorian(historian);
    deleteOperations.setHistorian(historian);

    framework =
        new CatalogFrameworkImpl(
            createOperations,
            updateOperations,
            deleteOperations,
            queryOperations,
            resOps,
            sourceOperations,
            transformOperations);
    // Conditionally bind objects if framework properties are setup
    if (!CollectionUtils.isEmpty(frameworkProperties.getCatalogProviders())) {
      sourceOperations.bind(provider);
    }
    sourceOperations.bind(storageProvider);

    resourceFramework =
        new CatalogFrameworkImpl(
            createOperations,
            updateOperations,
            deleteOperations,
            queryOperations,
            resOps,
            sourceOperations,
            transformOperations);
    // Conditionally bind objects if framework properties are setup
    if (!CollectionUtils.isEmpty(frameworkProperties.getCatalogProviders())) {
      sourceOperations.bind(provider);
    }
    sourceOperations.bind(storageProvider);

    ThreadContext.bind(mock(Subject.class));
  }

  // Start testing MetacardWriter

  /** Tests that the framework properly passes a create request to the local provider. */
  @Test
  public void testCreate() throws Exception {
    List<Metacard> metacards = new ArrayList<>();

    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);
    metacards.add(newCard);

    CreateResponse response = framework.create(new CreateRequestImpl(metacards, null));
    assertEquals(response.getCreatedMetacards().size(), provider.size());
    for (Metacard curCard : response.getCreatedMetacards()) {
      assertNotNull(curCard.getId());
    }

    // make sure that the event was posted correctly
    assertTrue(eventAdmin.wasEventPosted());
    Metacard[] array = {};
    array = response.getCreatedMetacards().toArray(array);
    assertTrue(eventAdmin.wasEventPosted());
    assertEquals(eventAdmin.getLastEvent(), array[array.length - 1]);
  }

  @Test
  public void testInjectsAttributesOnCreate() throws Exception {
    final String title = "Create";
    final String injectAttributeName = "new attribute";
    final double injectAttributeValue = 2;
    final MetacardImpl originalMetacard = new MetacardImpl();
    originalMetacard.setTitle(title);
    originalMetacard.setAttribute(injectAttributeName, injectAttributeValue);
    final List<Metacard> metacards = Collections.singletonList(originalMetacard);
    final CreateRequest request = new CreateRequestImpl(metacards, null);

    final AttributeDescriptor injectAttribute =
        new AttributeDescriptorImpl(
            injectAttributeName, true, true, false, false, BasicTypes.DOUBLE_TYPE);
    stubMetacardInjection(injectAttribute);

    final CreateResponse response = framework.create(request);

    final Metacard createdMetacard = response.getCreatedMetacards().get(0);
    final MetacardType createdMetacardType = createdMetacard.getMetacardType();
    final MetacardType originalMetacardType = originalMetacard.getMetacardType();
    assertThat(createdMetacardType.getName(), is(originalMetacardType.getName()));

    final Set<AttributeDescriptor> expectedAttributeDescriptors =
        new HashSet<>(originalMetacardType.getAttributeDescriptors());
    expectedAttributeDescriptors.add(injectAttribute);
    assertThat(createdMetacardType.getAttributeDescriptors(), is(expectedAttributeDescriptors));

    assertThat(createdMetacard.getTitle(), is(title));
    assertThat(
        createdMetacard.getAttribute(injectAttributeName).getValue(), is(injectAttributeValue));
  }

  private void registerDefaults() {
    defaultAttributeValueRegistry.setDefaultValue(Metacard.TITLE, DEFAULT_TITLE);
    defaultAttributeValueRegistry.setDefaultValue(
        CUSTOM_METACARD_TYPE_NAME, Metacard.TITLE, DEFAULT_TITLE_CUSTOM);
    defaultAttributeValueRegistry.setDefaultValue(Metacard.EXPIRATION, DEFAULT_EXPIRATION);
    defaultAttributeValueRegistry.setDefaultValue(
        CUSTOM_METACARD_TYPE_NAME, Metacard.EXPIRATION, DEFAULT_EXPIRATION_CUSTOM);
  }

  private List<Metacard> getMetacards(String title, Date expiration) {
    List<Metacard> metacards = new ArrayList<>();

    MetacardImpl basicMetacardHasBoth = new MetacardImpl();
    basicMetacardHasBoth.setId("1");
    basicMetacardHasBoth.setTitle(title);
    basicMetacardHasBoth.setExpirationDate(expiration);
    metacards.add(basicMetacardHasBoth);

    MetacardImpl basicMetacardHasTitle = new MetacardImpl();
    basicMetacardHasTitle.setId("2");
    basicMetacardHasTitle.setTitle(title);
    metacards.add(basicMetacardHasTitle);

    MetacardImpl basicMetacardHasExpiration = new MetacardImpl();
    basicMetacardHasExpiration.setId("3");
    basicMetacardHasExpiration.setExpirationDate(expiration);
    metacards.add(basicMetacardHasExpiration);

    MetacardImpl basicMetacardHasNeither = new MetacardImpl();
    basicMetacardHasNeither.setId("4");
    metacards.add(basicMetacardHasNeither);

    MetacardType customMetacardType =
        new MetacardTypeImpl(
            CUSTOM_METACARD_TYPE_NAME, MetacardImpl.BASIC_METACARD.getAttributeDescriptors());
    MetacardImpl customMetacardHasNeither = new MetacardImpl(customMetacardType);
    customMetacardHasNeither.setId("5");
    metacards.add(customMetacardHasNeither);

    return metacards;
  }

  private void verifyDefaults(
      List<Metacard> metacards,
      String originalTitle,
      Date originalExpiration,
      String expectedDefaultTitle,
      Date expectedDefaultExpiration,
      String expectedDefaultTitleCustom,
      Date expectedDefaultDateCustom) {
    Metacard neitherDefault = metacards.get(0);
    assertThat(neitherDefault.getTitle(), is(originalTitle));
    assertThat(neitherDefault.getExpirationDate(), is(originalExpiration));

    Metacard expirationDefault = metacards.get(1);
    assertThat(expirationDefault.getTitle(), is(originalTitle));
    assertThat(expirationDefault.getExpirationDate(), is(expectedDefaultExpiration));

    Metacard titleDefault = metacards.get(2);
    assertThat(titleDefault.getTitle(), is(expectedDefaultTitle));
    assertThat(titleDefault.getExpirationDate(), is(originalExpiration));

    Metacard basicBothDefault = metacards.get(3);
    assertThat(basicBothDefault.getTitle(), is(expectedDefaultTitle));
    assertThat(basicBothDefault.getExpirationDate(), is(expectedDefaultExpiration));

    Metacard customBothDefault = metacards.get(4);
    assertThat(customBothDefault.getTitle(), is(expectedDefaultTitleCustom));
    assertThat(customBothDefault.getExpirationDate(), is(expectedDefaultDateCustom));
  }

  @Test
  public void testCreateWithDefaultValues() throws IngestException, SourceUnavailableException {
    registerDefaults();

    final String title = "some title";
    final Date expiration = new Date();
    CreateRequest createRequest = new CreateRequestImpl(getMetacards(title, expiration));
    CreateResponse createResponse = framework.create(createRequest);
    verifyDefaults(
        createResponse.getCreatedMetacards(),
        title,
        expiration,
        DEFAULT_TITLE,
        DEFAULT_EXPIRATION,
        DEFAULT_TITLE_CUSTOM,
        DEFAULT_EXPIRATION_CUSTOM);
  }

  /** Tests that the framework properly passes a create request to the local provider. */
  @Test
  public void testCreateStorage() throws Exception {
    List<ContentItem> contentItems = new ArrayList<>();

    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return new ByteArrayInputStream("blah".getBytes());
          }
        };
    ContentItemImpl newItem =
        new ContentItemImpl(
            uuidGenerator.generateUuid(),
            byteSource,
            "application/octet-stream",
            "blah",
            0L,
            newCard);
    contentItems.add(newItem);

    CreateResponse response = framework.create(new CreateStorageRequestImpl(contentItems, null));
    assertEquals(response.getCreatedMetacards().size(), provider.size());
    assertEquals(response.getCreatedMetacards().size(), storageProvider.size());
    for (Metacard curCard : response.getCreatedMetacards()) {
      assertNotNull(curCard.getId());
    }

    // make sure that the event was posted correctly
    assertTrue(eventAdmin.wasEventPosted());
    Metacard[] array = {};
    array = response.getCreatedMetacards().toArray(array);
    assertTrue(eventAdmin.wasEventPosted());
    assertEquals(eventAdmin.getLastEvent(), array[array.length - 1]);
  }

  /**
   * Tests that the framework properly passes a create request to the local provider with attribute
   * overrides.
   */
  @Test
  public void testCreateStorageWithAttributeOverrides() throws Exception {
    List<ContentItem> contentItems = new ArrayList<>();

    Map<String, Serializable> propertiesMap = new HashMap<>();
    HashMap<String, String> attributeMap = new HashMap<>();
    attributeMap.put(Metacard.TITLE, "test");
    attributeMap.put("foo", "bar");
    propertiesMap.put(Constants.ATTRIBUTE_OVERRIDES_KEY, attributeMap);

    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);

    MetacardType metacardType = mock(MetacardType.class);

    AttributeDescriptor stringAttributeDescriptor =
        new AttributeDescriptorImpl(
            Metacard.TITLE,
            true,
            true,
            true,
            true,
            new AttributeType<String>() {
              private static final long serialVersionUID = 1L;

              @Override
              public Class<String> getBinding() {
                return String.class;
              }

              @Override
              public AttributeFormat getAttributeFormat() {
                return AttributeFormat.STRING;
              }
            });

    when(metacardType.getAttributeDescriptor(Metacard.TITLE)).thenReturn(stringAttributeDescriptor);

    newCard.setType(metacardType);

    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return new ByteArrayInputStream("blah".getBytes());
          }
        };
    ContentItemImpl newItem =
        new ContentItemImpl(
            uuidGenerator.generateUuid(),
            byteSource,
            "application/octet-stream",
            "blah",
            0L,
            newCard);
    contentItems.add(newItem);

    CreateResponse response =
        framework.create(new CreateStorageRequestImpl(contentItems, propertiesMap));
    assertEquals(response.getCreatedMetacards().size(), provider.size());
    assertEquals(response.getCreatedMetacards().size(), storageProvider.size());
    for (Metacard curCard : response.getCreatedMetacards()) {
      assertNotNull(curCard.getId());
      // Assert valid attribute is set for the metacard
      assertThat(curCard.getTitle(), is("test"));
      // Assert invalid attribute is not set for the metacard
      assertThat(curCard.getAttribute("foo"), nullValue());
    }

    // Assert That Attribute Overrides do not exist after create
    assertThat(attributeMap.get(Constants.ATTRIBUTE_OVERRIDES_KEY), nullValue());
  }

  /**
   * Tests that the framework properly passes a create request to the local provider with attribute
   * overrides.
   */
  @Test
  public void testCreateStorageWithAttributeOverridesInvalidType() throws Exception {
    List<ContentItem> contentItems = new ArrayList<>();

    Map<String, Serializable> propertiesMap = new HashMap<>();
    HashMap<String, Object> attributeMap = new HashMap<>();
    attributeMap.put(Metacard.CREATED, "bad date");
    propertiesMap.put(Constants.ATTRIBUTE_OVERRIDES_KEY, attributeMap);

    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);

    MetacardType metacardType = mock(MetacardType.class);

    AttributeDescriptor dateAttributeDescriptor =
        new AttributeDescriptorImpl(
            Metacard.CREATED,
            true,
            true,
            true,
            true,
            new AttributeType<Date>() {
              private static final long serialVersionUID = 1L;

              @Override
              public Class<Date> getBinding() {
                return Date.class;
              }

              @Override
              public AttributeFormat getAttributeFormat() {
                return AttributeFormat.DATE;
              }
            });

    when(metacardType.getAttributeDescriptor(Metacard.TITLE)).thenReturn(dateAttributeDescriptor);

    newCard.setType(metacardType);

    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return new ByteArrayInputStream("blah".getBytes());
          }
        };
    ContentItemImpl newItem =
        new ContentItemImpl(
            uuidGenerator.generateUuid(),
            byteSource,
            "application/octet-stream",
            "blah",
            0L,
            newCard);
    contentItems.add(newItem);

    CreateResponse response =
        framework.create(new CreateStorageRequestImpl(contentItems, propertiesMap));
    assertEquals(response.getCreatedMetacards().size(), provider.size());
    assertEquals(response.getCreatedMetacards().size(), storageProvider.size());
    for (Metacard curCard : response.getCreatedMetacards()) {
      assertNotNull(curCard.getId());
      // Assert value is not set for invalid format
      assertThat(curCard.getCreatedDate(), nullValue());
    }
  }

  /** Tests that the framework properly passes an update request to the local provider. */
  @Test
  public void testUpdate() throws Exception {
    List<Metacard> metacards = new ArrayList<>();
    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);
    metacards.add(newCard);

    // create the entry manually in the provider
    CreateResponse response = provider.create(new CreateRequestImpl(metacards, null));

    Metacard insertedCard = response.getCreatedMetacards().get(0);

    Result mockFederationResult = mock(Result.class);
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId(insertedCard.getId());
    when(mockFederationResult.getMetacard()).thenReturn(metacard);

    QueryResponseImpl queryResponse =
        new QueryResponseImpl(
            mock(QueryRequest.class), Collections.singletonList(mockFederationResult), 1);
    when(mockFederationStrategy.federate(anyList(), anyObject())).thenReturn(queryResponse);

    List<Entry<Serializable, Metacard>> updatedEntries = new ArrayList<>();
    updatedEntries.add(new SimpleEntry<>(insertedCard.getId(), insertedCard));
    UpdateRequest request = new UpdateRequestImpl(updatedEntries, Metacard.ID, null);
    // send update to framework
    List<Update> returnedCards = framework.update(request).getUpdatedMetacards();
    for (Update curCard : returnedCards) {
      assertNotNull(curCard.getNewMetacard().getId());
    }

    // make sure that the event was posted correctly
    assertTrue(eventAdmin.wasEventPosted());
    assertEquals(
        eventAdmin.getLastEvent().getId(),
        returnedCards.get(returnedCards.size() - 1).getOldMetacard().getId());
  }

  @Test
  public void testUpdateWithDefaults() throws Exception {
    final String title = "some title";
    final Date expiration = new Date();
    List<Metacard> metacards = getMetacards(title, expiration);

    CreateRequest createRequest = new CreateRequestImpl(metacards);
    CreateResponse createResponse = framework.create(createRequest);

    verifyDefaults(createResponse.getCreatedMetacards(), title, expiration, null, null, null, null);

    registerDefaults();

    List<Result> mockFederationResults =
        metacards
            .stream()
            .map(
                m -> {
                  Result mockResult = mock(Result.class);
                  when(mockResult.getMetacard()).thenReturn(m);
                  return mockResult;
                })
            .collect(Collectors.toList());

    QueryResponseImpl queryResponse =
        new QueryResponseImpl(mock(QueryRequest.class), mockFederationResults, 1);
    when(mockFederationStrategy.federate(anyList(), anyObject())).thenReturn(queryResponse);

    UpdateRequest updateRequest =
        new UpdateRequestImpl(
            new String[] {"1", "2", "3", "4", "5"}, createResponse.getCreatedMetacards());
    UpdateResponse updateResponse = framework.update(updateRequest);

    List<Metacard> updatedMetacards =
        updateResponse
            .getUpdatedMetacards()
            .stream()
            .map(Update::getNewMetacard)
            .collect(Collectors.toList());
    verifyDefaults(
        updatedMetacards,
        title,
        expiration,
        DEFAULT_TITLE,
        DEFAULT_EXPIRATION,
        DEFAULT_TITLE_CUSTOM,
        DEFAULT_EXPIRATION_CUSTOM);
  }

  @Test
  public void testInjectsAttributesOnUpdate() throws Exception {
    final String injectAttributeName = "new attribute";
    final AttributeDescriptor injectAttribute =
        new AttributeDescriptorImpl(
            injectAttributeName, true, true, false, false, BasicTypes.DOUBLE_TYPE);
    stubMetacardInjection(injectAttribute);

    final String id =
        framework
            .create(new CreateRequestImpl(Collections.singletonList(new MetacardImpl()), null))
            .getCreatedMetacards()
            .get(0)
            .getId();

    final String title = "Update";
    final double injectAttributeValue = -1;
    final MetacardImpl metacard = new MetacardImpl();
    metacard.setId(id);
    metacard.setTitle(title);
    metacard.setAttribute(injectAttributeName, injectAttributeValue);
    final UpdateRequest request = new UpdateRequestImpl(id, metacard);

    List<Result> mockFederationResults =
        Stream.of(metacard)
            .map(
                m -> {
                  Result mockResult = mock(Result.class);
                  when(mockResult.getMetacard()).thenReturn(m);
                  return mockResult;
                })
            .collect(Collectors.toList());

    QueryResponseImpl queryResponse =
        new QueryResponseImpl(mock(QueryRequest.class), mockFederationResults, 1);
    when(mockFederationStrategy.federate(anyList(), anyObject())).thenReturn(queryResponse);

    final UpdateResponse response = framework.update(request);

    final Metacard updatedMetacard = response.getUpdatedMetacards().get(0).getNewMetacard();
    final MetacardType originalMetacardType = metacard.getMetacardType();
    final MetacardType updatedMetacardType = updatedMetacard.getMetacardType();
    assertThat(updatedMetacardType.getName(), is(originalMetacardType.getName()));

    final Set<AttributeDescriptor> expectedAttributeDescriptors =
        new HashSet<>(originalMetacardType.getAttributeDescriptors());
    expectedAttributeDescriptors.add(injectAttribute);
    assertThat(updatedMetacardType.getAttributeDescriptors(), is(expectedAttributeDescriptors));

    assertThat(updatedMetacard.getTitle(), is(title));
    assertThat(
        updatedMetacard.getAttribute(injectAttributeName).getValue(), is(injectAttributeValue));
  }

  /** Tests that the framework properly passes an update request to the local provider. */
  @Test
  public void testUpdateStorage() throws Exception {
    List<ContentItem> contentItems = new ArrayList<>();

    MetacardImpl metacard = new MetacardImpl();
    metacard.setId(null);
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return new ByteArrayInputStream("blah".getBytes());
          }
        };
    ContentItemImpl contentItem =
        new ContentItemImpl(
            uuidGenerator.generateUuid(),
            byteSource,
            "application/octet-stream",
            "blah",
            0L,
            metacard);
    contentItems.add(contentItem);

    CreateResponse response = framework.create(new CreateStorageRequestImpl(contentItems, null));

    Metacard insertedCard = response.getCreatedMetacards().get(0);
    List<ContentItem> updatedContentItems = new ArrayList<>();
    updatedContentItems.add(
        new ContentItemImpl(
            insertedCard.getId(), byteSource, "application/octet-stream", insertedCard));
    UpdateStorageRequest request = new UpdateStorageRequestImpl(updatedContentItems, null);
    List<Result> mockFederationResults =
        Stream.of(insertedCard)
            .map(
                m -> {
                  Result mockResult = mock(Result.class);
                  when(mockResult.getMetacard()).thenReturn(m);
                  return mockResult;
                })
            .collect(Collectors.toList());

    QueryResponseImpl queryResponse =
        new QueryResponseImpl(mock(QueryRequest.class), mockFederationResults, 1);
    when(mockFederationStrategy.federate(anyList(), anyObject())).thenReturn(queryResponse);

    // send update to framework
    List<Update> returnedCards = framework.update(request).getUpdatedMetacards();
    assertThat(returnedCards, hasSize(1));
    final Metacard newMetacard = returnedCards.get(0).getNewMetacard();
    assertThat(newMetacard.getId(), notNullValue());
    assertThat(newMetacard.getResourceURI().toString(), is(contentItem.getUri()));
    assertThat(newMetacard.getResourceSize(), is(Long.toString(byteSource.size())));

    assertThat(response.getCreatedMetacards(), hasSize(storageProvider.size()));

    // make sure that the event was posted correctly
    assertThat(eventAdmin.wasEventPosted(), is(true));
  }

  /**
   * Tests that the framework properly passes an update request to the local provider when the
   * content item has a qualifier.
   */
  @Test
  public void testUpdateItemWithQualifier() throws Exception {
    // store one item
    MetacardImpl metacard = new MetacardImpl();
    ByteSource byteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return new ByteArrayInputStream("blah".getBytes());
          }
        };
    ContentItemImpl contentItem =
        new ContentItemImpl(
            uuidGenerator.generateUuid(),
            byteSource,
            "application/octet-stream",
            "blah",
            0L,
            metacard);
    CreateResponse response =
        framework.create(
            new CreateStorageRequestImpl(Collections.singletonList(contentItem), null));
    Metacard createResponseMetacard = response.getCreatedMetacards().get(0);

    // update with 2 more content items that have a qualifier and the same id and metacard as the
    // already-created item
    List<ContentItem> updateRequestContentItems = new ArrayList<>();

    ByteSource q1ByteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return new ByteArrayInputStream("q1 data".getBytes());
          }
        };
    ContentItem q1ContentItem =
        new ContentItemImpl(
            createResponseMetacard.getId(),
            "q1",
            q1ByteSource,
            "application/octet-stream",
            createResponseMetacard);
    updateRequestContentItems.add(q1ContentItem);
    ByteSource q2ByteSource =
        new ByteSource() {
          @Override
          public InputStream openStream() throws IOException {
            return new ByteArrayInputStream("q2 data".getBytes());
          }
        };
    ContentItem q2ContentItem =
        new ContentItemImpl(
            createResponseMetacard.getId(),
            "q2",
            q2ByteSource,
            "application/octet-stream",
            createResponseMetacard);
    updateRequestContentItems.add(q2ContentItem);

    UpdateStorageRequest request = new UpdateStorageRequestImpl(updateRequestContentItems, null);
    List<Result> mockFederationResults =
        Stream.of(createResponseMetacard)
            .map(
                m -> {
                  Result mockResult = mock(Result.class);
                  when(mockResult.getMetacard()).thenReturn(m);
                  return mockResult;
                })
            .collect(Collectors.toList());

    QueryResponseImpl queryResponse =
        new QueryResponseImpl(mock(QueryRequest.class), mockFederationResults, 1);
    when(mockFederationStrategy.federate(anyList(), anyObject())).thenReturn(queryResponse);

    // send update to framework
    List<Update> returnedCards = framework.update(request).getUpdatedMetacards();
    assertThat(returnedCards, hasSize(1));
    final Metacard updateResponseMetacard = returnedCards.get(0).getNewMetacard();
    assertThat(updateResponseMetacard.getId(), notNullValue());
    assertThat(updateResponseMetacard.getResourceURI().toString(), is(contentItem.getUri()));
    assertThat(updateResponseMetacard.getResourceSize(), is(Long.toString(byteSource.size())));

    assertThat(response.getCreatedMetacards(), hasSize(storageProvider.size()));

    // make sure that the event was posted correctly
    assertThat(eventAdmin.wasEventPosted(), is(true));
  }

  /** Tests that the framework properly passes a delete request to the local provider. */
  @Test
  public void testDelete() throws Exception {
    List<Metacard> metacards = new ArrayList<>();
    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);
    metacards.add(newCard);

    // create the entry manually in the provider
    Metacard insertedCard =
        provider
            .create(new CreateRequestImpl(metacards, null))
            .getCreatedMetacards()
            .iterator()
            .next();

    String[] ids = new String[1];
    ids[0] = insertedCard.getId();

    Result mockFederationResult = mock(Result.class);
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId(ids[0]);
    when(mockFederationResult.getMetacard()).thenReturn(metacard);

    QueryResponseImpl queryResponse =
        new QueryResponseImpl(
            mock(QueryRequest.class), Collections.singletonList(mockFederationResult), 1);
    when(mockFederationStrategy.federate(anyList(), anyObject())).thenReturn(queryResponse);

    when(mockRemoteDeleteOperations.performRemoteDelete(any(), any())).then(returnsSecondArg());
    deleteOperations.setRemoteDeleteOperations(mockRemoteDeleteOperations);

    // send delete to framework
    List<Metacard> returnedCards =
        framework.delete(new DeleteRequestImpl(ids)).getDeletedMetacards();
    assertEquals(ids.length, returnedCards.size());
    // make sure that the event was posted correctly
    Metacard[] array = {};
    array = returnedCards.toArray(array);
    assertTrue(eventAdmin.wasEventPosted());
    assertEquals(eventAdmin.getLastEvent(), array[array.length - 1]);
  }

  @Test
  public void testInjectsAttributesOnDelete() throws Exception {
    final String title = "Delete this";
    final String injectAttributeName = "new attribute";
    final double injectAttributeValue = 11.1;
    final MetacardImpl metacard = new MetacardImpl();
    metacard.setTitle(title);
    metacard.setAttribute(injectAttributeName, injectAttributeValue);

    final String id =
        framework
            .create(new CreateRequestImpl(Collections.singletonList(metacard), null))
            .getCreatedMetacards()
            .get(0)
            .getId();

    final DeleteRequest request = new DeleteRequestImpl(id);

    final AttributeDescriptor injectAttribute =
        new AttributeDescriptorImpl(
            injectAttributeName, true, true, false, false, BasicTypes.DOUBLE_TYPE);
    stubMetacardInjection(injectAttribute);

    List<Result> mockFederationResults =
        Stream.of(metacard)
            .map(
                m -> {
                  Result mockResult = mock(Result.class);
                  when(mockResult.getMetacard()).thenReturn(m);
                  return mockResult;
                })
            .collect(Collectors.toList());

    QueryResponseImpl queryResponse =
        new QueryResponseImpl(mock(QueryRequest.class), mockFederationResults, 1);
    when(mockFederationStrategy.federate(anyList(), anyObject())).thenReturn(queryResponse);

    when(mockRemoteDeleteOperations.performRemoteDelete(any(), any())).then(returnsSecondArg());
    deleteOperations.setRemoteDeleteOperations(mockRemoteDeleteOperations);

    final DeleteResponse response = framework.delete(request);

    final Metacard deletedMetacard = response.getDeletedMetacards().get(0);
    final MetacardType originalMetacardType = metacard.getMetacardType();
    final MetacardType deletedMetacardType = deletedMetacard.getMetacardType();
    assertThat(deletedMetacardType.getName(), is(originalMetacardType.getName()));

    final Set<AttributeDescriptor> expectedAttributeDescriptors =
        new HashSet<>(originalMetacardType.getAttributeDescriptors());
    expectedAttributeDescriptors.add(injectAttribute);
    assertThat(deletedMetacardType.getAttributeDescriptors(), is(expectedAttributeDescriptors));

    assertThat(deletedMetacard.getTitle(), is(title));
    assertThat(
        deletedMetacard.getAttribute(injectAttributeName).getValue(), is(injectAttributeValue));
  }

  /**
   * Tests that the framework properly passes an update by identifier request to the local provider.
   */
  @Test
  public void testUpdateByIdentifier() throws Exception {
    List<Metacard> metacards = new ArrayList<>();
    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);
    newCard.setResourceURI(new URI("DDF:///12345"));
    metacards.add(newCard);

    // create the entry manually in the provider
    List<Metacard> insertedCards =
        provider.create(new CreateRequestImpl(metacards)).getCreatedMetacards();

    ArrayList<URI> list = new ArrayList<>();

    list.add(new URI("DDF:///12345"));

    UpdateRequest request =
        new UpdateRequestImpl((URI[]) list.toArray(new URI[list.size()]), insertedCards);

    List<Result> mockFederationResults =
        metacards
            .stream()
            .map(
                m -> {
                  Result mockResult = mock(Result.class);
                  when(mockResult.getMetacard()).thenReturn(m);
                  return mockResult;
                })
            .collect(Collectors.toList());

    QueryResponseImpl queryResponse =
        new QueryResponseImpl(mock(QueryRequest.class), mockFederationResults, 1);
    when(mockFederationStrategy.federate(anyList(), anyObject())).thenReturn(queryResponse);

    // send update to framework
    UpdateResponse updateResponse = framework.update(request);
    List<Update> returnedCards = updateResponse.getUpdatedMetacards();
    assertNotNull(returnedCards);
    assertEquals(list.size(), returnedCards.size());
    assertTrue(provider.hasReceivedUpdateByIdentifier());

    // make sure that the event was posted correctly
    assertTrue(eventAdmin.wasEventPosted());
    assertEquals(
        eventAdmin.getLastEvent().getId(),
        returnedCards.get(returnedCards.size() - 1).getOldMetacard().getId());
  }

  /**
   * Tests that the framework properly passes a delete by identifier request to the local provider.
   */
  @Ignore
  @Test
  public void testDeleteByIdentifier() {
    // TODO create
  }

  // End testing MetacardWriter

  // Start testing CatalogFramework

  @Ignore
  @Test
  public void testFederateRead() {
    // TODO create
  }

  @Ignore
  @Test
  public void testFederateReadWithFrameworkName() {
    // TODO create
  }

  /*
   * Test for "ResourceResponse returns null ResourceRequest in the PostResourcePlugin"
   *
   * The error this test case addresses is as follows: The PostResourcePlugin receives a
   * ResourceResponse with a null ResourceRequest.
   */
  @Test
  @Ignore
  public void
      testGetResourceWhenNonNullResourceRequestExpectPostResourcePluginToReceiveResourceResponseWithNonNullResourceRequest()
          throws Exception {

    String sourceId = "myId";
    resourceFramework.setId(sourceId);
    ResourceCacheImpl resourceCache = mock(ResourceCacheImpl.class);
    when(resourceCache.containsValid(isA(String.class), isA(Metacard.class))).thenReturn(false);

    String resourceSiteName = "myId";

    // Execute
    LOGGER.debug("Testing CatalogFramework.getResource(ResourceRequest, String)...");
    ResourceResponse resourceResponse =
        resourceFramework.getResource(mockResourceRequest, resourceSiteName);
    LOGGER.debug("resourceResponse: {}", resourceResponse);

    // Verify
    /*
     * Verify that when PostResoucePlugin.process() is called, the ResourceResponse argument
     * contains a non-null ResourceRequest.
     */
    verify(mockPostResourcePlugin).process(argument.capture());
    assertNotNull(
        "PostResourcePlugin received a ResourceResponse with a null ResourceRequest.",
        argument.getValue().getRequest());

    /*
     * We really don't need to assert this since we return our mockResourceResponse from
     * PostResourcePlugin.process()
     */
    // assertNotNull("ResourceResponse.getResource() returned a ResourceResponse with a null
    // ResourceRequest.",
    // resourceResponse.getRequest());
  }

  @Test(expected = FederationException.class)
  public void testPreQueryStopExecution()
      throws UnsupportedQueryException, FederationException, SourceUnavailableException {

    SourcePoller poller = mock(SourcePoller.class);
    when(poller.getCachedSource(isA(Source.class))).thenReturn(null);

    MockMemoryProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());

    FederationStrategy federationStrategy = mock(FederationStrategy.class);

    QueryRequest request = mock(QueryRequest.class);

    when(request.getQuery()).thenReturn(mock(Query.class));

    PreQueryPlugin stopQueryPlugin =
        new PreQueryPlugin() {

          @Override
          public QueryRequest process(QueryRequest input)
              throws PluginExecutionException, StopProcessingException {
            throw new StopProcessingException("Testing that the framework will stop the query.");
          }
        };

    FrameworkProperties frameworkProperties = new FrameworkProperties();
    frameworkProperties.setSourcePoller(poller);
    frameworkProperties.setPreQuery(Arrays.asList(stopQueryPlugin));
    frameworkProperties.setFederationStrategy(federationStrategy);
    frameworkProperties.setCatalogProviders(Collections.singletonList(provider));

    CatalogFrameworkImpl framework = createFramework(frameworkProperties);
    framework.query(request);
  }

  private CatalogFrameworkImpl createFramework(FrameworkProperties frameworkProperties) {
    OperationsSecuritySupport opsSecurity = new OperationsSecuritySupport();
    OperationsMetacardSupport opsMetacard = new OperationsMetacardSupport(frameworkProperties);
    SourceOperations sourceOperations = new SourceOperations(frameworkProperties);
    QueryOperations queryOperations =
        new QueryOperations(frameworkProperties, sourceOperations, opsSecurity, opsMetacard);
    OperationsStorageSupport opsStorage =
        new OperationsStorageSupport(sourceOperations, queryOperations);
    OperationsCatalogStoreSupport opsCatStore =
        new OperationsCatalogStoreSupport(frameworkProperties, sourceOperations);
    CreateOperations createOperations =
        new CreateOperations(
            frameworkProperties,
            queryOperations,
            sourceOperations,
            opsSecurity,
            opsMetacard,
            opsCatStore,
            opsStorage);
    UpdateOperations updateOperations =
        new UpdateOperations(
            frameworkProperties,
            queryOperations,
            sourceOperations,
            opsSecurity,
            opsMetacard,
            opsCatStore,
            opsStorage);
    DeleteOperations deleteOperations =
        new DeleteOperations(
            frameworkProperties, queryOperations, sourceOperations, opsSecurity, opsMetacard);
    ResourceOperations resourceOperations =
        new ResourceOperations(frameworkProperties, queryOperations, opsSecurity);
    TransformOperations transformOperations = new TransformOperations(frameworkProperties);

    Historian historian = new Historian();
    historian.setHistoryEnabled(false);

    opsStorage.setHistorian(historian);
    updateOperations.setHistorian(historian);
    deleteOperations.setHistorian(historian);
    deleteOperations.setOpsCatStoreSupport(opsCatStore);

    CatalogFrameworkImpl catalogFramework =
        new CatalogFrameworkImpl(
            createOperations,
            updateOperations,
            deleteOperations,
            queryOperations,
            resourceOperations,
            sourceOperations,
            transformOperations);

    // Conditionally bind objects if framework properties are setup
    if (CollectionUtils.isNotEmpty(frameworkProperties.getCatalogProviders())) {
      sourceOperations.bind(provider);
    }
    if (CollectionUtils.isNotEmpty(frameworkProperties.getStorageProviders())) {
      sourceOperations.bind(storageProvider);
    }
    return catalogFramework;
  }

  @Test(expected = FederationException.class)
  public void testPostQueryStopExecution()
      throws UnsupportedQueryException, FederationException, SourceUnavailableException {

    SourcePoller poller = mock(SourcePoller.class);

    when(poller.getCachedSource(isA(Source.class))).thenReturn(null);

    BundleContext context = null;

    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.like(
            filterFactory.property(Metacard.METADATA), "goodyear", "*", "?", "/", false);

    QueryRequest request = new QueryRequestImpl(new QueryImpl(filter));

    SourceResponseImpl sourceResponse = new SourceResponseImpl(request, new ArrayList<>());

    QueryResponseImpl queryResponse = new QueryResponseImpl(sourceResponse, "anyId");

    CatalogProvider provider = mock(CatalogProvider.class);

    when(provider.query(isA(QueryRequest.class))).thenReturn(sourceResponse);

    FederationStrategy federationStrategy = mock(FederationStrategy.class);

    when(federationStrategy.federate(isA(List.class), isA(QueryRequest.class)))
        .thenReturn(queryResponse);

    PostQueryPlugin stopQueryPlugin =
        new PostQueryPlugin() {

          @Override
          public QueryResponse process(QueryResponse input)
              throws PluginExecutionException, StopProcessingException {
            throw new StopProcessingException("Testing that the framework will stop the query.");
          }
        };
    FrameworkProperties props = new FrameworkProperties();
    props.setCatalogProviders(Collections.singletonList((CatalogProvider) provider));
    props.setBundleContext(context);
    props.setPostQuery(Arrays.asList(stopQueryPlugin));
    props.setFederationStrategy(federationStrategy);
    props.setQueryResponsePostProcessor(mock(QueryResponsePostProcessor.class));
    props.setSourcePoller(poller);
    props.setFilterBuilder(new GeotoolsFilterBuilder());

    CatalogFrameworkImpl framework = createFramework(props);
    framework.query(request);
  }

  @Ignore
  @Test
  public void testFederateQueryWithFrameworkName() {
    // TODO create
  }

  @Test
  public void testInjectsAttributesOnQuery() throws Exception {
    final Metacard original = new MetacardImpl();
    final String id =
        framework
            .create(new CreateRequestImpl(Collections.singletonList(original), null))
            .getCreatedMetacards()
            .get(0)
            .getId();

    final AttributeDescriptor injectAttribute =
        new AttributeDescriptorImpl(
            "new attribute", true, true, false, false, BasicTypes.DOUBLE_TYPE);
    stubMetacardInjection(injectAttribute);

    final FilterFactory filterFactory = new FilterFactoryImpl();
    final Filter filter =
        filterFactory.equals(filterFactory.property(Metacard.ID), filterFactory.literal(id));

    final QueryRequest request = new QueryRequestImpl(new QueryImpl(filter));

    final QueryResponse response = framework.query(request);

    final Metacard queryMetacard = response.getResults().get(0).getMetacard();
    final MetacardType originalMetacardType = original.getMetacardType();
    final MetacardType queryMetacardType = queryMetacard.getMetacardType();
    assertThat(originalMetacardType.getName(), is(queryMetacardType.getName()));

    final Set<AttributeDescriptor> expectedAttributeDescriptors =
        new HashSet<>(originalMetacardType.getAttributeDescriptors());
    expectedAttributeDescriptors.add(injectAttribute);
    assertThat(queryMetacardType.getAttributeDescriptors(), is(expectedAttributeDescriptors));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testQueryTransformWithTransformException() throws Exception {
    BundleContext context = mock(BundleContext.class);

    Transform transform = mock(Transform.class);
    when(transform.transform(any(SourceResponse.class), any(String.class), any(Map.class)))
        .thenThrow(new CatalogTransformerException("Could not transform"));

    CatalogFramework framework =
        this.createDummyCatalogFramework(
            provider, storageProvider, context, eventAdmin, true, transform);
    SourceResponse response = new SourceResponseImpl(null, null);

    framework.transform(response, NO_TRANSFORMER_ID, new HashMap<>());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testQueryTransformWithNullResponse() throws Exception {
    BundleContext context = mock(BundleContext.class);

    Transform transform = mock(Transform.class);
    when(transform.transform(any(SourceResponse.class), any(String.class), any(Map.class)))
        .thenThrow(IllegalArgumentException.class);

    CatalogFramework framework =
        this.createDummyCatalogFramework(
            provider, storageProvider, context, eventAdmin, true, transform);

    framework.transform((SourceResponse) null, NO_TRANSFORMER_ID, new HashMap<>());
  }

  @Test
  public void testQueryTransform() throws Exception {
    BundleContext context = mock(BundleContext.class);

    Transform transform = mock(Transform.class);
    when(transform.transform(any(SourceResponse.class), any(String.class), any(Map.class)))
        .thenReturn(new BinaryContentImpl(null));

    CatalogFramework framework =
        this.createDummyCatalogFramework(
            provider, storageProvider, context, eventAdmin, true, transform);
    SourceResponse response = new SourceResponseImpl(null, null);

    BinaryContent content = framework.transform(response, NO_TRANSFORMER_ID, new HashMap<>());

    assertNotNull(content);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testMetacardTransformWithTransformException() throws Exception {
    BundleContext context = mock(BundleContext.class);

    Transform transform = mock(Transform.class);
    when(transform.transform(any(List.class), any(String.class), any(Map.class)))
        .thenThrow(new CatalogTransformerException("Could not transform"));

    CatalogFramework framework =
        this.createDummyCatalogFramework(
            provider, storageProvider, context, eventAdmin, true, transform);
    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);

    framework.transform(newCard, NO_TRANSFORMER_ID, new HashMap<>());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMetacardTransformWithNullMetacard() throws Exception {
    BundleContext context = mock(BundleContext.class);

    Transform transform = mock(Transform.class);
    when(transform.transform(any(List.class), any(String.class), any(Map.class)))
        .thenThrow(IllegalArgumentException.class);

    CatalogFramework framework =
        this.createDummyCatalogFramework(
            provider, storageProvider, context, eventAdmin, true, transform);

    framework.transform((Metacard) null, NO_TRANSFORMER_ID, new HashMap<>());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMetacardTransformWithInvalidSyntaxException() throws Exception {
    BundleContext context = mock(BundleContext.class);

    Transform transform = mock(Transform.class);
    when(transform.transform(any(List.class), any(String.class), any(Map.class)))
        .thenThrow(IllegalArgumentException.class);

    CatalogFramework framework =
        this.createDummyCatalogFramework(
            provider, storageProvider, context, eventAdmin, true, transform);

    framework.transform((Metacard) null, NO_TRANSFORMER_ID, new HashMap<>());
  }

  @Test
  public void testMetacardTransform() throws Exception {
    BundleContext context = mock(BundleContext.class);

    Transform transform = mock(Transform.class);
    when(transform.transform(any(List.class), any(String.class), any(Map.class)))
        .thenReturn(Collections.singletonList(new BinaryContentImpl(null)));

    CatalogFramework framework =
        this.createDummyCatalogFramework(
            provider, storageProvider, context, eventAdmin, true, transform);
    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);

    BinaryContent content = framework.transform(newCard, NO_TRANSFORMER_ID, new HashMap<>());

    assertNotNull(content);
  }

  @Test
  public void testGetSites() {
    framework.setId("ddf");
    framework.getSourceOperations().setId("ddf");

    Set<String> ids = new HashSet<>();
    for (FederatedSource source : federatedSources) {
      ids.add(source.getId());
    }
    ids.add(framework.getId());

    SourceInfoRequest request = new SourceInfoRequestSources(true, ids);

    SourceInfoResponse response = null;
    try {
      response = framework.getSourceInfo(request);
    } catch (SourceUnavailableException e) {
      fail();
    }
    Set<SourceDescriptor> sourceDescriptors = response.getSourceInfo();

    List<String> siteNames = new ArrayList<>();
    for (SourceDescriptor descriptor : sourceDescriptors) {
      LOGGER.debug("Descriptor id: {}", descriptor.getSourceId());
      siteNames.add(descriptor.getSourceId());
    }

    // add a plus one for now to simulate that the framework is ad
    // assertTrue( sourceDescriptor.containsAll( federatedSources ) );
    // assertTrue( sourceDescriptor.containsAll( expectedSourceSet ) );
    assertEquals(ids.size(), sourceDescriptors.size());

    String[] expectedOrdering = {"A", "B", "C", framework.getId()};

    assertArrayEquals(expectedOrdering, siteNames.toArray(new String[siteNames.size()]));
  }

  @Test
  public void testGetFederatedSources() {
    SourceInfoRequest request = new SourceInfoRequestEnterprise(true);
    SourceInfoResponse response = null;
    try {
      response = framework.getSourceInfo(request);
    } catch (SourceUnavailableException e) {
      fail();
    }
    Set<SourceDescriptor> sourceDescriptors = response.getSourceInfo();
    for (SourceDescriptor descriptor : sourceDescriptors) {
      LOGGER.debug("Descriptor id: {}", descriptor.getSourceId());
    }

    // The "+1" is to account for the CatalogFramework source descriptor.
    // Even if no local catalog provider is configured, the catalog framework's
    // site info is included in the SourceDescriptos list.
    assertEquals(federatedSources.size() + 1, sourceDescriptors.size());
  }

  @Test
  public void testGetUnavailableFederatedSources() {
    List<FederatedSource> federatedSources = createDefaultFederatedSourceList(false);
    CatalogProvider catalogProvider = mock(CatalogProvider.class);
    // Mock register the federated sources in the container
    SourcePollerRunner runner = new SourcePollerRunner();
    SourcePoller poller = new SourcePoller(runner);
    for (FederatedSource source : federatedSources) {
      runner.bind(source);
    }
    runner.bind(catalogProvider);
    FrameworkProperties frameworkProperties = new FrameworkProperties();
    frameworkProperties.setSourcePoller(poller);
    Map<String, FederatedSource> sources = new HashMap<>();
    for (FederatedSource federatedSource : federatedSources) {
      sources.put(federatedSource.getId(), federatedSource);
    }
    frameworkProperties.setFederatedSources(sources);
    frameworkProperties.setCatalogProviders(Collections.singletonList(catalogProvider));
    CatalogFrameworkImpl framework = createFramework(frameworkProperties);

    SourceInfoRequest request = new SourceInfoRequestEnterprise(true);
    SourceInfoResponse response = null;
    try {
      response = framework.getSourceInfo(request);
    } catch (SourceUnavailableException e) {
      fail();
    }
    Set<SourceDescriptor> sourceDescriptors = response.getSourceInfo();
    for (SourceDescriptor descriptor : sourceDescriptors) {
      LOGGER.debug("Descriptor id: {}", descriptor.getSourceId());
      if (StringUtils.isNotBlank(descriptor.getId())) {
        assertFalse(descriptor.isAvailable());
        // No contentTypes should be listed if the source is unavailable
        assertTrue(descriptor.getContentTypes().isEmpty());
      }
    }

    // The "+1" is to account for the CatalogFramework source descriptor.
    // Even if no local catalog provider is configured, the catalog
    // framework's
    // site info is included in the SourceDescriptos list.
    assertEquals(federatedSources.size() + 1, sourceDescriptors.size());
  }

  @Test
  public void testGetFederatedSourcesDuplicates() {
    List<FederatedSource> federatedSources = createDefaultFederatedSourceList(true);
    // Duplicate Site
    FederatedSource siteC2 = new MockSource("C", "Site C2", "v1.0", "DDF", null, true, new Date());
    federatedSources.add(siteC2);

    // Expected Sites
    List<FederatedSource> expectedSources = createDefaultFederatedSourceList(true);

    // Mock register the federated sources in the container
    // Mock the source poller
    SourcePoller mockPoller = mock(SourcePoller.class);
    when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

    FrameworkProperties frameworkProperties = new FrameworkProperties();
    frameworkProperties.setSourcePoller(mockPoller);
    Map<String, FederatedSource> sources = new HashMap<>();
    for (FederatedSource federatedSource : expectedSources) {
      sources.put(federatedSource.getId(), federatedSource);
    }
    frameworkProperties.setFederatedSources(sources);
    CatalogFrameworkImpl framework = createFramework(frameworkProperties);

    // Returned Sites
    SourceInfoRequest request = new SourceInfoRequestEnterprise(true);

    SourceInfoResponse response = null;
    try {
      response = framework.getSourceInfo(request);
    } catch (SourceUnavailableException e) {
      LOGGER.debug("SourceUnavilable", e);
      fail();
    }
    Set<SourceDescriptor> sourceDescriptors = response.getSourceInfo();
    // should contain ONLY the original federated sites and the catalog framework's
    // site info (even though it has no local catalog provider configured) - hence,
    // the "+1"
    assertEquals(expectedSources.size(), sourceDescriptors.size());
  }

  @Test
  public void testGetAllSiteNames() {
    String frameworkName = "DDF";
    CatalogProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());
    List<FederatedSource> federatedSources = createDefaultFederatedSourceList(true);

    // Expected Set of Names
    Set<String> expectedNameSet = new HashSet<>();
    for (FederatedSource curSite : federatedSources) {
      expectedNameSet.add(curSite.getId());
    }

    // Mock register the provider in the container
    // Mock the source poller
    SourcePoller mockPoller = mock(SourcePoller.class);
    when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

    FrameworkProperties frameworkProperties = new FrameworkProperties();
    frameworkProperties.setSourcePoller(mockPoller);
    Map<String, FederatedSource> sources = new HashMap<>();
    for (FederatedSource federatedSource : federatedSources) {
      sources.put(federatedSource.getId(), federatedSource);
    }
    frameworkProperties.setFederatedSources(sources);
    frameworkProperties.setCatalogProviders(Collections.singletonList(provider));

    CatalogFrameworkImpl framework = createFramework(frameworkProperties);
    framework.setId(frameworkName);

    // Returned Set of Names
    // Returned Sites
    SourceInfoRequest request = new SourceInfoRequestEnterprise(true);
    SourceInfoResponse response = null;
    try {
      response = framework.getSourceInfo(request);
    } catch (SourceUnavailableException e) {
      LOGGER.debug("SourceUnavilable", e);
      fail();
    }
    assert (response != null);
    Set<SourceDescriptor> sourceDescriptors = response.getSourceInfo();
    // should contain ONLY the original federated sites
    assertEquals(expectedNameSet.size(), sourceDescriptors.size());
    Set<String> returnedSourceIds = new HashSet<>();

    for (SourceDescriptor sd : sourceDescriptors) {
      returnedSourceIds.add(sd.getSourceId());
    }

    for (String id : returnedSourceIds) {
      LOGGER.debug("returned sourceId: {}", id);
    }
    assertTrue(expectedNameSet.equals(returnedSourceIds));
  }

  // End testing CatalogFramework

  // Test negative use-cases (expected errors)

  /**
   * Tests that the framework properly throws a catalog exception when the local provider is not
   * available for create.
   *
   * @throws SourceUnavailableException
   */
  @Test(expected = SourceUnavailableException.class)
  public void testProviderUnavailableCreate() throws SourceUnavailableException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF", new HashSet<>(), false, null);
    CatalogFramework framework =
        createDummyCatalogFramework(provider, storageProvider, eventAdmin, false);
    List<Metacard> metacards = new ArrayList<>();
    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);
    metacards.add(newCard);

    CreateRequest create = new CreateRequestImpl(metacards);

    // expected to throw exception due to catalog provider being unavailable
    try {
      framework.create(create);
    } catch (IngestException e) {
      fail();
    }
  }

  /**
   * Tests that the framework properly throws a catalog exception when the local provider is not
   * available for update by id.
   *
   * @throws SourceUnavailableException
   */
  @Test(expected = SourceUnavailableException.class)
  public void testProviderUnavailableUpdateByID() throws SourceUnavailableException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF", new HashSet<>(), false, null);
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, false);
    List<Metacard> metacards = new ArrayList<>();
    List<URI> uris = new ArrayList<>();
    // expected to throw exception due to catalog provider being unavailable
    try {
      MetacardImpl newCard = new MetacardImpl();
      newCard.setId(null);
      newCard.setResourceURI(new URI("uri:///1234"));
      metacards.add(newCard);
      uris.add(new URI("uri:///1234"));

      UpdateRequest update =
          new UpdateRequestImpl((URI[]) uris.toArray(new URI[uris.size()]), metacards);

      framework.update(update);
    } catch (URISyntaxException e) {
      fail();
    } catch (IngestException e) {
      fail();
    }
  }

  /**
   * Tests that the framework properly throws a catalog exception when the local provider is not
   * available for update by identifier.
   *
   * @throws IngestException
   * @throws SourceUnavailableException
   */
  @Test(expected = SourceUnavailableException.class)
  public void testProviderUnavailableUpdateByIdentifier() throws SourceUnavailableException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF", new HashSet<>(), false, null);
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, false);
    List<Metacard> metacards = new ArrayList<>();
    List<URI> uris = new ArrayList<>();

    // expected to throw exception due to catalog provider being unavailable
    try {
      MetacardImpl newCard = new MetacardImpl();
      newCard.setId(null);
      newCard.setResourceURI(new URI("uri:///1234"));
      metacards.add(newCard);
      uris.add(new URI("uri:////1234"));

      UpdateRequest update =
          new UpdateRequestImpl((URI[]) uris.toArray(new URI[uris.size()]), metacards);

      framework.update(update);
    } catch (URISyntaxException e) {
      fail();
    } catch (IngestException e) {
      fail();
    }
  }

  /**
   * Tests that the framework properly throws a catalog exception when the local provider is not
   * available for delete by id.
   *
   * @throws IngestException
   * @throws SourceUnavailableException
   */
  @Test(expected = SourceUnavailableException.class)
  public void testProviderUnavailableDeleteByID() throws SourceUnavailableException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF", new HashSet<>(), false, null);
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, false);
    List<String> ids = new ArrayList<>();
    ids.add("1234");

    DeleteRequest request = new DeleteRequestImpl((String[]) ids.toArray(new String[ids.size()]));

    // expected to throw exception due to catalog provider being unavailable
    try {
      framework.delete(request);
    } catch (IngestException e) {
      fail();
    }
  }

  /**
   * Tests that the framework properly throws a catalog exception when the local provider is not
   * available for delete by identifier.
   *
   * @throws IngestException
   * @throws SourceUnavailableException
   */
  @Test(expected = SourceUnavailableException.class)
  public void testProviderUnavailableDeleteByIdentifier() throws SourceUnavailableException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider("Provider", "Provider", "v1.0", "DDF", new HashSet<>(), false, null);
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, false);
    List<URI> uris = new ArrayList<>();
    try {
      uris.add(new URI("id://1234"));
      DeleteRequest request = new DeleteRequestImpl((URI[]) uris.toArray(new URI[uris.size()]));

      // expected to throw exception due to catalog provider being
      // unavailable
      framework.delete(request);
    } catch (URISyntaxException e) {
      fail();
    } catch (IngestException e) {
      fail();
    }
  }

  /**
   * Tests that the framework properly throws a catalog exception when there are no sites (federated
   * or local) that are available to perform the query.
   *
   * @throws SourceUnavailableException
   */
  @Ignore
  @Test(expected = SourceUnavailableException.class)
  public void testNoSitesAvailableFederatedQuery() throws SourceUnavailableException {
    CatalogFramework framework = this.createDummyCatalogFramework(null, null, null, false);

    QueryRequest request = new QueryRequestImpl(null);

    try {
      framework.query(request);
    } catch (UnsupportedQueryException e) {
      // we don't even care what the query was
    } catch (FederationException e) {
      fail();
    }
  }

  /**
   * Tests that the framework properly throws a catalog exception when the query being passed in is
   * null.
   *
   * @throws UnsupportedQueryException
   */
  @Test(expected = UnsupportedQueryException.class)
  public void testNullQuery() throws UnsupportedQueryException {
    boolean isAvailable = false;
    CatalogProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), isAvailable, new Date());

    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, null, true);

    try {
      framework.query(null);
    } catch (FederationException e) {
      fail();
    } catch (SourceUnavailableException e) {
      fail();
    }
  }

  /**
   * Tests that the framework properly throws a catalog exception when the federated query being
   * passed in is null.
   *
   * @throws UnsupportedQueryException
   */
  @Test(expected = UnsupportedQueryException.class)
  public void testNullFederatedQuery() throws UnsupportedQueryException {
    boolean isAvailable = false;
    CatalogProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), isAvailable, new Date());
    createDefaultFederatedSourceList(isAvailable);

    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, null, true);

    try {
      framework.query(null, null);
    } catch (FederationException e) {
      fail();
    } catch (SourceUnavailableException e) {
      fail();
    }
  }

  @Test(expected = IngestException.class)
  public void testNullEntriesCreate() throws IngestException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, true);

    // call framework with null request
    try {
      framework.create((CreateRequest) null);
    } catch (SourceUnavailableException e) {
      fail();
    }
  }

  @Test(expected = IngestException.class)
  public void testNullEntriesUpdate() throws IngestException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, true);

    // call framework with null request
    try {
      framework.update((UpdateRequest) null);
    } catch (SourceUnavailableException e) {
      fail();
    }
  }

  @Test(expected = IngestException.class)
  public void testNullIdsDelete() throws IngestException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, true);

    // call framework with null request
    try {
      framework.delete(null);
    } catch (SourceUnavailableException e) {
      fail();
    }
  }

  @Test(expected = IngestException.class)
  public void testProviderRuntimeExceptionOnCreate() throws IngestException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    // use exception provider instead of memory
    MockExceptionProvider provider =
        new MockExceptionProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, null);
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, true);
    List<Metacard> metacards = new ArrayList<>();
    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);
    metacards.add(newCard);

    CreateRequest create = new CreateRequestImpl((Metacard) null);
    try {
      framework.create(create);
    } catch (SourceUnavailableException e) {
      fail();
    }
  }

  @Test(expected = IngestException.class)
  public void testProviderRuntimeExceptionOnUpdateByID() throws IngestException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    // use exception provider instead of memory
    MockExceptionProvider provider =
        new MockExceptionProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, null);
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, true);
    List<Entry<Object, Metacard>> metacards = new ArrayList<>();
    HashMap<Object, Metacard> map = new HashMap<>();

    // expected to throw exception due to catalog provider being unavailable
    try {
      MetacardImpl newCard = new MetacardImpl();
      newCard.setId(null);
      newCard.setResourceURI(new URI("uri:///1234"));
      map.put(Metacard.ID, newCard);
      metacards.addAll(map.entrySet());

      UpdateRequest update = new UpdateRequestImpl(null, Metacard.ID, null);
      framework.update(update);
    } catch (URISyntaxException e) {
      fail();
    } catch (SourceUnavailableException e) {
      fail();
    }
  }

  @Test(expected = IngestException.class)
  public void testProviderRuntimeExceptionOnUpdateByIdentifier() throws IngestException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    // use exception provider instead of memory
    MockExceptionProvider provider =
        new MockExceptionProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, null);
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, true);
    List<Entry<Object, Metacard>> metacards = new ArrayList<>();
    HashMap<Object, Metacard> map = new HashMap<>();

    try {
      MetacardImpl newCard = new MetacardImpl();
      newCard.setId(null);
      newCard.setResourceURI(new URI("uri:///1234"));
      map.put(Metacard.ID, newCard);
      metacards.addAll(map.entrySet());

      UpdateRequest update = new UpdateRequestImpl(null, Metacard.RESOURCE_URI, null);
      framework.update(update);
    } catch (URISyntaxException e) {
      fail();
    } catch (SourceUnavailableException e) {
      fail();
    }
  }

  @Test(expected = IngestException.class)
  public void testProviderRuntimeExceptionOnDeleteByID() throws IngestException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    // use exception provider instead of memory
    MockExceptionProvider provider =
        new MockExceptionProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, null);
    MockMemoryStorageProvider storageProvider = new MockMemoryStorageProvider();
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, true);
    List<String> ids = new ArrayList<>();
    ids.add("1234");

    DeleteRequest request = new DeleteRequestImpl((String[]) ids.toArray(new String[ids.size()]));

    // expected to throw exception due to catalog provider
    try {
      framework.delete(request);
    } catch (SourceUnavailableException e) {
      fail();
    }
  }

  @Test(expected = IngestException.class)
  public void testProviderRuntimeExceptionOnDeleteByIdentifier() throws IngestException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    // use exception provider instead of memory
    MockExceptionProvider provider =
        new MockExceptionProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, null);
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, true);
    // List<MetacardType> identifiers = new ArrayList<MetacardType>();
    // identifiers.add( new MetacardTypeImpl( "id", "1234" ) );
    ArrayList<URI> uris = new ArrayList<>();

    DeleteRequest request = new DeleteRequestImpl((URI[]) uris.toArray(new URI[uris.size()]));
    // expected to throw exception due to catalog provider being unavailable
    try {
      framework.delete(request);
    } catch (SourceUnavailableException e) {
      fail();
    }
  }

  @Ignore
  @Test(expected = CatalogTransformerException.class)
  public void testMetacardTransformWithBadShortname() throws CatalogTransformerException {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());
    // TODO pass in bundle context
    CatalogFramework framework =
        this.createDummyCatalogFramework(provider, storageProvider, eventAdmin, true);
    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);

    framework.transform(newCard, NO_TRANSFORMER_ID, new HashMap<>());
  }

  /**
   * Tests that you can get a resource's (product) options. Covers the case where the source ID
   * specified is actually the local catalog provider's site name (so this reduces down to a
   * getResourceOptions for local provider); and the case where a federated source is specified.
   *
   * <p>Test for DDF-1763.
   *
   * @throws Exception
   */
  @Test
  public void testGetResourceOptions() throws Exception {
    String localProviderName = "ddf";
    String federatedSite1Name = "fed-site-1";
    String metacardId = "123";

    // The resource's URI
    URI metacardUri =
        new URI(
            "http:///27+Nov+12+12%3A30%3A04?MyPhotograph%0Ahttp%3A%2F%2F172.18.14.53%3A8080%2Fabc%2Fimages%2FActionable.jpg%0AMyAttachment%0Ahttp%3A%2F%2F172.18.14.53%3A8080%2Fabc#abc.xyz.dao.URLResourceOptionDataAccessObject");

    Set<String> supportedOptions = new HashSet<>();
    supportedOptions.add("MyPhotograph");
    supportedOptions.add("MyAttachment");

    // Catalog Provider
    CatalogProvider provider = mock(CatalogProvider.class);
    when(provider.getId()).thenReturn(localProviderName);
    when(provider.isAvailable(isA(SourceMonitor.class))).thenReturn(true);
    when(provider.isAvailable()).thenReturn(true);

    // Federated Source 1
    FederatedSource federatedSource1 = mock(FederatedSource.class);
    when(federatedSource1.getId()).thenReturn(federatedSite1Name);
    when(federatedSource1.isAvailable(isA(SourceMonitor.class))).thenReturn(true);
    when(federatedSource1.isAvailable()).thenReturn(true);
    when(federatedSource1.getOptions(isA(Metacard.class))).thenReturn(supportedOptions);

    List<FederatedSource> federatedSources = new ArrayList<>();
    federatedSources.add(federatedSource1);

    // Mock register the provider in the container
    // Mock the source poller
    SourcePoller mockPoller = mock(SourcePoller.class);
    when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setId(metacardId);
    metacard.setResourceURI(metacardUri);
    Result result = new ResultImpl(metacard);
    List<Result> results = new ArrayList<>();
    results.add(result);

    QueryResponse queryResponse = mock(QueryResponse.class);
    when(queryResponse.getResults()).thenReturn(results);
    FederationStrategy strategy = mock(FederationStrategy.class);
    when(strategy.federate(isA(federatedSources.getClass()), isA(QueryRequest.class)))
        .thenReturn(queryResponse);

    ResourceReader resourceReader = mock(ResourceReader.class);
    Set<String> supportedSchemes = new HashSet<>();
    supportedSchemes.add("http");
    when(resourceReader.getSupportedSchemes()).thenReturn(supportedSchemes);
    when(resourceReader.getOptions(isA(Metacard.class))).thenReturn(supportedOptions);
    List<ResourceReader> resourceReaders = new ArrayList<>();
    resourceReaders.add(resourceReader);

    FrameworkProperties props = new FrameworkProperties();
    props.setCatalogProviders(Collections.singletonList((CatalogProvider) provider));
    props.setFederatedSources(Collections.singletonMap(federatedSite1Name, federatedSource1));
    props.setResourceReaders(resourceReaders);
    props.setFederationStrategy(strategy);
    props.setQueryResponsePostProcessor(mock(QueryResponsePostProcessor.class));
    props.setSourcePoller(mockPoller);
    props.setFilterBuilder(new GeotoolsFilterBuilder());
    props.setDefaultAttributeValueRegistry(defaultAttributeValueRegistry);

    CatalogFrameworkImpl framework = createFramework(props);
    framework.setId("ddf");

    Set<String> ids = new HashSet<>();
    for (FederatedSource source : federatedSources) {
      ids.add(source.getId());
    }
    ids.add(framework.getId());

    // site name = local provider
    Map<String, Set<String>> optionsMap =
        framework.getResourceOptions(metacardId, localProviderName);
    LOGGER.debug("localProvider optionsMap = {}", optionsMap);
    assertThat(optionsMap, hasEntry("RESOURCE_OPTION", supportedOptions));

    // site name = federated site's name
    optionsMap = framework.getResourceOptions(metacardId, federatedSite1Name);
    LOGGER.debug("federatedSource optionsMap = {}", optionsMap);
    assertThat(optionsMap, hasEntry("RESOURCE_OPTION", supportedOptions));

    // site name = null (should default to local provider)
    optionsMap = framework.getResourceOptions(metacardId, null);
    LOGGER.debug("localProvider optionsMap = {}", optionsMap);
    assertThat(optionsMap, hasEntry("RESOURCE_OPTION", supportedOptions));

    // site name = empty string (should default to local provider)
    optionsMap = framework.getResourceOptions(metacardId, "");
    LOGGER.debug("localProvider optionsMap = {}", optionsMap);
    assertThat(optionsMap, hasEntry("RESOURCE_OPTION", supportedOptions));
  }

  @Test
  public void testCreateWithStores() throws Exception {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());

    Map<String, CatalogStore> storeMap = new HashMap<>();

    MockCatalogStore store = new MockCatalogStore("catalogStoreId-1", true);
    storeMap.put(store.getId(), store);

    CatalogFramework framework = createDummyCatalogFramework(provider, storeMap, null, eventAdmin);

    List<Metacard> metacards = new ArrayList<>();
    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);
    metacards.add(newCard);
    Map<String, Serializable> reqProps = new HashMap<>();
    HashSet<String> destinations = new HashSet<>();

    // ==== test writing to store and not local ====
    destinations.add("catalogStoreId-1");
    CreateResponse response =
        framework.create(new CreateRequestImpl(metacards, reqProps, destinations));
    assertEquals(0, provider.size());
    assertEquals(response.getCreatedMetacards().size(), store.size());
    assertEquals(1, store.size());
    assertFalse(eventAdmin.wasEventPosted());

    // ==== test writing to store and local ====
    destinations.add("mockMemoryProvider");
    newCard.setId(null);
    reqProps = new HashMap<>();
    response = framework.create(new CreateRequestImpl(metacards, reqProps, destinations));
    assertEquals(1, provider.size());
    assertEquals(response.getCreatedMetacards().size(), provider.size());
    assertEquals(2, store.size());
    assertTrue(eventAdmin.wasEventPosted());

    // ==== test writing to local when no destination ====
    destinations.clear();
    newCard.setId(null);
    reqProps = new HashMap<>();
    response = framework.create(new CreateRequestImpl(metacards, reqProps, destinations));
    assertEquals(2, provider.size());
    assertEquals(response.getCreatedMetacards().size(), 1);
    assertEquals(2, store.size());
  }

  @Ignore // TODO (DDF-2436) -
  @Test
  public void testUpdateWithStores() throws Exception {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());

    Map<String, CatalogStore> storeMap = new HashMap<>();
    Map<String, FederatedSource> sourceMap = new HashMap<>();
    MockCatalogStore store = new MockCatalogStore("catalogStoreId-1", true);
    storeMap.put(store.getId(), store);
    sourceMap.put(store.getId(), store);

    CatalogFramework framework =
        createDummyCatalogFramework(provider, storeMap, sourceMap, eventAdmin);
    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.like(filterFactory.property(Metacard.METADATA), "*", "*", "?", "/", false);

    List<Metacard> metacards = new ArrayList<>();
    String id = UUID.randomUUID().toString();
    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(id);
    newCard.setAttribute("myKey", "myValue1");
    metacards.add(newCard);
    Map<String, Serializable> reqProps = new HashMap<>();
    HashSet<String> destinations = new HashSet<>();
    destinations.add("mockMemoryProvider");
    destinations.add("catalogStoreId-1");
    framework.create(new CreateRequestImpl(metacards, reqProps, destinations));

    MetacardImpl updateCard = new MetacardImpl();
    updateCard.setId(id);
    updateCard.setAttribute("myKey", "myValue2");
    List<Entry<Serializable, Metacard>> updates = new ArrayList<>();
    updates.add(new SimpleEntry<>(id, updateCard));
    destinations.remove("mockMemoryProvider");
    framework.update(new UpdateRequestImpl(updates, Metacard.ID, new HashMap<>(), destinations));
    assertThat(provider.hasReceivedUpdateByIdentifier(), is(false));
    assertThat(store.hasReceivedUpdateByIdentifier(), is(true));
    QueryResponse storeResponse =
        framework.query(new QueryRequestImpl(new QueryImpl(filter), destinations));
    assertThat(storeResponse.getResults().size(), is(1));
    assertThat(
        storeResponse.getResults().get(0).getMetacard().getAttribute("myKey").getValue(),
        equalTo("myValue2"));
    destinations.clear();
    QueryResponse providerResponse =
        framework.query(new QueryRequestImpl(new QueryImpl(filter), destinations));
    assertThat(providerResponse.getResults().size(), is(1));
    assertThat(
        providerResponse.getResults().get(0).getMetacard().getAttribute("myKey").getValue(),
        equalTo("myValue1"));
  }

  @Ignore // TODO (DDF-2436) -
  @Test
  public void testDeleteWithStores() throws Exception {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());

    Map<String, CatalogStore> storeMap = new HashMap<>();
    Map<String, FederatedSource> sourceMap = new HashMap<>();
    MockCatalogStore store = new MockCatalogStore("catalogStoreId-1", true);
    storeMap.put(store.getId(), store);
    sourceMap.put(store.getId(), store);

    CatalogFramework framework =
        createDummyCatalogFramework(provider, storeMap, sourceMap, eventAdmin);
    FilterFactory filterFactory = new FilterFactoryImpl();

    Filter filter =
        filterFactory.like(filterFactory.property(Metacard.METADATA), "*", "*", "?", "/", false);

    List<Metacard> metacards = new ArrayList<>();
    String id = UUID.randomUUID().toString().replaceAll("-", "");
    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(id);
    newCard.setAttribute("myKey", "myValue1");
    metacards.add(newCard);
    Map<String, Serializable> reqProps = new HashMap<>();
    HashSet<String> destinations = new HashSet<>();
    destinations.add("mockMemoryProvider");
    destinations.add("catalogStoreId-1");
    framework.create(new CreateRequestImpl(metacards, reqProps, destinations));

    DeleteRequest deleteRequest =
        new DeleteRequestImpl(
            Collections.singletonList(id), Metacard.ID, new HashMap<>(), destinations);
    DeleteResponse response = framework.delete(deleteRequest);
    assertThat(response.getDeletedMetacards().size(), is(1));
    QueryResponse queryResponse =
        framework.query(new QueryRequestImpl(new QueryImpl(filter), true));
    assertThat(queryResponse.getResults().size(), is(0));
  }

  @Test(expected = FederationException.class)
  public void testFederatedQueryPermissionsNoSubject() throws Exception {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());

    Map<String, CatalogStore> storeMap = new HashMap<>();
    Map<String, FederatedSource> sourceMap = new HashMap<>();

    Map<String, Set<String>> securityAttributes = new HashMap<>();
    securityAttributes.put("role", Collections.singleton("myRole"));
    MockCatalogStore store = new MockCatalogStore("catalogStoreId-1", true, securityAttributes);
    storeMap.put(store.getId(), store);
    sourceMap.put(store.getId(), store);

    CatalogFramework framework =
        createDummyCatalogFramework(provider, storeMap, sourceMap, eventAdmin);

    FilterBuilder builder = new GeotoolsFilterBuilder();

    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.CONTENT_TYPE).is().like().text("someType"));
    QueryRequestImpl request =
        new QueryRequestImpl(query, Collections.singletonList("catalogStoreId-1"));
    framework.query(request);
  }

  @Test(expected = FederationException.class)
  public void testFederatedQueryPermissionsNotPermitted() throws Exception {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());

    Map<String, CatalogStore> storeMap = new HashMap<>();
    Map<String, FederatedSource> sourceMap = new HashMap<>();

    Map<String, Set<String>> securityAttributes = new HashMap<>();
    securityAttributes.put("role", Collections.singleton("myRole"));
    MockCatalogStore store = new MockCatalogStore("catalogStoreId-1", true, securityAttributes);
    storeMap.put(store.getId(), store);
    sourceMap.put(store.getId(), store);

    CatalogFramework framework =
        createDummyCatalogFramework(provider, storeMap, sourceMap, eventAdmin);

    FilterBuilder builder = new GeotoolsFilterBuilder();
    Subject subject = mock(Subject.class);
    when(subject.isPermitted(any(KeyValueCollectionPermission.class))).thenReturn(false);
    HashMap<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, subject);
    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.CONTENT_TYPE).is().like().text("someType"));
    QueryRequestImpl request =
        new QueryRequestImpl(
            query, false, Collections.singletonList("catalogStoreId-1"), properties);
    framework.query(request);
  }

  @Test
  public void testFederatedQueryPermissions() throws Exception {
    MockEventProcessor eventAdmin = new MockEventProcessor();
    MockMemoryProvider provider =
        new MockMemoryProvider(
            "Provider", "Provider", "v1.0", "DDF", new HashSet<>(), true, new Date());

    Map<String, CatalogStore> storeMap = new HashMap<>();
    Map<String, FederatedSource> sourceMap = new HashMap<>();

    Map<String, Set<String>> securityAttributes = new HashMap<>();
    securityAttributes.put("role", Collections.singleton("myRole"));
    MockCatalogStore store = new MockCatalogStore("catalogStoreId-1", true, securityAttributes);
    storeMap.put(store.getId(), store);
    sourceMap.put(store.getId(), store);

    CatalogFramework framework =
        createDummyCatalogFramework(provider, storeMap, sourceMap, eventAdmin);

    List<Metacard> metacards = new ArrayList<>();
    MetacardImpl newCard = new MetacardImpl();
    newCard.setId(null);
    newCard.setContentTypeName("someType");
    metacards.add(newCard);
    Map<String, Serializable> reqProps = new HashMap<>();
    HashSet<String> destinations = new HashSet<>();

    // ==== test writing to store and not local ====
    destinations.add("catalogStoreId-1");
    framework.create(new CreateRequestImpl(metacards, reqProps, destinations));

    FilterBuilder builder = new GeotoolsFilterBuilder();
    Subject subject = mock(Subject.class);
    when(subject.isPermitted(any(KeyValueCollectionPermission.class))).thenReturn(true);
    HashMap<String, Serializable> properties = new HashMap<>();
    properties.put(SecurityConstants.SECURITY_SUBJECT, subject);
    QueryImpl query =
        new QueryImpl(builder.attribute(Metacard.CONTENT_TYPE).is().like().text("someType"));
    QueryRequestImpl request =
        new QueryRequestImpl(
            query, false, Collections.singletonList("catalogStoreId-1"), properties);
    QueryResponse response = framework.query(request);

    assertThat(response.getResults().size(), is(1));
  }

  /**
   * Tests that multiple ResourceReaders with the same scheme will be invoked if the first one did
   * not return a Response.
   *
   * @throws Exception
   */
  @Test
  @Ignore // CACHE
  public void
      testGetResourceToTestSecondResourceReaderWithSameSchemeGetsCalledIfFirstDoesNotReturnAnything()
          throws Exception {
    String localProviderName = "ddf";
    final String EXPECTED = "result from mockResourceResponse2";
    final String DDF = "ddf";

    // Mock a Catalog Provider
    CatalogProvider provider = mock(CatalogProvider.class);
    when(provider.getId()).thenReturn(localProviderName);
    when(provider.isAvailable(isA(SourceMonitor.class))).thenReturn(true);
    when(provider.isAvailable()).thenReturn(true);

    // Mock register the provider in the container
    // Mock the source poller
    SourcePoller mockPoller = mock(SourcePoller.class);
    when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

    // Create two ResourceReaders. The first should not return anything
    // and the second should.
    ResourceReader resourceReader1 = mock(ResourceReader.class);
    ResourceReader resourceReader2 = mock(ResourceReader.class);

    // Set the supported Schemes so that both ResourceReaders use
    // the same scheme ("DAD")
    Set<String> supportedSchemes = new HashSet<>();
    supportedSchemes.add("DAD");

    when(resourceReader1.getSupportedSchemes()).thenReturn(supportedSchemes);
    when(resourceReader2.getSupportedSchemes()).thenReturn(supportedSchemes);

    List<ResourceReader> resourceReaders = new ArrayList<>();
    resourceReaders.add(resourceReader1);
    resourceReaders.add(resourceReader2);

    // Set up the requests and responses. The first ResourceReader will return null
    // and the second one will retrieve a value, showing that if more than one
    // ResourceReader with the same scheme are used, they will be called until a
    // response is returned
    ResourceRequest mockResourceRequest = mock(ResourceRequest.class);
    URI myURI = new URI("DAD", "host", "/path", "fragment");
    when(mockResourceRequest.getAttributeValue()).thenReturn(myURI);
    when(mockResourceRequest.getAttributeName())
        .thenReturn(new String(ResourceRequest.GET_RESOURCE_BY_PRODUCT_URI));

    Result result = mock(Result.class);
    Metacard metacard = mock(Metacard.class);
    when(metacard.getResourceURI()).thenReturn(myURI);
    when(result.getMetacard()).thenReturn(metacard);
    List<Result> results = new ArrayList<>();
    results.add(result);

    QueryResponse queryResponse = mock(QueryResponse.class);
    when(queryResponse.getResults()).thenReturn(results);

    List<Source> federatedSources = new ArrayList<>();

    FederationStrategy strategy = mock(FederationStrategy.class);
    when(strategy.federate(isA(federatedSources.getClass()), isA(QueryRequest.class)))
        .thenReturn(queryResponse);

    ResourceResponse mockResourceResponse1 = mock(ResourceResponse.class);
    when(mockResourceResponse1.getRequest()).thenReturn(mockResourceRequest);
    when(mockResourceResponse1.getResource()).thenReturn(null);
    when(resourceReader1.retrieveResource(any(URI.class), anyMap())).thenReturn(null);

    Resource mockResource = mock(Resource.class);
    when(mockResource.getName()).thenReturn(EXPECTED);
    ResourceResponse mockResourceResponse2 = mock(ResourceResponse.class);
    when(mockResourceResponse2.getResource()).thenReturn(mockResource);
    when(resourceReader2.retrieveResource(any(URI.class), anyMap()))
        .thenReturn(mockResourceResponse2);

    FrameworkProperties frameworkProperties = new FrameworkProperties();
    frameworkProperties.setSourcePoller(mockPoller);
    frameworkProperties.setResourceReaders(resourceReaders);
    frameworkProperties.setFederationStrategy(strategy);
    frameworkProperties.setCatalogProviders(Collections.singletonList(provider));

    SourceOperations sourceOps = new SourceOperations(frameworkProperties);
    QueryOperations queryOps = new QueryOperations(frameworkProperties, sourceOps, null, null);
    ResourceOperations resOps = new ResourceOperations(frameworkProperties, queryOps, null);
    resOps.setId(DDF);

    CatalogFrameworkImpl catalogFramework =
        new CatalogFrameworkImpl(null, null, null, null, resOps, null, null);

    sourceOps.bind(provider);
    ResourceResponse response = catalogFramework.getResource(mockResourceRequest, DDF);

    // Verify that the Response is as expected
    org.junit.Assert.assertEquals(EXPECTED, response.getResource().getName());

    // Verify that resourceReader1 was called 1 time
    // This line is equivalent to verify(resourceReader1,
    // times(1)).retrieveResource(any(URI.class), anyMap());
    verify(resourceReader1).retrieveResource(any(URI.class), anyMap());
  }

  /** ************************** utility methods **************************** */
  private void stubMetacardInjection(AttributeDescriptor... injectedAttributes) {
    doAnswer(
            invocationOnMock -> {
              Metacard original = (Metacard) invocationOnMock.getArguments()[0];
              MetacardType originalMetacardType = original.getMetacardType();
              MetacardType newMetacardType =
                  new MetacardTypeImpl(
                      originalMetacardType.getName(),
                      originalMetacardType,
                      Sets.newHashSet(injectedAttributes));
              MetacardImpl newMetacard = new MetacardImpl(original);
              newMetacard.setType(newMetacardType);
              newMetacard.setSourceId(original.getSourceId());
              return newMetacard;
            })
        .when(attributeInjector)
        .injectAttributes(any(Metacard.class));
  }

  private List<FederatedSource> createDefaultFederatedSourceList(boolean isAvailable) {
    FederatedSource siteA =
        new MockSource("A", "Site A", "v1.0", "DDF", null, isAvailable, new Date());
    FederatedSource siteB =
        new MockSource("B", "Site B", "v1.0", "DDF", null, isAvailable, new Date());
    FederatedSource siteC =
        new MockSource("C", "Site C", "v1.0", "DDF", null, isAvailable, new Date());
    ArrayList<FederatedSource> federatedSources = new ArrayList<>();
    federatedSources.add(siteC);
    federatedSources.add(siteB);
    federatedSources.add(siteA);

    return federatedSources;
  }

  private CatalogFramework createDummyCatalogFramework(
      CatalogProvider provider,
      Map<String, CatalogStore> stores,
      Map<String, FederatedSource> sources,
      MockEventProcessor eventAdmin) {

    SourcePoller mockPoller = mock(SourcePoller.class);
    when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(null);

    FederationStrategy federationStrategy =
        new FederationStrategy() {
          @Override
          public QueryResponse federate(List<Source> sources, QueryRequest query)
              throws FederationException {
            List<Result> results = new ArrayList<>();
            for (Source source : sources) {
              try {
                SourceResponse response = source.query(query);
                results.addAll(response.getResults());
              } catch (UnsupportedQueryException e) {

              }
            }
            return new QueryResponseImpl(query, results, results.size());
          }
        };

    ArrayList<PostIngestPlugin> postIngestPlugins = new ArrayList<>();
    postIngestPlugins.add(eventAdmin);
    FrameworkProperties frameworkProperties = new FrameworkProperties();
    frameworkProperties.setCatalogProviders(Collections.singletonList(provider));
    frameworkProperties.setStorageProviders(Collections.singletonList(storageProvider));
    frameworkProperties.setCatalogStoresMap(stores);
    frameworkProperties.setSourcePoller(mockPoller);
    frameworkProperties.setPreIngest(new ArrayList<>());
    frameworkProperties.setPostIngest(postIngestPlugins);
    frameworkProperties.setPreQuery(new ArrayList<>());
    frameworkProperties.setPostQuery(new ArrayList<>());
    frameworkProperties.setPolicyPlugins(new ArrayList<>());
    frameworkProperties.setAccessPlugins(new ArrayList<>());
    frameworkProperties.setFederatedSources(sources);
    frameworkProperties.setConnectedSources(new ArrayList<>());
    frameworkProperties.setFederationStrategy(federationStrategy);
    frameworkProperties.setQueryResponsePostProcessor(new QueryResponsePostProcessor(null, null));
    frameworkProperties.setFilterBuilder(new GeotoolsFilterBuilder());
    frameworkProperties.setValidationQueryFactory(
        new ValidationQueryFactory(new GeotoolsFilterAdapterImpl(), new GeotoolsFilterBuilder()));
    frameworkProperties.setDefaultAttributeValueRegistry(defaultAttributeValueRegistry);

    return createFramework(frameworkProperties);
  }

  private CatalogFramework createDummyCatalogFramework(
      CatalogProvider provider,
      StorageProvider storageProvider,
      MockEventProcessor admin,
      boolean sourceAvailability) {
    return createDummyCatalogFramework(
        provider, storageProvider, null, admin, sourceAvailability, null);
  }

  private CatalogFramework createDummyCatalogFramework(
      CatalogProvider provider,
      StorageProvider storageProvider,
      BundleContext context,
      MockEventProcessor admin,
      boolean sourceAvailability,
      Transform transform) {
    // Mock register the provider in the container
    // Mock the source poller
    SourcePoller mockPoller = mock(SourcePoller.class);
    CachedSource mockSource = mock(CachedSource.class);
    when(mockSource.isAvailable()).thenReturn(sourceAvailability);
    when(mockPoller.getCachedSource(isA(Source.class))).thenReturn(mockSource);

    FrameworkProperties frameworkProperties = new FrameworkProperties();
    frameworkProperties.setCatalogProviders(Collections.singletonList(provider));
    frameworkProperties.setStorageProviders(Collections.singletonList(storageProvider));
    frameworkProperties.setSourcePoller(mockPoller);
    frameworkProperties.setBundleContext(context);
    frameworkProperties.setDefaultAttributeValueRegistry(defaultAttributeValueRegistry);
    frameworkProperties.setTransform(transform);

    return createFramework(frameworkProperties);
  }

  public static class MockCatalogStore extends MockMemoryProvider implements CatalogStore {
    private Map<String, Set<String>> attributes = new HashMap<>();

    public MockCatalogStore(String id, boolean isAvailable, Map<String, Set<String>> attributes) {
      this(id, isAvailable);
      this.attributes = attributes;
    }

    public MockCatalogStore(String id, boolean isAvailable) {
      super(id, isAvailable);
    }

    @Override
    public Map<String, Set<String>> getSecurityAttributes() {
      return attributes;
    }
  }
}
