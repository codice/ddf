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
package ddf.catalog.source.solr;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrQuery.ORDER;
import org.apache.solr.schema.DateField;
import org.joda.time.DateTime;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.parboiled.Parboiled;
import org.parboiled.parserunners.BasicParseRunner;
import org.parboiled.support.ParsingResult;

import com.spatial4j.core.distance.DistanceUtils;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTWriter;

import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.filter.FilterDelegate;
import ddf.catalog.source.solr.textpath.SimplePathNode;
import ddf.catalog.source.solr.textpath.SimplePathParser;
import ddf.measure.Distance;
import ddf.measure.Distance.LinearUnit;

/**
 * Translates filter-proxy calls into Solr query syntax.
 * 
 */
public class SolrFilterDelegate extends FilterDelegate<SolrQuery> {

    private static final String SCORE_DISTANCE = "{! score=distance}";

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    // *, ?, and / are escaped by the filter adapter
    private static final String[] LUCENE_SPECIAL_CHARACTERS = new String[] {"+", "-", "&&", "||",
        "!", "(", ")", "{", "}", "[", "]", "^", "\"", "~", ":"};

    private static final String[] ESCAPED_LUCENE_SPECIAL_CHARACTERS = new String[] {"\\+", "\\-",
        "\\&&", "\\||", "\\!", "\\(", "\\)", "\\{", "\\}", "\\[", "\\]", "\\^", "\\\"", "\\~",
        "\\:"};

    private static final String INTERSECTS_OPERATION = "Intersects";

    private static final String LUCENE_SPATIAL_INDEX = "_geohash_index";

    private static final String JTS_SPATIAL_INDEX = "_geo_index";

    private static final double NEAREST_NEIGHBOR_DISTANCE_LIMIT = metersToDegrees(new Distance(
            1000, LinearUnit.NAUTICAL_MILE).getAs(LinearUnit.METER));

    // Using quantization of 12 to reduce error below 1%
    private static final int QUADRANT_SEGMENTS = 12;

    private static final Logger LOGGER = Logger.getLogger(SolrFilterDelegate.class);

    private static final WKTWriter WKT_WRITER = new WKTWriter();

    private static final String END_PAREN = " ) ";

    private static final String START_PAREN = " ( ";

    private static final String OR = " OR ";

    private static final String AND = " AND ";

    private static final String TO = " TO ";

    private static final String QUOTE = "\"";

    private static DateField dateFormatter = new DateField();

    private static final String SOLR_WILDCARD_CHAR = "*";

    private static final String SOLR_SINGLE_WILDCARD_CHAR = "?";

    public static final Map<String, String> FIELD_MAP;

    private static final String ANY_TEXT_FIELD = "any_text";

    private static final double DEFAULT_ERROR_IN_METERS = 1;

    private static final double DEFAULT_ERROR_IN_DEGREES = metersToDegrees(DEFAULT_ERROR_IN_METERS);

    static {
        Map<String, String> tempMap = new HashMap<String, String>();
        tempMap.put(Metacard.ANY_TEXT, ANY_TEXT_FIELD);
        tempMap.put(Metacard.ANY_GEO, Metacard.GEOGRAPHY + JTS_SPATIAL_INDEX);
        FIELD_MAP = Collections.unmodifiableMap(tempMap);
    }

    private DynamicSchemaResolver resolver;

    private SortBy sortBy;

    public SolrFilterDelegate(DynamicSchemaResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public SolrQuery and(List<SolrQuery> operands) {
        return logicalOperator(operands, AND);
    }

    @Override
    public SolrQuery or(List<SolrQuery> operands) {
        return logicalOperator(operands, OR);
    }

    @Override
    public SolrQuery not(SolrQuery operand) {
        return new SolrQuery(" NOT " + operand.getQuery());
    }

    @Override
    public SolrQuery propertyIsFuzzy(String propertyName, String searchPhrase) {
        String mappedPropertyName = getMappedPropertyName(propertyName, AttributeFormat.STRING,
                false);

        StringBuilder phraseBuilder = new StringBuilder();
        for (String term : StringUtils.split(searchPhrase)) {
            // On fuzzy searches, no text analysis is performed on the search phrase. Expect fuzzy
            // terms to be
            // case insensitive.
            phraseBuilder.append("+").append(mappedPropertyName).append(":")
                    .append(term.toLowerCase()).append("~ ");
        }

        return new SolrQuery(phraseBuilder.toString());
    }

    @Override
    public SolrQuery propertyIsEqualTo(String propertyName, String literal, boolean isCaseSensitive) {
        if (!isCaseSensitive) {
            throw new UnsupportedOperationException(
                    "Case insensitive exact searches are not supported.");
        }
        verifyInputData(propertyName, literal);

        String mappedPropertyName = getMappedPropertyName(propertyName, AttributeFormat.STRING,
                true);
        return new SolrQuery(mappedPropertyName + ":" + QUOTE + escapeSpecialCharacters(literal)
                + QUOTE);
    }

    @Override
    public SolrQuery propertyIsEqualTo(String propertyName, Date exactDate) {
        String mappedPropertyName = getMappedPropertyName(propertyName, AttributeFormat.DATE, true);

        SolrQuery query = new SolrQuery();
        query.setQuery(" " + mappedPropertyName + ":" + QUOTE + dateFormatter.toExternal(exactDate)
                + QUOTE);

        return query;
    }

    @Override
    public SolrQuery propertyIsLike(String propertyName, String pattern, boolean isCaseSensitive) {
        verifyInputData(propertyName, pattern);

        String mappedPropertyName = getMappedPropertyName(propertyName, AttributeFormat.STRING,
                false);
        if (isCaseSensitive) {
            mappedPropertyName = resolver.getCaseSensitiveField(mappedPropertyName);
        }

        String searchPhrase = escapeSpecialCharacters(pattern);
        if (!searchPhrase.contains(SOLR_WILDCARD_CHAR)
                && !searchPhrase.contains(SOLR_SINGLE_WILDCARD_CHAR)) {
            // Not an exact phrase
            searchPhrase = QUOTE + searchPhrase + QUOTE;
        } else {
            searchPhrase = "(" + searchPhrase + ")";
        }

        return new SolrQuery(mappedPropertyName + ":" + searchPhrase);
    }

    @Override
    public SolrQuery propertyIsGreaterThan(String propertyName, int literal) {
        return getGreaterThanQuery(propertyName, AttributeFormat.INTEGER, literal);
    }

    @Override
    public SolrQuery propertyIsGreaterThan(String propertyName, short literal) {
        return getGreaterThanQuery(propertyName, AttributeFormat.SHORT, literal);
    }

    @Override
    public SolrQuery propertyIsGreaterThan(String propertyName, long literal) {
        return getGreaterThanQuery(propertyName, AttributeFormat.LONG, literal);
    }

    @Override
    public SolrQuery propertyIsGreaterThan(String propertyName, float literal) {
        return getGreaterThanQuery(propertyName, AttributeFormat.FLOAT, literal);
    }

    @Override
    public SolrQuery propertyIsGreaterThan(String propertyName, double literal) {
        return getGreaterThanQuery(propertyName, AttributeFormat.DOUBLE, literal);
    }

    @Override
    public SolrQuery propertyIsGreaterThanOrEqualTo(String propertyName, short literal) {
        return getGreaterThanOrEqualToQuery(propertyName, AttributeFormat.SHORT, literal);
    }

    @Override
    public SolrQuery propertyIsGreaterThanOrEqualTo(String propertyName, int literal) {
        return getGreaterThanOrEqualToQuery(propertyName, AttributeFormat.INTEGER, literal);
    }

    @Override
    public SolrQuery propertyIsGreaterThanOrEqualTo(String propertyName, long literal) {
        return getGreaterThanOrEqualToQuery(propertyName, AttributeFormat.LONG, literal);
    }

    @Override
    public SolrQuery propertyIsGreaterThanOrEqualTo(String propertyName, float literal) {
        return getGreaterThanOrEqualToQuery(propertyName, AttributeFormat.FLOAT, literal);
    }

    @Override
    public SolrQuery propertyIsGreaterThanOrEqualTo(String propertyName, double literal) {
        return getGreaterThanOrEqualToQuery(propertyName, AttributeFormat.DOUBLE, literal);
    }

    public SolrQuery during(String propertyName, Date startDate, Date endDate) {
        String formattedStartDate = dateFormatter.toExternal(startDate);
        String formattedEndDate = dateFormatter.toExternal(endDate);

        SolrQuery query = new SolrQuery();
        query.setQuery(" " + getMappedPropertyName(propertyName, AttributeFormat.DATE, false)
                + ":[ " + formattedStartDate + TO + formattedEndDate + " ] ");

        return query;
    }

    public SolrQuery before(String propertyName, Date date) {
        String formattedEndDate = dateFormatter.toExternal(date);

        SolrQuery query = new SolrQuery();
        query.setQuery(" " + getMappedPropertyName(propertyName, AttributeFormat.DATE, false)
                + ":[ *" + TO + formattedEndDate + " ] ");

        return query;
    }

    @Override
    public SolrQuery relative(String propertyName, long duration) {
        DateTime now = new DateTime();
        Date start = now.minus(duration).toDate();
        Date end = now.toDate();

        String formattedStartDate = dateFormatter.toExternal(start);
        String formattedEndDate = dateFormatter.toExternal(end);

        SolrQuery query = new SolrQuery();
        query.setQuery(" " + getMappedPropertyName(propertyName, AttributeFormat.DATE, false)
                + ":[ " + formattedStartDate + TO + formattedEndDate + " ] ");

        return query;
    }

    @Override
    public SolrQuery nearestNeighbor(String propertyName, String wkt) {

        Geometry geo = WktNormalizer.readWkt(wkt);

        if (geo != null) {
            Point pnt = null;
            if (isPoint(geo)) {
                pnt = (Point) geo;
            } else {
                pnt = geo.getCentroid();
            }

            String nearestNeighborQuery = geoPointToCircleQuery(propertyName,
                    NEAREST_NEIGHBOR_DISTANCE_LIMIT, pnt);

            return getSolrQueryWithSort(sortBy, nearestNeighborQuery);

        } else {
            throw new UnsupportedOperationException("Unable to read given WKT: " + wkt);
        }
    }

    private String geoPointToCircleQuery(String propertyName, double distanceInDegrees, Point pnt) {
        String circle = "Circle(" + pnt.getX() + " " + pnt.getY() + " d=" + distanceInDegrees + ")";
        String pointRadiusQuery = getLuceneGeometryIndex(propertyName) + ":\""
                + INTERSECTS_OPERATION + "(" + circle + ")\"";

        return pointRadiusQuery;
    }

    @Override
    public SolrQuery contains(String propertyName, String wkt) {
        return operationToQuery("Contains", propertyName, wkt);
    }

    @Override
    public SolrQuery dwithin(String propertyName, String wkt, double distance) {
        Geometry geo = WktNormalizer.readWkt(wkt);

        if (geo != null) {
            double distanceInDegrees = metersToDegrees(distance);
            if (isPoint(geo)) {
                Point pnt = (Point) geo;
                String pointRadiusQuery = geoPointToCircleQuery(propertyName, distanceInDegrees,
                        pnt);

                return getSolrQueryWithSort(sortBy, pointRadiusQuery);
            } else {
                Geometry bufferGeo = geo.buffer(distanceInDegrees, QUADRANT_SEGMENTS);
                String bufferWkt = WKT_WRITER.write(bufferGeo);
                return operationToQuery(INTERSECTS_OPERATION, propertyName, bufferWkt);
            }
        } else {
            throw new UnsupportedOperationException("Unable to read given WKT: " + wkt);
        }
    }

    private String getLuceneGeometryIndex(String propertyName) {
        String geoIndex = getMappedPropertyName(propertyName, AttributeFormat.GEOMETRY, false);
        if (geoIndex != null) {
            return geoIndex.replaceFirst(JTS_SPATIAL_INDEX, LUCENE_SPATIAL_INDEX);
        } else {
            throw new UnsupportedOperationException("propertyName should not be null.");
        }
    }

    private static double metersToDegrees(double distance) {
        return DistanceUtils.dist2Degrees(
                (new Distance(distance, LinearUnit.METER).getAs(LinearUnit.KILOMETER)),
                DistanceUtils.EARTH_MEAN_RADIUS_KM);
    }

    @Override
    public SolrQuery intersects(String propertyName, String wkt) {
        String geoIndexName = getLuceneGeometryIndex(propertyName);

        // Bug in spatial4j that does not properly find intersection between
        // points. Therefore, converting to a point-radius to account for the
        // error.
        if (StringUtils.isNotBlank(wkt) && wkt.toUpperCase().contains("POINT")) {
            Geometry geo = WktNormalizer.readWkt(wkt);

            if (geo != null) {

                if (isPoint(geo)) {
                    Point pnt = (Point) geo;
                    String pointRadiusQuery = geoPointToCircleQuery(propertyName,
                            DEFAULT_ERROR_IN_DEGREES, pnt);

                    return getSolrQueryWithSort(sortBy, pointRadiusQuery);
                }
                if (MultiPoint.class.getSimpleName().equals(geo.getGeometryType())
                        && geo.getCoordinates().length == 1) {
                    Point pnt = GEOMETRY_FACTORY.createPoint(geo.getCoordinate());
                    String pointRadiusQuery = geoPointToCircleQuery(propertyName,
                            DEFAULT_ERROR_IN_DEGREES, pnt);

                    return getSolrQueryWithSort(sortBy, pointRadiusQuery);
                }
            }
        }

        return operationOnIndexToQuery(INTERSECTS_OPERATION, geoIndexName, wkt);
    }

    @Override
    public SolrQuery within(String propertyName, String wkt) {
        return operationToQuery("IsWithin", propertyName, wkt);
    }

    @Override
    public SolrQuery disjoint(String propertyName, String wkt) {
        return operationToQuery("IsDisjointTo", propertyName, wkt);
    }

    @Override
    public SolrQuery overlaps(String propertyName, String wkt) {
        return operationToQuery("Overlaps", propertyName, wkt);
    }

    @Override
    public SolrQuery xpathExists(String xpath) {
    	
        return getTextPathQuery(xpath, null);
    }

    @Override
    public SolrQuery xpathIsLike(String xpath, String pattern, boolean isCaseSensitive) {
        if (isCaseSensitive) {
            throw new UnsupportedOperationException(
                    "Case sensitive TextPath is not currently supported.");
        }
        return getTextPathQuery(xpath, pattern);
    }

    @Override
    public SolrQuery xpathIsFuzzy(String xpath, String literal) {
        // TODO: make a fuzzy search instead of doing a best effort exact search
        return getTextPathQuery(xpath, literal);
    }

    private SolrQuery getSolrQueryWithSort(SortBy sortBy, String givenSpatialString) {

        if (sortBy != null && sortBy.getPropertyName() != null
                && Result.DISTANCE.equals(sortBy.getPropertyName().getPropertyName())) {

            String spatialQueryWithDistance = SCORE_DISTANCE + givenSpatialString;

            SolrQuery solrQuery = new SolrQuery(spatialQueryWithDistance);

            solrQuery.setFields("*", "score");

            ORDER sortOrder = ORDER.asc;

            if (SortOrder.DESCENDING.equals(sortBy.getSortOrder())) {
                sortOrder = ORDER.desc;
            }

            solrQuery.setSortField("score", sortOrder);

            return new SolrQuery(spatialQueryWithDistance);

        } else {
            return new SolrQuery(givenSpatialString);
        }
    }

    private SolrQuery getTextPathQuery(String textPath, String searchPhrase) {
    	
        if(ConfigurationStore.getInstance().isDisableTextPath()) {
        	throw new UnsupportedOperationException("TextPath support has been disabled.");
        }
        SimplePathParser parser = Parboiled.createParser(SimplePathParser.class);
        ParsingResult<SimplePathNode> result = new BasicParseRunner<SimplePathNode>(
                parser.TextPath()).run(textPath);

        if (!result.parseErrors.isEmpty() || result.resultValue == null) {
            throw new UnsupportedOperationException("Unable to parse given TextPath.");
        }

        String query = getMappedPropertyName(Metacard.METADATA, AttributeFormat.XML, false) + ":";

        if (searchPhrase != null) {
            query += result.resultValue.getValue(searchPhrase);
        } else {
            query += result.resultValue.getValue();
        }

        return new SolrQuery(query);
    }

    private SolrQuery getGreaterThanOrEqualToQuery(String propertyName, AttributeFormat format,
            Number literal) {
        String mappedPropertyName = getMappedPropertyName(propertyName, format, true);

        SolrQuery query = new SolrQuery();
        query.setQuery(" " + mappedPropertyName + ":[ " + literal.toString() + TO + "* ] ");

        return query;
    }

    private SolrQuery getGreaterThanQuery(String propertyName, AttributeFormat format,
            Number literal) {
        String mappedPropertyName = getMappedPropertyName(propertyName, format, true);

        SolrQuery query = new SolrQuery();
        query.setQuery(" " + mappedPropertyName + ":{ " + literal.toString() + TO + "* ] ");

        return query;
    }

    // @Override
    // public SolrQuery beyond(String propertyName, String wkt, double distance)
    // {
    // return pointRadiusToQuerySolrQuery("IsDisjointTo", propertyName, wkt,
    // distance);
    // }

    /*
     * If we eventually support the OGC Equals operation, we will need the IsEqualTo Spatial4j
     * string
     */
    // @Override
    // public Object visit(Equals filter, Object data) {
    // return operationToQuery("IsEqualTo",filter,data);
    // }

    private String getMappedPropertyName(String propertyName, AttributeFormat format,
            boolean isSearchedAsExactString) {
        String specialField = FIELD_MAP.get(propertyName);
        if (specialField != null) {
            return specialField;
        }

        String mappedPropertyName = resolver
                .getField(propertyName, format, isSearchedAsExactString);
        return mappedPropertyName;
    }

    private SolrQuery logicalOperator(List<SolrQuery> operands, String operator) {
        if (operands == null || operands.size() < 1) {
            throw new UnsupportedOperationException("[" + operator
                    + "] operation must contain 1 or more filters.");
        }

        // Due to a bug in how solr parses queries, a sorted spatial operand (combined with any
        // other operand)
        // must come first in any given expression, so we have to add it to the beginning of the
        // operand list.
        for (int i = 0; i < operands.size(); i++) {
            SolrQuery operand = operands.get(i);
            if (operand == null) {
                throw new UnsupportedOperationException("Null operand found");
            }
            String operandAsString = operand.toString();
            try {
                if (operandAsString.contains(URLEncoder.encode(SCORE_DISTANCE, "UTF-8"))) {
                    SolrQuery temp = operands.get(0);
                    operands.set(0, operand);
                    operands.set(i, temp);
                    break;
                }
            } catch (UnsupportedEncodingException e) {
                LOGGER.info("Unable to encode " + SCORE_DISTANCE, e);
            }
        }

        int startIndex = 0;
        SolrQuery query = operands.get(startIndex);
        startIndex++;

        if (query == null) {
            throw new UnsupportedOperationException(
                    "Query was not interpreted properly. Query should not be null.");
        }

        StringBuilder builder = new StringBuilder();
        builder.append(START_PAREN);
        builder.append(query.getQuery());

        for (int i = startIndex; i < operands.size(); i++) {
            SolrQuery localQuery = operands.get(i);

            if (localQuery != null) {
                String localPhrase = localQuery.getQuery();
                builder.append(operator + localPhrase);
            } else {
                throw new UnsupportedOperationException(
                        "Query was not interpreted properly. Query should not be null.");
            }

        }
        builder.append(END_PAREN);
        query.setQuery(builder.toString());

        return query;
    }

    private void verifyInputData(String propertyName, String pattern) {
        if (propertyName == null || propertyName.isEmpty()) {
            throw new UnsupportedOperationException("PropertyName is required for search.");
        }
        if (pattern == null || pattern.isEmpty()) {
            throw new UnsupportedOperationException("Literal value is required for search.");
        }
    }

    private String escapeSpecialCharacters(String searchPhrase) {
        return StringUtils.replaceEach(searchPhrase, LUCENE_SPECIAL_CHARACTERS,
                ESCAPED_LUCENE_SPECIAL_CHARACTERS);
    }

    private SolrQuery operationToQuery(String operation, String propertyName, String wkt) {
        String geoIndexName = getMappedPropertyName(propertyName, AttributeFormat.GEOMETRY, false);
        return operationOnIndexToQuery(operation, geoIndexName, wkt);
    }

    private SolrQuery operationOnIndexToQuery(String operation, String indexName, String wkt) {
        if (StringUtils.isNotEmpty(wkt)) {
            String normalizedWkt = WktNormalizer.normalizeWkt(wkt);
            String geoQuery = indexName + ":\"" + operation + "(" + normalizedWkt + ")\"";

            return new SolrQuery(geoQuery);
        } else {
            throw new UnsupportedOperationException("Wkt should not be null or empty.");
        }
    }

    private boolean isPoint(Geometry geo) {
        if (geo == null) {
            return false;
        }
        return Point.class.getSimpleName().equals(geo.getGeometryType());
    }

    public void setSortPolicy(SortBy sortBy) {
        this.sortBy = sortBy;
    }

    public static SolrFilterDelegate newInstance(DynamicSchemaResolver resolver) {
        return new SolrFilterDelegate(resolver);
    }

}
