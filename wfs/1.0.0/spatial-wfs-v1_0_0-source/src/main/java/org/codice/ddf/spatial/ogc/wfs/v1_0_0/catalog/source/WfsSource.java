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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.source;

import ddf.catalog.Constants;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.util.impl.MaskableImpl;
import ddf.security.settings.SecuritySettingsService;
import ogc.schema.opengis.filter.v_1_0_0.FilterType;
import ogc.schema.opengis.wfs.v_1_0_0.GetFeatureType;
import ogc.schema.opengis.wfs.v_1_0_0.ObjectFactory;
import ogc.schema.opengis.wfs.v_1_0_0.QueryType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.FeatureTypeType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.WFSCapabilitiesType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.codice.ddf.spatial.ogc.catalog.MetadataTransformer;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityCommand;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.codice.ddf.spatial.ogc.catalog.common.ContentTypeFilterDelegate;
import org.codice.ddf.spatial.ogc.catalog.common.TrustedRemoteSource;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GenericFeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.Wfs10Constants;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.converter.FeatureConverterFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.ClientException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Provides a Federated and Connected source implementation for OGC WFS servers.
 * 
 */
public class WfsSource extends MaskableImpl implements FederatedSource, ConnectedSource {

    private String wfsUrl;

    private Map<QName, WfsFilterDelegate> featureTypeFilters = new HashMap<QName, WfsFilterDelegate>();

    private String username;

    private String password;

    private Boolean disableCnCheck = Boolean.FALSE;

    private FilterAdapter filterAdapter;

    private RemoteWfs remoteWfs;

    private BundleContext context;

    private Map<String, ServiceRegistration> metacardTypeServiceRegistrations = new HashMap<String, ServiceRegistration>();

    private static final Logger LOGGER = LoggerFactory.getLogger(WfsSource.class);

    private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

    private static final String DESCRIPTION = "description";

    private static final String ORGANIZATION = "organization";

    private static final String VERSION = "version";

    private static final String TITLE = "name";

    private static final String WFSURL_PROPERTY = "wfsUrl";

    private static final String ID_PROPERTY = "id";

    private static final String USERNAME_PROPERTY = "username";

    private static final String PASSWORD_PROPERTY = "password";

    private static final String NON_QUERYABLE_PROPS_PROPERTY = "nonQueryableProperties";

    private static final String SPATIAL_FILTER_PROPERTY = "forceSpatialFilter";

    private static final String NO_FORCED_SPATIAL_FILTER = "NO_FILTER";

    private static final String CONNECTION_TIMEOUT_PROPERTY = "connectionTimeout";

    private static final String RECEIVE_TIMEOUT_PROPERTY = "receiveTimeout";

    private static final String WFS_ERROR_MESSAGE = "Error received from Wfs Server.";

    public static final int WFS_MAX_FEATURES_RETURNED = 1000;

    protected static final int WFS_QUERY_PAGE_SIZE_MULTIPLIER = 3;

    private static Properties describableProperties = new Properties();

    private String[] nonQueryableProperties;

    private List<FeatureConverterFactory> featureConverterFactories;

    private static final String DEFAULT_WFS_TRANSFORMER_ID = "wfs";

    static {
        try (InputStream properties = WfsSource.class.getResourceAsStream(
                DESCRIBABLE_PROPERTIES_FILE)) {
            describableProperties.load(properties);
        } catch (IOException e) {
            LOGGER.info(e.getMessage(), e);
        }
    }

    private static final String POLL_INTERVAL_PROPERTY = "pollInterval";

    private Integer pollInterval;

    private Integer connectionTimeout;

    private Integer receiveTimeout;

    private String forceSpatialFilter = NO_FORCED_SPATIAL_FILTER;

    private List<String> supportedGeoFilters;

    private ScheduledExecutorService scheduler;

    private ScheduledFuture<?> availabilityPollFuture;

    private AvailabilityTask availabilityTask;

    private Set<SourceMonitor> sourceMonitors = new HashSet<SourceMonitor>();

    private SecuritySettingsService securitySettingsService;

    public WfsSource(RemoteWfs remoteWfs, FilterAdapter filterAdapter, BundleContext context,
            AvailabilityTask task) {
        this.remoteWfs = remoteWfs;
        this.filterAdapter = filterAdapter;
        this.context = context;
        this.availabilityTask = task;
        configureWfsFeatures();
    }

    public WfsSource() {
        // Required for bean creation
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Init is called when the bundle is initially configured.
     * 
     * <p>
     * The init process creates a RemoteWfs object using the connection parameters from the
     * configuration.
     * 
     */
    public void init() {
        setupAvailabilityPoll();
    }

    public void destroy() {
        unregisterAllMetacardTypes();
        availabilityPollFuture.cancel(true);
        scheduler.shutdownNow();
    }

    /**
     * Refresh is called if the bundle configuration is updated.
     * 
     * <p>
     * If any of the connection related properties change, an attempt is made to re-connect.
     * 
     * @param configuration
     */
    public void refresh(Map<String, Object> configuration) {

        LOGGER.debug("WfsSource {}: Refresh called", getId());
        String wfsUrl = (String) configuration.get(WFSURL_PROPERTY);
        String password = (String) configuration.get(PASSWORD_PROPERTY);
        String username = (String) configuration.get(USERNAME_PROPERTY);
        Boolean disableCnCheckProp = (Boolean) configuration
                .get(TrustedRemoteSource.DISABLE_CN_CHECK_PROPERTY);
        String id = (String) configuration.get(ID_PROPERTY);

        setConnectionTimeout((Integer) configuration.get(CONNECTION_TIMEOUT_PROPERTY));
        setReceiveTimeout((Integer) configuration.get(RECEIVE_TIMEOUT_PROPERTY));

        updateTimeouts();

        String[] nonQueryableProperties = (String[]) configuration
                .get(NON_QUERYABLE_PROPS_PROPERTY);

        this.nonQueryableProperties = nonQueryableProperties;

        Integer newPollInterval = (Integer) configuration.get(POLL_INTERVAL_PROPERTY);
        super.setId(id);

        // only attempt to re-connect on a refresh if not connected OR username,
        // password, or the URL changes
        if (remoteWfs == null
                || hasWfsUrlChanged(wfsUrl)
                || hasPasswordChanged(password) 
                || hasUsernameChanged(username)
                || hasDisableCnCheck(disableCnCheckProp)) {
            this.wfsUrl = wfsUrl;
            this.password = password;
            this.username = username;
            this.disableCnCheck = disableCnCheckProp;

            connectToRemoteWfs();
            configureWfsFeatures();
        } else {
            // Only need to update the supportedGeos if we don't reconnect.
            String spatialFilter = (String) configuration.get(SPATIAL_FILTER_PROPERTY);
            if (!StringUtils.equals(forceSpatialFilter, spatialFilter)) {
                List<String> geoFilters = new ArrayList<String>();
                if (NO_FORCED_SPATIAL_FILTER.equals(spatialFilter)) {
                    geoFilters.addAll(supportedGeoFilters);
                } else {
                    geoFilters.add(spatialFilter);
                }
                for (WfsFilterDelegate delegate : featureTypeFilters.values()) {
                    delegate.setSupportedGeoFilters(geoFilters);
                }
            }
        }

        if (!pollInterval.equals(newPollInterval)) {
            LOGGER.debug("Poll Interval was changed for source {}.", getId());
            setPollInterval(newPollInterval);
            availabilityPollFuture.cancel(true);
            setupAvailabilityPoll();
        }
    }

    public void updateTimeouts() {
        if (remoteWfs != null) {
            remoteWfs.setTimeouts(connectionTimeout, receiveTimeout);
        }
    }

    private boolean hasWfsUrlChanged(String wfsUrl) {
        return !StringUtils.equals(this.wfsUrl, wfsUrl);
    }
    
    private boolean hasPasswordChanged(String password) {
        return !StringUtils.equals(this.password, password);
    }

    private boolean hasUsernameChanged(String username) {
        return !StringUtils.equals(this.username, username);
    }
    
    private boolean hasDisableCnCheck(Boolean disableCnCheck) {
        return this.disableCnCheck != disableCnCheck;
    }

    private void setupAvailabilityPoll() {
        LOGGER.debug("Setting Availability poll task for {} minute(s) on Source {}", pollInterval,
                getId());
        WfsSourceAvailabilityCommand command = new WfsSourceAvailabilityCommand();
        long interval = TimeUnit.MINUTES.toMillis(pollInterval);
        if (availabilityPollFuture == null || availabilityPollFuture.isCancelled()) {
            if (availabilityTask == null) {
                availabilityTask = new AvailabilityTask(interval, command, getId());
            } else {
                availabilityTask.setInterval(interval);
            }
            // Run the availability check immediately prior to scheduling it in a thread.
            // This is necessary to allow the catalog framework to have the correct
            // availability when the source is bound
            availabilityTask.run();
            // Run the availability check every 1 second. The actually call to
            // the remote server will only occur if the pollInterval has
            // elapsed.
            availabilityPollFuture = scheduler.scheduleWithFixedDelay(availabilityTask,
                    AvailabilityTask.NO_DELAY, AvailabilityTask.ONE_SECOND, TimeUnit.SECONDS);
        }

    }

    private void connectToRemoteWfs() {
        LOGGER.debug(
                "WfsSource {}: Connecting to remote WFS Server {}. SSL cert verification disabled? {}",
                getId(), wfsUrl, this.disableCnCheck);

        try {
            remoteWfs = new RemoteWfs(wfsUrl, username, password, disableCnCheck);
            remoteWfs.setSecuritySettings(securitySettingsService);
            remoteWfs.setTlsParameters();
            remoteWfs.setTimeouts(connectionTimeout, receiveTimeout);
        } catch (IllegalArgumentException iae) {
            LOGGER.warn("Unable to create RemoteWfs.", iae);
            remoteWfs = null;
        }
    }

    private void availabilityChanged(boolean isAvailable) {
        if (isAvailable) {
            LOGGER.info("WFS source {} is available.", getId());
        } else {
            LOGGER.info("WFS source {} is unavailable.", getId());
            this.remoteWfs = null;
        }

        for (SourceMonitor monitor : this.sourceMonitors) {
            if (isAvailable) {
                LOGGER.debug("Notifying source monitor that WFS source {} is available.", getId());
                monitor.setAvailable();
            } else {
                LOGGER.debug("Notifying source monitor that WFS source {} is unavailable.", getId());
                monitor.setUnavailable();
            }
        }
    }

    private WFSCapabilitiesType getCapabilities() {
        WFSCapabilitiesType capabilities = null;
        if (remoteWfs != null) {
            try {
                capabilities = remoteWfs.getCapabilities(new GetCapabilitiesRequest());
            } catch (WfsException wfse) {
                LOGGER.warn(WFS_ERROR_MESSAGE, wfse);
            } catch (WebApplicationException wae) {
                handleWebApplicationException(wae);
            }
        }
        return capabilities;
    }

    private void configureWfsFeatures() {
        WFSCapabilitiesType capabilities = getCapabilities();

        if (capabilities != null) {
            List<FeatureTypeType> featureTypes = getFeatureTypes(capabilities);
            List<String> supportedGeo = getSupportedGeo(capabilities);
            buildFeatureFilters(featureTypes, supportedGeo);
        } else {
            LOGGER.warn("WfsSource {}: WFS Server did not return any capabilities.", getId());
        }
    }

    private List<FeatureTypeType> getFeatureTypes(WFSCapabilitiesType capabilities) {
        List<FeatureTypeType> featureTypes = capabilities.getFeatureTypeList().getFeatureType();
        if (featureTypes.isEmpty()) {
            LOGGER.warn("\"WfsSource {}: No feature types found.", getId());
        }
        return featureTypes;
    }

    private List<String> getSupportedGeo(WFSCapabilitiesType capabilities) {
        supportedGeoFilters = new ArrayList<String>();
        List<Object> geoTypes = capabilities.getFilterCapabilities().getSpatialCapabilities()
                .getSpatialOperators().getBBOXOrEqualsOrDisjoint();

        for (Object geoType : geoTypes) {
            supportedGeoFilters.add(geoType.getClass().getSimpleName());
        }
        if (!NO_FORCED_SPATIAL_FILTER.equals(forceSpatialFilter)) {
            return Arrays.asList(forceSpatialFilter);
        }
        return supportedGeoFilters;
    }

    private void buildFeatureFilters(List<FeatureTypeType> featureTypes, List<String> supportedGeo) {

        // Use local Map for metacardtype registrations and once they are populated with latest
        // MetacardTypes, then do actual registration
        Map<String, MetacardTypeRegistration> mcTypeRegs = new HashMap<String, MetacardTypeRegistration>();

        for (FeatureTypeType featureTypeType : featureTypes) {
            String ftName = featureTypeType.getName().getLocalPart();

            if (mcTypeRegs.containsKey(ftName)) {
                LOGGER.debug(
                        "WfsSource {}: MetacardType {} is already registered - skipping to next metacard type",
                        getId(), ftName);
                continue;
            }

            LOGGER.debug("ftName: {}", ftName);
            try {
                XmlSchema schema = remoteWfs.describeFeatureType(new DescribeFeatureTypeRequest(
                        featureTypeType.getName()));

                if ((schema != null)) {
                    FeatureMetacardType ftMetacard = new FeatureMetacardType(schema,
                            featureTypeType.getName(),
                            nonQueryableProperties != null ? Arrays.asList(nonQueryableProperties)
                                    : new ArrayList<String>(), Wfs10Constants.GML_NAMESPACE);

                    Dictionary<String, Object> props = new Hashtable<String, Object>();
                    props.put(Metacard.CONTENT_TYPE, new String[] {ftName});

                    LOGGER.debug("WfsSource {}: Registering MetacardType: {}", getId(), ftName);

                    // Update local map with enough info to create actual MetacardType registrations
                    // later
                    mcTypeRegs.put(ftName, new MetacardTypeRegistration(ftMetacard, props,
                            featureTypeType.getSRS()));

                    FeatureConverter featureConverter = null;

                    if (!CollectionUtils.isEmpty(featureConverterFactories)) {
                        for (FeatureConverterFactory factory : featureConverterFactories) {
                            if (ftName.equalsIgnoreCase(factory.getFeatureType())) {
                                featureConverter = factory.createConverter();
                                LOGGER.debug(
                                        "WFS Source {}: Features of type: {} will be converted using {}",
                                        getId(), ftName, featureConverter.getClass().getSimpleName());
                                break;
                            }

                        }

                        if (featureConverter == null) {
                            LOGGER.warn(
                                    "WfsSource {}: Unable to find a feature specific converter; {} will be converted using the GenericFeatureConverter",
                                    getId(), ftName);
                            featureConverter = new GenericFeatureConverter();
                        }
                    } else {
                        LOGGER.warn(
                                "WfsSource {}: Unable to find a feature specific converter; {} will be converted using the GenericFeatureConverter",
                                getId(), ftName);
                        featureConverter = new GenericFeatureConverter();

                    }
                    featureConverter.setSourceId(getId());
                    featureConverter.setMetacardType(ftMetacard);
                    featureConverter.setWfsUrl(wfsUrl);

                    // Add the Feature Type name as an alias for xstream
                    remoteWfs.getFeatureCollectionReader().registerConverter(featureConverter);
                }

            } catch (WfsException wfse) {
                LOGGER.warn(WFS_ERROR_MESSAGE, wfse);
            } catch (WebApplicationException wae) {
                handleWebApplicationException(wae);
            } catch (IllegalArgumentException ie) {
                LOGGER.warn(WFS_ERROR_MESSAGE, ie);
            }

        }

        // Unregister all MetacardType services - the DescribeFeatureTypeRequest should
        // have returned all of the most current metacard types that will now be registered.
        // As Source(s) are added/removed from this instance or to other Source(s)
        // that this instance is federated to, the list of metacard types will change.
        // This is done here vs. inside the above loop so that minimal time is spent clearing and
        // registering the MetacardTypes - the concern is that if this registration is too lengthy
        // a query could come in that is handled while the MetacardType registrations are
        // in a state of flux.
        unregisterAllMetacardTypes();
        this.featureTypeFilters.clear();
        if (!mcTypeRegs.isEmpty()) {
            for (String ftName : mcTypeRegs.keySet()) {
                MetacardTypeRegistration mcTypeReg = mcTypeRegs.get(ftName);
                FeatureMetacardType ftMetacard = mcTypeReg.getFtMetacard();
                ServiceRegistration serviceRegistration = context.registerService(
                        MetacardType.class.getName(), ftMetacard, mcTypeReg.getProps());
                this.metacardTypeServiceRegistrations.put(ftName, serviceRegistration);
                this.featureTypeFilters.put(ftMetacard.getFeatureType(), new WfsFilterDelegate(
                        ftMetacard, supportedGeo, mcTypeReg.getSrs()));
            }
        }

        if (featureTypeFilters.isEmpty()) {
            LOGGER.warn(
                    "Wfs Source {}: No Feature Type schemas validated. Marking source as unavailable",
                    getId());
        }
        LOGGER.debug("Wfs Source {}: Number of validated Features = {}", getId(),
                featureTypeFilters.size());
    }

    @Override
    public boolean isAvailable() {
        return availabilityTask.isAvailable();
    }

    @Override
    public boolean isAvailable(SourceMonitor callback) {
        this.sourceMonitors.add(callback);
        return isAvailable();
    }

    @Override
    public SourceResponse query(QueryRequest request) throws UnsupportedQueryException {

        Query query = request.getQuery();
        LOGGER.debug("WFS Source {}: Received query: \n{}", getId(), query);

        if (query.getStartIndex() < 1) {
            throw new UnsupportedQueryException(
                    "Start Index is one-based and must be an integer greater than 0; should not be ["
                            + query.getStartIndex() + "]");
        }

        SourceResponseImpl simpleResponse = null;

        // WFS v1.0 specification does not support response indicating total
        // number
        // of features satisfying query constraints.
        // Hence, we save off the original
        // page size from the query request and create a copy of the query,
        // changing
        // the page size by a multiplier and the current page number of results
        // so that
        // more features are returned as the user pages through the results,
        // getting
        // a better sense of how many total features exist that satisfy the
        // query.
        int origPageSize = query.getPageSize();
        if (origPageSize <= 0 || origPageSize > WFS_MAX_FEATURES_RETURNED) {
            origPageSize = WFS_MAX_FEATURES_RETURNED;
        }
        QueryImpl modifiedQuery = new QueryImpl(query);

        // Determine current page number of results being requested.
        // Example: startIndex = 21 and origPageSize=10, then requesting to go
        // to page number 3.
        // Note: Integer division will truncate remainders so 4 / 2 will return 0 and not .5.  Also,
        // pages are numbered 1 - N so we add 1 to the result
        int pageNumber = query.getStartIndex() / origPageSize + 1;

        // Modified page size is based on current page number and a constant
        // multiplier,
        // but limited to a max value to prevent time consuming queries just to
        // get an
        // approximation of total number of features.
        // So as page number increases the pageSize increases.
        // Example:
        // pageNumber=2, modifiedPageSize=60
        // pageNumber=3, modifiedPageSize=90
        int modifiedPageSize = Math.min(pageNumber * origPageSize * WFS_QUERY_PAGE_SIZE_MULTIPLIER,
                WFS_MAX_FEATURES_RETURNED);
        LOGGER.debug("WFS Source {}: modified page size = {}", getId(), modifiedPageSize);
        modifiedQuery.setPageSize(modifiedPageSize);

        GetFeatureType getFeature = buildGetFeatureRequest(modifiedQuery);

        try {
            LOGGER.debug("WFS Source {}: Sending query ...", getId());
            WfsFeatureCollection featureCollection = remoteWfs.getFeature(getFeature);

            if (featureCollection == null) {
                throw new UnsupportedQueryException("Invalid results returned from server");
            }
            availabilityTask.updateLastAvailableTimestamp(System.currentTimeMillis());
            LOGGER.debug("WFS Source {}: Received featureCollection with {} metacards.", getId(),
                    featureCollection.getFeatureMembers().size());

            // Only return the number of results originally asked for in the
            // query, or the entire list of results if it is smaller than the
            // original page size.
            int numberOfResultsToReturn = Math.min(origPageSize, featureCollection
                    .getFeatureMembers().size());
            List<Result> results = new ArrayList<Result>(numberOfResultsToReturn);

            int stopIndex = Math.min((origPageSize * pageNumber) + query.getStartIndex(),
                    featureCollection.getFeatureMembers().size() + 1);

            LOGGER.debug("WFS Source {}: startIndex = {}, stopIndex = {}, origPageSize = {}, pageNumber = {}"
                    , getId(), query.getStartIndex(), stopIndex, origPageSize, pageNumber);

            for (int i = query.getStartIndex(); i < stopIndex; i++) {
                Metacard mc = featureCollection.getFeatureMembers().get(i - 1);
                mc = transform(mc, DEFAULT_WFS_TRANSFORMER_ID);
                Result result = new ResultImpl(mc);
                results.add(result);
                debugResult(result);
            }
            Long totalHits = new Long(featureCollection.getFeatureMembers().size());
            simpleResponse = new SourceResponseImpl(request, results, totalHits);
        } catch (WfsException wfse) {
            LOGGER.warn(WFS_ERROR_MESSAGE, wfse);
            throw new UnsupportedQueryException("Error received from WFS Server", wfse);
        } catch (ClientException ce) {
            String msg = handleClientException(ce);
            throw new UnsupportedQueryException(msg, ce);
        }

        return simpleResponse;
    }

    private GetFeatureType buildGetFeatureRequest(Query query) throws UnsupportedQueryException {
        List<ContentType> contentTypes = getContentTypesFromQuery(query);
        List<QueryType> queries = new ArrayList<QueryType>();

        for (Entry<QName, WfsFilterDelegate> filterDelegateEntry : featureTypeFilters.entrySet()) {
            if (contentTypes.isEmpty()
                    || isFeatureTypeInQuery(contentTypes, filterDelegateEntry.getKey()
                            .getLocalPart())) {
                QueryType wfsQuery = new QueryType();
                wfsQuery.setTypeName(filterDelegateEntry.getKey());
                FilterType filter = filterAdapter.adapt(query, filterDelegateEntry.getValue());
                if (filter != null) {
                    if (areAnyFiltersSet(filter)) {
                        wfsQuery.setFilter(filter);
                    }
                    queries.add(wfsQuery);
                } else {
                    LOGGER.debug("WFS Source {}: {} has an invalid filter.", getId(),
                            filterDelegateEntry.getKey());
                }
            }
        }
        if (queries != null && !queries.isEmpty()) {

            GetFeatureType getFeatureType = new GetFeatureType();
            getFeatureType.setMaxFeatures(BigInteger.valueOf(query.getPageSize()));
            getFeatureType.getQuery().addAll(queries);
            getFeatureType.setService(Wfs10Constants.WFS);
            getFeatureType.setVersion(Wfs10Constants.VERSION_1_0_0);
            logMessage(getFeatureType);
            return getFeatureType;
        } else {
            throw new UnsupportedQueryException(
                    "Unable to build query. No filters could be created from query criteria.");
        }
    }

    private boolean areAnyFiltersSet(FilterType filter) {
        if (filter != null) {
            return (filter.isSetComparisonOps() || filter.isSetFeatureId()
                    || filter.isSetLogicOps() || filter.isSetSpatialOps());
        } else {
            return false;
        }
    }

    private boolean isFeatureTypeInQuery(final List<ContentType> contentTypes,
            final String featureTypeName) {

        for (ContentType contentType : contentTypes) {
            if (featureTypeName.equalsIgnoreCase(contentType.getName())) {
                return true;
            }
        }
        return false;
    }

    private Metacard transform(Metacard mc, String transformerId) {
        if (mc == null) {
            throw new IllegalArgumentException("Metacard is null");
        }

        ServiceReference[] refs = null;
        try {
            refs = context.getServiceReferences(MetadataTransformer.class.getName(), "("
                    + Constants.SERVICE_ID + "=" + transformerId + ")");
        } catch (InvalidSyntaxException e) {
            LOGGER.warn("Invalid transformer ID. Returning original metacard.", e);
            return mc;
        }

        if (refs == null || refs.length == 0) {
            LOGGER.debug("MetadataTransformer not found.  Returning original metacard.");
            return mc;
        } else {
            try {
                MetadataTransformer transformer = (MetadataTransformer) context.getService(refs[0]);
                return transformer.transform(mc);
            } catch (CatalogTransformerException e) {
                LOGGER.warn(
                        "Transformation Failed for transformer: {}. Returning original metacard",
                        transformerId, e);
                return mc;
            }
        }
    }

    private List<ContentType> getContentTypesFromQuery(final Query query) {
        List<ContentType> contentTypes = null;

        try {
            contentTypes = filterAdapter.adapt(query, new ContentTypeFilterDelegate());
        } catch (UnsupportedQueryException e) {
            LOGGER.warn("WFS Source {}: Unable to get content types from query.", getId(), e);
        }

        return contentTypes != null ? contentTypes : new ArrayList<ContentType>();
    }

    private void unregisterAllMetacardTypes() {
        for (ServiceRegistration metacardTypeServiceRegistration : metacardTypeServiceRegistrations
                .values()) {
            if (metacardTypeServiceRegistration != null) {
                metacardTypeServiceRegistration.unregister();
            }
        }
        metacardTypeServiceRegistrations.clear();
    }

    @Override
    public Set<ContentType> getContentTypes() {
        Set<QName> typeNames = featureTypeFilters.keySet();
        Set<ContentType> contentTypes = new HashSet<ContentType>();
        for (QName featureName : typeNames) {
            contentTypes.add(new ContentTypeImpl(featureName.getLocalPart(), getVersion()));
        }
        return contentTypes;
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
    public String getDescription() {
        return describableProperties.getProperty(DESCRIPTION);
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
        return describableProperties.getProperty(VERSION);
    }

    @Override
    public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> arguments)
        throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        StringBuilder strBuilder = new StringBuilder();
        strBuilder.append("<html><script type=\"text/javascript\">window.location.replace(\"");
        strBuilder.append(uri);
        strBuilder.append("\");</script></html>");

        Resource resource = new ResourceImpl(IOUtils.toInputStream(strBuilder.toString()),
                MediaType.TEXT_HTML, getId() + " Resource");

        return new ResourceResponseImpl(resource);
    }

    @Override
    public Set<String> getSupportedSchemes() {
        // TODO Auto-generated method stub -
        return null;
    }

    @Override
    public Set<String> getOptions(Metacard metacard) {
        // TODO Auto-generated method stub
        return null;
    }

    public void setWfsUrl(String wfsUrl) {
        this.wfsUrl = wfsUrl;
    }

    public String getWfsUrl() {
        return wfsUrl;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setDisableCnCheck(Boolean disableCnCheck) {
        this.disableCnCheck = disableCnCheck;
    }

    public void setPollInterval(Integer interval) {
        this.pollInterval = interval;
    }

    public void setConnectionTimeout(Integer timeout) {
        this.connectionTimeout = timeout;
    }

    public void setReceiveTimeout(Integer timeout) {
        this.receiveTimeout = timeout;
    }

    public void setFilterAdapter(FilterAdapter filterAdapter) {
        this.filterAdapter = filterAdapter;
    }

    public void setRemoteWfs(RemoteWfs remoteWfs) {
        this.remoteWfs = remoteWfs;
    }

    public void setFilterDelgates(Map<QName, WfsFilterDelegate> delegates) {
        this.featureTypeFilters = delegates;
    }

    public void setContext(BundleContext context) {
        this.context = context;
    }

    public void setNonQueryableProperties(String[] newNonQueryableProperties) {
        if(newNonQueryableProperties == null) {
            this.nonQueryableProperties = new String[0];
        } else {
            this.nonQueryableProperties = Arrays.copyOf(newNonQueryableProperties, newNonQueryableProperties.length);
        }
    }

    public String getForceSpatialFilter() {
        return forceSpatialFilter;
    }

    public void setForceSpatialFilter(String forceSpatialFilter) {
        this.forceSpatialFilter = forceSpatialFilter;
    }

    public void setFeatureConverterFactoryList(List<FeatureConverterFactory> factories) {
        this.featureConverterFactories = factories;
    }

    private String handleWebApplicationException(WebApplicationException wae) {
        Response response = wae.getResponse();
        WfsException wfsException = new WfsResponseExceptionMapper().fromResponse(response);
        String msg = "Error received from WFS Server " + getId() + "\n" + wfsException.getMessage();

        LOGGER.warn(msg, wae);

        return msg;
    }

    private String handleClientException(ClientException ce) {
        String msg = "";
        if (ce.getCause() instanceof WebApplicationException) {
            msg = handleWebApplicationException((WebApplicationException) ce.getCause());
        } else {
            msg = "Error received from WFS Server " + getId();
        }
        LOGGER.warn(msg);
        return msg;
    }

    private void logMessage(GetFeatureType getFeature) {
        if (LOGGER.isDebugEnabled()) {
            try {
                StringWriter writer = new StringWriter();
                JAXBContext contextObj = JAXBContext.newInstance(GetFeatureType.class);
    
                Marshaller marshallerObj = contextObj.createMarshaller();
                marshallerObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
    
                marshallerObj.marshal(new ObjectFactory().createGetFeature(getFeature), writer);
                LOGGER.debug("WfsSource {}: {}", getId(), writer.toString());
            } catch (JAXBException e) {
                LOGGER.debug("An error occurred debugging the GetFeature request", e);
            }
        }
    }

    private void debugResult(Result result) {
        if (LOGGER.isDebugEnabled()) {
            if (result != null && result.getMetacard() != null) {
                StringBuffer sb = new StringBuffer();
                sb.append("\nid:\t" + result.getMetacard().getId());
                sb.append("\nmetacardType:\t" + result.getMetacard().getMetacardType());
                if (result.getMetacard().getMetacardType() != null) {
                    sb.append("\nmetacardType name:\t"
                            + result.getMetacard().getMetacardType().getName());
                }
                sb.append("\ncontentType:\t" + result.getMetacard().getContentTypeName());
                sb.append("\ntitle:\t" + result.getMetacard().getTitle());
                sb.append("\nsource:\t" + result.getMetacard().getSourceId());
                sb.append("\nmetadata:\t" + result.getMetacard().getMetadata());
                sb.append("\nlocation:\t" + result.getMetacard().getLocation());
    
                LOGGER.debug("Transform complete. Metacard: {}", sb.toString());
            }
        }
    }

    public void setSecuritySettings(SecuritySettingsService securitySettings) {
        this.securitySettingsService = securitySettings;
    }

    private class MetacardTypeRegistration {

        private FeatureMetacardType ftMetacard;

        private Dictionary<String, Object> props;

        private String srs;

        public MetacardTypeRegistration(FeatureMetacardType ftMetacard,
                Dictionary<String, Object> props, String srs) {
            this.ftMetacard = ftMetacard;
            this.props = props;
            this.srs = srs;
        }

        public FeatureMetacardType getFtMetacard() {
            return ftMetacard;
        }

        public Dictionary<String, Object> getProps() {
            return props;
        }

        public String getSrs() {
            return srs;
        }

    }

    /**
     * Callback class to check the Availability of the WfsSource.
     * 
     * NOTE: Ideally, the framework would call isAvailable on the Source and the SourcePoller would
     * have an AvailabilityTask that cached each Source's availability. Until that is done, allow
     * the command to handle the logic of managing availability.
     * 
     * @author kcwire
     * 
     */
    private class WfsSourceAvailabilityCommand implements AvailabilityCommand {

        public WfsSourceAvailabilityCommand() {
        }

        @Override
        public boolean isAvailable() {
            LOGGER.debug("Checking availability for source {} ", getId());
            boolean oldAvailability = WfsSource.this.isAvailable();
            boolean newAvailability = false;
            // If the Remote object is null attempt to initialize it and
            // configure
            // all the capabilities.
            if (remoteWfs == null) {
                connectToRemoteWfs();
            }
            // Simple "ping" to ensure the source is responding
            newAvailability = (null != getCapabilities());
            if (oldAvailability != newAvailability) {
                availabilityChanged(newAvailability);
                // If the source becomes available, configure it.
                if (newAvailability) {
                    configureWfsFeatures();
                }
            }
            return newAvailability;
        }

    }
}
