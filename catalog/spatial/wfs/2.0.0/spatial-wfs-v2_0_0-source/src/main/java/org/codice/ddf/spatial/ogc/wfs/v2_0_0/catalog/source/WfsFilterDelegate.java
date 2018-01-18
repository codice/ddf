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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;
import ddf.catalog.data.Metacard;
import ddf.catalog.filter.impl.SimpleFilterDelegate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import net.opengis.filter.v_2_0_0.AbstractIdType;
import net.opengis.filter.v_2_0_0.BBOXType;
import net.opengis.filter.v_2_0_0.BinaryComparisonOpType;
import net.opengis.filter.v_2_0_0.BinaryLogicOpType;
import net.opengis.filter.v_2_0_0.BinarySpatialOpType;
import net.opengis.filter.v_2_0_0.BinaryTemporalOpType;
import net.opengis.filter.v_2_0_0.ComparisonOperatorType;
import net.opengis.filter.v_2_0_0.ComparisonOperatorsType;
import net.opengis.filter.v_2_0_0.ComparisonOpsType;
import net.opengis.filter.v_2_0_0.ConformanceType;
import net.opengis.filter.v_2_0_0.DistanceBufferType;
import net.opengis.filter.v_2_0_0.FilterCapabilities;
import net.opengis.filter.v_2_0_0.FilterType;
import net.opengis.filter.v_2_0_0.GeometryOperandsType;
import net.opengis.filter.v_2_0_0.GeometryOperandsType.GeometryOperand;
import net.opengis.filter.v_2_0_0.LiteralType;
import net.opengis.filter.v_2_0_0.LowerBoundaryType;
import net.opengis.filter.v_2_0_0.MeasureType;
import net.opengis.filter.v_2_0_0.ObjectFactory;
import net.opengis.filter.v_2_0_0.PropertyIsBetweenType;
import net.opengis.filter.v_2_0_0.PropertyIsLikeType;
import net.opengis.filter.v_2_0_0.ResourceIdType;
import net.opengis.filter.v_2_0_0.ScalarCapabilitiesType;
import net.opengis.filter.v_2_0_0.SpatialCapabilitiesType;
import net.opengis.filter.v_2_0_0.SpatialOperatorType;
import net.opengis.filter.v_2_0_0.SpatialOperatorsType;
import net.opengis.filter.v_2_0_0.SpatialOpsType;
import net.opengis.filter.v_2_0_0.TemporalCapabilitiesType;
import net.opengis.filter.v_2_0_0.TemporalOperandsType;
import net.opengis.filter.v_2_0_0.TemporalOperandsType.TemporalOperand;
import net.opengis.filter.v_2_0_0.TemporalOperatorType;
import net.opengis.filter.v_2_0_0.TemporalOperatorsType;
import net.opengis.filter.v_2_0_0.UnaryLogicOpType;
import net.opengis.filter.v_2_0_0.UpperBoundaryType;
import net.opengis.gml.v_3_2_1.AbstractRingPropertyType;
import net.opengis.gml.v_3_2_1.CoordinatesType;
import net.opengis.gml.v_3_2_1.DirectPositionType;
import net.opengis.gml.v_3_2_1.EnvelopeType;
import net.opengis.gml.v_3_2_1.LineStringType;
import net.opengis.gml.v_3_2_1.LinearRingType;
import net.opengis.gml.v_3_2_1.PointType;
import net.opengis.gml.v_3_2_1.PolygonType;
import net.opengis.gml.v_3_2_1.TimeInstantType;
import net.opengis.gml.v_3_2_1.TimePeriodType;
import net.opengis.gml.v_3_2_1.TimePositionType;
import net.opengis.ows.v_1_1_0.AllowedValues;
import net.opengis.ows.v_1_1_0.DomainType;
import net.opengis.ows.v_1_1_0.ValueType;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.codice.ddf.libs.geo.util.GeospatialUtil;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureAttributeDescriptor;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.FeatureMetacardType;
import org.codice.ddf.spatial.ogc.wfs.catalog.mapper.MetacardMapper;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.COMPARISON_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.CONFORMANCE_CONSTRAINTS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.SPATIAL_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.TEMPORAL_OPERATORS;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to convert DDF OGC Filters into WFS compatible OGC Filters. This
 * class will return an "Invalid"(null) filter if a translation could not be made. It will return an
 * "Empty" filter, meaning no filters are set, only if it is a Content Type filter.
 */
public class WfsFilterDelegate extends SimpleFilterDelegate<FilterType> {

  private static final Logger LOGGER = LoggerFactory.getLogger(WfsFilterDelegate.class);

  private static final String MISSING_PARAMETERS_MSG = "Required parameters are missing";

  private static final String PROPERTY_NOT_QUERYABLE = "'%s' is not a queryable property.";

  private static final String PROPERTY_NOT_QUERYABLE_ARG = "{} is not a queryable property";

  // Regex to match coords in WKT
  private static final Pattern COORD_PATTERN =
      Pattern.compile("-?\\.?\\d+(\\.?\\d+)?\\s-?\\.?\\d+(\\.?\\d+)?");

  private FeatureMetacardType featureMetacardType;

  private ObjectFactory filterObjectFactory = new ObjectFactory();

  private net.opengis.gml.v_3_2_1.ObjectFactory gml320ObjectFactory =
      new net.opengis.gml.v_3_2_1.ObjectFactory();

  private boolean logicalOps;

  private Set<COMPARISON_OPERATORS> comparisonOps;

  private Map<SPATIAL_OPERATORS, SpatialOperatorType> spatialOps =
      new ConcurrentHashMap<SPATIAL_OPERATORS, SpatialOperatorType>(
          new EnumMap<SPATIAL_OPERATORS, SpatialOperatorType>(SPATIAL_OPERATORS.class));

  private List<QName> geometryOperands;

  private List<QName> temporalOperands;

  private Map<TEMPORAL_OPERATORS, TemporalOperatorType> temporalOps =
      new ConcurrentHashMap<TEMPORAL_OPERATORS, TemporalOperatorType>(
          new EnumMap<TEMPORAL_OPERATORS, TemporalOperatorType>(TEMPORAL_OPERATORS.class));

  private boolean isSortingSupported;

  private Set<SortOrder> allowedSortOrders;

  private String coordinateOrder;

  private String srsName;

  private boolean isEpsg4326 = false;

  private MetacardMapper metacardToFeatureMapper;

  public WfsFilterDelegate(
      FeatureMetacardType featureMetacardType,
      FilterCapabilities filterCapabilities,
      String srsName,
      MetacardMapper metacardToFeatureMapper,
      String coordinateOrder) {

    if (featureMetacardType == null) {
      throw new IllegalArgumentException("FeatureMetacardType can not be null");
    }
    this.featureMetacardType = featureMetacardType;
    this.metacardToFeatureMapper = metacardToFeatureMapper;
    this.srsName = srsName;
    if (GeospatialUtil.EPSG_4326.equalsIgnoreCase(srsName)
        || GeospatialUtil.EPSG_4326_URN.equalsIgnoreCase(srsName)) {
      isEpsg4326 = true;
    } else {
      LOGGER.debug(
          "Unable to convert geometry to {}. All geospatial queries for this featureType will be invalidated!",
          srsName);
    }
    this.coordinateOrder = coordinateOrder;
    this.allowedSortOrders = new HashSet<SortOrder>();
    this.isSortingSupported = false;
    updateAllowedOperations(filterCapabilities);
  }

  private final void updateAllowedOperations(FilterCapabilities filterCapabilities) {
    comparisonOps =
        Collections.newSetFromMap(
            new ConcurrentHashMap<COMPARISON_OPERATORS, Boolean>(
                new EnumMap<COMPARISON_OPERATORS, Boolean>(COMPARISON_OPERATORS.class)));

    geometryOperands = new ArrayList<QName>();
    temporalOperands = new ArrayList<QName>();

    if (filterCapabilities == null) {
      LOGGER.debug("WFS 2.0 Service doesn't support any filters");
      return;
    }

    // CONFORMANCE
    configureConformance(filterCapabilities.getConformance());

    ScalarCapabilitiesType scalarCapabilities = filterCapabilities.getScalarCapabilities();
    if (scalarCapabilities != null) {
      // LOGICAL OPERATORS
      if (scalarCapabilities.getLogicalOperators() != null) {
        logicalOps = true;
      }

      // COMPARISON OPERATORS
      ComparisonOperatorsType comparisonOperators = scalarCapabilities.getComparisonOperators();
      if (comparisonOperators != null) {
        for (ComparisonOperatorType comp : comparisonOperators.getComparisonOperator()) {
          if (null != comp) {
            comparisonOps.add(COMPARISON_OPERATORS.valueOf(comp.getName()));
          }
        }
      }
    }

    // SPATIAL OPERATORS
    SpatialCapabilitiesType spatialCapabilities = filterCapabilities.getSpatialCapabilities();
    if (spatialCapabilities != null) {
      if (spatialCapabilities.getSpatialOperators() != null) {
        setSpatialOps(spatialCapabilities.getSpatialOperators());
      }

      // GEOMETRY OPERANDS
      GeometryOperandsType geometryOperandsType = spatialCapabilities.getGeometryOperands();
      if (geometryOperandsType != null) {
        for (GeometryOperandsType.GeometryOperand geoOperand :
            geometryOperandsType.getGeometryOperand()) {
          if (geoOperand.getName() != null) {
            geometryOperands.add(geoOperand.getName());
          }
        }
        LOGGER.debug("geometryOperands: {}", geometryOperands);
      }
    }

    // TEMPORAL OPERATORS
    TemporalCapabilitiesType temporalCapabilitiesType =
        filterCapabilities.getTemporalCapabilities();
    if (temporalCapabilitiesType != null) {
      if (temporalCapabilitiesType.getTemporalOperators() != null) {
        setTemporalOps(temporalCapabilitiesType.getTemporalOperators());
      }

      // TEMPORAL OPERANDS
      TemporalOperandsType temporalOperandsType = temporalCapabilitiesType.getTemporalOperands();
      if (temporalOperandsType != null) {
        for (TemporalOperandsType.TemporalOperand temporalOperand :
            temporalOperandsType.getTemporalOperand()) {
          if (temporalOperand.getName() != null) {
            temporalOperands.add(temporalOperand.getName());
          }
        }
        LOGGER.debug("temporalOperands: {}", temporalOperands);
      }
    }
  }

  private void configureConformance(ConformanceType conformance) {
    if (conformance != null) {
      List<DomainType> constraints = conformance.getConstraint();
      if (!CollectionUtils.isEmpty(constraints)) {
        for (DomainType constraint : constraints) {
          if (CONFORMANCE_CONSTRAINTS.ImplementsSorting.equals(
              CONFORMANCE_CONSTRAINTS.valueOf(constraint.getName()))) {
            configureSorting(constraint);
          }
        }
      }
    }
  }

  private void configureSorting(DomainType constraint) {
    if (constraint.getNoValues() != null && constraint.getDefaultValue() != null) {
      if (StringUtils.equalsIgnoreCase(
          constraint.getDefaultValue().getValue(), Boolean.TRUE.toString())) {
        this.isSortingSupported = true;
      } else if (StringUtils.equalsIgnoreCase(
          constraint.getDefaultValue().getValue(), Boolean.FALSE.toString())) {
        this.isSortingSupported = false;
      }
    }

    if (constraint.getAllowedValues() != null) {
      this.isSortingSupported = true;
      AllowedValues allowedValues = constraint.getAllowedValues();
      List<Object> values = allowedValues.getValueOrRange();
      for (Object value : values) {
        if (value instanceof ValueType) {
          String sortOrder = ((ValueType) value).getValue();
          // Could be ASC, ASCENDING, etc.
          if (StringUtils.startsWithIgnoreCase(sortOrder, "A")) {
            allowedSortOrders.add(SortOrder.ASCENDING);
          } else if (StringUtils.startsWithIgnoreCase(sortOrder, "D")) {
            allowedSortOrders.add(SortOrder.DESCENDING);
          }
        }
      }
    }
  }

  @Override
  public FilterType and(List<FilterType> filtersToBeAnded) {
    filtersToBeAnded = applyTemporalFallbacks(filtersToBeAnded);
    return buildAndOrFilter(
        filtersToBeAnded, filterObjectFactory.createAnd(new BinaryLogicOpType()));
  }

  @Override
  public FilterType or(List<FilterType> filtersToBeOred) {
    if (filtersToBeOred.contains(null)) {
      throw new UnsupportedOperationException("Invalid filters found in list of filters!");
    }
    return buildAndOrFilter(filtersToBeOred, filterObjectFactory.createOr(new BinaryLogicOpType()));
  }

  @Override
  public FilterType not(FilterType filterToBeNoted) {
    areLogicalOperationsSupported();
    FilterType returnFilter = new FilterType();
    if (filterToBeNoted == null) {
      return null;
    }
    UnaryLogicOpType notType = new UnaryLogicOpType();
    if (filterToBeNoted.isSetComparisonOps()) {
      notType.setComparisonOps(filterToBeNoted.getComparisonOps());
    } else if (filterToBeNoted.isSetLogicOps()) {
      notType.setLogicOps(filterToBeNoted.getLogicOps());
    } else if (filterToBeNoted.isSetSpatialOps()) {
      notType.setSpatialOps(filterToBeNoted.getSpatialOps());
    } else {
      return returnFilter;
    }
    returnFilter.setLogicOps(filterObjectFactory.createNot(notType));
    return returnFilter;
  }

  private Set<String> getFeatureIds(List<FilterType> filters) {
    Set<String> ids = new HashSet<String>();

    // This filter delegate requires that if one filter is a featureId
    // filter, they
    // must all be.
    if (!CollectionUtils.isEmpty(filters)) {
      boolean isFeatureIdFilter = filters.get(0) != null && filters.get(0).isSetId();

      for (FilterType filterType : filters) {

        if ((filterType != null && filterType.isSetId()) != isFeatureIdFilter) {
          throw new UnsupportedOperationException(
              "Query with mix of feature ID and non-feature ID queries not supported");
        }
        if (isFeatureIdFilter) {
          List<JAXBElement<? extends AbstractIdType>> idFilterTypeList = filterType.getId();
          for (JAXBElement<? extends AbstractIdType> idFilter : idFilterTypeList) {

            AbstractIdType absId = idFilter.getValue();

            ResourceIdType resId = (ResourceIdType) absId;
            ids.add(resId.getRid());
          }
        }
      }
    }

    return ids;
  }

  private FilterType buildFeatureIdFilter(Set<String> ids) {
    FilterType filterType = new FilterType();

    for (String id : ids) {
      List<JAXBElement<? extends AbstractIdType>> idFilterTypeList = filterType.getId();
      ResourceIdType resId = new ResourceIdType();
      resId.setRid(id);
      idFilterTypeList.add(filterObjectFactory.createResourceId(resId));
    }
    return filterType;
  }

  private FilterType buildAndOrFilter(
      List<FilterType> filters, JAXBElement<BinaryLogicOpType> andOrFilter) {

    areLogicalOperationsSupported();

    if (filters == null || filters.isEmpty()) {
      return null;
    }
    removeEmptyFilters(filters);

    // Check if these filters contain featureID(s)
    Set<String> featureIds = getFeatureIds(filters);

    if (!CollectionUtils.isEmpty(featureIds)) {
      return buildFeatureIdFilter(featureIds);
    }

    // If we have 1 filter don't wrap it with AND/OR
    if (filters.size() == 1) {
      return filters.get(0);
    }

    for (FilterType filterType : filters) {
      // Determine which filterType is set
      if (filterType.isSetComparisonOps()) {
        andOrFilter
            .getValue()
            .getComparisonOpsOrSpatialOpsOrTemporalOps()
            .add(filterType.getComparisonOps());
      } else if (filterType.isSetLogicOps()) {
        andOrFilter
            .getValue()
            .getComparisonOpsOrSpatialOpsOrTemporalOps()
            .add(filterType.getLogicOps());
      } else if (filterType.isSetSpatialOps()) {
        andOrFilter
            .getValue()
            .getComparisonOpsOrSpatialOpsOrTemporalOps()
            .add(filterType.getSpatialOps());
      } else if (filterType.isSetTemporalOps()) {
        andOrFilter
            .getValue()
            .getComparisonOpsOrSpatialOpsOrTemporalOps()
            .add(filterType.getTemporalOps());
      }
    }
    FilterType returnFilter = new FilterType();
    returnFilter.setLogicOps(andOrFilter);

    return returnFilter;
  }

  protected List<FilterType> applyTemporalFallbacks(List<FilterType> filters) {
    if (null == filters || filters.isEmpty()) {
      return filters;
    }
    String startDate = "";
    String endDate = "";
    String property = "";
    List<FilterType> newFilters = new ArrayList<>();

    for (FilterType filterType : filters) {
      if (null == filterType) {
        continue;
      }
      if (filterType.isSetTemporalOps()
          && (!isTemporalOpSupported(TEMPORAL_OPERATORS.Before)
              && !isTemporalOpSupported(TEMPORAL_OPERATORS.After))
          && !isDuringFilter(filterType)) {
        BinaryTemporalOpType binaryTemporalOpType =
            (BinaryTemporalOpType) filterType.getTemporalOps().getValue();
        property = binaryTemporalOpType.getValueReference();
        JAXBElement temporalExpression = binaryTemporalOpType.getExpression();
        TimeInstantType timeInstant = (TimeInstantType) temporalExpression.getValue();
        TimePositionType timePositionType = timeInstant.getTimePosition();
        List<String> value = timePositionType.getValue();

        if (isAfterFilter(filterType)) {
          startDate = value.get(0);
        } else if (isBeforeFilter(filterType)) {
          endDate = value.get(0);
        }
      } else {
        newFilters.add(filterType);
      }
    }

    if (isTemporalOpSupported(TEMPORAL_OPERATORS.During) && (StringUtils.isNotEmpty(startDate))) {
      if (StringUtils.isEmpty(endDate)) {
        endDate = convertDateToIso8601Format(new Date());
      }

      FilterType duringFilter =
          buildDuringFilterType(
              featureMetacardType.getFeatureType().getLocalPart() + "." + property,
              startDate,
              endDate);
      newFilters.add(duringFilter);
    } else if (isTemporalOpSupported(TEMPORAL_OPERATORS.During)
        && (StringUtils.isEmpty(startDate) && StringUtils.isNotEmpty(endDate))) {

      for (FilterType filterType : filters) {
        if (!filterType.isSetTemporalOps()) {
          BinaryLogicOpType binaryLogicOpType =
              (BinaryLogicOpType) filterType.getLogicOps().getValue();
          List<JAXBElement<?>> list = binaryLogicOpType.getComparisonOpsOrSpatialOpsOrTemporalOps();
          for (JAXBElement<?> element : list) {

            if (StringUtils.contains(
                element.getDeclaredType().toString(), ("BinaryTemporalOpType"))) {

              BinaryTemporalOpType binTemp = (BinaryTemporalOpType) element.getValue();
              JAXBElement temporalExpression = binTemp.getExpression();
              TimePeriodType timePeriod = (TimePeriodType) temporalExpression.getValue();
              TimePositionType timeEndPosition = timePeriod.getEndPosition();
              List<String> newValue = new ArrayList<String>();
              newValue.add(endDate);
              timeEndPosition.unsetValue();
              timeEndPosition.setValue(newValue);
              timePeriod.setEndPosition(timeEndPosition);
            }
          }

        } else if ((!isTemporalOpSupported(TEMPORAL_OPERATORS.Before)
                && !isTemporalOpSupported(TEMPORAL_OPERATORS.After))
            && isDuringFilter(filterType)) {
          BinaryTemporalOpType binaryTemporalOpType =
              (BinaryTemporalOpType) filterType.getTemporalOps().getValue();
          JAXBElement temporalExpression = binaryTemporalOpType.getExpression();
          TimePeriodType timePeriod = (TimePeriodType) temporalExpression.getValue();
          TimePositionType timeEndPosition = timePeriod.getEndPosition();
          List<String> newValue = new ArrayList<String>();
          newValue.add(endDate);
          timeEndPosition.unsetValue();
          timeEndPosition.setValue(newValue);
          timePeriod.setEndPosition(timeEndPosition);
        }
      }
    }
    return newFilters;
  }

  private void removeEmptyFilters(List<FilterType> filters) {
    // Loop through the filters and remove any empty filters
    List<FilterType> filtersToBeRemoved = new ArrayList<FilterType>(filters.size());
    Boolean foundInvalidFilter = false;
    for (FilterType filterType : filters) {
      if (filterType == null) {
        foundInvalidFilter = true;
      } else if (!isFilterSet(filterType)) {
        filtersToBeRemoved.add(filterType);
      }
    }
    // If we found an invalid filter we want to return an invalid filter.
    if (foundInvalidFilter) {
      filters.clear();
      filters.add(null);
    } else {
      filters.removeAll(filtersToBeRemoved);
      filters.removeAll(Collections.singleton(null));
      if (filters.isEmpty()) {
        filters.add(new FilterType());
      }
    }
  }

  private Boolean isFilterSet(FilterType filter) {
    return (filter.isSetComparisonOps()
        || filter.isSetLogicOps()
        || filter.isSetSpatialOps()
        || filter.isSetId()
        || filter.isSetTemporalOps());
  }

  @Override
  public FilterType propertyIsEqualTo(
      String propertyName, String literal, boolean isCaseSensitive) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, Date date) {
    return buildPropertyIsFilterType(
        propertyName, convertDateToIso8601Format(date), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, int literal) {
    return buildPropertyIsFilterType(
        propertyName, Integer.valueOf(literal), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, short literal) {
    return buildPropertyIsFilterType(
        propertyName, Short.valueOf(literal), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, long literal) {
    return buildPropertyIsFilterType(
        propertyName, Long.valueOf(literal), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, float literal) {
    return buildPropertyIsFilterType(
        propertyName, Float.valueOf(literal), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, double literal) {
    return buildPropertyIsFilterType(
        propertyName, Double.valueOf(literal), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsEqualTo(String propertyName, boolean literal) {
    return buildPropertyIsFilterType(
        propertyName, Boolean.valueOf(literal), PROPERTY_IS_OPS.PropertyIsEqualTo);
  }

  @Override
  public FilterType propertyIsLike(String propertyName, String literal, boolean isCaseSensitive) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsLike);
  }

  @Override
  public FilterType propertyIsNotEqualTo(
      String propertyName, String literal, boolean isCaseSensitive) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsNotEqualTo);
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, Date literal) {
    return buildPropertyIsFilterType(
        propertyName, convertDateToIso8601Format(literal), PROPERTY_IS_OPS.PropertyIsNotEqualTo);
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, Number literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsNotEqualTo);
  }

  @Override
  public FilterType propertyIsNotEqualTo(String propertyName, boolean literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsNotEqualTo);
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, String literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsGreaterThan);
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, Date literal) {
    return buildPropertyIsFilterType(
        propertyName, convertDateToIso8601Format(literal), PROPERTY_IS_OPS.PropertyIsGreaterThan);
  }

  @Override
  public FilterType propertyIsGreaterThan(String propertyName, Number literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsGreaterThan);
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, String literal) {
    return buildPropertyIsFilterType(
        propertyName, literal, PROPERTY_IS_OPS.PropertyIsGreaterThanOrEqualTo);
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, Date literal) {
    return buildPropertyIsFilterType(
        propertyName,
        convertDateToIso8601Format(literal),
        PROPERTY_IS_OPS.PropertyIsGreaterThanOrEqualTo);
  }

  @Override
  public FilterType propertyIsGreaterThanOrEqualTo(String propertyName, Number literal) {
    return buildPropertyIsFilterType(
        propertyName, literal, PROPERTY_IS_OPS.PropertyIsGreaterThanOrEqualTo);
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, String literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsLessThan);
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, Date literal) {
    return buildPropertyIsFilterType(
        propertyName, convertDateToIso8601Format(literal), PROPERTY_IS_OPS.PropertyIsLessThan);
  }

  @Override
  public FilterType propertyIsLessThan(String propertyName, Number literal) {
    return buildPropertyIsFilterType(propertyName, literal, PROPERTY_IS_OPS.PropertyIsLessThan);
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, String literal) {
    return buildPropertyIsFilterType(
        propertyName, literal, PROPERTY_IS_OPS.PropertyIsLessThanOrEqualTo);
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, Date literal) {
    return buildPropertyIsFilterType(
        propertyName,
        convertDateToIso8601Format(literal),
        PROPERTY_IS_OPS.PropertyIsLessThanOrEqualTo);
  }

  @Override
  public FilterType propertyIsLessThanOrEqualTo(String propertyName, Number literal) {
    return buildPropertyIsFilterType(
        propertyName, literal, PROPERTY_IS_OPS.PropertyIsLessThanOrEqualTo);
  }

  @Override
  public FilterType propertyIsBetween(
      String propertyName, String lowerBoundary, String upperBoundary) {
    return buildPropertyIsBetweenFilterType(propertyName, lowerBoundary, upperBoundary);
  }

  @Override
  public FilterType propertyIsBetween(String propertyName, Date lowerBoundary, Date upperBoundary) {
    return buildPropertyIsBetweenFilterType(
        propertyName,
        convertDateToIso8601Format(lowerBoundary),
        convertDateToIso8601Format(upperBoundary));
  }

  @Override
  public FilterType during(String propertyName, Date startDate, Date endDate) {
    return buildDuringFilterType(
        mapMetacardAttribute(propertyName),
        convertDateToIso8601Format(startDate),
        convertDateToIso8601Format(endDate));
  }

  @Override
  public FilterType relative(String propertyName, long duration) {
    DateTime now = new DateTime();
    DateTime startDate = now.minus(duration);
    return buildDuringFilterType(
        mapMetacardAttribute(propertyName),
        convertDateToIso8601Format(startDate),
        convertDateToIso8601Format(now));
  }

  @Override
  public FilterType after(String propertyName, Date date) {
    return buildAfterFilterType(
        mapMetacardAttribute(propertyName), convertDateToIso8601Format(date));
  }

  @Override
  public FilterType before(String propertyName, Date date) {
    return buildBeforeFilterType(
        mapMetacardAttribute(propertyName), convertDateToIso8601Format(date));
  }

  @Override
  public FilterType propertyIsBetween(
      String propertyName, Number lowerBoundary, Number upperBoundary) {
    return buildPropertyIsBetweenFilterType(propertyName, lowerBoundary, upperBoundary);
  }

  protected boolean isAfterFilter(FilterType filter) {
    if (isFilterSet(filter)) {
      return TEMPORAL_OPERATORS.After.toString()
          .equals(filter.getTemporalOps().getName().getLocalPart());
    } else {
      return false;
    }
  }

  protected boolean isBeforeFilter(FilterType filter) {
    if (isFilterSet(filter)) {
      return TEMPORAL_OPERATORS.Before.toString()
          .equals(filter.getTemporalOps().getName().getLocalPart());
    } else {
      return false;
    }
  }

  protected boolean isDuringFilter(FilterType filter) {
    if (isFilterSet(filter)) {
      return TEMPORAL_OPERATORS.During.toString()
          .equals(filter.getTemporalOps().getName().getLocalPart());
    } else {
      return false;
    }
  }

  private FilterType buildPropertyIsBetweenFilterType(
      String propertyName, Object lowerBoundary, Object upperBoundary) {

    if (!isValidInputParameters(propertyName, lowerBoundary, upperBoundary)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    FilterType filter = new FilterType();

    if (featureMetacardType.getProperties().contains(propertyName)) {
      FeatureAttributeDescriptor featureAttributeDescriptor =
          (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(propertyName);
      if (featureAttributeDescriptor.isIndexed()) {
        filter.setComparisonOps(
            createPropertyIsBetween(
                featureAttributeDescriptor.getPropertyName(), lowerBoundary, upperBoundary));
      } else {
        throw new IllegalArgumentException(String.format(PROPERTY_NOT_QUERYABLE, propertyName));
      }
    } else {
      return null;
    }
    return filter;
  }

  private FilterType buildDuringFilterType(String propertyName, String startDate, String endDate) {

    if (!isTemporalOpSupported(TEMPORAL_OPERATORS.During)) {
      throw new UnsupportedOperationException(
          "Temporal Operator [" + TEMPORAL_OPERATORS.During + "] is not supported.");
    }

    TemporalOperand timePeriodTemporalOperand = new TemporalOperand();
    timePeriodTemporalOperand.setName(
        new QName(Wfs20Constants.GML_3_2_NAMESPACE, Wfs20Constants.TIME_PERIOD));
    if (!isTemporalOperandSupported(timePeriodTemporalOperand)) {
      throw new UnsupportedOperationException(
          "Temporal Operand [" + timePeriodTemporalOperand.getName() + "] is not supported.");
    }

    if (!isValidInputParameters(propertyName, startDate, endDate)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (!isPropertyTemporalType(propertyName)) {
      throw new IllegalArgumentException(
          "Property ["
              + propertyName
              + "] is not of type "
              + timePeriodTemporalOperand.getName()
              + ".");
    }

    FilterType filter = filterObjectFactory.createFilterType();

    if (featureMetacardType.getProperties().contains(propertyName)) {
      FeatureAttributeDescriptor featureAttributeDescriptor =
          (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(propertyName);
      if (featureAttributeDescriptor.isIndexed()) {
        filter.setTemporalOps(
            createDuring(
                featureAttributeDescriptor.getPropertyName(),
                featureMetacardType.getName(),
                startDate,
                endDate));
      } else {
        throw new IllegalArgumentException(String.format(PROPERTY_NOT_QUERYABLE, propertyName));
      }
    } else {
      return null;
    }
    return filter;
  }

  private FilterType buildAfterFilterType(String propertyName, String date) {

    TemporalOperand timeInstantTemporalOperand = new TemporalOperand();
    timeInstantTemporalOperand.setName(
        new QName(Wfs20Constants.GML_3_2_NAMESPACE, Wfs20Constants.TIME_INSTANT));
    if (!isTemporalOperandSupported(timeInstantTemporalOperand)) {
      throw new UnsupportedOperationException(
          "Temporal Operand [" + timeInstantTemporalOperand.getName() + "] is not supported.");
    }

    if (!isValidInputParameters(propertyName, date)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (!isPropertyTemporalType(propertyName)) {
      throw new IllegalArgumentException(
          "Property ["
              + propertyName
              + "] is not of type "
              + timeInstantTemporalOperand.getName()
              + ".");
    }

    FilterType filter = filterObjectFactory.createFilterType();

    if (featureMetacardType.getProperties().contains(propertyName)) {
      FeatureAttributeDescriptor featureAttributeDescriptor =
          (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(propertyName);
      if (featureAttributeDescriptor.isIndexed()) {
        filter.setTemporalOps(
            createAfter(
                featureAttributeDescriptor.getPropertyName(), featureMetacardType.getName(), date));
      } else {
        throw new IllegalArgumentException(String.format(PROPERTY_NOT_QUERYABLE, propertyName));
      }
    } else {
      return null;
    }

    if (!isTemporalOpSupported(TEMPORAL_OPERATORS.After)) {
      if (isTemporalOpSupported(TEMPORAL_OPERATORS.During)) {
        return buildDuringFilterType(propertyName, date, convertDateToIso8601Format(new Date()));
      }
    }

    return filter;
  }

  private FilterType buildBeforeFilterType(String propertyName, String date) {

    TemporalOperand timeInstantTemporalOperand = new TemporalOperand();
    timeInstantTemporalOperand.setName(
        new QName(Wfs20Constants.GML_3_2_NAMESPACE, Wfs20Constants.TIME_INSTANT));
    if (!isTemporalOperandSupported(timeInstantTemporalOperand)) {
      throw new UnsupportedOperationException(
          "Temporal Operand [" + timeInstantTemporalOperand.getName() + "] is not supported.");
    }

    if (!isValidInputParameters(propertyName, date)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (!isPropertyTemporalType(propertyName)) {
      throw new IllegalArgumentException(
          "Property ["
              + propertyName
              + "] is not of type "
              + timeInstantTemporalOperand.getName()
              + ".");
    }

    FilterType filter = filterObjectFactory.createFilterType();

    if (featureMetacardType.getProperties().contains(propertyName)) {
      FeatureAttributeDescriptor featureAttributeDescriptor =
          (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(propertyName);
      if (featureAttributeDescriptor.isIndexed()) {
        filter.setTemporalOps(
            createBefore(
                featureAttributeDescriptor.getPropertyName(), featureMetacardType.getName(), date));
      } else {
        throw new IllegalArgumentException(String.format(PROPERTY_NOT_QUERYABLE, propertyName));
      }
    } else {
      return null;
    }
    return filter;
  }

  private FilterType buildPropertyIsFilterType(
      String propertyName, Object literal, PROPERTY_IS_OPS propertyIsType) {
    if (!isValidInputParameters(propertyName, literal)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }
    FilterType returnFilter = new FilterType();

    if (Metacard.CONTENT_TYPE.equals(propertyName)) {
      return returnFilter;
    }
    // Special Case - If we get an ANY_TEXT we want to convert that to a
    // series of OR's
    if ((Metacard.ANY_TEXT.equalsIgnoreCase(propertyName))) {
      if (CollectionUtils.isEmpty(featureMetacardType.getTextualProperties())) {
        LOGGER.debug("Feature Type does not have Textual Properties to query.");
        return null;
      }

      if (featureMetacardType.getTextualProperties().size() == 1) {
        FeatureAttributeDescriptor attrDescriptor =
            (FeatureAttributeDescriptor)
                featureMetacardType.getAttributeDescriptor(
                    featureMetacardType.getTextualProperties().get(0));
        if (attrDescriptor.isIndexed()) {
          returnFilter.setComparisonOps(
              createPropertyIsFilter(attrDescriptor.getPropertyName(), literal, propertyIsType));
        } else {
          LOGGER.debug("All textual properties have been blacklisted.  Removing from query.");
          return null;
        }
      } else {
        List<FilterType> binaryCompOpsToBeOred = new ArrayList<FilterType>();
        for (String property : featureMetacardType.getTextualProperties()) {
          // only build filters for queryable properties
          FeatureAttributeDescriptor attrDesc =
              (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(property);
          if (attrDesc.isIndexed()) {
            FilterType filter = new FilterType();
            filter.setComparisonOps(
                createPropertyIsFilter(attrDesc.getPropertyName(), literal, propertyIsType));
            binaryCompOpsToBeOred.add(filter);
          } else {
            LOGGER.debug(PROPERTY_NOT_QUERYABLE_ARG, property);
          }
        }
        if (!binaryCompOpsToBeOred.isEmpty()) {
          returnFilter = or(binaryCompOpsToBeOred);
        } else {
          LOGGER.debug("All textual properties have been blacklisted.  Removing from query.");
          return null;
        }
      }
      // filter is for a specific property; check to see if it is valid
    } else if (featureMetacardType.getProperties().contains(propertyName)) {
      FeatureAttributeDescriptor attrDesc =
          (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(propertyName);
      if (attrDesc.isIndexed()) {
        returnFilter.setComparisonOps(
            createPropertyIsFilter(attrDesc.getPropertyName(), literal, propertyIsType));
      } else {
        // blacklisted property encountered
        throw new IllegalArgumentException(String.format(PROPERTY_NOT_QUERYABLE, propertyName));
      }
    } else if (Metacard.ID.equals(propertyName)) {
      LOGGER.debug("feature id query for : {}", literal);
      String[] idTokens = literal.toString().split("\\.");
      if (idTokens.length > 1) {
        if (idTokens[0].equals(featureMetacardType.getName())) {
          LOGGER.debug("feature type matches metacard type; creating featureID filter");
          returnFilter.getId().add(createFeatureIdFilter(literal.toString()));
        } else {
          LOGGER.debug("feature type does not match metacard type; invalidating filter");
          return null;
        }
      } else {
        returnFilter.getId().add(createFeatureIdFilter(literal.toString()));
      }

    } else {
      return null;
    }
    return returnFilter;
  }

  private JAXBElement<? extends ComparisonOpsType> createPropertyIsFilter(
      String property, Object literal, PROPERTY_IS_OPS operation) {
    switch (operation) {
      case PropertyIsEqualTo:
        JAXBElement<BinaryComparisonOpType> propIsEqualTo =
            filterObjectFactory.createPropertyIsEqualTo(new BinaryComparisonOpType());
        propIsEqualTo.getValue().getExpression().add(createPropertyNameType(property));
        propIsEqualTo.getValue().getExpression().add(createLiteralType(literal));

        return propIsEqualTo;

      case PropertyIsNotEqualTo:
        JAXBElement<BinaryComparisonOpType> propIsNotEqualTo =
            filterObjectFactory.createPropertyIsNotEqualTo(new BinaryComparisonOpType());
        propIsNotEqualTo.getValue().getExpression().add(createPropertyNameType(property));
        propIsNotEqualTo.getValue().getExpression().add(createLiteralType(literal));

        return propIsNotEqualTo;

      case PropertyIsGreaterThan:
        JAXBElement<BinaryComparisonOpType> propIsGreaterThan =
            filterObjectFactory.createPropertyIsGreaterThan(new BinaryComparisonOpType());
        propIsGreaterThan.getValue().getExpression().add(createPropertyNameType(property));
        propIsGreaterThan.getValue().getExpression().add(createLiteralType(literal));

        return propIsGreaterThan;

      case PropertyIsGreaterThanOrEqualTo:
        JAXBElement<BinaryComparisonOpType> propIsGreaterThanOrEqualTo =
            filterObjectFactory.createPropertyIsGreaterThanOrEqualTo(new BinaryComparisonOpType());
        propIsGreaterThanOrEqualTo.getValue().getExpression().add(createPropertyNameType(property));
        propIsGreaterThanOrEqualTo.getValue().getExpression().add(createLiteralType(literal));

        return propIsGreaterThanOrEqualTo;

      case PropertyIsLessThan:
        JAXBElement<BinaryComparisonOpType> propIsLessThan =
            filterObjectFactory.createPropertyIsLessThan(new BinaryComparisonOpType());
        propIsLessThan.getValue().getExpression().add(createPropertyNameType(property));
        propIsLessThan.getValue().getExpression().add(createLiteralType(literal));

        return propIsLessThan;

      case PropertyIsLessThanOrEqualTo:
        JAXBElement<BinaryComparisonOpType> propIsLessThanOrEqualTo =
            filterObjectFactory.createPropertyIsLessThanOrEqualTo(new BinaryComparisonOpType());
        propIsLessThanOrEqualTo.getValue().getExpression().add(createPropertyNameType(property));
        propIsLessThanOrEqualTo.getValue().getExpression().add(createLiteralType(literal));

        return propIsLessThanOrEqualTo;

      case PropertyIsLike:
        JAXBElement<PropertyIsLikeType> propIsLike =
            filterObjectFactory.createPropertyIsLike(new PropertyIsLikeType());

        propIsLike.getValue().setEscapeChar(Wfs20Constants.ESCAPE);
        propIsLike.getValue().setSingleChar(SINGLE_CHAR);
        propIsLike.getValue().setWildCard(Wfs20Constants.WILD_CARD);
        propIsLike.getValue().getExpression().add(createLiteralType(literal));
        propIsLike.getValue().getExpression().add(createPropertyNameType(property));
        return propIsLike;

      default:
        throw new UnsupportedOperationException("Unsupported Property Comparison Type");
    }
  }

  private JAXBElement<PropertyIsBetweenType> createPropertyIsBetween(
      String property, Object lowerBoundary, Object upperBoundary) {
    PropertyIsBetweenType propertyIsBetween = new PropertyIsBetweenType();
    propertyIsBetween.setLowerBoundary(createLowerBoundary(lowerBoundary));
    propertyIsBetween.setUpperBoundary(createUpperBoundary(upperBoundary));
    propertyIsBetween.setExpression(createPropertyNameType(property));

    return filterObjectFactory.createPropertyIsBetween(propertyIsBetween);
  }

  private JAXBElement<BinaryTemporalOpType> createDuring(
      String property, String type, String startDate, String endDate) {
    JAXBElement<BinaryTemporalOpType> during =
        filterObjectFactory.createDuring(
            createBinaryTemporalOpType(property, type, startDate, endDate));
    return during;
  }

  private JAXBElement<BinaryTemporalOpType> createAfter(String property, String type, String date) {
    JAXBElement<BinaryTemporalOpType> after =
        filterObjectFactory.createAfter(createBinaryTemporalOpType(property, type, date));
    return after;
  }

  private JAXBElement<BinaryTemporalOpType> createBefore(
      String property, String type, String date) {
    JAXBElement<BinaryTemporalOpType> before =
        filterObjectFactory.createBefore(createBinaryTemporalOpType(property, type, date));
    return before;
  }

  private BinaryTemporalOpType createBinaryTemporalOpType(
      String property, String type, String startDate, String endDate) {
    BinaryTemporalOpType binaryTemporalOpType = filterObjectFactory.createBinaryTemporalOpType();
    binaryTemporalOpType.setValueReference(property);
    binaryTemporalOpType.setExpression(
        gml320ObjectFactory.createTimePeriod(createTimePeriodType(type, startDate, endDate)));

    return binaryTemporalOpType;
  }

  private BinaryTemporalOpType createBinaryTemporalOpType(
      String property, String type, String date) {
    BinaryTemporalOpType binaryTemporalOpType = filterObjectFactory.createBinaryTemporalOpType();
    binaryTemporalOpType.setValueReference(property);
    binaryTemporalOpType.setExpression(
        gml320ObjectFactory.createTimeInstant(createTimeInstantType(type, date)));

    return binaryTemporalOpType;
  }

  private TimePositionType createTimePositionType(String dateTime) {

    TimePositionType timePosition = gml320ObjectFactory.createTimePositionType();
    timePosition.getValue().add(dateTime);
    return timePosition;
  }

  private TimePeriodType createTimePeriodType(String type, String startDate, String endDate) {

    TimePeriodType timePeriodType = gml320ObjectFactory.createTimePeriodType();
    timePeriodType.setBeginPosition(createTimePositionType(startDate));
    timePeriodType.setEndPosition(createTimePositionType(endDate));
    timePeriodType.setId(type + "." + System.currentTimeMillis());
    return timePeriodType;
  }

  private TimeInstantType createTimeInstantType(String type, String date) {
    TimeInstantType timeInstantType = gml320ObjectFactory.createTimeInstantType();
    timeInstantType.setTimePosition(createTimePositionType(date));
    timeInstantType.setId(type + "." + System.currentTimeMillis());
    return timeInstantType;
  }

  private JAXBElement<ResourceIdType> createFeatureIdFilter(final String id) {
    ResourceIdType resId = new ResourceIdType();
    resId.setRid(id);
    ObjectFactory objFact = new ObjectFactory();

    return objFact.createResourceId(resId);
  }

  private boolean isValidInputParameters(String propertyName, Object literal) {
    if (literal == null
        || StringUtils.isEmpty(propertyName)
        || StringUtils.isEmpty(literal.toString())) {
      return false;
    }
    return true;
  }

  private boolean isValidInputParameters(String propertyName, String literal, double distance) {
    boolean isValid = isValidInputParameters(propertyName, literal);
    if (Double.valueOf(distance) < 0) {
      isValid = false;
    }
    return isValid;
  }

  private boolean isValidInputParameters(
      String propertyName, Object lowerBoundary, Object upperBoundary) {

    if (lowerBoundary == null || upperBoundary == null) {
      return false;
    }
    if (StringUtils.isEmpty(propertyName)
        || StringUtils.isEmpty(lowerBoundary.toString())
        || StringUtils.isEmpty(upperBoundary.toString())) {

      return false;
    }
    return true;
  }

  private boolean isPropertyTemporalType(String propertyName) {
    return featureMetacardType.getTemporalProperties().contains(propertyName);
  }

  private String convertDateToIso8601Format(DateTime inputDate) {
    DateTimeFormatter dtf = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC);
    return dtf.print(inputDate).toString();
  }

  private String convertDateToIso8601Format(Date inputDate) {
    DateTimeFormatter dtf = ISODateTimeFormat.dateTimeNoMillis().withZone(DateTimeZone.UTC);
    return dtf.print(new DateTime(inputDate)).toString();
  }

  // spatial operators
  @Override
  public FilterType beyond(String propertyName, String wkt, double distance) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt, distance)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (spatialOps.containsKey(SPATIAL_OPERATORS.Beyond)) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Beyond.toString(), propertyName, wkt, distance);
    } else if (spatialOps.containsKey(SPATIAL_OPERATORS.DWithin)) {
      return not(dwithin(propertyName, wkt, distance));
    } else {
      LOGGER.debug("WFS Source does not support Beyond filters");
      return null;
    }
  }

  @Override
  public FilterType contains(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (spatialOps.containsKey(SPATIAL_OPERATORS.Contains)) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Contains.toString(), propertyName, wkt, null);
    } else if (spatialOps.containsKey(SPATIAL_OPERATORS.Within)) {
      return not(within(propertyName, wkt));
    } else {
      LOGGER.debug("WFS Source does not support Contains filters");
      return null;
    }
  }

  @Override
  public FilterType crosses(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (spatialOps.containsKey(SPATIAL_OPERATORS.Crosses)) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Crosses.toString(), propertyName, wkt, null);
    } else {
      LOGGER.debug("WFS Source does not support Crosses filters");
      return null;
    }
  }

  @Override
  public FilterType disjoint(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (spatialOps.containsKey(SPATIAL_OPERATORS.Disjoint)) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Disjoint.toString(), propertyName, wkt, null);
    } else if (spatialOps.containsKey(SPATIAL_OPERATORS.BBOX)) {
      return not(bbox(propertyName, wkt));
    } else if (spatialOps.containsKey(SPATIAL_OPERATORS.Intersects)) {
      return not(intersects(propertyName, wkt));
    } else {
      LOGGER.debug("WFS Source does not support Disjoint or BBOX filters");
      return null;
    }
  }

  @Override
  public FilterType dwithin(String propertyName, String wkt, double distance) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt, distance)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (spatialOps.containsKey(SPATIAL_OPERATORS.DWithin)) {
      return this.buildGeospatialFilterType(
          SPATIAL_OPERATORS.DWithin.toString(), propertyName, wkt, distance);
    } else if (spatialOps.containsKey(SPATIAL_OPERATORS.Beyond)) {
      return not(beyond(propertyName, wkt, distance));
    } else if (spatialOps.containsKey(SPATIAL_OPERATORS.Intersects)) {
      String bufferedWkt = bufferGeometry(wkt, distance);
      return intersects(propertyName, bufferedWkt);
    } else {
      LOGGER.debug(
          "WFS Source does not support the DWithin filter or any of its fallback filters (Not Beyond or Intersects).");
      return null;
    }
  }

  @Override
  public FilterType intersects(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (spatialOps.containsKey(SPATIAL_OPERATORS.Intersects)) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Intersects.toString(), propertyName, wkt, null);
    } else if (spatialOps.containsKey(SPATIAL_OPERATORS.BBOX)) {
      return bbox(propertyName, wkt);
    } else if (spatialOps.containsKey(SPATIAL_OPERATORS.Disjoint)) {
      return not(disjoint(propertyName, wkt));
    } else {
      LOGGER.debug("WFS Source does not support Intersect or BBOX");
      return null;
    }
  }

  @Override
  public FilterType overlaps(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (spatialOps.containsKey(SPATIAL_OPERATORS.Overlaps)) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Overlaps.toString(), propertyName, wkt, null);
    } else {
      LOGGER.debug("WFS Source does not support Overlaps filters");
      return null;
    }
  }

  @Override
  public FilterType touches(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (spatialOps.containsKey(SPATIAL_OPERATORS.Touches)) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Touches.toString(), propertyName, wkt, null);
    } else {
      LOGGER.debug("WFS Source does not support Beyond filters");
      return null;
    }
  }

  @Override
  public FilterType within(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (spatialOps.containsKey(SPATIAL_OPERATORS.Within)) {
      return buildGeospatialFilterType(
          SPATIAL_OPERATORS.Within.toString(), propertyName, wkt, null);
    } else if (spatialOps.containsKey(SPATIAL_OPERATORS.Contains)) {
      return contains(propertyName, wkt);
    } else {
      LOGGER.debug("WFS Source does not support Within filters");
      return null;
    }
  }

  private FilterType bbox(String propertyName, String wkt) {
    if (!isEpsg4326) {
      return null;
    }

    if (!isValidInputParameters(propertyName, wkt)) {
      throw new IllegalArgumentException(MISSING_PARAMETERS_MSG);
    }

    if (spatialOps.containsKey(SPATIAL_OPERATORS.BBOX)) {
      return buildGeospatialFilterType(SPATIAL_OPERATORS.BBOX.toString(), propertyName, wkt, null);
    } else {
      LOGGER.debug("WFS Source does not support BBOX filters");
      return null;
    }
  }

  private FilterType buildGeospatialFilterType(
      String spatialOpType, String propertyName, String wkt, Double distance) {
    FilterType returnFilter = new FilterType();
    if (Metacard.ANY_GEO.equals(propertyName)) {

      if (CollectionUtils.isEmpty(featureMetacardType.getGmlProperties())) {
        LOGGER.debug("Feature Type does not have GEO properties to query");
        return null;
      }

      if (featureMetacardType.getGmlProperties().size() == 1) {
        FeatureAttributeDescriptor attrDesc =
            (FeatureAttributeDescriptor)
                featureMetacardType.getAttributeDescriptor(
                    featureMetacardType.getGmlProperties().get(0));
        if (attrDesc != null && attrDesc.isIndexed()) {
          returnFilter.setSpatialOps(
              createSpatialOpType(spatialOpType, attrDesc.getPropertyName(), wkt, distance));
        } else {
          LOGGER.debug("All GEO properties have been blacklisted. Removing from query");
          return null;
        }

      } else {
        List<FilterType> filtersToBeOred = new ArrayList<FilterType>();
        for (String property : featureMetacardType.getGmlProperties()) {
          FeatureAttributeDescriptor attrDesc =
              (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(property);
          if (attrDesc != null && attrDesc.isIndexed()) {
            FilterType filter = new FilterType();
            filter.setSpatialOps(
                createSpatialOpType(spatialOpType, attrDesc.getPropertyName(), wkt, distance));
            filtersToBeOred.add(filter);
          } else {
            LOGGER.debug(PROPERTY_NOT_QUERYABLE_ARG, property);
          }
        }
        if (!filtersToBeOred.isEmpty()) {
          returnFilter = or(filtersToBeOred);
        } else {
          LOGGER.debug("All GEO properties have been blacklisted. Removing from query.");
          returnFilter = null;
        }
      }
    } else if (featureMetacardType.getGmlProperties().contains(propertyName)) {
      FeatureAttributeDescriptor attrDesc =
          (FeatureAttributeDescriptor) featureMetacardType.getAttributeDescriptor(propertyName);
      if (attrDesc != null && attrDesc.isIndexed()) {
        FilterType filter = new FilterType();
        filter.setSpatialOps(
            createSpatialOpType(spatialOpType, attrDesc.getPropertyName(), wkt, distance));
        return filter;
      } else {
        // blacklisted property encountered
        throw new IllegalArgumentException(String.format(PROPERTY_NOT_QUERYABLE, propertyName));
      }
    } else {
      return null;
    }
    return returnFilter;
  }

  private JAXBElement<? extends SpatialOpsType> createSpatialOpType(
      String operation, String propertyName, String wkt, Double distance) {

    switch (SPATIAL_OPERATORS.valueOf(operation)) {
      case BBOX:
        return buildBBoxType(propertyName, wkt);
      case Beyond:
        return buildDistanceBufferType(
            filterObjectFactory.createBeyond(new DistanceBufferType()),
            propertyName,
            wkt,
            distance);
      case Contains:
        return buildBinarySpatialOpType(
            filterObjectFactory.createContains(new BinarySpatialOpType()), propertyName, wkt);
      case Crosses:
        return buildBinarySpatialOpType(
            filterObjectFactory.createCrosses(new BinarySpatialOpType()), propertyName, wkt);
      case Disjoint:
        return buildBinarySpatialOpType(
            filterObjectFactory.createDisjoint(new BinarySpatialOpType()), propertyName, wkt);
      case DWithin:
        return buildDistanceBufferType(
            filterObjectFactory.createDWithin(new DistanceBufferType()),
            propertyName,
            wkt,
            distance);
      case Intersects:
        return buildBinarySpatialOpType(
            filterObjectFactory.createIntersects(new BinarySpatialOpType()), propertyName, wkt);
      case Overlaps:
        return buildBinarySpatialOpType(
            filterObjectFactory.createOverlaps(new BinarySpatialOpType()), propertyName, wkt);
      case Touches:
        return buildBinarySpatialOpType(
            filterObjectFactory.createTouches(new BinarySpatialOpType()), propertyName, wkt);
      case Within:
        return buildBinarySpatialOpType(
            filterObjectFactory.createWithin(new BinarySpatialOpType()), propertyName, wkt);
      default:
        throw new UnsupportedOperationException(
            "Unsupported geospatial filter type "
                + SPATIAL_OPERATORS.valueOf(operation)
                + " specified");
    }
  }

  private JAXBElement<BinarySpatialOpType> buildBinarySpatialOpType(
      JAXBElement<BinarySpatialOpType> bsot, String propertyName, String wkt) {
    bsot.getValue().setValueReference(propertyName);
    bsot.getValue().setExpression(createGeometryOperand(wkt));
    return bsot;
  }

  private JAXBElement<DistanceBufferType> buildDistanceBufferType(
      JAXBElement<DistanceBufferType> dbt, String propertyName, String wkt, double distance) {
    MeasureType measureType = new MeasureType();
    measureType.setValue(distance);
    // the filter adapter normalizes all distances to meters
    measureType.setUom(Wfs20Constants.METERS);
    dbt.getValue().setDistance(measureType);
    dbt.getValue().setExpression(filterObjectFactory.createValueReference(propertyName));
    dbt.getValue().setAny(createGeometryOperand(wkt));
    return dbt;
  }

  private JAXBElement<BBOXType> buildBBoxType(String propertyName, String wkt) {
    BBOXType bboxType = new BBOXType();
    bboxType.setExpression(filterObjectFactory.createValueReference(propertyName));
    try {
      bboxType.setAny(createEnvelope(getGeometryFromWkt(wkt)));
    } catch (ParseException e) {
      throw new UnsupportedOperationException("Unable to parse WKT Geometry [" + wkt + "]", e);
    }
    return filterObjectFactory.createBBOX(bboxType);
  }

  private JAXBElement<?> createGeometryOperand(String wkt) {
    String convertedWkt = convertWktToLatLonOrdering(wkt);
    Geometry wktGeometry = null;
    try {
      wktGeometry = getGeometryFromWkt(convertedWkt);
    } catch (ParseException e) {
      throw new UnsupportedOperationException(
          "Unable to parse WKT Geometry [" + convertedWkt + "]", e);
    }
    if (wktGeometry instanceof Polygon) {
      GeometryOperand polygonOperand = new GeometryOperand();
      polygonOperand.setName(Wfs20Constants.POLYGON);
      if (isGeometryOperandSupported(polygonOperand)) {
        return createPolygon(wktGeometry);
      }
      GeometryOperand envelopeOperand = new GeometryOperand();
      envelopeOperand.setName(Wfs20Constants.ENVELOPE);
      if (isGeometryOperandSupported(envelopeOperand)) {
        return createEnvelope(wktGeometry);
      }
    } else if (wktGeometry instanceof Point) {
      GeometryOperand pointOperand = new GeometryOperand();
      pointOperand.setName(Wfs20Constants.POINT);
      if (isGeometryOperandSupported(pointOperand)) {
        return createPoint(wktGeometry);
      }
    } else if (wktGeometry instanceof LineString) {
      GeometryOperand lineStringOperand = new GeometryOperand();
      lineStringOperand.setName(Wfs20Constants.LINESTRING);
      if (isGeometryOperandSupported(lineStringOperand)) {
        return createLineString(wktGeometry);
      }
    }
    throw new UnsupportedOperationException(
        "Geometry Operand from WKT [" + convertedWkt + "] is not supported.");
  }

  private JAXBElement<PolygonType> createPolygon(Geometry geometry) {
    PolygonType polygonType = gml320ObjectFactory.createPolygonType();
    polygonType.setSrsName(GeospatialUtil.EPSG_4326_URN);
    LinearRingType ring = gml320ObjectFactory.createLinearRingType();
    ring.setCoordinates(createCoordinates(geometry));
    AbstractRingPropertyType abstractRing = gml320ObjectFactory.createAbstractRingPropertyType();
    abstractRing.setAbstractRing(gml320ObjectFactory.createLinearRing(ring));
    polygonType.setExterior(abstractRing);

    return gml320ObjectFactory.createPolygon(polygonType);
  }

  private JAXBElement<EnvelopeType> createEnvelope(Geometry geometry) {
    EnvelopeType envelopeType = gml320ObjectFactory.createEnvelopeType();
    envelopeType.setSrsName(GeospatialUtil.EPSG_4326_URN);
    Envelope envelope = geometry.getEnvelopeInternal();
    DirectPositionType lowerCorner = gml320ObjectFactory.createDirectPositionType();
    lowerCorner.getValue().add(envelope.getMinX());
    lowerCorner.getValue().add(envelope.getMinY());
    envelopeType.setLowerCorner(lowerCorner);
    DirectPositionType upperCorner = gml320ObjectFactory.createDirectPositionType();
    upperCorner.getValue().add(envelope.getMaxX());
    upperCorner.getValue().add(envelope.getMaxY());
    envelopeType.setUpperCorner(upperCorner);
    return gml320ObjectFactory.createEnvelope(envelopeType);
  }

  private JAXBElement<LineStringType> createLineString(Geometry geometry) {
    LineStringType lineStringType = gml320ObjectFactory.createLineStringType();
    lineStringType.setSrsName(GeospatialUtil.EPSG_4326_URN);
    lineStringType.setCoordinates(createCoordinates(geometry));
    return gml320ObjectFactory.createLineString(lineStringType);
  }

  private CoordinatesType createCoordinates(Geometry geometry) {
    CoordinatesType coords = gml320ObjectFactory.createCoordinatesType();
    Coordinate[] coordinates = geometry.getCoordinates();
    if (coordinates != null && coordinates.length > 0) {
      StringBuffer coordString = new StringBuffer();

      for (Coordinate coordinate : coordinates) {
        coordString.append(coordinate.x).append(",").append(coordinate.y).append(" ");
      }
      coords.setValue(coordString.toString());
      coords.setDecimal(".");
      coords.setCs(",");
      coords.setTs(" ");
      coords.setValue(coordString.toString());
      return coords;
    } else {
      throw new IllegalArgumentException("Unable to parse Geometry coordinates from WKT String");
    }
  }

  private JAXBElement<PointType> createPoint(Geometry geometry) {
    Coordinate[] coordinates = geometry.getCoordinates();

    if (coordinates != null && coordinates.length > 0) {
      StringBuilder coordString = new StringBuilder();
      coordString.append(coordinates[0].x).append(",").append(coordinates[0].y);

      CoordinatesType coordinatesType = new CoordinatesType();
      coordinatesType.setValue(coordString.toString());

      PointType point = new PointType();
      point.setSrsName(srsName);
      point.setCoordinates(coordinatesType);

      return gml320ObjectFactory.createPoint(point);
    } else {
      throw new IllegalArgumentException("Unable to parse Point coordinates from WKT String");
    }
  }

  private boolean isGeometryOperandSupported(GeometryOperand geoOperand) {
    return geometryOperands.contains(geoOperand.getName());
  }

  private JAXBElement<LiteralType> createLiteralType(Object literalValue) {
    JAXBElement<LiteralType> literalType = filterObjectFactory.createLiteral(new LiteralType());
    literalType.getValue().getContent().add(literalValue.toString());
    return literalType;
  }

  private JAXBElement<String> createPropertyNameType(String propertyNameValue) {
    return filterObjectFactory.createValueReference(propertyNameValue);
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

  private Geometry getGeometryFromWkt(String wkt) throws ParseException {
    return new WKTReader().read(wkt);
  }

  private String bufferGeometry(String wkt, double distance) {
    LOGGER.debug("Buffering WKT {} by distance {} meter(s).", wkt, distance);
    String bufferedWkt = null;
    try {
      Geometry geometry = getGeometryFromWkt(wkt);
      double bufferInDegrees = metersToDegrees(distance);
      LOGGER.debug(
          "Buffering {} by {} degree(s).", geometry.getClass().getSimpleName(), bufferInDegrees);
      Geometry bufferedGeometry = geometry.buffer(bufferInDegrees);
      bufferedWkt = new WKTWriter().write(bufferedGeometry);
      LOGGER.debug("Buffered WKT: {}.", bufferedWkt);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Unable to parse WKT String", e);
    }

    return bufferedWkt;
  }

  /**
   * This method approximates the degrees in latitude for the given distance (in meters) using the
   * formula for the meridian distance on Earth.
   *
   * <p>degrees = distance in meters/radius of Earth in meters * 180.0/pi
   *
   * <p>The approximate degrees in latitude can be used to compute a buffer around a given geometry
   * (see bufferGeometry()).
   */
  private double metersToDegrees(double distance) {
    double degrees =
        (distance / Wfs20Constants.EARTH_MEAN_RADIUS_METERS) * Wfs20Constants.RADIANS_TO_DEGREES;
    LOGGER.debug("{} meter(s) is approximately {} degree(s) of latitude.", distance, degrees);
    return degrees;
  }

  public boolean isLogicalOps() {
    return logicalOps;
  }

  public Set<COMPARISON_OPERATORS> getComparisonOps() {
    return comparisonOps;
  }

  public Map<SPATIAL_OPERATORS, SpatialOperatorType> getSpatialOps() {
    return spatialOps;
  }

  public void setSpatialOps(SpatialOperatorsType spatialOperators) {
    spatialOps =
        new ConcurrentHashMap<SPATIAL_OPERATORS, SpatialOperatorType>(
            new EnumMap<SPATIAL_OPERATORS, SpatialOperatorType>(SPATIAL_OPERATORS.class));

    for (SpatialOperatorType spatialOp : spatialOperators.getSpatialOperator()) {
      LOGGER.debug("Adding key [spatialOp Name: {}]", spatialOp.getName());
      spatialOps.put(SPATIAL_OPERATORS.valueOf(spatialOp.getName()), spatialOp);
      LOGGER.debug("spatialOps Map: {}", spatialOps);
    }
  }

  public List<QName> getGeometryOperands() {
    return geometryOperands;
  }

  public Map<TEMPORAL_OPERATORS, TemporalOperatorType> getTemporalOps() {
    return temporalOps;
  }

  public void setTemporalOps(TemporalOperatorsType temporalOperators) {
    temporalOps =
        new ConcurrentHashMap<TEMPORAL_OPERATORS, TemporalOperatorType>(
            new EnumMap<TEMPORAL_OPERATORS, TemporalOperatorType>(TEMPORAL_OPERATORS.class));

    for (TemporalOperatorType temporalOp : temporalOperators.getTemporalOperator()) {
      LOGGER.debug("Adding key [temporalOp Name: {}]", temporalOp.getName());
      temporalOps.put(TEMPORAL_OPERATORS.valueOf(temporalOp.getName()), temporalOp);
      LOGGER.debug("temporalOps Map: {}", temporalOps);
    }
  }

  public List<QName> getTemporalOperands() {
    return temporalOperands;
  }

  public boolean isSortingSupported() {
    return this.isSortingSupported;
  }

  public Set<SortOrder> getAllowedSortOrders() {
    return this.allowedSortOrders;
  }

  public String getSrsName() {
    return srsName;
  }

  public boolean isEpsg4326() {
    return isEpsg4326;
  }

  private boolean isTemporalOpSupported(TEMPORAL_OPERATORS temporalOp) {
    return temporalOps.containsKey(temporalOp);
  }

  private boolean isTemporalOperandSupported(TemporalOperand temporalOperand) {
    return temporalOperands.contains(temporalOperand.getName());
  }

  private void areLogicalOperationsSupported() {
    if (!logicalOps) {
      throw new UnsupportedOperationException("Logical Operations are not supported.");
    }
  }

  private String mapMetacardAttribute(String metacardAttribute) {
    final ThreadLocal<String> featureProperty = new ThreadLocal<>();
    if (this.metacardToFeatureMapper != null) {
      this.metacardToFeatureMapper
          .getEntry(e -> e.getAttributeName().equals(metacardAttribute))
          .ifPresent(e -> featureProperty.set(e.getFeatureProperty()));
    } else {
      LOGGER.debug("{} is null.", MetacardMapper.class.getSimpleName());
      return metacardAttribute;
    }

    if (featureProperty.get() == null) {
      LOGGER.debug("Unable to find feature property for metacard attribute {}.", metacardAttribute);
      return metacardAttribute;
    } else {
      LOGGER.debug(
          "Found feature property {} for metacard attribute {}.",
          featureProperty.get(),
          metacardAttribute);
      return featureProperty.get();
    }
  }

  /**
   * The WKT passed into the spatial methods has the coordinates ordered in LON/LAT. This method
   * will convert the WKT to LAT/LON ordering.
   */
  private String convertWktToLatLonOrdering(String wktInLonLat) {

    if (GeospatialUtil.LAT_LON_ORDER.equals(coordinateOrder)) {
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

  private static enum PROPERTY_IS_OPS {
    PropertyIsEqualTo,
    PropertyIsLike,
    PropertyIsNotEqualTo,
    PropertyIsGreaterThan,
    PropertyIsGreaterThanOrEqualTo,
    PropertyIsLessThan,
    PropertyIsLessThanOrEqualTo;
  }
}
