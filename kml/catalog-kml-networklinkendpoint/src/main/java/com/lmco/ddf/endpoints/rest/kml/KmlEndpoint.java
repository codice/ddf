/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

package com.lmco.ddf.endpoints.rest.kml;


import ddf.catalog.Constants;
import ddf.catalog.data.Metacard;
import ddf.catalog.event.Subscription;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.util.DdfConfigurationManager;
import ddf.catalog.util.DdfConfigurationWatcher;
import ddf.service.kml.KMLTransformer;
import ddf.service.kml.subscription.KmlSubscription;
import ddf.service.kml.subscription.KmlUpdateDeliveryMethod;
import de.micromata.opengis.kml.v_2_2_0.Change;
import de.micromata.opengis.kml.v_2_2_0.Create;
import de.micromata.opengis.kml.v_2_2_0.Delete;
import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Folder;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.KmlFactory;
import de.micromata.opengis.kml.v_2_2_0.Link;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import de.micromata.opengis.kml.v_2_2_0.NetworkLinkControl;
import de.micromata.opengis.kml.v_2_2_0.Placemark;
import de.micromata.opengis.kml.v_2_2_0.Update;
import de.micromata.opengis.kml.v_2_2_0.ViewRefreshMode;
import org.apache.log4j.Logger;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;
import java.io.Serializable;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Endpoint used to create a KML Network Link in order to receive updated query
 * results from the DDF. The KML Network Link will link Google Earth to DDF
 * through the OpenSearch Endpoint. As a users changes the Google Earth view,
 * the query results will be updated for that view.
 * 
 * @author Ian Barnett
 * 
 */
@Path( "/" )
public class KmlEndpoint implements DdfConfigurationWatcher
{

    private static final String URL_KEY = "url";
	private static final String FORWARD_SLASH = "/";
	private static final String CATALOG_URL_PATH = "catalog";
	private static final int DEFAULT_MAX_SESSION_LENGTH = -1; // -1 indicates do
                                                              // not terminate
                                                              // session
                                                              // explicitly
    private static final String NETWORK_LINK_FOLDER_NAME = "DDF OpenSearch Network Link";
    private static final String DOCUMENT_ID_POSTFIX = "-doc";
    private static final String ENCODED_COMMA = "%2C";

    private static final String SUBSCRIPTION_QUERY_PARAM = "subscription";
    private static final String GEO_QUERY_PARAM = "geometry";
    private static final String RADIUS_QUERY_PARAM = "radius";
    private static final String POLYGON_QUERY_PARAM = "polygon";
    private static final String LAT_QUERY_PARAM = "lat";
    private static final String LONG_QUERY_PARAM = "lon";
    private static final String BBOX_QUERY_PARAM = "bbox";
    private static final String SOURCES_QUERY_PARAM = "src";

    private static final String KML_MIME_TYPE = "application/vnd.google-earth.kml+xml";
    private static final String KML_TRANSFORM_PARAM = "kml";
    private static final String KML_URL_PATH = "kml";
    private static final String OPENSEARCH_URL_PATH = "query";
    private static final String OPENSEARCH_SORT_KEY = "sort";
    private static final String OPENSEARCH_DEFAULT_SORT = "date:desc";
    private static final String OPENSEARCH_FORMAT_KEY = "format";

    private static final long TIMEOUT_MS = 1000 * 60 * 10; // 10 Minutes

    /** Default refresh time after the View stops moving */
    private static final double DEFAULT_VIEW_REFRESH_TIME = 2.0;

    /**
     * The format of the bounding box query parameters Google Earth attaches to
     * the end of the query URL.
     */
    private static final String VIEW_FORMAT_STRING = "bbox=[bboxWest],[bboxSouth],[bboxEast],[bboxNorth]";

    private BundleContext context;

    private Map<String, URL> hrefMap;
    private ConcurrentHashMap<String, Long> lastUpdated;
    private KMLTransformer kmlTransformer;
    private ExecutorService executorService;
	private String ddfHost;
	private String ddfPort;
	private String servicesContextRoot;

    private static final Logger LOGGER = Logger.getLogger(KmlEndpoint.class);

    public KmlEndpoint( BundleContext context, KMLTransformer kmlTransformer )
    {
        LOGGER.trace("ENTERING: KML Enpoint Constructor");
        this.context = context;
        this.kmlTransformer = kmlTransformer;
        this.lastUpdated = new ConcurrentHashMap<String, Long>();

        this.hrefMap = new HashMap<String, URL>();
        this.executorService = Executors.newCachedThreadPool();
        LOGGER.trace("EXITING: KML Enpoint Constructor");
    }

    private String marshalKml( Kml kmlResult )
    {
        String kmlResultString = null;

        StringWriter writer = new StringWriter();
        kmlResult.marshal(writer);
        kmlResultString = writer.toString();

        return kmlResultString;
    }

    /**
     * Kml REST Get. Returns a KML Network Link to the DDF OpenSearch Endpoint
     * Any query parameters passed in will be used in the OpenSearch query.
     * 
     * @param uriInfo
     * @return KML Network Link
     */
    @GET
    @Path( FORWARD_SLASH )
    public Response getKmlNetworkLink( @Context UriInfo uriInfo )
    {
        LOGGER.trace("ENTERING: getKmlNetworkLink");
        try
        {
            // Create and keep track of subscription id
            String subId = UUID.randomUUID().toString();
            URL networkLinkUrl = createKmlQueryUrl(uriInfo, subId);

            hrefMap.put(subId, networkLinkUrl);
            lastUpdated.put(subId, System.currentTimeMillis());
            this.executorService.submit(new SubscriptionTimeoutTracker(this.context, subId));

            String kml = generateViewBasedNetworkLink(networkLinkUrl);
            LOGGER.debug("KML Network Link: " + kml);

            LOGGER.trace("EXITING: getKmlNetworkLink");
            return Response.ok(kml, KML_MIME_TYPE).build();
        }
        catch (UnknownHostException e){
        	//TODO: Return informative error message
        	LOGGER.error("Unable to determine DDF Host.");
        	throw new WebApplicationException();
        }
        catch (Exception e)
        {
            LOGGER.error("Error obtaining KML NetworkLink.", e);
            throw new WebApplicationException();
        }
    }

    /**
     * Generates xml for View-based Network Link
     * 
     * @param networkLinkUrl
     * @return KML View-based NetworkLink
     * @throws JAXBException
     * @throws PluginExecutionException
     */
    private String generateViewBasedNetworkLink( URL networkLinkUrl ) throws JAXBException, PluginExecutionException
    {
        // create folder and give it a name
        Kml kml = KmlFactory.createKml();
        Folder folder = KmlFactory.createFolder();
        kml.setFeature(folder);
        folder.setName(NETWORK_LINK_FOLDER_NAME);
        folder.setOpen(true);
        List<Feature> folderObjects = folder.getFeature();

        // create network link and give it a name
        NetworkLink networkLink = KmlFactory.createNetworkLink();
        folderObjects.add(networkLink);
        networkLink.setName(networkLinkUrl.getQuery());
        networkLink.setOpen(true);

        // create link and add it to networkLinkElements
        Link link = KmlFactory.createLink();
        link.setHref(networkLinkUrl.toString());
        link.setViewRefreshMode(ViewRefreshMode.ON_STOP);
        link.setViewRefreshTime(DEFAULT_VIEW_REFRESH_TIME);
        link.setViewFormat(VIEW_FORMAT_STRING);
        link.setViewBoundScale(1);
        networkLink.setLink(link);

        return marshalKml(kml);
    }

    /**
     * Creates a URL to the DDF OpenSearch Endpoint for the KML NetworkLink to
     * obtain query results.
     * 
     * @param queryUriInfo
     * @param subscriptionId
     * @return URL to the DDF OpenSearch Endpoint specifying format=kml and the
     *         query parameters passed in to the initial request to obtain the
     *         NetworkLink.
     * @throws UnknownHostException
     * @throws MalformedURLException
     */
    protected URL createKmlQueryUrl( UriInfo queryUriInfo, String subscriptionId ) throws UnknownHostException,
        MalformedURLException
    {
    	LOGGER.debug("ENTERING: createKmlQueryUrl");
        UriBuilder builder = UriBuilder.fromUri(queryUriInfo.getBaseUri());

        builder = generateDdfEndpointUrl(servicesContextRoot + FORWARD_SLASH + CATALOG_URL_PATH + FORWARD_SLASH + OPENSEARCH_URL_PATH, builder);
        
        MultivaluedMap<String, String> queryParams = queryUriInfo.getQueryParameters(false);
        Set<Entry<String, List<String>>> entrySet = queryParams.entrySet();
        for ( Entry<String, List<String>> currParam : entrySet )
        {
            List<String> currQueryVal = currParam.getValue();
            String currQueryKey = currParam.getKey();

            // Since this will be a view-based query, it doesn't make sense to
            // send in query params relating to geometry.
            // The bounding box determined by the view in Google Earth will
            // determine the geometry to search over.
            if (GEO_QUERY_PARAM.equals(currQueryKey) || POLYGON_QUERY_PARAM.equals(currQueryKey)
                    || RADIUS_QUERY_PARAM.equals(currQueryKey) || LAT_QUERY_PARAM.equals(currQueryKey)
                    || LONG_QUERY_PARAM.equals(currQueryKey) || BBOX_QUERY_PARAM.equals(currQueryKey))
            {
                LOGGER
                    .info("Geometry based query parameter received.  "
                            + "Since the view-based NetworkLink will determine the desired query geometry, this param will be ignored.");
            }
            else
            {
                if (currQueryVal.size() == 1)
                {
                    builder = builder.queryParam(currQueryKey, currParam.getValue().get(0));
                }
                else
                {
                    builder = builder.queryParam(currQueryKey, currParam.getValue());
                }
            }

        }

        // TODO: Support federation because current subscription will not find
        // out about updates from federated sites
        // For now, set every query to local
        // remove all current values for "src"
        builder = builder.replaceQueryParam(SOURCES_QUERY_PARAM, new Object[0]);
        // set src param for opensearch to "local"
        // otherwise, OpenSearch defaults to enterprise search as true.
        builder = builder.replaceQueryParam(SOURCES_QUERY_PARAM, "local");
        LOGGER.debug("using temporal sorting");
        builder = builder.queryParam(OPENSEARCH_SORT_KEY, OPENSEARCH_DEFAULT_SORT);
        builder = builder.queryParam(OPENSEARCH_FORMAT_KEY, KML_TRANSFORM_PARAM);
        builder = builder.queryParam(Constants.SUBSCRIPTION_KEY, subscriptionId);

        URI resultingUri = builder.build();
        LOGGER.debug("network link url: " + resultingUri);
        
        LOGGER.debug("EXITING: createKmlQueryUrl");
        return resultingUri.toURL();
    }

    /**
     * REST operation located at /update. Used to obtain a NetworkLinkControl
     * object for the purposes of updating entries obtained from the view-based
     * NetworkLink
     * 
     * @param uriInfo
     * @param subIdQueryParam
     * @param incomingBoundingBox
     * @return HTTP response containing NetworkLinkControl xml element.
     */
    @GET
    @Path( "/update" )
    public Response getKmlNetworkLinkUpdate( @Context UriInfo uriInfo,
        @QueryParam( SUBSCRIPTION_QUERY_PARAM ) String subIdQueryParam,
        @QueryParam( BBOX_QUERY_PARAM ) String incomingBoundingBox )
    {
        try
        {
            LOGGER.trace("ENTERING: getKmlNetworkLinkUpdate");
            String kmlResult = generateNetworkLinkControl(subIdQueryParam, incomingBoundingBox, uriInfo);
            LOGGER.debug("KML Update Response: " + kmlResult);

            LOGGER.trace("EXITING: getKmlNetworkLinkUpdate");
            return Response.ok(kmlResult, "application/vnd.google-earth.kml+xml").build();
        }
        catch (Exception e)
        {
            LOGGER.error("Error performing network link update", e);
            throw new WebApplicationException(e);
        }
    }

    /**
     * Creates NetworkLinkControl xml by accessing the specified Subscription's
     * DeliveryMethod and obtaining all Metacards that were updated, created, or
     * deleted in the provided interval.
     * 
     * @param subIdQueryParam
     * @param incomingBoundingBox
     * @param uriInfo
     * @return NetworkLinkControl xml containing the Update element with
     *         Changes, Creates, and Deletes
     * @throws JAXBException
     * @throws URISyntaxException
     * @throws InvalidSyntaxException
     * @throws PluginExecutionException
     */
    private String generateNetworkLinkControl( String subIdQueryParam, String incomingBoundingBox, UriInfo uriInfo )
        throws JAXBException, URISyntaxException, InvalidSyntaxException, PluginExecutionException
    {
        LOGGER.trace("ENTERING: generateNetworkLinkControl");

        URL targetHref = hrefMap.get(subIdQueryParam);
        UriBuilder targetHrefBuilder = UriBuilder.fromUri(targetHref.toURI());

        targetHrefBuilder = targetHrefBuilder.queryParam(BBOX_QUERY_PARAM, incomingBoundingBox);
        String targetHrefString = targetHrefBuilder.build().toString();
        targetHrefString = targetHrefString.replaceAll(ENCODED_COMMA, ",");
        LOGGER.debug("target href: " + targetHrefString);

        // Generate kml NetworkLinkControl update
        Kml kml = KmlFactory.createKml();

        // Create NetworkLinkControl element and add to kmlType
        NetworkLinkControl netLinkControl = KmlFactory.createNetworkLinkControl();
        kml.setNetworkLinkControl(netLinkControl);

        // Generate Update and add to NetworkLinkControl
        Update update = generateUpdateKml(targetHrefString, subIdQueryParam, uriInfo);
        if (update.getCreateOrDeleteOrChange() != null && update.getCreateOrDeleteOrChange().isEmpty())
        {
            return null;
        }
        netLinkControl.setUpdate(update);
        netLinkControl.setMaxSessionLength(DEFAULT_MAX_SESSION_LENGTH);

        String kmlResultString = marshalKml(kml);

        // This is needed to compensate for a bug in GE and JAK. If a
        // NetworkLinkControl element has this namespace
        // declared, it will not work as of Google Earth 6.0.3.2197
        kmlResultString = kmlResultString.replaceAll("xmlns:xal=\"urn:oasis:names:tc:ciq:xsdschema:xAL:2\\.0\"", "");
        LOGGER.debug("updating last-updated time.");
        lastUpdated.put(subIdQueryParam, System.currentTimeMillis());

        LOGGER.trace("EXITING: generateNetworkLinkControl");
        return kmlResultString;
    }

    /**
     * Used to create the KML "Update" element and all its Change, Create and
     * Delete child elements
     * 
     * @param subscriptionId
     * @param updateType
     * @param uriInfo
     * @throws InvalidSyntaxException
     * @throws PluginExecutionException
     */
    private Update generateUpdateKml( String targetHref, String subscriptionId, UriInfo uriInfo )
        throws InvalidSyntaxException, PluginExecutionException
    {
        LOGGER.trace("ENTERING: generateUpdateKml");
        List<Object> updateList = new ArrayList<Object>();
        KmlSubscription currentSubscription = getSubscription(subscriptionId);
        KmlUpdateDeliveryMethod delMethod = (KmlUpdateDeliveryMethod) currentSubscription.getDeliveryMethod();
        Map<String, Serializable> arguments = new HashMap<String, Serializable>();

        try
        {
        // Handle Created events
        Queue<Metacard> createdQueue = delMethod.getCreated();
	        LOGGER.debug("created entries: " + createdQueue.size());
        while (!createdQueue.isEmpty())
        {
	            Metacard currCreatedMetacard = createdQueue.poll();	
            if (currCreatedMetacard != null)
            {
                // create "Create" type and add to "Update" list
                Create create = KmlFactory.createCreate();
                updateList.add(create);
                Folder createFolder = create.createAndAddFolder();
                createFolder.setTargetId(subscriptionId);
	
                // Add Invocation URL and subscription id to arguments
                try
                {
                    String restUrlToMetacard = createRestLinkToMetacard(uriInfo, currCreatedMetacard.getId());
                    LOGGER.debug("rest url to metacard: " + restUrlToMetacard);
		    arguments.put(URL_KEY, restUrlToMetacard);
                }
                catch (UnknownHostException e)
                {
	                    LOGGER.warn("Error creating URL to REST service, KML will not have link to Metacard.");
                }
                arguments.put(Constants.SUBSCRIPTION_KEY, subscriptionId);
	
                // create "Document" and add "Placemark" to the document
                Document doc = kmlTransformer.transformEntry(null, currCreatedMetacard, arguments);
                createFolder.addToFeature(doc);
            }
        }
	
        // Handle deleted events
        Queue<Metacard> deletedQueue = delMethod.getDeleted();
	        LOGGER.debug("deleted entries: " + deletedQueue.size());
        while (!deletedQueue.isEmpty())
        {
	            Metacard currDeletedMetacard = deletedQueue.poll();	
            if (currDeletedMetacard != null)
            {
                // create "Delete" type and add to "Update" list
                Delete delete = KmlFactory.createDelete();
                // List<JAXBElement<? extends AbstractFeatureType>>
                // deletedPlacemarks = delete.getAbstractFeatureGroup();
                updateList.add(delete);
	
                // create "Placemark" and add to "Delete"
                // All metacards are currently wrapped in a Document, to delete
                // the document the id is needed
                Document doc = KmlFactory.createDocument();
                delete.addToFeature(doc);
                // "-doc" is needed to distinguish from placemark
                doc.setTargetId(currDeletedMetacard.getId() + DOCUMENT_ID_POSTFIX);
	
                Placemark placemark = KmlFactory.createPlacemark();
                doc.addToFeature(placemark);
                placemark.setTargetId(currDeletedMetacard.getId());
            }
        }
        // Handle updated events
        Queue<Metacard> changeQueue = delMethod.getUpdated();
	        LOGGER.debug("updated entries: " + changeQueue.size());	
        while (!changeQueue.isEmpty())
        {
            Metacard currChangedMetacard = changeQueue.poll();
            if (currChangedMetacard != null)
	            {	
                // Add Invocation URL and subscription id to arguments
	                arguments = new HashMap<String, Serializable>();
                try
                {
                    arguments.put(URL_KEY, createRestLinkToMetacard(uriInfo, currChangedMetacard.getId()));
                }
                catch (UnknownHostException e)
                {
	                    LOGGER.warn("Error creating URL to REST service, KML will not have link to Metacard.");
                }
                arguments.put(Constants.SUBSCRIPTION_KEY, subscriptionId);
	
                // create "Create" type and add to "Update" list
                Change change = KmlFactory.createChange();
                updateList.add(change);
	
                Document doc = kmlTransformer.transformEntry(null, currChangedMetacard, arguments);
                List<Feature> features = doc.getFeature();
                if (features != null && !features.isEmpty())
                {
                    Placemark placemark = (Placemark) features.get(0);
                    String id = placemark.getId();
                    placemark.setTargetId(id);
                    placemark.setId(null);
                    change.addToAbstractObject(placemark);
                }
            }
        }
	      } catch (CatalogTransformerException e) {
			LOGGER.warn("CatalogTransformerException thrown while generatingKML", e);
		}

        LOGGER.trace("EXITING: generateUpdateKml");
        return KmlFactory.createUpdate(targetHref, updateList);
    }
    
//  TODO break up handling events  private void handleUpdatedEvents(KmlUpdateDeliveryMethod delMethod, List<Object> updateList, UriInfo uriInfo, String subscriptionId  )
//    {
//    	HashMap<String, Serializable> arguments = new HashMap<String, Serializable>();
//        Queue<Metacard> changeQueue = delMethod.getUpdated();
//        LOGGER.debug("updated entries: " + changeQueue.size());	
//        try
//        {
//	        while (!changeQueue.isEmpty())
//	        {
//	            Metacard currChangedMetacard = changeQueue.poll();
//	            if (currChangedMetacard != null)
//	            {	
//	                // Add Invocation URL and subscription id to arguments
//	                arguments = new HashMap<String, Serializable>();
//	                try
//	                {
//	                    arguments.put("url", createRestUrl(uriInfo, currChangedMetacard.getId()));
//	                }
//	                catch (UnknownHostException e)
//	                {
//	                    LOGGER.warn("Error creating URL to REST service, KML will not have link to Metacard.");
//	                }
//	                arguments.put(Constants.SUBSCRIPTION_KEY, subscriptionId);
//	
//	                // create "Create" type and add to "Update" list
//	                Change change = KmlFactory.createChange();
//	                updateList.add(change);
//	
//	                Document doc = kmlTransformer.transformEntry(null, currChangedMetacard, arguments);
//	                List<Feature> features = doc.getFeature();
//	                if (features != null && !features.isEmpty())
//	                {
//	                    Placemark placemark = (Placemark) features.get(0);
//	                    String id = placemark.getId();
//	                    placemark.setTargetId(id);
//	                    placemark.setId(null);
//	                    change.addToAbstractObject(placemark);
//	                }
//	            }
//	        }
//        } catch (Exception e) {
//        	LOGGER.warn("Unable to handle update events", e);
//        }
//    }

    private String createRestLinkToMetacard( UriInfo uriInfo, String metacardId ) throws UnknownHostException
    {
    	LOGGER.trace("ENTERING: createRestLinkToMetacard");
        UriBuilder restUriBuilder = uriInfo.getAbsolutePathBuilder();
        
        restUriBuilder = generateDdfEndpointUrl(servicesContextRoot + FORWARD_SLASH + CATALOG_URL_PATH + FORWARD_SLASH + metacardId, restUriBuilder);
        
        LOGGER.trace("EXITING: createRestLinkToMetacard");
        return restUriBuilder.build().toString();
    }

	private UriBuilder generateDdfEndpointUrl(String path, UriBuilder uriBuilder) throws UnknownHostException {
		LOGGER.trace("ENTERING: generateDdfEndpointUrl");
		if(ddfHost != null && ddfPort != null && servicesContextRoot != null){
        	uriBuilder = uriBuilder.host(ddfHost);
        	
        	try {
        		int portInt = Integer.parseInt(ddfPort);
        		uriBuilder = uriBuilder.port(portInt);
        	}
        	catch (NumberFormatException nfe){
        		LOGGER.debug("Cannot convert the current DDF port: " + ddfPort + " to an integer.  Defaulting to port in invocation.");
        		throw new UnknownHostException("Unable to determine port DDF is using.");
        	}
        	
        	uriBuilder = uriBuilder.replacePath(path);
        }
        else {
        	LOGGER.debug("DDF Port is null, unable to determine host DDF is running on.");
        	throw new UnknownHostException("Unable to determine port DDF is using.");	
        }
		
		LOGGER.trace("EXITING: generateDdfEndpointUrl");
		return uriBuilder;
	}

    // private Placemark generatePlacemark( Metacard metacard )
    // {
    // // Generate a placemark
    // Placemark placemark = KmlFactory.createPlacemark();
    // placemark.setId(metacard.getId());
    // placemark.setVisibility(true);
    // placemark.setOpen(true);
    // placemark.setName(metacard.getTitles().iterator().next());
    //
    // // parse geoWkt from metacard
    // Collection<String> geoWktList = metacard.getGeospatialCoverageWKTs();
    // String geoWktString = geoWktList.iterator().next(); //TODO: This only
    // assumes one geo
    // if(geoWktString != null)
    // {
    // logger.debug("parsing metacards geospatial data");
    // try
    // {
    // com.vividsolutions.jts.geom.Geometry currMetacardGeo =
    // wktReader.read(geoWktString);
    // if (currMetacardGeo instanceof Point)
    // {
    // logger.debug("metcard contains point.");
    // Point currMetacardPoint = (Point) currMetacardGeo;
    // de.micromata.opengis.kml.v_2_2_0.Point kmlPoint =
    // KmlFactory.createPoint();
    // placemark.setGeometry(kmlPoint);
    //
    // List<de.micromata.opengis.kml.v_2_2_0.Coordinate> kmlPointCoords =
    // kmlPoint.getCoordinates();
    // de.micromata.opengis.kml.v_2_2_0.Coordinate kmlCoord =
    // KmlFactory.createCoordinate(
    // currMetacardPoint.getX(), currMetacardPoint.getY());
    // kmlPointCoords.add(kmlCoord);
    // }
    // else if (currMetacardGeo instanceof Polygon)
    // {
    // logger.debug("metcard contains polygon.");
    // Polygon currMetacardPoly = (Polygon) currMetacardGeo;
    //
    // de.micromata.opengis.kml.v_2_2_0.Polygon kmlPoly =
    // KmlFactory.createPolygon();
    // placemark.setGeometry(kmlPoly);
    // Boundary kmlPolyOuterBoundary = KmlFactory.createBoundary();
    // kmlPoly.setOuterBoundaryIs(kmlPolyOuterBoundary);
    //
    // LinearRing kmlPolyLinearRing = KmlFactory.createLinearRing();
    // kmlPolyOuterBoundary.setLinearRing(kmlPolyLinearRing);
    // List<de.micromata.opengis.kml.v_2_2_0.Coordinate> kmlLinearRingCoords =
    // kmlPolyLinearRing
    // .getCoordinates();
    //
    // // the wkt poly should repeat the first coordinate
    // Coordinate[] metacardPolyCoords = currMetacardPoly.getCoordinates();
    // for ( Coordinate currMetacardCoord : metacardPolyCoords )
    // {
    // de.micromata.opengis.kml.v_2_2_0.Coordinate kmlCoord =
    // KmlFactory.createCoordinate(currMetacardCoord.x, currMetacardCoord.y);
    // kmlLinearRingCoords.add(kmlCoord);
    // }
    // }
    // else
    // {
    // logger.warn("Unsupported geometry type: " +
    // currMetacardGeo.getClass().getName()
    // + ".  Geometry will be ignored.");
    // }
    //
    // // TODO: Implement linestring, multipoint, multilinestring,
    // // multipolygon
    // // TODO: handle holes in polygon
    // }
    // catch (com.vividsolutions.jts.io.ParseException e)
    // {
    // logger
    // .warn("Error parsing geometry in created metacard, returning NetworkLinkControl without geometry.");
    // }
    // }
    // return placemark;
    // }

    /**
     * Obtains subscription from OSGi registry by subscription id
     * 
     * @param subscriptionId
     * @return
     * @throws InvalidSyntaxException
     * @throws PluginExecutionException
     */
    private KmlSubscription getSubscription( String subscriptionId ) throws InvalidSyntaxException,
        PluginExecutionException
    {
        LOGGER.debug("obtaining subscription: " + subscriptionId);
        String filter = "(subscription-id=" + subscriptionId + ")";
        ServiceReference[] serviceRefs = this.context.getServiceReferences(Subscription.class.getName(), filter);
        if (serviceRefs == null)
        {
            LOGGER.error("Unable to obtain update data for this network link: no subscription found.");
            throw new PluginExecutionException(
                "Unable to obtain update data for this network link: no subscription found.");
        }
        
        Object service =  this.context.getService(serviceRefs[0]);
        
        return (KmlSubscription)service;
    }

    // public long getTimeoutMs()
    // {
    // return timeoutMs;
    // }
    //
    // public void setTimeoutMs( long timeoutMillis )
    // {
    // this.timeoutMs = timeoutMillis;
    // }

    private class SubscriptionTimeoutTracker implements Runnable
    {

        private String subscriptionId;
        private BundleContext context;

        public SubscriptionTimeoutTracker( BundleContext context, String subscriptionId )
        {
            this.context = context;
            this.subscriptionId = subscriptionId;
        }

        @Override
        public void run()
        {
            LOGGER.trace("ENTERING: SubscriptionTimeoutTracker");
            boolean keepRunning = true;
            try
            {
                while (keepRunning)
                {
                	Long subscriptionLastUpdated = lastUpdated.get(this.subscriptionId);
                	//lastUpdated should not return null
                	if(subscriptionLastUpdated == null)
                	{
                		subscriptionLastUpdated = Long.valueOf(0);
                	}
                    long deadline = subscriptionLastUpdated + TIMEOUT_MS;
                    long remainingTime = deadline - System.currentTimeMillis();
                    // logger.debug("timeout thread deadline: " + deadline);
                    // logger.debug("timeout thread time remaining: " +
                    // remainingTime);
                    Thread.sleep(remainingTime);
                    // logger.debug("subscription id: " + this.subscriptionId);
                    // logger.debug("timeout thread current time: " +
                    // System.currentTimeMillis());
                    // logger.debug("timeout thread last-updated: " +
                    // lastUpdated.get(this.subscriptionId));

                    if (System.currentTimeMillis() >= (TIMEOUT_MS + subscriptionLastUpdated))
                    {
                        LOGGER.info("KML Subscription timed out.  Removing subscription: " + this.subscriptionId);
                        keepRunning = false;
                        String filter = "(subscription-id=" + subscriptionId + ")";
                        try
                        {
                            ServiceReference[] serviceRefs = this.context.getServiceReferences(
                                Subscription.class.getName(), filter);
                            if (serviceRefs != null && serviceRefs.length > 0)
                            {
                                LOGGER.info("Found subscription, unregistering.");
                                ServiceRegistration subscriptionSvcReg = (ServiceRegistration) this.context
                                    .getService(serviceRefs[0]);
                                subscriptionSvcReg.unregister();
                            }
                            else
                            {
                                LOGGER.info("Could not find subscription, ending timeout.");
                            }

                        }
                        catch (InvalidSyntaxException e)
                        {
                            LOGGER.error("Error unregistering subscription.", e);
                        }

                    }
                }
                LOGGER.trace("EXITING: SubscriptionTimeoutTracker");
            }
            catch (InterruptedException e)
            {
                LOGGER.debug("Timeout interrupted, ending timeout.");
            }
        }
    }

	@Override
	public void ddfConfigurationUpdated(Map properties) {
		String methodName = "ddfConfigurationUpdated";
		LOGGER.debug( "ENTERING: " + methodName );
        
        if ( properties != null && !properties.isEmpty() )
        {
            Object value = properties.get( DdfConfigurationManager.HOST );
            if ( value != null )
            {
                this.ddfHost = value.toString();
                LOGGER.debug( "ddfHost = " + this.ddfHost );
            }
            else
            {
            	LOGGER.debug( "ddfHost = NULL" );
            }
            
            value = properties.get( DdfConfigurationManager.PORT );
            if ( value != null )
            {
                this.ddfPort = value.toString();
                LOGGER.debug( "ddfPort = " + this.ddfPort );
            }
            else
            {
            	LOGGER.debug( "ddfPort = NULL" );
            }
            
            value = properties.get( DdfConfigurationManager.SERVICES_CONTEXT_ROOT );
            if ( value != null )
            {
                this.servicesContextRoot = value.toString();
                LOGGER.debug( "servicesContextRoot = " + this.servicesContextRoot );
            }
            else
            {
            	LOGGER.debug( "servicesContextRoot = NULL" );
            }
        }
        else
        {
        	LOGGER.debug( "properties are NULL or empty" );
        }
        
        LOGGER.debug( "EXITING: " + methodName );
	}
}
