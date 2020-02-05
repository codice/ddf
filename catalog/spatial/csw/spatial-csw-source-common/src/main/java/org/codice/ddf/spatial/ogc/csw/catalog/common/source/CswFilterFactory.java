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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import net.opengis.filter.v_1_1_0.AbstractIdType;
import net.opengis.filter.v_1_1_0.BBOXType;
import net.opengis.filter.v_1_1_0.BinaryComparisonOpType;
import net.opengis.filter.v_1_1_0.BinaryLogicOpType;
import net.opengis.filter.v_1_1_0.BinarySpatialOpType;
import net.opengis.filter.v_1_1_0.DistanceBufferType;
import net.opengis.filter.v_1_1_0.DistanceType;
import net.opengis.filter.v_1_1_0.FeatureIdType;
import net.opengis.filter.v_1_1_0.FilterType;
import net.opengis.filter.v_1_1_0.FunctionType;
import net.opengis.filter.v_1_1_0.LiteralType;
import net.opengis.filter.v_1_1_0.LowerBoundaryType;
import net.opengis.filter.v_1_1_0.ObjectFactory;
import net.opengis.filter.v_1_1_0.PropertyIsBetweenType;
import net.opengis.filter.v_1_1_0.PropertyIsFuzzyType;
import net.opengis.filter.v_1_1_0.PropertyIsLikeType;
import net.opengis.filter.v_1_1_0.PropertyIsNullType;
import net.opengis.filter.v_1_1_0.PropertyNameType;
import net.opengis.filter.v_1_1_0.UnaryLogicOpType;
import net.opengis.filter.v_1_1_0.UpperBoundaryType;
import net.opengis.gml.v_3_1_1.AbstractGeometryType;
import net.opengis.gml.v_3_1_1.DirectPositionType;
import net.opengis.gml.v_3_1_1.EnvelopeType;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswAxisOrder;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants.BinarySpatialOperand;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.jvnet.ogc.gml.v_3_1_1.jts.JTSToGML311GeometryConverter;
import org.jvnet.ogc.gml.v_3_1_1.jts.MarshallerImpl;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Factory for creating OGC filters. */
public class CswFilterFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(CswFilterFactory.class);

  // Regex to match coords in WKT
  private static final Pattern COORD_PATTERN =
      Pattern.compile("-?\\.?\\d+(\\.?\\d+)?\\s-?\\.?\\d+(\\.?\\d+)?");

  private static final JAXBContext JAXB_CONTEXT = initJaxbContext();

  private final ObjectFactory filterObjectFactory = new ObjectFactory();

  private final net.opengis.gml.v_3_1_1.ObjectFactory gmlObjectFactory =
      new net.opengis.gml.v_3_1_1.ObjectFactory();

  private final CswAxisOrder cswAxisOrder;

  private final boolean isSetUsePosList;

  /**
   * Constructor for CswFilterFactory.
   *
   * @param cswAxisOrder The order that axes are provided in.
   * @param isSetUsePosList True if a single <posList> element, rather than a set of <pos> elements,
   *     should be used in LinearRings when constructing XML Filter strings.
   */
  public CswFilterFactory(CswAxisOrder cswAxisOrder, boolean isSetUsePosList) {
    this.cswAxisOrder = cswAxisOrder;
    this.isSetUsePosList = isSetUsePosList;
  }

  private static JAXBContext initJaxbContext() {
    JAXBContext jaxbContext = null;

    // JAXB context path
    // "net.opengis.cat.csw.v_2_0_2:net.opengis.filter.v_1_1_0:net.opengis.gml.v_3_1_1:net.opengis.ows.v_1_0_0"
    String contextPath =
        StringUtils.join(
            new String[] {
              CswConstants.OGC_CSW_PACKAGE,
              CswConstants.OGC_FILTER_PACKAGE,
              CswConstants.OGC_GML_PACKAGE,
              CswConstants.OGC_OWS_PACKAGE
            },
            ":");

    try {
      LOGGER.debug("Creating JAXB context with context path: {}.", contextPath);
      jaxbContext =
          JAXBContext.newInstance(contextPath, CswJAXBElementProvider.class.getClassLoader());
      LOGGER.debug("{}", jaxbContext);
    } catch (JAXBException e) {
      LOGGER.info("Unable to create JAXB context using contextPath: {}.", contextPath, e);
    }

    return jaxbContext;
  }

  public FilterType buildNotFilter(FilterType filter) {
    FilterType returnFilter = new FilterType();

    if (filter == null) {
      return returnFilter;
    }
    UnaryLogicOpType notType = new UnaryLogicOpType();
    if (filter.isSetComparisonOps()) {
      notType.setComparisonOps(filter.getComparisonOps());
    } else if (filter.isSetLogicOps()) {
      notType.setLogicOps(filter.getLogicOps());
    } else if (filter.isSetSpatialOps()) {
      notType.setSpatialOps(filter.getSpatialOps());
    } else {
      return returnFilter;
    }
    returnFilter.setLogicOps(filterObjectFactory.createNot(notType));
    return returnFilter;
  }

  public FilterType buildAndFilter(List<FilterType> filters) {
    return buildAndOrFilter(filters, filterObjectFactory.createAnd(new BinaryLogicOpType()));
  }

  public FilterType buildOrFilter(List<FilterType> filters) {
    return buildAndOrFilter(filters, filterObjectFactory.createOr(new BinaryLogicOpType()));
  }

  public FilterType buildPropertyIsBetweenFilter(
      String propertyName, Object lowerBoundary, Object upperBoundary) {
    FilterType filter = new FilterType();
    filter.setComparisonOps(createPropertyIsBetween(propertyName, lowerBoundary, upperBoundary));
    return filter;
  }

  public FilterType buildPropertyIsNullFilter(String propertyName) {
    FilterType filter = new FilterType();
    filter.setComparisonOps(createPropertyIsNull(propertyName));
    return filter;
  }

  public FilterType buildPropertyIsEqualToFilter(
      String propertyName, Object literal, boolean isCaseSensitive) {
    FilterType filter = new FilterType();
    filter.setComparisonOps(createPropertyIsEqualTo(propertyName, literal, isCaseSensitive));

    return filter;
  }

  public FilterType buildPropertyIsNotEqualToFilter(
      String propertyName, Object literal, boolean isCaseSensitive) {
    FilterType filter = new FilterType();
    filter.setComparisonOps(createPropertyIsNotEqualTo(propertyName, literal, isCaseSensitive));

    return filter;
  }

  public FilterType buildPropertyIsLikeFilter(
      String propertyName, Object literal, boolean isCaseSensitive) {
    FilterType filter = new FilterType();
    filter.setComparisonOps(createPropertyIsLike(propertyName, literal, isCaseSensitive));
    return filter;
  }

  public FilterType buildPropertyIsFuzzyFilter(String propertyName, Object literal) {
    FilterType filter = new FilterType();
    filter.setComparisonOps(createPropertyIsFuzzy(propertyName, literal));
    return filter;
  }

  public FilterType buildPropertyIsGreaterThanFilter(String propertyName, Object literal) {
    FilterType filter = new FilterType();
    filter.setComparisonOps(createPropertyIsGreaterThan(propertyName, literal));
    return filter;
  }

  public FilterType buildPropertyIsGreaterThanOrEqualToFilter(String propertyName, Object literal) {
    FilterType filter = new FilterType();
    filter.setComparisonOps(createPropertyIsGreaterThanOrEqualTo(propertyName, literal));
    return filter;
  }

  public FilterType buildPropertyIsLessThanFilter(String propertyName, Object literal) {
    FilterType filter = new FilterType();
    filter.setComparisonOps(createPropertyIsLessThan(propertyName, literal));
    return filter;
  }

  public FilterType buildPropertyIsLessThanOrEqualToFilter(String propertyName, Object literal) {
    FilterType filter = new FilterType();
    filter.setComparisonOps(createPropertyIsLessThanOrEqualTo(propertyName, literal));
    return filter;
  }

  public FilterType buildContainsGeospatialFilter(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    FilterType filter = new FilterType();
    filter.setSpatialOps(createContainsType(propertyName, wkt, geometryOrEnvelope));
    return filter;
  }

  public FilterType buildIntersectsGeospatialFilter(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    FilterType filter = new FilterType();
    filter.setSpatialOps(createIntersectsType(propertyName, wkt, geometryOrEnvelope));
    return filter;
  }

  public FilterType buildCrossesGeospatialFilter(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    FilterType filter = new FilterType();
    filter.setSpatialOps(createCrossesType(propertyName, wkt, geometryOrEnvelope));
    return filter;
  }

  public FilterType buildTouchesGeospatialFilter(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    FilterType filter = new FilterType();
    filter.setSpatialOps(createTouchesType(propertyName, wkt, geometryOrEnvelope));
    return filter;
  }

  public FilterType buildOverlapsGeospatialFilter(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    FilterType filter = new FilterType();
    filter.setSpatialOps(createOverlapsType(propertyName, wkt, geometryOrEnvelope));
    return filter;
  }

  public FilterType buildBeyondGeospatialFilter(String propertyName, String wkt, double distance) {
    FilterType filter = new FilterType();
    filter.setSpatialOps(createBeyondType(propertyName, wkt, distance));
    return filter;
  }

  public FilterType buildDisjointGeospatialFilter(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    FilterType filter = new FilterType();
    filter.setSpatialOps(createDisjointType(propertyName, wkt, geometryOrEnvelope));
    return filter;
  }

  public FilterType buildDWithinGeospatialFilter(String propertyName, String wkt, double distance) {
    FilterType filter = new FilterType();
    filter.setSpatialOps(createDWithinType(propertyName, wkt, distance));
    return filter;
  }

  public FilterType buildWithinGeospatialFilter(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    FilterType filter = new FilterType();
    filter.setSpatialOps(createWithinType(propertyName, wkt, geometryOrEnvelope));
    return filter;
  }

  public FilterType buildBBoxGeospatialFilter(String propertyName, String wkt) {
    FilterType filter = new FilterType();
    filter.setSpatialOps(createBBoxType(propertyName, wkt));
    return filter;
  }

  private FilterType buildAndOrFilter(
      List<FilterType> filters, JAXBElement<BinaryLogicOpType> binaryLogicFilter) {

    if (filters.isEmpty()) {
      throw new UnsupportedOperationException("No valid filters found in the list of filters.");
    }

    removeEmptyFilters(filters);

    Set<String> featureIds = getFeatureIds(filters);

    if (!CollectionUtils.isEmpty(featureIds)) {
      return buildFeatureIdFilter(featureIds);
    }

    if (filters.size() == 1) {
      return filters.get(0);
    }

    for (FilterType filter : filters) {
      if (filter.isSetComparisonOps()) {
        binaryLogicFilter
            .getValue()
            .getComparisonOpsOrSpatialOpsOrLogicOps()
            .add(filter.getComparisonOps());
      } else if (filter.isSetLogicOps()) {
        binaryLogicFilter
            .getValue()
            .getComparisonOpsOrSpatialOpsOrLogicOps()
            .add(filter.getLogicOps());
      } else if (filter.isSetSpatialOps()) {
        binaryLogicFilter
            .getValue()
            .getComparisonOpsOrSpatialOpsOrLogicOps()
            .add(filter.getSpatialOps());
      }
    }
    FilterType returnFilter = new FilterType();
    returnFilter.setLogicOps(binaryLogicFilter);

    return returnFilter;
  }

  private void removeEmptyFilters(List<FilterType> filters) {
    List<FilterType> filtersToBeRemoved = new ArrayList<FilterType>(filters.size());

    for (FilterType filter : filters) {
      if (filter == null) {
        throw new UnsupportedOperationException("Invalid filter specified!");
      } else if (!isFilterSet(filter)) {
        filtersToBeRemoved.add(filter);
      }
    }
    // remove any null filters or those marked for removal
    filters.removeAll(filtersToBeRemoved);
    // add an empty filter back in if the list is empty
    if (filters.isEmpty()) {
      filters.add(new FilterType());
    }
  }

  private boolean isFilterSet(FilterType filter) {
    return (filter.isSetComparisonOps()
        || filter.isSetId()
        || filter.isSetLogicOps()
        || filter.isSetSpatialOps());
  }

  private Set<String> getFeatureIds(List<FilterType> filters) {
    Set<String> ids = new HashSet<String>();

    if (!CollectionUtils.isEmpty(filters)) {
      boolean isFeatureIdFilter = filters.get(0) != null && filters.get(0).isSetId();

      for (FilterType filter : filters) {
        if ((filter != null && filter.isSetId()) != isFeatureIdFilter) {
          throw new UnsupportedOperationException(
              "Query with mix of FeatureID and non-FeatureID queries not supported.");
        }

        if (isFeatureIdFilter) {
          Iterator<JAXBElement<? extends AbstractIdType>> iter = filter.getId().iterator();
          while (iter.hasNext()) {
            @SuppressWarnings("unchecked")
            FeatureIdType idType = ((JAXBElement<FeatureIdType>) iter.next()).getValue();
            ids.add(idType.getFid());
          }
        }
      }
    }

    return ids;
  }

  private FilterType buildFeatureIdFilter(Set<String> ids) {
    FilterType filterType = new FilterType();

    for (String id : ids) {
      filterType.getId().add(createFeatureIdFilter(id));
    }
    return filterType;
  }

  private JAXBElement<FeatureIdType> createFeatureIdFilter(final String id) {

    FeatureIdType featureIdType = new FeatureIdType();
    featureIdType.setFid(id);

    return filterObjectFactory.createFeatureId(featureIdType);
  }

  private JAXBElement<BinarySpatialOpType> createIntersectsType(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    PropertyNameType propertyNameType = createPropertyNameType(propertyName);
    return filterObjectFactory.createIntersects(
        createBinarySpatialOpType(propertyNameType, wkt, geometryOrEnvelope));
  }

  private JAXBElement<BinarySpatialOpType> createCrossesType(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    PropertyNameType propertyNameType = createPropertyNameType(propertyName);
    return filterObjectFactory.createCrosses(
        createBinarySpatialOpType(propertyNameType, wkt, geometryOrEnvelope));
  }

  private JAXBElement<DistanceBufferType> createBeyondType(
      String propertyName, String wkt, double distance) {
    wkt = convertWktToLatLonOrdering(wkt);
    Geometry geometry = getGeometryFromWkt(wkt);
    JAXBElement<? extends AbstractGeometryType> geometryJaxbElement = convertGeometry(geometry);
    PropertyNameType propertyNameType = createPropertyNameType(propertyName);
    DistanceType distanceType = createDistanceType(distance, CswConstants.METERS);
    DistanceBufferType distanceBufferType =
        createDistanceBufferType(propertyNameType, geometryJaxbElement, distanceType);

    return filterObjectFactory.createBeyond(distanceBufferType);
  }

  private JAXBElement<BinarySpatialOpType> createDisjointType(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    PropertyNameType propertyNameType = createPropertyNameType(propertyName);
    return filterObjectFactory.createDisjoint(
        createBinarySpatialOpType(propertyNameType, wkt, geometryOrEnvelope));
  }

  private JAXBElement<DistanceBufferType> createDWithinType(
      String propertyName, String wkt, double distance) {
    wkt = convertWktToLatLonOrdering(wkt);
    Geometry geometry = getGeometryFromWkt(wkt);
    JAXBElement<? extends AbstractGeometryType> geometryJaxbElement = convertGeometry(geometry);
    PropertyNameType propertyNameType = createPropertyNameType(propertyName);
    DistanceType distanceType = createDistanceType(distance, CswConstants.METERS);
    DistanceBufferType distanceBufferType =
        createDistanceBufferType(propertyNameType, geometryJaxbElement, distanceType);

    return filterObjectFactory.createDWithin(distanceBufferType);
  }

  private JAXBElement<BinarySpatialOpType> createContainsType(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    PropertyNameType propertyNameType = createPropertyNameType(propertyName);
    return filterObjectFactory.createContains(
        createBinarySpatialOpType(propertyNameType, wkt, geometryOrEnvelope));
  }

  private JAXBElement<BinarySpatialOpType> createTouchesType(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    PropertyNameType propertyNameType = createPropertyNameType(propertyName);
    return filterObjectFactory.createTouches(
        createBinarySpatialOpType(propertyNameType, wkt, geometryOrEnvelope));
  }

  private JAXBElement<BinarySpatialOpType> createOverlapsType(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    PropertyNameType propertyNameType = createPropertyNameType(propertyName);
    return filterObjectFactory.createOverlaps(
        createBinarySpatialOpType(propertyNameType, wkt, geometryOrEnvelope));
  }

  private JAXBElement<BinarySpatialOpType> createWithinType(
      String propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    PropertyNameType propertyNameType = createPropertyNameType(propertyName);
    return filterObjectFactory.createWithin(
        createBinarySpatialOpType(propertyNameType, wkt, geometryOrEnvelope));
  }

  @SuppressWarnings("unchecked")
  private JAXBElement<? extends AbstractGeometryType> convertGeometry(Geometry geometry) {
    geometry.setUserData(CswConstants.SRS_NAME);
    JAXBElement<? extends AbstractGeometryType> abstractGeometry = null;
    try {
      Map<String, String> geoConverterProps = new HashMap<String, String>();
      geoConverterProps.put(
          CswJTSToGML311GeometryConverter.USE_POS_LIST_GEO_CONVERTER_PROP_KEY,
          String.valueOf(isSetUsePosList));

      JTSToGML311GeometryConverter converter =
          new CswJTSToGML311GeometryConverter(geoConverterProps);

      Marshaller marshaller = new MarshallerImpl(JAXB_CONTEXT.createMarshaller(), converter);
      StringWriter writer = new StringWriter();
      marshaller.marshal(geometry, writer);
      String xmlGeo = writer.toString();
      LOGGER.debug("Geometry as XML: {}", xmlGeo);

      XMLInputFactory xmlInputFactory = XMLInputFactory.newFactory();
      xmlInputFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
      xmlInputFactory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
      xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
      XMLStreamReader xmlStreamReader =
          xmlInputFactory.createXMLStreamReader(new StringReader(xmlGeo));

      Unmarshaller unmarshaller = JAXB_CONTEXT.createUnmarshaller();
      Object object = unmarshaller.unmarshal(xmlStreamReader);
      LOGGER.debug("Unmarshalled as => {}", object);
      if (object instanceof JAXBElement) {
        abstractGeometry = (JAXBElement<? extends AbstractGeometryType>) object;
      } else {
        LOGGER.debug(
            "Unable to cast to JAXBElement<? extends AbstractGeometryType>.  Object is of type [{}].",
            object.getClass().getName());
      }
    } catch (JAXBException | XMLStreamException e) {
      LOGGER.debug("Unable to unmarshal geometry [{}]", geometry.getClass().getName(), e);
    }

    return abstractGeometry;
  }

  private JAXBElement<BBOXType> createBBoxType(String propertyName, String wkt) {
    BBOXType bboxType = new BBOXType();
    JAXBElement<EnvelopeType> envelope = createEnvelopeType(wkt);
    bboxType.setEnvelope(envelope);
    bboxType.setPropertyName(createPropertyNameType(propertyName));

    return filterObjectFactory.createBBOX(bboxType);
  }

  private DistanceType createDistanceType(double distance, String units) {
    DistanceType distanceType = new DistanceType();
    distanceType.setUnits(units);
    /**
     * With the original filter-v_1_1_0-schema_1.1.0.jar, we were unable to set content (the actual
     * distance value) on DistanceType. The attribute mixed="true" is missing from the complexType
     * DistanceType in filter.xsd. This prevents Beyond and DWithin from working. To correct this,
     * we fixed filter.xsd and rebuilt the JAXB bindings (see project
     * ogc-filter-v_1_1_0-schema-bindings).
     */
    distanceType.setValue(distance);
    return distanceType;
  }

  @SuppressWarnings("unchecked")
  private DistanceBufferType createDistanceBufferType(
      PropertyNameType propertyName,
      JAXBElement<? extends AbstractGeometryType> geometry,
      DistanceType distance) {
    DistanceBufferType distanceBuffer = new DistanceBufferType();
    distanceBuffer.setDistance(distance);
    distanceBuffer.setGeometry((JAXBElement<AbstractGeometryType>) geometry);
    distanceBuffer.setPropertyName(propertyName);
    return distanceBuffer;
  }

  private JAXBElement<EnvelopeType> createEnvelopeType(String wkt) {
    EnvelopeType envelopeType = new EnvelopeType();

    wkt = convertWktToLatLonOrdering(wkt);
    Envelope envelope = getEnvelopeFromWkt(wkt);

    if (envelope != null) {
      envelopeType.setLowerCorner(createDirectPositionType(envelope.getMinX(), envelope.getMinY()));
      envelopeType.setUpperCorner(createDirectPositionType(envelope.getMaxX(), envelope.getMaxY()));
    }

    return gmlObjectFactory.createEnvelope(envelopeType);
  }

  private Envelope getEnvelopeFromWkt(String wkt) {
    Geometry geo = getGeometryFromWkt(wkt);
    Envelope envelope = geo.getEnvelopeInternal();

    return envelope;
  }

  private DirectPositionType createDirectPositionType(Double x, Double y) {
    DirectPositionType directPositionType = new DirectPositionType();
    List<Double> coord = new ArrayList<Double>(2);
    coord.add(x);
    coord.add(y);
    directPositionType.setValue(coord);
    LOGGER.debug("Created direct position type ({}, {})", x, y);
    return directPositionType;
  }

  private PropertyNameType createPropertyNameType(String propertyName) {
    PropertyNameType propertyNameType = new PropertyNameType();
    propertyNameType.setContent(Arrays.asList(new Object[] {propertyName}));
    return propertyNameType;
  }

  private JAXBElement<PropertyIsLikeType> createPropertyIsLike(
      String propertyName, Object literal, boolean isCaseSensitive) {
    PropertyIsLikeType propertyIsLikeType = new PropertyIsLikeType();
    propertyIsLikeType.setEscapeChar(CswConstants.ESCAPE);
    propertyIsLikeType.setSingleChar(CswConstants.SINGLE_CHAR);
    propertyIsLikeType.setWildCard(CswConstants.WILD_CARD);
    propertyIsLikeType.setPropertyName(
        createPropertyNameType(Arrays.asList(new Object[] {propertyName})).getValue());
    propertyIsLikeType.setLiteral(createLiteralType(literal).getValue());
    propertyIsLikeType.setMatchCase(isCaseSensitive);
    return filterObjectFactory.createPropertyIsLike(propertyIsLikeType);
  }

  private JAXBElement<PropertyIsFuzzyType> createPropertyIsFuzzy(
      String propertyName, Object literal) {
    PropertyIsFuzzyType propertyIsFuzzyType = new PropertyIsFuzzyType();

    propertyIsFuzzyType.setPropertyName(
        createPropertyNameType(Arrays.asList(new Object[] {propertyName})).getValue());
    propertyIsFuzzyType.setLiteral(createLiteralType(literal).getValue());

    return filterObjectFactory.createPropertyIsFuzzy(propertyIsFuzzyType);
  }

  private JAXBElement<BinaryComparisonOpType> createPropertyIsEqualTo(
      String propertyName, Object literal, boolean isCaseSensitive) {
    BinaryComparisonOpType propertyIsEqualTo = new BinaryComparisonOpType();
    propertyIsEqualTo.setMatchCase(isCaseSensitive);
    propertyIsEqualTo
        .getExpression()
        .add(createPropertyNameType(Arrays.asList(new Object[] {propertyName})));
    propertyIsEqualTo.getExpression().add(createLiteralType(literal));
    return filterObjectFactory.createPropertyIsEqualTo(propertyIsEqualTo);
  }

  private JAXBElement<BinaryComparisonOpType> createPropertyIsNotEqualTo(
      String propertyName, Object literal, boolean isCaseSensitive) {
    BinaryComparisonOpType propertyIsNotEqualTo = new BinaryComparisonOpType();
    propertyIsNotEqualTo.setMatchCase(isCaseSensitive);
    propertyIsNotEqualTo
        .getExpression()
        .add(createPropertyNameType(Arrays.asList(new Object[] {propertyName})));
    propertyIsNotEqualTo.getExpression().add(createLiteralType(literal));
    return filterObjectFactory.createPropertyIsNotEqualTo(propertyIsNotEqualTo);
  }

  private JAXBElement<BinaryComparisonOpType> createPropertyIsGreaterThan(
      String propertyName, Object literal) {
    BinaryComparisonOpType propertyIsGreaterThan = new BinaryComparisonOpType();
    propertyIsGreaterThan
        .getExpression()
        .add(createPropertyNameType(Arrays.asList(new Object[] {propertyName})));
    propertyIsGreaterThan.getExpression().add(createLiteralType(literal));
    return filterObjectFactory.createPropertyIsGreaterThan(propertyIsGreaterThan);
  }

  private JAXBElement<BinaryComparisonOpType> createPropertyIsGreaterThanOrEqualTo(
      String propertyName, Object literal) {
    BinaryComparisonOpType propertyIsGreaterThanAOrEqualTo = new BinaryComparisonOpType();
    propertyIsGreaterThanAOrEqualTo
        .getExpression()
        .add(createPropertyNameType(Arrays.asList(new Object[] {propertyName})));
    propertyIsGreaterThanAOrEqualTo.getExpression().add(createLiteralType(literal));
    return filterObjectFactory.createPropertyIsGreaterThanOrEqualTo(
        propertyIsGreaterThanAOrEqualTo);
  }

  private JAXBElement<BinaryComparisonOpType> createPropertyIsLessThan(
      String propertyName, Object literal) {
    BinaryComparisonOpType propertyIsLessThan = new BinaryComparisonOpType();
    propertyIsLessThan
        .getExpression()
        .add(createPropertyNameType(Arrays.asList(new Object[] {propertyName})));
    propertyIsLessThan.getExpression().add(createLiteralType(literal));
    return filterObjectFactory.createPropertyIsLessThan(propertyIsLessThan);
  }

  private JAXBElement<BinaryComparisonOpType> createPropertyIsLessThanOrEqualTo(
      String propertyName, Object literal) {
    BinaryComparisonOpType propertyIsLessThanAOrEqualTo = new BinaryComparisonOpType();
    propertyIsLessThanAOrEqualTo
        .getExpression()
        .add(createPropertyNameType(Arrays.asList(new Object[] {propertyName})));
    propertyIsLessThanAOrEqualTo.getExpression().add(createLiteralType(literal));
    return filterObjectFactory.createPropertyIsLessThanOrEqualTo(propertyIsLessThanAOrEqualTo);
  }

  private JAXBElement<PropertyIsBetweenType> createPropertyIsBetween(
      String propertyName, Object lowerBoundary, Object upperBoundary) {
    PropertyIsBetweenType propertyIsBetween = new PropertyIsBetweenType();
    propertyIsBetween.setLowerBoundary(createLowerBoundary(lowerBoundary));
    propertyIsBetween.setUpperBoundary(createUpperBoundary(upperBoundary));
    propertyIsBetween.setExpression(
        createPropertyNameType(Arrays.asList(new Object[] {propertyName})));
    return filterObjectFactory.createPropertyIsBetween(propertyIsBetween);
  }

  private JAXBElement<PropertyIsNullType> createPropertyIsNull(String propertyName) {
    PropertyIsNullType propertyIsNull = new PropertyIsNullType();
    propertyIsNull.setPropertyName(
        createPropertyNameType(Arrays.asList(new Object[] {propertyName})).getValue());
    return filterObjectFactory.createPropertyIsNull(propertyIsNull);
  }

  private LowerBoundaryType createLowerBoundary(Object lowerBoundary) {
    LowerBoundaryType lowerBoundaryType = new LowerBoundaryType();
    lowerBoundaryType.setExpression(createLiteralType(lowerBoundary));
    return lowerBoundaryType;
  }

  private UpperBoundaryType createUpperBoundary(Object upperBoundary) {
    UpperBoundaryType upperBoundaryType = new UpperBoundaryType();
    upperBoundaryType.setExpression(createLiteralType(upperBoundary));
    return upperBoundaryType;
  }

  private JAXBElement<LiteralType> createLiteralType(Object literalValue) {
    JAXBElement<LiteralType> literalType = filterObjectFactory.createLiteral(new LiteralType());
    literalType.getValue().getContent().add(literalValue.toString());
    return literalType;
  }

  private JAXBElement<PropertyNameType> createPropertyNameType(List<Object> propertyNameValues) {
    JAXBElement<PropertyNameType> propertyNameType =
        filterObjectFactory.createPropertyName(new PropertyNameType());
    propertyNameType.getValue().setContent(propertyNameValues);
    return propertyNameType;
  }

  private Geometry getGeometryFromWkt(String wkt) {
    try {
      return new WKTReader().read(wkt);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Unable to parse WKT: " + wkt, e);
    }
  }

  /**
   * The WKT passed into the spatial methods has the coordinates ordered in LON/LAT. This method
   * will convert the WKT to LAT/LON ordering.
   */
  private String convertWktToLatLonOrdering(String wktInLonLat) {

    if (cswAxisOrder != CswAxisOrder.LON_LAT) {
      LOGGER.debug(
          "Converting WKT from LON/LAT coordinate ordering to LAT/LON coordinate ordering.");

      // Normalize all whitespace in WKT before processing.
      wktInLonLat = normalizeWhitespaceInWkt(wktInLonLat);

      Matcher matcher = COORD_PATTERN.matcher(wktInLonLat);

      StringBuffer stringBuffer = new StringBuffer();

      while (matcher.find()) {
        String lonLatCoord = matcher.group();
        String latLonCoord = StringUtils.reverseDelimited(lonLatCoord, ' ');
        LOGGER.debug(
            "Converted LON/LAT coord: ({}) to LAT/LON coord: ({}).", lonLatCoord, latLonCoord);
        matcher.appendReplacement(stringBuffer, latLonCoord);
      }

      matcher.appendTail(stringBuffer);

      String wktInLatLon = stringBuffer.toString();
      LOGGER.debug("Original WKT with coords in LON/LAT ordering:  {}", wktInLonLat);
      LOGGER.debug("Converted WKT with coords in LAT/LON ordering: {}", wktInLatLon);

      return wktInLatLon;
    } else {
      LOGGER.debug("The configured CSW source requires coordinates in LON/LAT ordering.");
      return wktInLonLat;
    }
  }

  private String normalizeWhitespaceInWkt(String wkt) {
    String normalizedWkt = wkt.replaceAll("(\\s+)?([(|)|,])(\\s+)?", "$2");
    normalizedWkt = normalizedWkt.replaceAll("\\s+", " ");
    return normalizedWkt;
  }

  @SuppressWarnings("unchecked")
  private BinarySpatialOpType createBinarySpatialOpTypeUsingGeometry(
      PropertyNameType propertyName, JAXBElement<? extends AbstractGeometryType> geometry) {
    BinarySpatialOpType binarySpatialOpType = new BinarySpatialOpType();
    binarySpatialOpType.getPropertyName().add(propertyName);
    binarySpatialOpType.setGeometry((JAXBElement<AbstractGeometryType>) geometry);
    return binarySpatialOpType;
  }

  private BinarySpatialOpType createBinarySpatialOpTypeUsingEnvelope(
      PropertyNameType propertyName, JAXBElement<EnvelopeType> envelope) {
    BinarySpatialOpType binarySpatialOpType = new BinarySpatialOpType();
    binarySpatialOpType.getPropertyName().add(propertyName);
    binarySpatialOpType.setEnvelope(envelope);
    return binarySpatialOpType;
  }

  private BinarySpatialOpType createBinarySpatialOpType(
      PropertyNameType propertyName, String wkt, BinarySpatialOperand geometryOrEnvelope) {
    BinarySpatialOpType binarySpatialOpType = null;

    if (geometryOrEnvelope == BinarySpatialOperand.GEOMETRY) {
      wkt = convertWktToLatLonOrdering(wkt);
      Geometry geometry = getGeometryFromWkt(wkt);
      JAXBElement<? extends AbstractGeometryType> geometryJaxbElement = convertGeometry(geometry);
      binarySpatialOpType =
          createBinarySpatialOpTypeUsingGeometry(propertyName, geometryJaxbElement);
    } else {
      JAXBElement<EnvelopeType> envelopeJaxbElement = createEnvelopeType(wkt);
      binarySpatialOpType =
          createBinarySpatialOpTypeUsingEnvelope(propertyName, envelopeJaxbElement);
    }

    return binarySpatialOpType;
  }

  // default implementation just assume the first argument is a parameter name and the rest are
  // values
  public FilterType buildPropertyIsEqualTo(
      String functionName, List<Object> arguments, Object literal) {
    int propertyCount = 1;
    List<JAXBElement<?>> expressions = new ArrayList<>();
    for (int i = 0; i < arguments.size(); i++) {
      if (i < propertyCount) {
        expressions.add(createPropertyNameType(Arrays.asList(arguments.get(i))));
      } else {
        expressions.add(createLiteralType(arguments.get(i)));
      }
    }
    return createPropertyIsEqualTo(functionName, expressions, literal);
  }

  public FilterType createPropertyIsEqualTo(
      String functionName, List<? extends JAXBElement<?>> expressions, Object literal) {

    FunctionType function = new FunctionType();
    function.setName(functionName);
    function.getExpression().addAll(expressions);
    FilterType filter = new FilterType();
    BinaryComparisonOpType propertyIsEqualTo = new BinaryComparisonOpType();
    propertyIsEqualTo.getExpression().add(filterObjectFactory.createFunction(function));
    propertyIsEqualTo.getExpression().add(createLiteralType(literal));
    filter.setComparisonOps(filterObjectFactory.createPropertyIsEqualTo(propertyIsEqualTo));

    return filter;
  }
}
