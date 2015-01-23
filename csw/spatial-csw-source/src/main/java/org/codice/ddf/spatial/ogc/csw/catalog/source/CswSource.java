/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import com.thoughtworks.xstream.converters.Converter;
import ddf.security.settings.SecuritySettingsService;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.ElementSetNameType;
import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.ObjectFactory;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import net.opengis.filter.v_1_1_0.FilterCapabilities;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.PropertyNameType;
import net.opengis.filter.v_1_1_0.SortByType;
import net.opengis.filter.v_1_1_0.SortOrderType;
import net.opengis.filter.v_1_1_0.SortPropertyType;
import net.opengis.filter.v_1_1_0.SpatialCapabilitiesType;
import net.opengis.filter.v_1_1_0.SpatialOperatorNameType;
import net.opengis.filter.v_1_1_0.SpatialOperatorType;
import net.opengis.filter.v_1_1_0.SpatialOperatorsType;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.Operation;
import net.opengis.ows.v_1_0_0.OperationsMetadata;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.codice.ddf.spatial.ogc.catalog.MetadataTransformer;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityCommand;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.codice.ddf.spatial.ogc.catalog.common.TrustedRemoteSource;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswTransformProvider;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortOrder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.util.impl.MaskableImpl;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.sts.client.configuration.STSClientConfiguration;

/**
 * CswSource provides a DDF {@link FederatedSource} and {@link ConnectedSource} for CSW 2.0.2
 * services.
 */
public class CswSource extends MaskableImpl implements FederatedSource, ConnectedSource {

    private FilterBuilder filterBuilder;

    protected CswSourceConfiguration cswSourceConfiguration;

    private FilterAdapter filterAdapter;

    protected CswFilterDelegate cswFilterDelegate;

    private Set<SourceMonitor> sourceMonitors = new HashSet<SourceMonitor>();

    private Map<String, ContentType> contentTypes;

    private List<ResourceReader> resourceReaders;

    private DomainType supportedOutputSchemas;

    private Set<ElementSetType> detailLevels;

    protected RemoteCsw remoteCsw;

    private BundleContext context;

    private String description = null;

    private CapabilitiesType capabilities;

    private List<ServiceRegistration<?>> registeredMetacardTypes = new ArrayList<ServiceRegistration<?>>();

    private String cswVersion;

    protected boolean contentTypeMappingUpdated;

    protected Converter cswTransformProvider;

    private static final Logger LOGGER = LoggerFactory.getLogger(CswSource.class);

    private static final String DEFAULT_CSW_TRANSFORMER_ID = "csw";

    protected static final String CSW_SERVER_ERROR = "Error received from CSW server.";

    private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

    private static final String DESCRIPTION = "description";

    private static final String ORGANIZATION = "organization";

    private static final String VERSION = "version";

    private static final String TITLE = "name";

    private static final int CONTENT_TYPE_SAMPLE_SIZE = 50;

    private static Properties describableProperties = new Properties();

    private static final JAXBContext JAXB_CONTEXT = initJaxbContext();

    static {
        try (InputStream properties = CswSource.class.getResourceAsStream(
                DESCRIBABLE_PROPERTIES_FILE)) {
            describableProperties.load(properties);
        } catch (IOException e) {
            LOGGER.info("Failed to load properties", e);
        }
    }

    protected static final String CSWURL_PROPERTY = "cswUrl";

    protected static final String ID_PROPERTY = "id";

    protected static final String USERNAME_PROPERTY = "username";

    protected static final String PASSWORD_PROPERTY = "password";

    protected static final String CONTENTTYPES_PROPERTY = "contentTypeNames";

    protected static final String EFFECTIVE_DATE_MAPPING_PROPERTY = "effectiveDateMapping";

    protected static final String CREATED_DATE_MAPPING_PROPERTY = "createdDateMapping";

    protected static final String MODIFIED_DATE_MAPPING_PROPERTY = "modifiedDateMapping";

    protected static final String CONTENT_TYPE_MAPPING_PROPERTY = "contentTypeMapping";

    protected static final String IS_LON_LAT_ORDER_PROPERTY = "isLonLatOrder";
    
    private static final String USE_POS_LIST_PROPERTY = "usePosList";

    protected static final String POLL_INTERVAL_PROPERTY = "pollInterval";

    protected static final String OUTPUT_SCHEMA_PROPERTY = "outputSchema";

    protected static final String FORCE_SPATIAL_FILTER_PROPERTY = "forceSpatialFilter";

    protected static final String NO_FORCE_SPATIAL_FILTER = "NO_FILTER";

    protected static final String CONNECTION_TIMEOUT_PROPERTY = "connectionTimeout";

    protected static final String RECEIVE_TIMEOUT_PROPERTY = "receiveTimeout";

    protected String forceSpatialFilter = NO_FORCE_SPATIAL_FILTER;

    private SpatialCapabilitiesType spatialCapabilities;

    private ScheduledExecutorService scheduler;

    protected ScheduledFuture<?> availabilityPollFuture;

    private AvailabilityTask availabilityTask;

    private boolean isConstraintCql;

    private SecuritySettingsService securitySettingsService;

    /**
     * Instantiates a CswSource. This constructor is for unit tests
     *
     * @param remoteCsw The JAXRS connection to a {@link Csw}
     * @param context   The {@link BundleContext} from the OSGi Framework
     */
    public CswSource(RemoteCsw remoteCsw, BundleContext context,
            CswSourceConfiguration cswSourceConfiguration, CswTransformProvider provider) {
        this.remoteCsw = remoteCsw;
        this.context = context;
        this.cswSourceConfiguration = cswSourceConfiguration;
        this.cswTransformProvider = provider;
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Instantiates a CswSource.
     */
    public CswSource() {
        cswSourceConfiguration = new CswSourceConfiguration();
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Initializes the CswSource by connecting to the Server
     */

    public void init() {
        LOGGER.debug("{}: Entering init()", cswSourceConfiguration.getId());

        setupAvailabilityPoll();
    }

    /**
     * Clean-up when shutting down the CswSource
     */

    public void destroy() {
        LOGGER.debug("{}: Entering destroy()", cswSourceConfiguration.getId());
        unregisterMetacardTypes();
        availabilityPollFuture.cancel(true);
        scheduler.shutdownNow();
    }

    /**
     * Reinitializes the CswSource when there is a configuration change. Otherwise, it checks with
     * the server to see if any capabilities have changed.
     *
     * @param configuration The configuration with which to connect to the server
     */

    public void refresh(Map<String, Object> configuration) {
        LOGGER.debug("{}: Entering refresh()", cswSourceConfiguration.getId());

        if (configuration == null) {
            LOGGER.error("Recieved null configuration during refresh for {}: {}", this.getClass()
                    .getSimpleName(), cswSourceConfiguration.getId());
            return;
        }

        String idProp = (String) configuration.get(ID_PROPERTY);
        if (StringUtils.isNotBlank(idProp)) {
            cswSourceConfiguration.setId(idProp);
        }

        String cswUrlProp = (String) configuration.get(CSWURL_PROPERTY);
        if (StringUtils.isNotBlank(cswUrlProp)) {
            cswSourceConfiguration.setCswUrl(cswUrlProp);
        }

        String passProp = (String) configuration.get(PASSWORD_PROPERTY);
        if (StringUtils.isNotBlank(passProp)) {
            cswSourceConfiguration.setPassword(passProp);
        }

        String userProp = (String) configuration.get(USERNAME_PROPERTY);
        if (StringUtils.isNotBlank(userProp)) {
            cswSourceConfiguration.setUsername(userProp);
        }

        Integer newConnTimeout = (Integer) configuration.get(CONNECTION_TIMEOUT_PROPERTY);
        if (newConnTimeout != null) {
            cswSourceConfiguration.setConnectionTimeout(newConnTimeout);
        }

        Integer newRecTimeout = (Integer) configuration.get(RECEIVE_TIMEOUT_PROPERTY);
        if (newRecTimeout != null) {
            cswSourceConfiguration.setReceiveTimeout(newRecTimeout);
        }

        updateTimeouts();

        String schemaProp = (String) configuration.get(OUTPUT_SCHEMA_PROPERTY);
        if (StringUtils.isNotBlank(schemaProp)) {
            String oldOutputSchema = cswSourceConfiguration.getOutputSchema();
            cswSourceConfiguration.setOutputSchema(schemaProp);

            LOGGER.debug("{}: new output schema: {}", cswSourceConfiguration.getId(),
                    cswSourceConfiguration.getOutputSchema());
            LOGGER.debug("{}: old output schema: {}", cswSourceConfiguration.getId(),
                    oldOutputSchema);
        }

        Boolean sslProp = (Boolean) configuration
                .get(TrustedRemoteSource.DISABLE_CN_CHECK_PROPERTY);
        if (sslProp != null) {
            cswSourceConfiguration.setDisableCnCheck(sslProp);
        }

        Boolean latLonProp = (Boolean) configuration.get(IS_LON_LAT_ORDER_PROPERTY);
        if (latLonProp != null) {
            cswSourceConfiguration.setIsLonLatOrder(latLonProp);
            if (cswSourceConfiguration.isLonLatOrder()) {
                LOGGER.debug("{}: Setting coordinate ordering to LON/LAT.",
                        cswSourceConfiguration.getId());
            } else {
                LOGGER.debug("{}: Setting coordinate ordering to LAT/LON.",
                        cswSourceConfiguration.getId());
            }
        }

        Boolean posListProp = (Boolean) configuration.get(USE_POS_LIST_PROPERTY);
        if (posListProp != null) {
            cswSourceConfiguration.setUsePosList(posListProp);
        }

        String spatialFilter = (String) configuration.get(FORCE_SPATIAL_FILTER_PROPERTY);
        if (StringUtils.isBlank(spatialFilter)) {
            spatialFilter = NO_FORCE_SPATIAL_FILTER;
        }
        forceSpatialFilter = spatialFilter;


        String[] contentTypeNames = (String[]) configuration.get(CONTENTTYPES_PROPERTY);
        if (contentTypeNames != null) {
            setContentTypeNames(Arrays.asList(contentTypeNames));
        }

        String createdProp = (String) configuration.get(CREATED_DATE_MAPPING_PROPERTY);
        if (StringUtils.isNotBlank(createdProp)) {
            cswSourceConfiguration.setCreatedDateMapping(createdProp);
        }

        String effectiveProp = (String) configuration.get(EFFECTIVE_DATE_MAPPING_PROPERTY);
        if (StringUtils.isNotBlank(effectiveProp)) {
            cswSourceConfiguration.setEffectiveDateMapping(effectiveProp);
        }

        String modifiedProp = (String) configuration.get(MODIFIED_DATE_MAPPING_PROPERTY);
        if (StringUtils.isNotBlank(modifiedProp)) {
            cswSourceConfiguration.setModifiedDateMapping(modifiedProp);
        }

        String currentContentTypeMapping = ((String) configuration
                .get(CONTENT_TYPE_MAPPING_PROPERTY));

        if (StringUtils.isNotBlank(currentContentTypeMapping)) {
            String previousContentTypeMapping = cswSourceConfiguration.getContentTypeMapping();
            LOGGER.debug("{}: Previous content type mapping: {}.", cswSourceConfiguration.getId(),
                    previousContentTypeMapping);
            contentTypeMappingUpdated = !currentContentTypeMapping
                    .equals(previousContentTypeMapping);
            currentContentTypeMapping = currentContentTypeMapping.trim();
            if (contentTypeMappingUpdated) {
                LOGGER.debug("{}: The content type has been updated from {} to {}.",
                        cswSourceConfiguration.getId(), previousContentTypeMapping,
                        currentContentTypeMapping);
            }
        } else {
            currentContentTypeMapping = CswRecordMetacardType.CSW_TYPE;
        }

        cswSourceConfiguration.setContentTypeMapping(currentContentTypeMapping);

        LOGGER.debug("{}: Current content type mapping: {}.", cswSourceConfiguration.getId(),
                currentContentTypeMapping);

        connectToRemoteCsw();
        configureCswSource();

        Integer newPollInterval = (Integer) configuration.get(POLL_INTERVAL_PROPERTY);

        if (newPollInterval != null
                && !newPollInterval.equals(cswSourceConfiguration.getPollIntervalMinutes())) {
            LOGGER.debug("Poll Interval was changed for source {}.",
                    cswSourceConfiguration.getId());
            cswSourceConfiguration.setPollIntervalMinutes(newPollInterval);
            availabilityPollFuture.cancel(true);
            setupAvailabilityPoll();
        }
    }

    protected void setupAvailabilityPoll() {
        LOGGER.debug("Setting Availability poll task for {} minute(s) on Source {}",
                cswSourceConfiguration.getPollIntervalMinutes(), cswSourceConfiguration.getId());
        CswSourceAvailabilityCommand command = new CswSourceAvailabilityCommand();
        long interval = TimeUnit.MINUTES.toMillis(cswSourceConfiguration.getPollIntervalMinutes());
        if (availabilityPollFuture == null || availabilityPollFuture.isCancelled()) {
            if (availabilityTask == null) {
                availabilityTask = new AvailabilityTask(interval, command,
                        cswSourceConfiguration.getId());
            } else {
                availabilityTask.setInterval(interval);
            }

            // Run the availability check immediately prior to scheduling it in a thread.
            // This is necessary to allow the catalog framework to have the correct
            // availability when the source is bound
            availabilityTask.run();
            // Schedule the availability check every 1 second. The actually call to
            // the remote server will only occur if the pollInterval has
            // elapsed.
            availabilityPollFuture = scheduler.scheduleWithFixedDelay(availabilityTask,
                    AvailabilityTask.NO_DELAY, AvailabilityTask.ONE_SECOND, TimeUnit.SECONDS);
        } else {
            LOGGER.debug("No changes being made on the poller.");
        }

    }

    protected void connectToRemoteCsw() {
        LOGGER.debug("Connecting to remote CSW Server " + cswSourceConfiguration.getCswUrl());

        try {
            remoteCsw = new RemoteCsw(cswTransformProvider, cswSourceConfiguration);
            remoteCsw.setSecuritySettings(securitySettingsService);
            remoteCsw.setTlsParameters();
            remoteCsw.setTimeouts(cswSourceConfiguration.getConnectionTimeout(),
                    cswSourceConfiguration.getReceiveTimeout());
        } catch (IllegalArgumentException iae) {
            LOGGER.error("Unable to create RemoteCsw.", iae);
            remoteCsw = null;
        }
    }

    public void setConnectionTimeout(Integer timeout) {
        this.cswSourceConfiguration.setConnectionTimeout(timeout);
    }

    public void setReceiveTimeout(Integer timeout) {
        this.cswSourceConfiguration.setReceiveTimeout(timeout);
    }

    public void updateTimeouts() {
        if (remoteCsw != null) {
            remoteCsw.setTimeouts(cswSourceConfiguration.getConnectionTimeout(),
                    cswSourceConfiguration.getReceiveTimeout());
        }
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    @Override
    public Set<ContentType> getContentTypes() {
        return new HashSet<ContentType>(contentTypes.values());
    }

    public List<String> getContentTypeNames() {
        return new ArrayList<String>(contentTypes.keySet());
    }

    public void setContentTypeNames(List<String> contentTypeNames) {
        this.contentTypes = new HashMap<String, ContentType>();

        for (String contentType : contentTypeNames) {
            addContentType(contentType);
        }
    }

    public List<ResourceReader> getResourceReaders() {
        return resourceReaders;
    }

    public void setResourceReaders(List<ResourceReader> resourceReaders) {
        this.resourceReaders = resourceReaders;
    }

    public void setOutputSchema(String outputSchema) {
        cswSourceConfiguration.setOutputSchema(outputSchema);
        LOGGER.debug("Setting output schema to: {}", outputSchema);
    }

    @Override
    public boolean isAvailable() {
        return availabilityTask.isAvailable();
    }

    @Override
    public boolean isAvailable(SourceMonitor sourceMonitor) {
        sourceMonitors.add(sourceMonitor);
        return isAvailable();
    }

    @Override
    public SourceResponse query(QueryRequest queryRequest) throws UnsupportedQueryException {
        return query(queryRequest, ElementSetType.FULL, null);
    }

    private SourceResponse query(QueryRequest queryRequest, ElementSetType elementSetName,
            List<QName> elementNames) throws UnsupportedQueryException {

        Query query = queryRequest.getQuery();
        LOGGER.debug("{}: Received query:\n{}", cswSourceConfiguration.getId(), query);

        GetRecordsType getRecordsType = createGetRecordsRequest(query, elementSetName,
                elementNames);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{}: GetRecords request:\n {}", cswSourceConfiguration.getId(),
                    getGetRecordsTypeAsXml(getRecordsType));
        }

        LOGGER.debug("{}: Sending query to: {}", cswSourceConfiguration.getId(),
                cswSourceConfiguration.getCswUrl());

        List<Result> results = null;
        Long totalHits = 0L;

        try {

            Subject subject = (Subject)queryRequest.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
            if (subject != null) {
                LOGGER.debug("Setting user credentials on outgoing CSW request.");
                remoteCsw.setSubject(subject);
            } else {
                LOGGER.debug("No user credentials found, sending CSW request with no user information.");
            }
            CswRecordCollection cswRecordCollection = this.remoteCsw.getRecords(getRecordsType);

            if (cswRecordCollection == null) {
                throw new UnsupportedQueryException("Invalid results returned from server");
            }
            this.availabilityTask.updateLastAvailableTimestamp(System.currentTimeMillis());
            LOGGER.debug("{}: Received [{}] record(s) of the [{}] record(s) matched from {}.",
                    cswSourceConfiguration.getId(),
                    cswRecordCollection.getNumberOfRecordsReturned(),
                    cswRecordCollection.getNumberOfRecordsMatched(),
                    cswSourceConfiguration.getCswUrl());
                
            results = createResults(cswRecordCollection);
            totalHits = cswRecordCollection.getNumberOfRecordsMatched();
        } catch (CswException cswe) {
            LOGGER.error(CSW_SERVER_ERROR, cswe);
            throw new UnsupportedQueryException(CSW_SERVER_ERROR, cswe);
        } catch (WebApplicationException wae) {
            String msg = handleWebApplicationException(wae);
            throw new UnsupportedQueryException(msg, wae);
        } catch (ClientException ce) {
            String msg = handleClientException(ce);
            throw new UnsupportedQueryException(msg, ce);
        }

        LOGGER.debug("{}: Adding {} result(s) to the source response.",
                cswSourceConfiguration.getId(), results.size());

        SourceResponseImpl sourceResponse = new SourceResponseImpl(queryRequest, results,
                totalHits);
        addContentTypes(sourceResponse);
        unregisterMetacardTypes();
        registerMetacardTypes();

        return sourceResponse;
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(describableProperties.getProperty(DESCRIPTION))
                .append(System.getProperty("line.separator")).append(description);
        return sb.toString();
    }

    @Override
    public String getId() {
        String sourceId = super.getId();
        // Note, returning "UNKNOWN" causes issues for collecting source metrics on
        // ConnectedSources. This method is called initially when the connected source is first
        // added and the sourceId is null at that time, but this causes metrics for an UNKNOWN
        // source to be created and never deleted. Returning super.getId() for the ConnectedSources
        // until a problem is discovered.
        return sourceId;
    }

    @Override
    public void maskId(String newSourceId) {
        final String methodName = "maskId";
        LOGGER.debug("ENTERING: {} with sourceId = {}", methodName, newSourceId);

        if (newSourceId != null) {
            super.maskId(newSourceId);
        }

        LOGGER.debug("EXITING: {}", methodName);
    }

    @Override
    public String getOrganization() {
        return describableProperties.getProperty(ORGANIZATION);
    }

    @Override
    public String getTitle() {
        return describableProperties.getProperty(TITLE);
    }

    @Override
    public String getVersion() {
        if (StringUtils.isNotBlank(cswVersion)) {
            return cswVersion;
        }
        return describableProperties.getProperty(VERSION);
    }

    @Override
    public Set<String> getOptions(Metacard arg0) {
        return null;
    }

    @Override
    public Set<String> getSupportedSchemes() {
        return null;
    }

    @Override
    public ResourceResponse retrieveResource(URI resourceUri, Map<String, Serializable> properties)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {

        LOGGER.debug("retrieving resource at : {}", resourceUri);

        ResourceResponse resource = null;

        if (resourceUri == null) {
            throw new ResourceNotFoundException("Unable to find resource due to null URI");
        }

        String scheme = resourceUri.getScheme();
        LOGGER.debug("Searching for ResourceReader that supports scheme = " + scheme);

        LOGGER.debug("resourceReaders.size() = {}", resourceReaders.size());
        Iterator<ResourceReader> iterator = resourceReaders.iterator();
        while (iterator.hasNext() && resource == null) {
            ResourceReader reader = iterator.next();
            if (reader.getSupportedSchemes() != null && reader.getSupportedSchemes().contains(
                    scheme)) {
                try {
                    LOGGER.debug("Found an acceptable resource reader ({}) for URI {}",
                            reader.getId(), resourceUri.toASCIIString());
                    resource = reader.retrieveResource(resourceUri, properties);
                    if (resource == null) {
                        LOGGER.info(
                                "Resource returned from ResourceReader {} was null. Checking other readers for URI: {}",
                                reader.getId(), resourceUri);
                    }
                } catch (ResourceNotFoundException e) {
                    LOGGER.debug(
                            "Enterprise Search: Product not found using resource reader with name {}",
                            reader.getId(), e);
                } catch (ResourceNotSupportedException e) {
                    LOGGER.debug(
                            "Enterprise Search: Product not found using resource reader with name {}",
                            reader.getId(), e);
                } catch (IOException ioe) {
                    LOGGER.debug(
                            "Enterprise Search: Product not found using resource reader with name {}",
                            reader.getId(), ioe);
                }
            }
        }

        if (resource == null) {
            throw new ResourceNotFoundException(
                    "Resource Readers could not find resource (or returned null resource) for URI: "
                            + resourceUri);
        }
        LOGGER.debug("Received resource, sending back: {}", resource.getResource().getName());

        return resource;
    }

    public void setCswUrl(String cswUrl) {
        LOGGER.debug("Setting cswUrl to {}", cswUrl);

        cswSourceConfiguration.setCswUrl(cswUrl);
    }

    public void setId(String id) {
        cswSourceConfiguration.setId(id);
        super.setId(id);
    }

    public void setUsername(String username) {
        cswSourceConfiguration.setUsername(username);
    }

    public void setPassword(String password) {
        cswSourceConfiguration.setPassword(password);
    }

    public void setDisableCnCheck(Boolean disableCnCheck) {
        cswSourceConfiguration.setDisableCnCheck(disableCnCheck);
    }

    public void setIsLonLatOrder(Boolean isLonLatOrder) {
        cswSourceConfiguration.setIsLonLatOrder(isLonLatOrder);
        LOGGER.debug("{}: LON/LAT order: {}", cswSourceConfiguration.getId(),
                cswSourceConfiguration.isLonLatOrder());
    }
    
    public void setUsePosList(Boolean usePosList) {
        cswSourceConfiguration.setUsePosList(usePosList);
        LOGGER.debug("Using posList rather than individual pos elements?: {}", 
                usePosList);
    }

    public void setIsCqlForced(Boolean isCqlForced) {
        cswSourceConfiguration.setIsCqlForced(isCqlForced);
    }

    public void setEffectiveDateMapping(String effectiveDateMapping) {
        cswSourceConfiguration.setEffectiveDateMapping(effectiveDateMapping);
    }

    public void setCreatedDateMapping(String createdDateMapping) {
        cswSourceConfiguration.setCreatedDateMapping(createdDateMapping);
    }

    public void setModifiedDateMapping(String modifiedDateMapping) {
        cswSourceConfiguration.setModifiedDateMapping(modifiedDateMapping);
    }

    public void setResourceUriMapping(String resourceUriMapping) {
        cswSourceConfiguration.setResourceUriMapping(resourceUriMapping);
    }

    public void setThumbnailMapping(String thumbnailMapping) {
        cswSourceConfiguration.setThumbnailMapping(thumbnailMapping);
    }

    public void setFilterAdapter(FilterAdapter filterAdapter) {
        this.filterAdapter = filterAdapter;
    }

    public void setFilterBuilder(FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
    }

    public void setContentTypeMapping(String contentTypeMapping) {
        if (StringUtils.isEmpty(contentTypeMapping)) {
            contentTypeMapping = CswRecordMetacardType.CSW_TYPE;
        }
        cswSourceConfiguration.setContentTypeMapping(contentTypeMapping);
    }

    public void setPollInterval(Integer interval) {
        this.cswSourceConfiguration.setPollIntervalMinutes(interval);
    }

    public void setCswTransformProvider(Converter provider) {
        this.cswTransformProvider = provider;
    }

    public Converter getCswTransformProvider() {
        return this.cswTransformProvider;
    }
   
    public String getForceSpatialFilter() {
        return forceSpatialFilter;
    }

    public void setForceSpatialFilter(String forceSpatialFilter) {
        this.forceSpatialFilter = forceSpatialFilter;
    }

    private GetRecordsType createGetRecordsRequest(Query query, ElementSetType elementSetName,
            List<QName> elementNames) throws UnsupportedQueryException {
        GetRecordsType getRecordsType = new GetRecordsType();
        getRecordsType.setVersion(cswVersion);
        getRecordsType.setService(CswConstants.CSW);
        getRecordsType.setResultType(ResultType.RESULTS);
        getRecordsType.setStartPosition(BigInteger.valueOf(query.getStartIndex()));
        getRecordsType.setMaxRecords(BigInteger.valueOf(query.getPageSize()));
        getRecordsType.setOutputFormat(MediaType.APPLICATION_XML);
        if (!isOutputSchemaSupported()) {
            String msg = "CSW Source: " + cswSourceConfiguration.getId()
                    + " does not support output schema: "
                    + cswSourceConfiguration.getOutputSchema() + ".";
            throw new UnsupportedQueryException(msg);
        }
        getRecordsType.setOutputSchema(cswSourceConfiguration.getOutputSchema());
        getRecordsType.setAbstractQuery(createQuery(query, elementSetName, elementNames));
        return getRecordsType;
    }

    private ElementSetNameType createElementSetName(ElementSetType type) {
        ElementSetNameType elementSetNameType = new ElementSetNameType();
        elementSetNameType.setValue(type);
        return elementSetNameType;
    }

    private JAXBElement<QueryType> createQuery(Query query, ElementSetType elementSetType,
            List<QName> elementNames) throws UnsupportedQueryException {
        QueryType queryType = new QueryType();
        queryType.setTypeNames(Arrays.asList(new QName[] {new QName(CswConstants.CSW_OUTPUT_SCHEMA,
                CswConstants.CSW_RECORD_LOCAL_NAME, CswConstants.CSW_NAMESPACE_PREFIX)}));
        if (null != elementSetType) {
            queryType.setElementSetName(createElementSetName(elementSetType));
        } else if (!CollectionUtils.isEmpty(elementNames)) {
            queryType.setElementName(elementNames);
        } else {
            queryType.setElementSetName(createElementSetName(ElementSetType.FULL));
        }
        SortByType sortBy = createSortBy(query);
        if (sortBy != null) {
            queryType.setSortBy(sortBy);
        }
        QueryConstraintType constraint = createQueryConstraint(query);
        if (null != constraint) {
            queryType.setConstraint(constraint);
        }
        ObjectFactory objectFactory = new ObjectFactory();
        return objectFactory.createQuery(queryType);
    }

    private SortByType createSortBy(Query query) {

        SortByType sortBy = null;

        if (query.getSortBy() != null) {
            sortBy = new SortByType();
            SortPropertyType sortProperty = new SortPropertyType();
            PropertyNameType propertyName = new PropertyNameType();
            String propName = query.getSortBy().getPropertyName().getPropertyName();
            if (propName != null) {
                if (Result.TEMPORAL.equals(propName) || Metacard.ANY_DATE.equals(propName)) {
                    propName = Metacard.MODIFIED;
                } else if (Result.RELEVANCE.equals(propName) || Metacard.ANY_TEXT
                        .equals(propName)) {
                    propName = Metacard.TITLE;
                } else if (Result.DISTANCE.equals(propName) || Metacard.ANY_GEO.equals(propName)) {
                    return null;
                }
            }

            propertyName.setContent(Arrays.asList((Object) cswFilterDelegate
                    .mapPropertyName(propName)));
            sortProperty.setPropertyName(propertyName);
            if (SortOrder.DESCENDING.equals(query.getSortBy().getSortOrder())) {
                sortProperty.setSortOrder(SortOrderType.DESC);
            } else {
                sortProperty.setSortOrder(SortOrderType.ASC);
            }
            sortBy.getSortProperty().add(sortProperty);
        }

        return sortBy;
    }

    private QueryConstraintType createQueryConstraint(Query query)
            throws UnsupportedQueryException {
        FilterType filter = createFilter(query);
        if (null == filter) {
            return null;
        }
        QueryConstraintType queryConstraintType = new QueryConstraintType();
        queryConstraintType.setVersion(CswConstants.CONSTRAINT_VERSION);
        if (isConstraintCql || cswSourceConfiguration.isCqlForced()) {
            queryConstraintType.setCqlText(CswCqlTextFilter.getInstance().getCqlText(filter));
        } else {
            queryConstraintType.setFilter(filter);
        }
        return queryConstraintType;
    }

    private FilterType createFilter(Query query) throws UnsupportedQueryException {
        OperationsMetadata operationsMetadata = capabilities.getOperationsMetadata();
        if (null == operationsMetadata) {
            LOGGER.error("{}: CSW Source contains no operations", cswSourceConfiguration.getId());
            return new FilterType();
        }
        if (null == capabilities.getFilterCapabilities()) {
            LOGGER.warn(
                    "{}: CSW Source did not provide Filter Capabilities, unable to preform query.",
                    cswSourceConfiguration.getId());
            throw new UnsupportedQueryException(cswSourceConfiguration.getId()
                    + ": CSW Source did not provide Filter Capabilities, unable to preform query.");
        }
        
        return this.filterAdapter.adapt(query, cswFilterDelegate);
    }

    protected List<Result> createResults(CswRecordCollection cswRecordCollection) {
        List<Result> results = new ArrayList<Result>();

        LOGGER.debug("Found {} metacard(s) in the CswRecordCollection.", cswRecordCollection
                .getCswRecords().size());

        String transformerId = getMetadataTransformerId();

        MetadataTransformer transformer = lookupMetadataTransformer(transformerId);

        for (Metacard metacard : cswRecordCollection.getCswRecords()) {
            metacard.setSourceId(getId());
            if (transformer != null) {
                metacard = transform(metacard, transformer);
            }
            Result result = new ResultImpl(metacard);
            results.add(result);
        }

        return results;
    }

    protected String getMetadataTransformerId() {
        return DEFAULT_CSW_TRANSFORMER_ID;
    }

    /**
     * Transforms the Metacard created from the CSW Record using the transformer specified by its
     * ID.
     *
     * @param metacard
     * @return
     */
    protected Metacard transform(Metacard metacard, MetadataTransformer transformer) {

        if (metacard == null) {
            throw new IllegalArgumentException(cswSourceConfiguration.getId()
                    + ": Metacard is null.");
        }

        try {
            return transformer.transform(metacard);
        } catch (CatalogTransformerException e) {
            LOGGER.warn("{} :Metadata Transformation Failed for metacard: {}",
                    cswSourceConfiguration.getId(), metacard.getId(), e);
        }
        return metacard;

    }

    protected MetadataTransformer lookupMetadataTransformer(String transformerId) {
        ServiceReference<?>[] refs = null;

        try {
            refs = context.getServiceReferences(MetadataTransformer.class.getName(), "("
                    + Constants.SERVICE_ID + "=" + transformerId + ")");
        } catch (InvalidSyntaxException e) {
            LOGGER.warn(cswSourceConfiguration.getId() + ": Invalid transformer ID.", e);
            return null;
        }

        if (refs == null || refs.length == 0) {
            LOGGER.info("{}: Metadata Transformer " + transformerId + " not found.",
                    cswSourceConfiguration.getId());
            return null;
        } else {
            return (MetadataTransformer) context.getService(refs[0]);
        }
    }

    private String getGetRecordsTypeAsXml(GetRecordsType getRecordsType) {
        Writer writer = new StringWriter();
        try {
            Marshaller marshaller = JAXB_CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            JAXBElement<GetRecordsType> jaxbElement = new JAXBElement<GetRecordsType>(new QName(
                    CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.GET_RECORDS),
                    GetRecordsType.class, getRecordsType);
            marshaller.marshal(jaxbElement, writer);
        } catch (JAXBException e) {
            LOGGER.error("{}: Unable to marshall {} to XML.", cswSourceConfiguration.getId(),
                    GetRecordsType.class, e);
        }
        return writer.toString();
    }

    protected CapabilitiesType getCapabilities() {
        CapabilitiesType caps = null;
        try {
            if (remoteCsw != null) {
                LOGGER.debug("Doing getCapabilities() call for CSW");
                GetCapabilitiesRequest request = new GetCapabilitiesRequest(CswConstants.CSW);
                request.setAcceptVersions(CswConstants.VERSION_2_0_2 + ","
                        + CswConstants.VERSION_2_0_1);

                if (context != null) {
                    LOGGER.debug("Checking if STSClientConfiguration is in OSGi registry");
                    ServiceReference ref = context.getServiceReference(STSClientConfiguration.class
                            .getName());

                    if (ref != null) {
                        STSClientConfiguration stsClientConfig = (STSClientConfiguration) context.getService(ref);
                        if (stsClientConfig != null) {
                            LOGGER.debug("stsClientConfig is not null - setting SAML assertion");
                            remoteCsw.setSAMLAssertion(stsClientConfig);
                        } else {
                            LOGGER.debug("stsClientConfig = null, so no security configured");
                        }
                    }
                }
                
                caps = remoteCsw.getCapabilities(request);
            }
        } catch (CswException cswe) {
            LOGGER.error(CSW_SERVER_ERROR, cswe);
        } catch (WebApplicationException wae) {
            LOGGER.error(wae.getMessage(), wae);
            handleWebApplicationException(wae);
        } catch (ClientException ce) {
            handleClientException(ce);
        }
        return caps;
    }

    protected void configureCswSource() {
        detailLevels = EnumSet.noneOf(ElementSetType.class);

        capabilities = getCapabilities();

        if (null != capabilities) {
            cswVersion = capabilities.getVersion();
            if (CswConstants.VERSION_2_0_1.equals(cswVersion)) {
                remoteCsw.setCsw201();
            }
            if (capabilities.getFilterCapabilities() == null) {
                return;
            }

            readGetRecordsOperation(capabilities);

            loadContentTypes();
            LOGGER.debug("{}: {}", cswSourceConfiguration.getId(), capabilities.toString());
        } else {
            LOGGER.error("{}: CSW Server did not return any capabilities.",
                    cswSourceConfiguration.getId());
        }
    }

    private Operation getOperation(OperationsMetadata operations, String operation) {
        for (Operation op : operations.getOperation()) {
            if (operation.equals(op.getName())) {
                return op;
            }
        }

        LOGGER.error("{}: CSW Server did not contain getRecords operation",
                cswSourceConfiguration.getId());
        return null;

    }

    /**
     * Parses the getRecords {@link Operation} to understand the capabilities of the Csw Server. A
     * sample GetRecords Operation may look like this:
     * <p/>
     * <pre>
     *   <ows:Operation name="GetRecords">
     *     <ows:DCP>
     *       <ows:HTTP>
     *         <ows:Get  xlink:href="http://www.cubewerx.com/cwcsw.cgi?" />
     *         <ows:Post xlink:href="http://www.cubewerx.com/cwcsw.cgi" />
     *       </ows:HTTP>
     *     </ows:DCP>
     *     <ows:Parameter name="TypeName">
     *       <ows:Value>csw:Record</ows:Value>
     *     </ows:Parameter>
     *     <ows:Parameter name="outputFormat">
     *       <ows:Value>application/xml</ows:Value>
     *       <ows:Value>text/html</ows:Value>
     *       <ows:Value>text/plain</ows:Value>
     *     </ows:Parameter>
     *     <ows:Parameter name="outputSchema">
     *       <ows:Value>http://www.opengis.net/cat/csw/2.0.2</ows:Value>
     *     </ows:Parameter>
     *     <ows:Parameter name="resultType">
     *       <ows:Value>hits</ows:Value>
     *       <ows:Value>results</ows:Value>
     *       <ows:Value>validate</ows:Value>
     *     </ows:Parameter>
     *     <ows:Parameter name="ElementSetName">
     *       <ows:Value>brief</ows:Value>
     *       <ows:Value>summary</ows:Value>
     *       <ows:Value>full</ows:Value>
     *     </ows:Parameter>
     *     <ows:Parameter name="CONSTRAINTLANGUAGE">
     *       <ows:Value>Filter</ows:Value>
     *     </ows:Parameter>
     *   </ows:Operation>
     * </pre>
     * 
     * @param capabilitiesType
     *            The capabilities the Csw Server supports
     */
    private void readGetRecordsOperation(CapabilitiesType capabilitiesType) {
        OperationsMetadata operationsMetadata = capabilitiesType.getOperationsMetadata();
        if (null == operationsMetadata) {
            LOGGER.error("{}: CSW Source contains no operations", cswSourceConfiguration.getId());
            return;
        }

        description = capabilitiesType.getServiceIdentification().getAbstract();

        Operation getRecordsOp = getOperation(operationsMetadata, CswConstants.GET_RECORDS);

        if (null == getRecordsOp) {
            LOGGER.error("{}: CSW Source contains no getRecords Operation",
                    cswSourceConfiguration.getId());
            return;
        }

        this.supportedOutputSchemas = getParameter(getRecordsOp,
                CswConstants.OUTPUT_SCHEMA_PARAMETER);

        DomainType constraintLanguage = getParameter(getRecordsOp,
                CswConstants.CONSTRAINT_LANGUAGE_PARAMETER);
        if (null != constraintLanguage) {
            DomainType outputFormatValues = getParameter(getRecordsOp,
                    CswConstants.OUTPUT_FORMAT_PARAMETER);
            DomainType resultTypesValues = getParameter(getRecordsOp,
                    CswConstants.RESULT_TYPE_PARAMETER);
            readSetDetailLevels(
                    getParameter(getRecordsOp, CswConstants.ELEMENT_SET_NAME_PARAMETER));

            List<String> constraints = new ArrayList<String>();
            for (String s : constraintLanguage.getValue()) {
                constraints.add(s.toLowerCase());
            }

            if (constraints.contains(CswConstants.CONSTRAINT_LANGUAGE_CQL.toLowerCase())
                    && !constraints
                    .contains(CswConstants.CONSTRAINT_LANGUAGE_FILTER.toLowerCase())) {
                isConstraintCql = true;
            } else {
                isConstraintCql = false;
            }

            setFilterDelegate(new CswRecordMetacardType(), getRecordsOp,
                    capabilitiesType.getFilterCapabilities(), outputFormatValues, 
                    resultTypesValues, cswSourceConfiguration);

            spatialCapabilities = capabilitiesType.getFilterCapabilities().getSpatialCapabilities();

            if (!NO_FORCE_SPATIAL_FILTER.equals(forceSpatialFilter)) {
                SpatialOperatorType sot = new SpatialOperatorType();
                SpatialOperatorNameType sont = SpatialOperatorNameType
                        .fromValue(forceSpatialFilter);
                sot.setName(sont);
                sot.setGeometryOperands(cswFilterDelegate.getGeoOpsForSpatialOp(sont));
                SpatialOperatorsType spatialOperators = new SpatialOperatorsType();
                spatialOperators.setSpatialOperator(Arrays.asList(sot));
                cswFilterDelegate.setSpatialOps(spatialOperators);
            }
        }
    }

    /**
     * Sets the {@link FilterDelegate} used by the CswSource. May be overridden
     * in order to provide a custom FilterDelegate implementation.
     *
     * @param cswRecordMetacardType
     * @param getRecordsOp
     * @param filterCapabilities
     * @param outputFormatValues
     * @param resultTypesValues
     * @param cswSourceConfiguration
     */
    protected void setFilterDelegate(CswRecordMetacardType cswRecordMetacardType,
            Operation getRecordsOp, FilterCapabilities filterCapabilities,
            DomainType outputFormatValues, DomainType resultTypesValues,
            CswSourceConfiguration cswSourceConfiguration) {
        LOGGER.trace("Setting cswFilterDelegate to default CswFilterDelegate");

        cswFilterDelegate = new CswFilterDelegate(cswRecordMetacardType, getRecordsOp,
                filterCapabilities, outputFormatValues, resultTypesValues,
                cswSourceConfiguration);
    }

    private void readSetDetailLevels(DomainType elementSetNamesValues) {
        if (null != elementSetNamesValues) {
            for (String esn : elementSetNamesValues.getValue()) {
                try {
                    detailLevels.add(ElementSetType.fromValue(esn.toLowerCase()));
                } catch (IllegalArgumentException iae) {
                    LOGGER.warn("{}: \"{}\" is not a ElementSetType, Error: {}",
                            cswSourceConfiguration.getId(), esn, iae);
                }
            }
        }
    }

    protected void loadContentTypes() {
        Filter filter = filterBuilder.attribute(CswConstants.ANY_TEXT).is().like()
                .text(CswConstants.WILD_CARD);
        Query query = new QueryImpl(filter, 1, CONTENT_TYPE_SAMPLE_SIZE, null, true, 0);
        QueryRequest queryReq = new QueryRequestImpl(query);

        try {
            query(queryReq);
        } catch (UnsupportedQueryException e) {
            LOGGER.error("{}: Failed to read Content-Types from CSW Server, Error: {}", getId(), e);
        }
    }

    /**
     * Searches every query response for previously unknown content types
     *
     * @param response A Query Response
     */
    private void addContentTypes(SourceResponse response) {
        if (response == null || response.getResults() == null) {
            return;
        }

        if (contentTypeMappingUpdated) {
            LOGGER.debug(
                    "{}: The content type mapping has been updated. Removing all old content types.",
                    cswSourceConfiguration.getId());
            contentTypes.clear();
        }

        for (Result result : response.getResults()) {
            Metacard metacard = result.getMetacard();
            if (metacard != null) {
                addContentType(metacard.getContentTypeName(), metacard.getContentTypeVersion(),
                        metacard.getContentTypeNamespace());
            }
        }

        Configuration[] managedConfigs = getManagedConfigs();
        if (managedConfigs != null) {

            for (Configuration managedConfig : managedConfigs) {
                Dictionary<String, Object> properties = managedConfig.getProperties();
                Set<String> current = new HashSet<String>(Arrays.asList((String[]) properties
                        .get(CONTENTTYPES_PROPERTY)));

                if (contentTypeMappingUpdated
                        || (current != null && !current.containsAll(contentTypes.keySet()))) {
                    LOGGER.debug("{}: Adding new content types {} for content type mapping: {}.",
                            cswSourceConfiguration.getId(), contentTypes.toString(),
                            cswSourceConfiguration.getContentTypeMapping());
                    properties.put(CONTENTTYPES_PROPERTY,
                            contentTypes.keySet().toArray(new String[0]));
                    properties.put(CONTENT_TYPE_MAPPING_PROPERTY,
                            cswSourceConfiguration.getContentTypeMapping());
                    try {
                        LOGGER.debug("{}: Updating CSW Federated Source configuration with {}.",
                                cswSourceConfiguration.getId(), properties.toString());
                        managedConfig.update(properties);
                    } catch (IOException e) {
                        LOGGER.warn(
                                "{}: Failed to update managedConfiguration with new contentTypes, Error: {}",
                                cswSourceConfiguration.getId(), e);
                    }
                }
            }
        }
    }

    private Configuration[] getManagedConfigs() {
        Configuration[] managedConfig = null;
        ServiceReference configurationAdminReference = context
                .getServiceReference(ConfigurationAdmin.class.getName());
        if (configurationAdminReference != null) {
            ConfigurationAdmin confAdmin = (ConfigurationAdmin) context
                    .getService(configurationAdminReference);
            try {
                managedConfig = confAdmin.listConfigurations("(&(" + ID_PROPERTY + "="
                        + cswSourceConfiguration.getId() + ")" + "(" + CSWURL_PROPERTY + "="
                        + cswSourceConfiguration.getCswUrl() + "))");
            } catch (IOException e) {
                LOGGER.warn("{}: Failed to capture managedConfig.  Exception: {}",
                        cswSourceConfiguration.getId(), e);
            } catch (InvalidSyntaxException e) {
                LOGGER.warn("{}: Failed to capture managedConfig.  Exception: {}",
                        cswSourceConfiguration.getId(), e);
            }
        }

        if (managedConfig != null) {
            LOGGER.debug("{}: managedConfig length: {}.", cswSourceConfiguration.getId(),
                    managedConfig.length);
        }

        return managedConfig;
    }

    private boolean addContentType(String name) {
        return addContentType(name, null, null);
    }

    private boolean addContentType(String name, String version, URI namespace) {
        if (!StringUtils.isEmpty(name) && !contentTypes.containsKey(name)) {
            if (version == null) {
                version = "";
            }
            contentTypes.put(name, new ContentTypeImpl(name, version, namespace));
            return true;
        }
        return false;
    }

    public DomainType getParameter(Operation operation, String name) {
        for (DomainType parameter : operation.getParameter()) {
            if (name.equalsIgnoreCase(parameter.getName())) {
                return parameter;
            }
        }
        if (LOGGER.isWarnEnabled()) {
            LOGGER.warn("{}: CSW Operation \"{}\" did not contain the \"{}\" parameter",
                    new Object[] {cswSourceConfiguration.getId(), operation.getName(), name});
        }
        return null;
    }

    protected String handleWebApplicationException(WebApplicationException wae) {
        Response response = wae.getResponse();
        CswException cswException = new CswResponseExceptionMapper().fromResponse(response);

        // Add the CswException message to the error message being logged. Do
        // not include the CswException stack trace because it will not be
        // meaningful since it will not show the root cause of the exception
        // because the ExceptionReport was sent from CSW as an "OK" JAX-RS
        // status rather than an error status.
        String msg = "Error received from CSW Server " + cswSourceConfiguration.getId() + "\n"
                + cswException.getMessage();
        LOGGER.error(msg, wae);

        return msg;
    }

    private String handleClientException(ClientException ce) {
        String msg = "";
        if (ce.getCause() instanceof WebApplicationException) {
            msg = handleWebApplicationException((WebApplicationException) ce.getCause());
        } else {
            msg = "Error received from CSW Server " + cswSourceConfiguration.getId();
        }
        LOGGER.error(msg);
        return msg;
    }

    private static JAXBContext initJaxbContext() {

        JAXBContext jaxbContext = null;
        
        String contextPath = StringUtils.join(new String[] {CswConstants.OGC_CSW_PACKAGE,
            CswConstants.OGC_FILTER_PACKAGE, CswConstants.OGC_GML_PACKAGE,
            CswConstants.OGC_OWS_PACKAGE}, ":");

        try {
            jaxbContext = JAXBContext.newInstance(contextPath,
                    CswSource.class.getClassLoader());
        } catch (JAXBException e) {
            LOGGER.error("Failed to initialize JAXBContext", e);
        }

        return jaxbContext;
    }

    private void availabilityChanged(boolean isAvailable) {

        if (isAvailable) {
            LOGGER.info("CSW source {} is available.", cswSourceConfiguration.getId());
        } else {
            LOGGER.info("CSW source {} is unavailable.", cswSourceConfiguration.getId());
            this.remoteCsw = null;
        }

        for (SourceMonitor monitor : this.sourceMonitors) {
            if (isAvailable) {
                LOGGER.debug("Notifying source monitor that CSW source {} is available.",
                        cswSourceConfiguration.getId());
                monitor.setAvailable();
            } else {
                LOGGER.debug("Notifying source monitor that CSW source {} is unavailable.",
                        cswSourceConfiguration.getId());
                monitor.setUnavailable();
            }
        }
    }

    private void registerMetacardTypes() {
        List<String> contentTypesNames = getContentTypeNames();

        if (!contentTypesNames.isEmpty()) {
            Dictionary<String, Object> metacardTypeProperties = new Hashtable<String, Object>();
            metacardTypeProperties.put(Metacard.CONTENT_TYPE,
                    contentTypesNames.toArray(new String[contentTypesNames.size()]));
            CswRecordMetacardType cswRecordMetacardType = new CswRecordMetacardType(
                    cswSourceConfiguration.getId());
            LOGGER.debug("{}: CSW Record Metacard Type hash code: {}",
                    cswSourceConfiguration.getId(), cswRecordMetacardType.hashCode());
            LOGGER.debug("{}: Registering CSW Record Metacard Type {} with content types: {}",
                    cswSourceConfiguration.getId(), cswRecordMetacardType.getClass().getName(),
                    contentTypesNames);
            ServiceRegistration<?> registeredMetacardType = context.registerService(
                    MetacardType.class.getName(), cswRecordMetacardType, metacardTypeProperties);
            registeredMetacardTypes.add(registeredMetacardType);
        } else {
            LOGGER.debug(
                    "{}: There are no metadata content types to register for CSW Record Metacard Type {}. Not registering CSW Record Metacard Type {}.",
                    cswSourceConfiguration.getId(), CswRecordMetacardType.class.getName(),
                    CswRecordMetacardType.class.getName());
        }
    }

    private void unregisterMetacardTypes() {
        for (ServiceRegistration<?> metacardType : registeredMetacardTypes) {
            LOGGER.debug(
                    "{}: Unregistering CSW Record Metacard Type {} with metadata content types {}",
                    cswSourceConfiguration.getId(), CswRecordMetacardType.class.getName(),
                    metacardType.getReference().getProperty(Metacard.CONTENT_TYPE));
            metacardType.unregister();
        }

        registeredMetacardTypes.removeAll(registeredMetacardTypes);
    }

    private boolean isOutputSchemaSupported() {
        return this.cswSourceConfiguration.getOutputSchema() != null &&
               this.supportedOutputSchemas != null ? this.supportedOutputSchemas.getValue()
                       .contains(cswSourceConfiguration.getOutputSchema()) : false;
    }

    /**
     * Callback class to check the Availability of the CswSource.
     * <p/>
     * NOTE: Ideally, the framework would call isAvailable on the Source and the SourcePoller would
     * have an AvailabilityTask that cached each Source's availability. Until that is done, allow
     * the command to handle the logic of managing availability.
     *
     */
    private class CswSourceAvailabilityCommand implements AvailabilityCommand {

        @Override
        public boolean isAvailable() {
            LOGGER.debug("Checking availability for source {} ", cswSourceConfiguration.getId());
            boolean oldAvailability = CswSource.this.isAvailable();
            boolean newAvailability = false;
            // If the Remote object is null attempt to initialize it and
            // configure
            // all the capabilities.
            if (remoteCsw == null) {
                connectToRemoteCsw();
            }
            // Simple "ping" to ensure the source is responding
            newAvailability = (getCapabilities() != null);
            if (oldAvailability != newAvailability) {
                availabilityChanged(newAvailability);
                // If the source becomes available, configure it.
                if (newAvailability) {
                    configureCswSource();
                }
            }
            return newAvailability;
        }

    }

    public void setAvailabilityTask(AvailabilityTask availabilityTask) {
        this.availabilityTask = availabilityTask;
    }

    public void setSecuritySettings(SecuritySettingsService securitySettings) {
        this.securitySettingsService = securitySettings;
    }
}
