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
package ddf.catalog.source.opensearch;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.KeyStore;
import java.util.Date;
import java.util.Map;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.Subject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import ddf.catalog.data.Result;
import ddf.catalog.impl.filter.SpatialDistanceFilter;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.impl.filter.TemporalFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.source.UnsupportedQueryException;


/**
 * Utility helper class that performs much of the translation logic used in
 * CddaOpenSearchSite.
 * 
 */
public final class OpenSearchSiteUtil
{

    // OpenSearch defined parameters
    public static final String SEARCH_TERMS = "{searchTerms}";
    // temporal
    public static final String TIME_START = "{time:start?}";
    public static final String TIME_END = "{time:end?}";
    public static final String TIME_NAME = "{cat:dateName?}";
    // geospatial
    public static final String GEO_LAT = "{geo:lat?}";
    public static final String GEO_LON = "{geo:lon?}";
    public static final String GEO_RADIUS = "{geo:radius?}";
    public static final String GEO_POLY = "{geo:polygon?}";
    public static final String GEO_BBOX = "{geo:box?}";
    // general options
    public static final String SRC = "{fs:routeTo?}";
    public static final String MAX_RESULTS = "{fs:maxResults?}";
    public static final String COUNT = "{count?}";
    public static final String MAX_TIMEOUT = "{fs:maxTimeout?}";
    public static final String USER_DN = "{idn:userDN?}";
    public static final String SORT = "{fsa:sort?}";
    public static final String FILTER = "{fsa:filter?}";
    // only for async searches
    public static final String START_INDEX = "{startIndex?}";
    public static final String START_PAGE = "{startPage?}";

    // xpath operations
    public static final String XPATH_TITLE = "/ddms:Resource/ddms:title";
    public static final String XPATH_ID = "/ddms:Resource/ddms:identifier[@ddms:qualifier='http://metadata.dod.mil/mdr/ns/MDR/0.1/MDR.owl#URI']/@ddms:value";
    public static final String XPATH_DATE = "/ddms:Resource/ddms:dates/@ddms:posted";

    // geospatial constants
    public static final double LAT_DEGREE_M = 111325;
    public static final Integer DEFAULT_TOTAL_MAX = 1000;
    public static final Integer MAX_LAT = 90;
    public static final Integer MIN_LAT = -90;
    public static final Integer MAX_LON = 180;
    public static final Integer MIN_LON = -180;
    public static final Integer MAX_ROTATION = 360;
    public static final Integer MAX_BBOX_POINTS = 4;

    public static final String ORDER_ASCENDING = "asc";
    public static final String ORDER_DESCENDING = "desc";
    public static final String SORT_DELIMITER = ":";
    public static final String SORT_RELEVANCE = "relevance";
    public static final String SORT_TEMPORAL = "date";

    private static Logger logger = Logger.getLogger(OpenSearchSiteUtil.class);

    private OpenSearchSiteUtil()
    {

    }

    /**
     * Populates general site information.
     * 
     * @param url Initial StringBuilder url that is not filled in.
     * @param query
     * @param user
     * @return A string builder object that contains the filled-in URL (in
     *         string format) that should be called.
     */
    public static StringBuilder populateSearchOptions( StringBuilder url, Query query, Subject user )
    {
        String maxTotalSize = null;
        String maxPerPage = null;
        String routeTo = "";
        String timeout = null;
        String start = "1";
        String dn = null;
        String filterStr = "";
        String sortStr = null;

        if (query != null)
        {

            maxPerPage = String.valueOf(query.getPageSize());
            if (query.getPageSize() > DEFAULT_TOTAL_MAX)
            {
                maxTotalSize = maxPerPage;
            }
            else if (query.getPageSize() <= 0)
            {
                maxTotalSize = String.valueOf(DEFAULT_TOTAL_MAX);
            }

            start = Integer.toString(query.getStartIndex());
            
            timeout = Long.toString(query.getTimeoutMillis());

            sortStr = translateToOpenSearchSort(query.getSortBy());

            if (user != null && !user.getPrincipals().isEmpty())
            {
                try
                {
                    dn = URLEncoder.encode(user.getPrincipals().iterator().next().getName(), "UTF-8");
                }
                catch (UnsupportedEncodingException e)
                {
                    logger.warn("Bad Encoding, ignoring user distinguished name");
                }
            }
        }
        checkAndReplace(url, start, START_INDEX);
        checkAndReplace(url, maxPerPage, COUNT);
        checkAndReplace(url, maxTotalSize, MAX_RESULTS);
        checkAndReplace(url, routeTo, SRC);
        checkAndReplace(url, timeout, MAX_TIMEOUT);
        checkAndReplace(url, dn, USER_DN);
        checkAndReplace(url, filterStr, FILTER);
        checkAndReplace(url, sortStr, SORT);

        return url;
    }

    private static String translateToOpenSearchSort( SortBy ddfSort )
    {
        String openSearchSortStr = null;
        String orderType = null;
        
        if (ddfSort == null || ddfSort.getSortOrder() == null) {
            return openSearchSortStr;
        }

        if (ddfSort.getSortOrder().equals(SortOrder.ASCENDING))
        {
            orderType = ORDER_ASCENDING;
        }
        else
        {
            orderType = ORDER_DESCENDING;
        }

        // QualifiedString type = ddfSort.getType();
        PropertyName sortByField = ddfSort.getPropertyName();

        if (Result.RELEVANCE.equals(sortByField.getPropertyName()))
        {
            // asc relevance not supported by spec
            openSearchSortStr = SORT_RELEVANCE + SORT_DELIMITER + ORDER_DESCENDING;
        }
        else if (Result.TEMPORAL.equals(sortByField.getPropertyName()))
        {
            openSearchSortStr = SORT_TEMPORAL + SORT_DELIMITER + orderType;
        }
        else
        {
            logger.warn("Couldn't determine sort policy, not adding sorting in request to federated site.");
        }

        return openSearchSortStr;
    }

    /**
     * Fills in the OpenSearch query URL with contextual information (Note:
     * Section 2.2 - Query: The OpenSearch specification does not define a
     * syntax for its primary query parameter, searchTerms, but it is generally
     * used to support simple keyword queries.)
     * 
     * @param url
     * @param searchPhrase
     * @return
     */
    public static StringBuilder populateContextual( final StringBuilder url, final String searchPhrase )
    {
        String queryStr = searchPhrase;
        if (queryStr != null)
        {
            try
            {
                queryStr = URLEncoder.encode(queryStr, "UTF-8");
            }
            catch (UnsupportedEncodingException uee)
            {
                logger.warn("Could not encode contextual string: " + uee.getMessage());
            }
        }

        checkAndReplace(url, queryStr, SEARCH_TERMS);

        return url;
    }

    /**
     * Fills in the opensearch query URL with temporal information (Start, End,
     * and Name). Currently name is empty due to incompatibility with endpoints.
     * 
     * @param url OpenSearch URL to populate
     * @param temporal TemporalCriteria that contains temporal data
     * @return The url object that was passed in with the temporal information
     *         added.
     */
    public static StringBuilder populateTemporal( StringBuilder url, TemporalFilter temporal )
    {
        DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
        String start = "";
        String end = "";
        String name = "";
        if (temporal != null)
        {
            long startLng = (temporal.getStartDate() != null) ? temporal.getStartDate().getTime() : 0;
            start = fmt.print(startLng);
            long endLng = (temporal.getEndDate() != null) ? temporal.getEndDate().getTime() : System
                .currentTimeMillis();
            end = fmt.print(endLng);
        }
        checkAndReplace(url, start, TIME_START);
        checkAndReplace(url, end, TIME_END);
        checkAndReplace(url, name, TIME_NAME);

        return url;
    }

    /**
     * Fills in the OpenSearch query URL with geospatial information (poly, lat,
     * lon, and radius).
     * 
     * @param url OpenSearch URL to populate
     * @param spatial SpatialCriteria that contains the spatial data
     * @return The url object that was passed in with the geospatial information
     *         added.
     */
    public static StringBuilder populateGeospatial( StringBuilder url, SpatialDistanceFilter spatial,
        boolean shouldConvertToBBox ) throws UnsupportedQueryException
    {
        String lat = "";
        String lon = "";
        String radiusStr = "";
        StringBuilder bbox = new StringBuilder("");
        StringBuilder poly = new StringBuilder("");

        if (spatial != null)
        {
            String wktStr = spatial.getGeometryWkt();
            double radius = spatial.getDistanceInMeters();

            if (wktStr.indexOf("POINT") != -1)
            {
                String[] latLon = createLatLonAryFromWKT(wktStr);
                lon = latLon[0];
                lat = latLon[1];
                radiusStr = Double.toString(radius);
                if (shouldConvertToBBox)
                {
                    double[] bboxCoords = createBBoxFromPointRadius(Double.parseDouble(lon), Double.parseDouble(lat),
                        radius);
                    for ( int i = 0; i < MAX_BBOX_POINTS; i++ )
                    {
                        if (i > 0)
                        {
                            bbox.append(",");
                        }
                        bbox.append(bboxCoords[i]);
                    }
                    lon = "";
                    lat = "";
                    radiusStr = "";
                }
            }
            else
            {
                logger.warn("WKT (" + wktStr + ") not supported for POINT-RADIUS search, use POINT.");
            }
        }

        checkAndReplace(url, lat, GEO_LAT);
        checkAndReplace(url, lon, GEO_LON);
        checkAndReplace(url, radiusStr, GEO_RADIUS);
        checkAndReplace(url, poly.toString(), GEO_POLY);
        checkAndReplace(url, bbox.toString(), GEO_BBOX);

        return url;
    }

    /**
     * Fills in the OpenSearch query URL with geospatial information (poly, lat,
     * lon, and radius).
     * 
     * @param url OpenSearch URL to populate
     * @param spatial SpatialCriteria that contains the spatial data
     * @return The url object that was passed in with the geospatial information
     *         added.
     */
    public static StringBuilder populateGeospatial( StringBuilder url, SpatialFilter spatial,
        boolean shouldConvertToBBox ) throws UnsupportedQueryException
    {
        String lat = "";
        String lon = "";
        String radiusStr = "";
        StringBuilder bbox = new StringBuilder("");
        StringBuilder poly = new StringBuilder("");

        if (spatial != null)
        {
            String wktStr = spatial.getGeometryWkt();
            if (wktStr.indexOf("POLYGON") != -1)
            {
                String[] polyAry = createPolyAryFromWKT(wktStr);
                if (shouldConvertToBBox)
                {
                    double[] bboxCoords = createBBoxFromPolygon(polyAry);
                    for ( int i = 0; i < MAX_BBOX_POINTS; i++ )
                    {
                        if (i > 0)
                        {
                            bbox.append(",");
                        }
                        bbox.append(bboxCoords[i]);
                    }
                }
                else
                {
                    for ( int i = 0; i < polyAry.length - 1; i += 2 )
                    {
                        if (i != 0)
                        {
                            poly.append(",");
                        }
                        poly.append(polyAry[i + 1] + "," + polyAry[i]);
                    }
                }
            }
            else
            {
                logger.warn("WKT (" + wktStr + ") not supported for SPATIAL search, use POLYGON.");
            }
        }

        checkAndReplace(url, lat, GEO_LAT);
        checkAndReplace(url, lon, GEO_LON);
        checkAndReplace(url, radiusStr, GEO_RADIUS);
        checkAndReplace(url, poly.toString(), GEO_POLY);
        checkAndReplace(url, bbox.toString(), GEO_BBOX);

        return url;
    }

    /**
     * Parses a WKT polygon string and returns a string array containing the lon
     * and lat.
     * 
     * @param wkt WKT String in the form of POLYGON((Lon Lat, Lon Lat...))
     * @return Lon on even # and Lat on odd #
     */
    public static String[] createPolyAryFromWKT( String wkt )
    {
        String lonLat = wkt.substring(wkt.indexOf("((") + 2, wkt.indexOf("))"));
        return lonLat.split(" |,\\p{Space}?");
    }

    /**
     * Parses a WKT Point string and returns a string array containing the lon
     * and lat.
     * 
     * @param wkt WKT String in the form of POINT( Lon Lat)
     * @return Lon at position 0, Lat at position 1
     */
    public static String[] createLatLonAryFromWKT( String wkt )
    {
        String lonLat = wkt.substring(wkt.indexOf('(') + 1, wkt.indexOf(')'));
        return lonLat.split(" ");
    }

    /**
     * Checks the input and replaces the items inside of the url.
     * 
     * @param url The URL to doing the replacement on. <b>NOTE:</b> replacement
     *            is done directly on this object.
     * @param inputStr Item to put into the URL.
     * @param definition Area inside of the URL to be replaced by.
     * @return the input url object with the items replaced.
     */
    private static StringBuilder checkAndReplace( StringBuilder url, String inputStr, String definition )
    {
        int start = url.indexOf(definition);
        if (start == -1)
        {
            logger.debug("Cannot find " + definition + " in OpenSearch Description Document, ignoring.");
        }
        else
        {
            String replacementStr = "";
            if (inputStr != null)
            {
                replacementStr = inputStr;
            }
            url.replace(start, start + definition.length(), replacementStr);
        }

        return url;
    }

    /**
     * Takes in an atom document and translates it to a DDMS document.
     * 
     * @param tf TransformerFactory - passed in to prevent multiple instances
     *            from being created
     * @param xmlDoc atom document
     * @param xsltDoc xslt document that performs the atom->ddms translation
     * @param classification default classification
     * @param ownerProducer default ownerProducer
     * @return new DDMS document.
     * @throws UnsupportedQueryException
     */
    public static Document normalizeAtomToDDMS( TransformerFactory tf, Document xmlDoc, Document xsltDoc,
        Map<String, String> classificationProperties ) throws ConversionException
    {
        ByteArrayOutputStream baos = null;
        ByteArrayInputStream bais = null;
        try
        {
            Transformer transformer = tf.newTransformer(new DOMSource(xsltDoc));
            StreamResult resultOutput = null;
            Source source = new DOMSource(xmlDoc);
            baos = new ByteArrayOutputStream();
            resultOutput = new StreamResult(baos);
            // add the arguments from the caller

            // logger.debug("classification properties: " +
            // classificationProperties);
            if (classificationProperties != null && !classificationProperties.isEmpty())
            {
                for ( Map.Entry<String, String> entry : classificationProperties.entrySet() )
                {
                    // logger.debug("parameter key: " + entry.getKey() +
                    // " value: " + entry.getValue());
                    String key = entry.getKey();
                    String value = entry.getValue();
                    if (key != null && !key.isEmpty() && value != null && !value.isEmpty())
                    {
                        transformer.setParameter(key, value);
                    }
                    else
                    {
                        logger.warn("Null or empty value for parameter: " + entry.getKey()
                                + ". Value will be set to U, USA.  Moving to next parameter.");
                    }
                }
            }
            else
            {
                logger.warn("All properties were null.  Using \"last-resort\" defaults: U, USA");
                transformer.setParameter("applyDefaultSecurity", Boolean.TRUE);
            }
            transformer.transform(source, resultOutput);
            bais = new ByteArrayInputStream(baos.toByteArray());
            return convertStreamToDocument(bais);
        }
        catch (TransformerConfigurationException tce)
        {
            throw new ConversionException(
                "Error while setting up transformation from atom document to ddms, could not configure transformer: "
                        + tce.getMessage(), tce);
        }
        catch (TransformerException te)
        {
            throw new ConversionException(
                "Error while normalizing atom document to ddms, could not configure transform: " + te.getMessage(), te);
        }
        finally
        {
            IOUtils.closeQuietly(bais);
            IOUtils.closeQuietly(baos);
        }
    }

    /**
     * This method converts an inputstream into a document.
     * 
     * @param input
     * @return
     * @throws ConversionException
     */
    public static Document convertStreamToDocument( InputStream input ) throws ConversionException
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(input);
        }
        catch (SAXException se)
        {
            throw new ConversionException("Error parsing response from server.", se);
        }
        catch (ParserConfigurationException pce)
        {
            throw new ConversionException("Error configuring parser for response from server.", pce);
        }
        catch (IOException ioe)
        {
            throw new ConversionException("Error parsing response from server.", ioe);
        }
    }

    /**
     * Parses the datetime string out of ddms-formatted document and returns a
     * date object.
     * 
     * @param date
     * @return
     */
    public static Date parseDate( String date )
    {
        Date returnDate = null;
        if (date != null && !date.isEmpty())
        {
            try
            {
                DateTimeFormatter dateFormatter = ISODateTimeFormat.date();
                returnDate = dateFormatter.parseDateTime(date).toDate();
            }
            catch (IllegalArgumentException iae)
            {
                logger
                    .warn("Could not parse out updated date in response, date will not be passed back from federated site.");
            }
        }
        return returnDate;
    }

    /**
     * A little gotcha in the normalization of the messages is that it's
     * not a guarantee that the DDMS returned will be correctly populated. To
     * mitigate this for users, the required atom data is added as attributes to
     * the DDMS. This operation pops these attributes out of the DDMS as they
     * are used.
     * 
     * @param ddmsNode Node to take attributes out of
     * @param attributeName Name of attribute
     * @return string representation of the attribute value
     */
    public static String popAttribute( Node ddmsNode, String attributeName )
    {
        String attribute = "";
        Node attributeNode;
        NamedNodeMap osAttributes = ddmsNode.getAttributes();
        attributeNode = osAttributes.getNamedItem(attributeName);
        if (attributeNode != null)
        {
            attribute = attributeNode.getNodeValue();
            osAttributes.removeNamedItem(attributeName);
        }
        return attribute;
    }

    /**
     * Takes in a point radius search and converts it to a (rough approximation)
     * bounding box.
     * 
     * @param lon latitude in decimal degrees (WGS-84)
     * @param lat longitude in decimal degrees (WGS-84)
     * @param radius radius, in meters
     * @return Array of bounding box coordinates in the following order: West
     *         South East North. Also described as minX, minY, maxX, maxY (where
     *         longitude is the X-axis, and latitude is the Y-axis).
     */
    public static double[] createBBoxFromPointRadius( double lon, double lat, double radius )
    {
        double minX;
        double minY;
        double maxX;
        double maxY;

        double lonDifference = radius / (LAT_DEGREE_M * Math.cos(lat));
        double latDifference = radius / LAT_DEGREE_M;
        minX = lon - lonDifference;
        if (minX < MIN_LON)
        {
            minX += MAX_ROTATION;
        }
        maxX = lon + lonDifference;
        if (maxX > MAX_LON)
        {
            maxX -= MAX_ROTATION;
        }
        minY = lat - latDifference;
        if (minY < MIN_LAT)
        {
            minY = Math.abs(minY + MAX_LAT) - MAX_LAT;
        }
        maxY = lat + latDifference;
        if (maxY > MAX_LAT)
        {
            maxY = MAX_LAT - (maxY - MAX_LAT);
        }

        return new double[]
        {
                minX, minY, maxX, maxY
        };
    }

    /**
     * Takes in an array of coordinates and converts it to a (rough
     * approximation) bounding box.
     * 
     * Note: Searches being performed where the polygon goes through the
     * international date line may return a bad bounding box.
     * 
     * @param polyAry array of coordinates (lon,lat,lon,lat,lon,lat..etc)
     * @return Array of bounding box coordinates in the following order: West
     *         South East North. Also described as minX, minY, maxX, maxY (where
     *         longitude is the X-axis, and latitude is the Y-axis).
     */
    public static double[] createBBoxFromPolygon( String[] polyAry )
    {
        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;

        double curX, curY;
        for ( int i = 0; i < polyAry.length - 1; i += 2 )
        {
            logger.debug("polyToBBox: lon - " + polyAry[i] + " lat - " + polyAry[i + 1]);
            curX = Double.parseDouble(polyAry[i]);
            curY = Double.parseDouble(polyAry[i + 1]);
            if (curX < minX)
            {
                minX = curX;
            }
            if (curX > maxX)
            {
                maxX = curX;
            }
            if (curY < minY)
            {
                minY = curY;
            }
            if (curY > maxY)
            {
                maxY = curY;
            }
        }
        return new double[]
        {
                minX, minY, maxX, maxY
        };
    }

}
