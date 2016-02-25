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
package ddf.catalog.nato.stanag4559.source;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.IOUtils;
import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityCommand;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.omg.CORBA.Any;
import org.omg.CORBA.IntHolder;
import org.opengis.filter.sort.SortBy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.nato.stanag4559.common.GIAS.AccessCriteria;
import ddf.catalog.nato.stanag4559.common.GIAS.AttributeInformation;
import ddf.catalog.nato.stanag4559.common.GIAS.CatalogMgr;
import ddf.catalog.nato.stanag4559.common.GIAS.CatalogMgrHelper;
import ddf.catalog.nato.stanag4559.common.GIAS.DataModelMgr;
import ddf.catalog.nato.stanag4559.common.GIAS.DataModelMgrHelper;
import ddf.catalog.nato.stanag4559.common.GIAS.HitCountRequest;
import ddf.catalog.nato.stanag4559.common.GIAS.Library;
import ddf.catalog.nato.stanag4559.common.GIAS.LibraryDescription;
import ddf.catalog.nato.stanag4559.common.GIAS.LibraryHelper;
import ddf.catalog.nato.stanag4559.common.GIAS.LibraryManager;
import ddf.catalog.nato.stanag4559.common.GIAS.OrderMgr;
import ddf.catalog.nato.stanag4559.common.GIAS.OrderMgrHelper;
import ddf.catalog.nato.stanag4559.common.GIAS.Polarity;
import ddf.catalog.nato.stanag4559.common.GIAS.ProductMgr;
import ddf.catalog.nato.stanag4559.common.GIAS.ProductMgrHelper;
import ddf.catalog.nato.stanag4559.common.GIAS.SortAttribute;
import ddf.catalog.nato.stanag4559.common.GIAS.SubmitQueryRequest;
import ddf.catalog.nato.stanag4559.common.GIAS.View;
import ddf.catalog.nato.stanag4559.common.Stanag4559;
import ddf.catalog.nato.stanag4559.common.Stanag4559Constants;
import ddf.catalog.nato.stanag4559.common.UCO.DAGListHolder;
import ddf.catalog.nato.stanag4559.common.UCO.InvalidInputParameter;
import ddf.catalog.nato.stanag4559.common.UCO.NameValue;
import ddf.catalog.nato.stanag4559.common.UCO.ProcessingFault;
import ddf.catalog.nato.stanag4559.common.UCO.SystemFault;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.service.ConfiguredService;
import ddf.catalog.source.ConnectedSource;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.util.impl.MaskableImpl;

public class Stanag4559Source extends MaskableImpl
        implements FederatedSource, ConnectedSource, ConfiguredService {

    public static final String PASSWORD = "password";

    public static final String ID = "id";

    public static final String USERNAME = "username";

    public static final String KEY = "key";

    public static final String IOR_URL = "iorUrl";

    public static final String POLL_INTERVAL = "pollInterval";

    public static final String DATE_MODFIIED = "dateTimeModified";

    public static final String DATE_CREATED = "dateTimeDeclared";

    public static final String MAX_HIT_COUNT = "maxHitCount";

    private static final Logger LOGGER = LoggerFactory.getLogger(Stanag4559Source.class);

    private static final String DESCRIBABLE_PROPERTIES_FILE = "/describable.properties";

    private static final String DESCRIPTION = "description";

    private static final String ORGANIZATION = "organization";

    private static final String VERSION = "version";

    private static final String TITLE = "name";

    private static final String WGS84 = "WGS84";

    private static final String GEOGRAPHIC_DATUM = "GeographicDatum";

    private static final String ASC = "ASC";

    private static final String UTF8 = "UTF-8";

    private static final String CATALOG_MGR = "CatalogMgr";

    private static final String ORDER_MGR = "OrderMgr";

    private static final String PRODUCT_MGR = "ProductMgr";

    private static final String DATA_MODEL_MGR = "DataModelMgr";

    private static Library library;

    private static Properties describableProperties = new Properties();

    /* Mandatory STANAG 4559 Managers */

    private CatalogMgr catalogMgr;

    private OrderMgr orderMgr;

    private ProductMgr productMgr;

    private DataModelMgr dataModelMgr;

    /* ---------------------------  */

    private AvailabilityTask availabilityTask;

    private String iorUrl;

    private String username;

    private String password;

    private String key;

    private String id;

    private String iorString;

    private Integer maxHitCount;

    private FilterAdapter filterAdapter;

    private org.omg.CORBA.ORB orb;

    private AccessCriteria accessCriteria;

    private Set<SourceMonitor> sourceMonitors = new HashSet<>();

    private ScheduledFuture<?> availabilityPollFuture;

    private ScheduledExecutorService scheduler;

    private Integer pollInterval;

    private String configurationPid;

    private SecureCxfClientFactory<Stanag4559> factory;

    private Stanag4559FilterDelegate stanag4559FilterDelegate;

    private Set<ContentType> contentTypes = Stanag4559Constants.CONTENT_TYPES;

    private View[] views;

    private HashMap<String, List<AttributeInformation>> queryableAttributes;

    private HashMap<String, String[]> resultAttributes;

    private HashMap<String, List<String>> sortableAttributes;

    private String description;

    static {
        try (InputStream properties = Stanag4559Source.class.getResourceAsStream(
                DESCRIBABLE_PROPERTIES_FILE)) {
            describableProperties.load(properties);
        } catch (IOException e) {
            LOGGER.info("Failed to load properties", e);
        }
    }

    /**
     * Constructor used for testing.
     */
    public Stanag4559Source(SecureCxfClientFactory factory,
            HashMap<String, String[]> resultAttributes,
            HashMap<String, List<String>> sortableAttributes,
            Stanag4559FilterDelegate filterDelegate) {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        this.factory = factory;
        this.resultAttributes = resultAttributes;
        this.stanag4559FilterDelegate = filterDelegate;
        this.sortableAttributes = sortableAttributes;
        initOrb();
    }

    public Stanag4559Source() {
        initOrb();
        scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    public void init() {
        createClientFactory();
        initCorbaClient();
        setupAvailabilityPoll();
    }

    private void createClientFactory() {
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(password)) {
            factory = new SecureCxfClientFactory(iorUrl,
                    Stanag4559.class,
                    null,
                    null,
                    true,
                    false,
                    null,
                    null,
                    username,
                    password);
        } else {
            factory = new SecureCxfClientFactory(iorUrl,
                    Stanag4559.class,
                    null,
                    null,
                    true,
                    false,
                    null,
                    null);
        }
    }

    /**
     * Initializes the Corba Client ORB and gets the mandatory interfaces that are required for a
     * STANAG 4559 complaint Federated Source, also queries the source for the queryable attributes
     * and views that it provides.
     */
    private void initCorbaClient() {
        accessCriteria = new AccessCriteria(username, password, key);
        getIorFileFromSource();
        initLibrary();
        setSourceDescription();
        initMandatoryManagers();
        initServerViews();
        initQueryableAttributes();
        initSortableAndResultAttributes();
        setFilterDelegate();
    }

    /**
     * Uses the SecureClientCxfFactory to obtain the IOR file from the provided URL via HTTP(S).
     */
    private void getIorFileFromSource() {
        Stanag4559 stanag4559 = factory.getClient();

        try (InputStream inputStream = stanag4559.getIorFile()) {
            iorString = IOUtils.toString(inputStream, UTF8);
        } catch (IOException e) {
            LOGGER.error("{} : Unable to process IOR String.", id);
        }

        if (StringUtils.isNotBlank(iorString)) {
            LOGGER.debug("{} : Successfully obtained IOR file from {}", getId(), iorUrl);
        } else {
            LOGGER.error("{} : Received an empty or null IOR String.", id);
        }
    }

    /**
     * Initializes the Corba ORB with no additional arguments
     */
    private void initOrb() {
        orb = org.omg.CORBA.ORB.init(new String[0], null);

        if (orb != null) {
            LOGGER.debug("{} : Successfully initialized CORBA orb.", getId());
        } else {
            LOGGER.error("{} : Unable to initialize CORBA orb.", getId());
        }
    }

    /**
     * Initializes the Root STANAG 4559 Library Interface
     */
    private void initLibrary() {
        org.omg.CORBA.Object obj = orb.string_to_object(iorString);
        library = LibraryHelper.narrow(obj);
        if (library == null) {
            LOGGER.error("{} : Unable to initialize the library interface.", getId());
        } else {
            LOGGER.debug("{} : Initialized Library Interface", getId());
        }
    }

    /**
     * Initializes all STANAG 4559 mandatory managers:
     * CatalogMgr
     * OrderMgr
     * DataModelMgr
     * ProductMgr
     */
    private void initMandatoryManagers() {
        try {
            LibraryManager libraryManager = library.get_manager(CATALOG_MGR, accessCriteria);
            setCatalogMgr(CatalogMgrHelper.narrow(libraryManager));

            libraryManager = library.get_manager(ORDER_MGR, accessCriteria);
            setOrderMgr(OrderMgrHelper.narrow(libraryManager));

            libraryManager = library.get_manager(PRODUCT_MGR, accessCriteria);
            setProductMgr(ProductMgrHelper.narrow(libraryManager));

            libraryManager = library.get_manager(DATA_MODEL_MGR, accessCriteria);
            setDataModelMgr(DataModelMgrHelper.narrow(libraryManager));

        } catch (ProcessingFault | SystemFault | InvalidInputParameter e) {
            LOGGER.error("{} : Unable to retrieve mandatory managers.", id, e);
        }

        if (catalogMgr != null && orderMgr != null & productMgr != null && dataModelMgr != null) {
            LOGGER.debug("{} : Initialized STANAG mandatory managers.", getId());
        } else {
            LOGGER.error("{} : Unable to initialize mandatory mangers.", getId());
        }
    }

    /**
     * Obtains all possible views that the Federated Source can provide. EX: NSIL_ALL_VIEW, NSIL_IMAGERY
     * According to ANNEX D, TABLE D-6, the passed parameter in get_view_names is an empty list(not used).
     *
     * @return an array of views
     */
    private void initServerViews() {
        View[] views = null;
        try {
            views = dataModelMgr.get_view_names(new NameValue[0]);
        } catch (ProcessingFault | SystemFault | InvalidInputParameter e) {
            LOGGER.error("{} : Unable to retrieve views.", id, e);
        }
        if (views == null) {
            LOGGER.error("{} : Unable to retrieve views.", id);
        }
        this.views = views;
    }

    /**
     * Obtains all possible attributes for all possible views that the Federated Source can provide, and
     * populates a sortableAttributes map, as well as resultAttributes map that will be used for querying
     * the server.
     * According to ANNEX D, TABLE D-6, the passed parameter in get_view_names is an empty list(not used).
     *
     * @return a map of each view and the attributes provided by the source for that view
     */
    private void initSortableAndResultAttributes() {
        HashMap<String, String[]> resultAttributesMap = new HashMap<>();
        HashMap<String, List<String>> sortableAttributesMap = new HashMap<>();

        try {
            for (int i = 0; i < views.length; i++) {

                List<String> sortableAttributesList = new ArrayList<>();

                AttributeInformation[] attributeInformationArray =
                        dataModelMgr.get_attributes(views[i].view_name, new NameValue[0]);
                String[] resultAttributes = new String[attributeInformationArray.length];

                for (int c = 0; c < attributeInformationArray.length; c++) {
                    AttributeInformation attributeInformation = attributeInformationArray[c];
                    resultAttributes[c] = attributeInformation.attribute_name;

                    if (attributeInformation.sortable) {
                        sortableAttributesList.add(attributeInformation.attribute_name);
                    }

                }
                sortableAttributesMap.put(views[0].view_name, sortableAttributesList);
                resultAttributesMap.put(views[0].view_name, resultAttributes);

            }
        } catch (ProcessingFault | SystemFault | InvalidInputParameter e) {
            LOGGER.error("{} : Unable to retrieve queryable attributes.", id, e);
        }

        if (resultAttributesMap.size() == 0) {
            LOGGER.warn("{} : Received empty attributes list from STANAG source.", getId());
        }

        this.sortableAttributes = sortableAttributesMap;
        this.resultAttributes = resultAttributesMap;
    }

    /**
     * Obtains all queryable attributes for all possible views that the Federated Source can provide.
     * According to ANNEX D, TABLE D-6, the passed parameter in get_view_names is an empty list(not used).
     *
     * @return a map of each view and the queryable attributes provided by the source for that view
     */
    private void initQueryableAttributes() {
        HashMap<String, List<AttributeInformation>> map = new HashMap<>();

        try {
            for (int i = 0; i < views.length; i++) {
                AttributeInformation[] attributeInformationArray =
                        dataModelMgr.get_queryable_attributes(views[i].view_name, new NameValue[0]);
                List<AttributeInformation> attributeInformationList = new ArrayList<>();
                for (int c = 0; c < attributeInformationArray.length; c++) {
                    attributeInformationList.add(attributeInformationArray[c]);
                }
                map.put(views[0].view_name, attributeInformationList);
            }
        } catch (ProcessingFault | SystemFault | InvalidInputParameter e) {
            LOGGER.error("{} : Unable to retrieve queryable attributes.", id, e);
        }

        if (map.size() == 0) {
            LOGGER.warn("{} : Received empty queryable attributes from STANAG source.", getId());
        }
        queryableAttributes = map;
    }

    /**
     * Obtains the description of the source from the Library interface.
     *
     * @return a description of the source
     */
    private void setSourceDescription() {
        String description = "";
        try {
            LibraryDescription librarDescription = library.get_library_description();
            description += librarDescription.library_name + " : ";
            description += librarDescription.library_description;
        } catch (ProcessingFault | SystemFault e) {
            LOGGER.error("{} : Unable to retrieve source description.", id, e);
        }

        if (StringUtils.isBlank(description)) {
            LOGGER.warn("{} :  Unable to retrieve source description.", getId());
        }
        this.description = description;
    }

    public void destroy() {
        orb.shutdown(true);
        availabilityPollFuture.cancel(true);
        scheduler.shutdownNow();
    }

    public void refresh(Map<String, Object> configuration) {
        LOGGER.debug("Entering Refresh : {}", getId());

        if (configuration == null || configuration.isEmpty()) {
            LOGGER.error("{} {} : Received null or empty configuration during refresh.",
                    this.getClass()
                            .getSimpleName(),
                    getId());
            return;
        }
        String username = (String) configuration.get(USERNAME);
        if (StringUtils.isNotBlank(username) && !username.equals(this.username)) {
            setUsername(username);
        }
        String password = (String) configuration.get(PASSWORD);
        if (StringUtils.isNotBlank(password) && !password.equals(this.password)) {
            setPassword(password);
        }
        String key = (String) configuration.get(KEY);
        if (StringUtils.isNotBlank(key) && !key.equals(this.key)) {
            setKey(key);
        }
        String id = (String) configuration.get(ID);
        if (StringUtils.isNotBlank(id) && !id.equals(this.id)) {
            setId(id);
        }
        String iorUrl = (String) configuration.get(IOR_URL);
        if (StringUtils.isNotBlank(iorUrl) && !iorUrl.equals(this.iorUrl)) {
            setIorUrl(iorUrl);
        }
        Integer pollInterval = (Integer) configuration.get(POLL_INTERVAL);
        if (pollInterval != null && !pollInterval.equals(this.pollInterval)) {
            setPollInterval(pollInterval);
        }
        Integer maxHitCount = (Integer) configuration.get(MAX_HIT_COUNT);
        if (maxHitCount != null && !maxHitCount.equals(this.maxHitCount)) {
            setMaxHitCount(maxHitCount);
        }
        init();
    }

    @Override
    public String getDescription() {
        StringBuilder sb = new StringBuilder();
        sb.append(describableProperties.getProperty(DESCRIPTION))
                .append(System.getProperty("line.separator"))
                .append(description);
        return sb.toString();
    }

    @Override
    public String getId() {
        String sourceId = super.getId();
        return sourceId;
    }

    @Override
    public SourceResponse query(QueryRequest queryRequest) throws UnsupportedQueryException {
        ddf.catalog.nato.stanag4559.common.GIAS.Query query = createQuery(queryRequest.getQuery());

        String[] results = resultAttributes.get(Stanag4559Constants.NSIL_ALL_VIEW);

        SortAttribute[] sortAttributes = getSortAttributes(queryRequest.getQuery()
                .getSortBy());
        NameValue[] propertiesList = getDefaultPropertyList();
        LOGGER.debug("{} : Sending query to source. {} {} {} {}",
                getId(),
                results,
                sortAttributes,
                sortAttributes,
                propertiesList);
        return submitQuery(queryRequest, query, results, sortAttributes, propertiesList);
    }

    /**
     * Uses the Stanag4559FilterDelegate to create a STANAG 4559 BQS (Boolean Syntax Query) from the DDF Query
     *
     * @param query - the query recieved from the Search-Ui
     * @return - a STANAG4559 complaint query
     * @throws UnsupportedQueryException
     */
    private ddf.catalog.nato.stanag4559.common.GIAS.Query createQuery(Query query)
            throws UnsupportedQueryException {
        String filter = createFilter(query);

        LOGGER.debug("{} : Created Query filter : {}", getId(), filter);

        return new ddf.catalog.nato.stanag4559.common.GIAS.Query(Stanag4559Constants.NSIL_ALL_VIEW,
                filter);
    }

    /**
     * Obtains the number of hits that the given query has received from the server.
     *
     * @param query      - a BQS query
     * @param properties - a list of properties for the query
     * @return - the hit count
     */
    private int getHitCount(ddf.catalog.nato.stanag4559.common.GIAS.Query query,
            NameValue[] properties) {
        IntHolder intHolder = new IntHolder();
        try {
            HitCountRequest hitCountRequest = catalogMgr.hit_count(query, properties);
            hitCountRequest.complete(intHolder);
        } catch (ProcessingFault | SystemFault | InvalidInputParameter e) {
            LOGGER.error("{} : Unable to get hit count for query.", getId());
        }

        LOGGER.debug("{} :  Received {} hit(s) from query.", getId(), intHolder.value);
        return intHolder.value;
    }

    /**
     * Submits and completes a BQS Query to the STANAG 4559 server and returns the response.
     *
     * @param queryRequest     - the query request generated from the search
     * @param query            - a BQS query
     * @param resultAttributes - a list of desired result attributes
     * @param sortAttributes   - a list of attributes to sort by
     * @param properties       - a list of properties for the query
     * @return - the server's response
     */
    private SourceResponse submitQuery(QueryRequest queryRequest,
            ddf.catalog.nato.stanag4559.common.GIAS.Query query, String[] resultAttributes,
            SortAttribute[] sortAttributes, NameValue[] properties) {
        DAGListHolder dagListHolder = new DAGListHolder();

        try {
            SubmitQueryRequest submitQueryRequest = catalogMgr.submit_query(query,
                    resultAttributes,
                    sortAttributes,
                    properties);
            submitQueryRequest.set_number_of_hits(maxHitCount);
            submitQueryRequest.complete_DAG_results(dagListHolder);
        } catch (ProcessingFault | SystemFault | InvalidInputParameter e) {
            LOGGER.error("{} : Unable to query source.", getId());
        }

        SourceResponseImpl sourceResponse = new SourceResponseImpl(queryRequest,
                new ArrayList<>(),
                (long) getHitCount(query, properties));

        return sourceResponse;
    }

    private void setFilterDelegate() {
        stanag4559FilterDelegate = new Stanag4559FilterDelegate(queryableAttributes,
                Stanag4559Constants.NSIL_ALL_VIEW);
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
        return describableProperties.getProperty(VERSION);
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
    public Set<String> getOptions(Metacard arg0) {
        return null;
    }

    @Override
    public Set<String> getSupportedSchemes() {
        return null;
    }

    @Override
    public ResourceResponse retrieveResource(URI resourceUri,
            Map<String, Serializable> requestProperties)
            throws IOException, ResourceNotFoundException, ResourceNotSupportedException {
        return null;
    }

    @Override
    public String getConfigurationPid() {
        return configurationPid;
    }

    @Override
    public void setConfigurationPid(String configurationPid) {
        this.configurationPid = configurationPid;
    }

    @Override
    public Set<ContentType> getContentTypes() {
        return contentTypes;
    }

    private String createFilter(Query query) throws UnsupportedQueryException {
        return this.filterAdapter.adapt(query, stanag4559FilterDelegate);
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setId(String id) {
        this.id = id;
        super.setId(id);
    }

    public void setIorUrl(String iorUrl) {
        this.iorUrl = iorUrl;
    }

    public void setMaxHitCount(Integer maxHitCount) {
        this.maxHitCount = maxHitCount;
    }

    public String getIorUrl() {
        return iorUrl;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public String getKey() {
        return key;
    }

    public Integer getMaxHitCount() {
        return maxHitCount;
    }

    public Integer getPollInterval() {
        return pollInterval;
    }

    public void setPollInterval(Integer interval) {
        this.pollInterval = interval;
    }

    public void setFilterAdapter(FilterAdapter filterAdapter) {
        this.filterAdapter = filterAdapter;
    }

    public void setCatalogMgr(CatalogMgr catalogMgr) {
        this.catalogMgr = catalogMgr;
    }

    public void setOrderMgr(OrderMgr orderMgr) {
        this.orderMgr = orderMgr;
    }

    public void setDataModelMgr(DataModelMgr dataModelMgr) {
        this.dataModelMgr = dataModelMgr;
    }

    public void setProductMgr(ProductMgr productMgr) {
        this.productMgr = productMgr;
    }

    public void setSortableAttributes(HashMap<String, List<String>> sortableAttributes) {
        this.sortableAttributes = sortableAttributes;
    }

    public void setAvailabilityTask(AvailabilityTask availabilityTask) {
        this.availabilityTask = availabilityTask;
    }

    public void setupAvailabilityPoll() {
        LOGGER.debug("Setting Availability poll task for {} minute(s) on Source {}",
                getPollInterval(),
                getId());
        Stanag4559AvailabilityCommand command = new Stanag4559AvailabilityCommand();
        long interval = TimeUnit.MINUTES.toMillis(getPollInterval());
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
            // Schedule the availability check every 1 second. The actually call to
            // the remote server will only occur if the pollInterval has
            // elapsed.
            availabilityPollFuture = scheduler.scheduleWithFixedDelay(availabilityTask,
                    AvailabilityTask.NO_DELAY,
                    AvailabilityTask.ONE_SECOND,
                    TimeUnit.SECONDS);
        } else {
            LOGGER.debug("No changes being made on the poller.");
        }

    }

    private void availabilityChanged(boolean isAvailable) {

        if (isAvailable) {
            LOGGER.info("STANAG 4559 source {} is available.", getId());
        } else {
            LOGGER.info("STANAG 4559 source {} is unavailable.", getId());
        }

        for (SourceMonitor monitor : this.sourceMonitors) {
            if (isAvailable) {
                LOGGER.debug("Notifying source monitor that STANAG 4559 source {} is available.",
                        getId());
                monitor.setAvailable();
            } else {
                LOGGER.debug("Notifying source monitor that STANAG 4559 source {} is unavailable.",
                        getId());
                monitor.setUnavailable();
            }
        }
    }

    /**
     * Returns the Default Property List defined in the STANAG 4559 specification.
     *
     * @return - default WGS84 Geographic Datum.
     */
    private NameValue[] getDefaultPropertyList() {
        Any defaultAnyProperty = orb.create_any();
        defaultAnyProperty.insert_string(WGS84);
        NameValue[] result = {new NameValue(GEOGRAPHIC_DATUM, defaultAnyProperty)};
        return result;
    }

    /**
     * Sets a SortAttribute[] to be used in a query.  The STANAG 4559 Spec has no mechanism to sort
     * queries by RELEVANCE or Shortest/Longest distance from a point, so they are ignored.
     *
     * @param sortBy - sortBy object specified in the Search UI
     * @return - an array of SortAttributes sent in the query to the source.
     */
    private SortAttribute[] getSortAttributes(SortBy sortBy) {
        if (sortBy == null || sortableAttributes == null) {
            return new SortAttribute[0];
        }

        String sortAttribute = sortBy.getPropertyName()
                .getPropertyName();
        Polarity sortPolarity;

        if (sortBy.getSortOrder()
                .toSQL()
                .equals(ASC)) {
            sortPolarity = Polarity.ASCENDING;
        } else {
            sortPolarity = Polarity.DESCENDING;
        }

        if (sortAttribute.equals(Metacard.MODIFIED) && isAttributeSupported(DATE_MODFIIED)) {
            SortAttribute[] sortAttributeArray = {new SortAttribute(DATE_MODFIIED, sortPolarity)};
            return sortAttributeArray;
        } else if (sortAttribute.equals(Metacard.CREATED) && isAttributeSupported(DATE_CREATED)) {
            SortAttribute[] sortAttributeArray = {new SortAttribute(DATE_CREATED, sortPolarity)};
            return sortAttributeArray;
        } else {
            return new SortAttribute[0];
        }
    }

    /**
     * Verifies that a given attribute exists in the list of sortableAttributes for NSIL_ALL_VIEW
     */
    private boolean isAttributeSupported(String attribute) {
        List<String> attributeInformationList =
                sortableAttributes.get(Stanag4559Constants.NSIL_ALL_VIEW);
        for (String sortableAttribute : attributeInformationList) {
            if (attribute.equals(sortableAttribute)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Callback class to check the Availability of the Stanag4559Source.
     * <p>
     * NOTE: Ideally, the framework would call isAvailable on the Source and the SourcePoller would
     * have an AvailabilityTask that cached each Source's availability. Until that is done, allow
     * the command to handle the logic of managing availability.
     */
    private class Stanag4559AvailabilityCommand implements AvailabilityCommand {

        @Override
        public boolean isAvailable() {
            LOGGER.debug("Checking availability for source {} ", getId());
            boolean oldAvailability = Stanag4559Source.this.isAvailable();
            String[] managers = null;

            // Refresh IOR String when polling for availability in case server conditions change
            try {
                getIorFileFromSource();
                initLibrary();
                managers = library.get_manager_types();
            } catch (Exception e) {
                LOGGER.error("{} : Connection Failure for source.", getId(), e);
            }

            // If the IOR string is not valid, or the source cannot communicate with the library, the
            // source is unavailable
            boolean newAvailability = (managers != null && StringUtils.isNotBlank(iorString));
            if (oldAvailability != newAvailability) {
                availabilityChanged(newAvailability);
                // If the source becomes available, configure it.
                if (newAvailability) {
                    initCorbaClient();
                }
            }
            return newAvailability;
        }
    }
}