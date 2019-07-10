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

import java.util.Arrays;
import java.util.List;
import javax.xml.namespace.QName;
import net.opengis.filter.v_2_0_0.ComparisonOperatorType;
import net.opengis.filter.v_2_0_0.ComparisonOperatorsType;
import net.opengis.filter.v_2_0_0.ConformanceType;
import net.opengis.filter.v_2_0_0.FilterCapabilities;
import net.opengis.filter.v_2_0_0.GeometryOperandsType;
import net.opengis.filter.v_2_0_0.GeometryOperandsType.GeometryOperand;
import net.opengis.filter.v_2_0_0.LogicalOperators;
import net.opengis.filter.v_2_0_0.ScalarCapabilitiesType;
import net.opengis.filter.v_2_0_0.SpatialCapabilitiesType;
import net.opengis.filter.v_2_0_0.SpatialOperatorType;
import net.opengis.filter.v_2_0_0.SpatialOperatorsType;
import net.opengis.filter.v_2_0_0.TemporalCapabilitiesType;
import net.opengis.filter.v_2_0_0.TemporalOperandsType;
import net.opengis.filter.v_2_0_0.TemporalOperandsType.TemporalOperand;
import net.opengis.filter.v_2_0_0.TemporalOperatorType;
import net.opengis.filter.v_2_0_0.TemporalOperatorsType;
import net.opengis.ows.v_1_1_0.DomainType;
import net.opengis.ows.v_1_1_0.NoValues;
import net.opengis.ows.v_1_1_0.ValueType;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.COMPARISON_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.CONFORMANCE_CONSTRAINTS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.SPATIAL_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs20Constants.TEMPORAL_OPERATORS;

public class MockWfsServer {

  public static FilterCapabilities getFilterCapabilities() {
    FilterCapabilities capabilities = new FilterCapabilities();
    ConformanceType conformance = new ConformanceType();
    for (CONFORMANCE_CONSTRAINTS constraint : CONFORMANCE_CONSTRAINTS.values()) {
      DomainType domain = new DomainType();
      NoValues noValues = new NoValues();
      domain.setNoValues(noValues);
      ValueType value = new ValueType();
      value.setValue("TRUE");
      domain.setDefaultValue(value);
      domain.setName(constraint.toString());
      conformance.getConstraint().add(domain);
    }
    capabilities.setConformance(conformance);

    ScalarCapabilitiesType scalar = new ScalarCapabilitiesType();
    scalar.setLogicalOperators(new LogicalOperators());
    scalar.setComparisonOperators(new ComparisonOperatorsType());
    for (COMPARISON_OPERATORS compOp : COMPARISON_OPERATORS.values()) {
      ComparisonOperatorType operator = new ComparisonOperatorType();
      operator.setName(compOp.toString());
      scalar.getComparisonOperators().getComparisonOperator().add(operator);
    }
    capabilities.setScalarCapabilities(scalar);

    SpatialCapabilitiesType spatial = new SpatialCapabilitiesType();
    spatial.setSpatialOperators(new SpatialOperatorsType());
    for (SPATIAL_OPERATORS spatialOp : SPATIAL_OPERATORS.values()) {
      SpatialOperatorType operator = new SpatialOperatorType();
      operator.setName(spatialOp.toString());
      spatial.getSpatialOperators().getSpatialOperator().add(operator);
    }

    GeometryOperandsType geometryOperands = new GeometryOperandsType();

    List<QName> qnames =
        Arrays.asList(
            Wfs20Constants.POINT,
            Wfs20Constants.ENVELOPE,
            Wfs20Constants.POLYGON,
            Wfs20Constants.LINESTRING);
    for (QName qName : qnames) {
      GeometryOperand operand = new GeometryOperand();
      operand.setName(qName);
      geometryOperands.getGeometryOperand().add(operand);
    }
    spatial.setGeometryOperands(geometryOperands);
    capabilities.setSpatialCapabilities(spatial);

    TemporalCapabilitiesType temporal = new TemporalCapabilitiesType();
    temporal.setTemporalOperators(new TemporalOperatorsType());
    for (TEMPORAL_OPERATORS temporalOp : TEMPORAL_OPERATORS.values()) {
      TemporalOperatorType operator = new TemporalOperatorType();
      operator.setName(temporalOp.toString());
      temporal.getTemporalOperators().getTemporalOperator().add(operator);
    }
    TemporalOperandsType temporalOperands = new TemporalOperandsType();
    List<QName> timeQNames =
        Arrays.asList(
            new QName(Wfs20Constants.GML_3_2_NAMESPACE, "TimePeriod"),
            new QName(Wfs20Constants.GML_3_2_NAMESPACE, "TimeInstant"));
    for (QName qName : timeQNames) {
      TemporalOperand operand = new TemporalOperand();
      operand.setName(qName);
      temporalOperands.getTemporalOperand().add(operand);
    }
    temporal.setTemporalOperands(temporalOperands);
    capabilities.setTemporalCapabilities(temporal);

    return capabilities;
  }

  public static String getBeyondXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Beyond>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Polygon srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:exterior>"
        + "<ns4:LinearRing>"
        + "<ns4:coordinates decimal=\".\" cs=\",\" ts=\" \">-10.0,30.0 30.0,30.0 30.0,10.0 -10.0,10.0 -10.0,30.0 </ns4:coordinates>"
        + "</ns4:LinearRing>"
        + "</ns4:exterior>"
        + "</ns4:Polygon>"
        + "<Distance uom=\"METERS\">1000.0</Distance>"
        + "</Beyond>"
        + "</ns5:Filter>";
  }

  public static String getDWithinAsIntersectsXml() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/gml/3.2\" xmlns:ns5=\"http://www.opengis.net/ogc\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns=\"http://www.opengis.net/fes/2.0\">"
        + "<Intersects>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns2:Polygon srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns2:exterior>"
        + "<ns2:LinearRing>"
        + "<ns2:coordinates decimal=\".\" cs=\",\" ts=\" \">"
        + "-10.0,31.79864073552333 -10.350897400284572,31.76408035813492"
        + " -10.688310010261736,31.66172736189105"
        + " -10.999271252553244,31.495515115037147"
        + " -11.271831061006903,31.271831061006903"
        + " -11.495515115037145,30.999271252553243"
        + " -11.66172736189105,30.688310010261738 "
        + "-11.764080358134919,30.350897400284573"
        + " -11.798640735523328,30.0 "
        + "-11.764080358134919,29.649102599715427"
        + " -11.66172736189105,29.311689989738262"
        + " -11.495515115037145,29.000728747446757"
        + " -11.271831061006905,28.728168938993097"
        + " -10.999271252553244,28.504484884962853"
        + " -10.688310010261736,28.33827263810895"
        + " -10.350897400284572,28.23591964186508"
        + " -9.999999999999998,28.20135926447667"
        + " -9.649102599715427,28.23591964186508"
        + " -9.311689989738262,28.338272638108954"
        + " -9.000728747446754,28.504484884962856"
        + " -8.728168938993093,28.728168938993097"
        + " -8.504484884962853,29.000728747446757 "
        + "-8.33827263810895,29.311689989738266"
        + " -8.23591964186508,29.649102599715434"
        + " -8.201359264476672,30.000000000000004"
        + " -8.235919641865081,30.350897400284577 "
        + "-8.338272638108954,30.68831001026174"
        + " -8.504484884962856,30.99927125255325"
        + " -8.7281689389931,31.271831061006907"
        + " -9.000728747446761,31.49551511503715"
        + " -9.31168998973827,31.661727361891053"
        + " -9.649102599715436,31.76408035813492"
        + " -10.0,31.79864073552333 "
        + "</ns2:coordinates>"
        + "</ns2:LinearRing>"
        + "</ns2:exterior>"
        + "</ns2:Polygon>"
        + "</Intersects>"
        + "</ns5:Filter>";
  }

  public static String getContainsXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Contains>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Polygon srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:exterior>"
        + "<ns4:LinearRing>"
        + "<ns4:coordinates decimal=\".\" cs=\",\" ts=\" \">-10.0,30.0 30.0,30.0 30.0,10.0 -10.0,10.0 -10.0,30.0 </ns4:coordinates>"
        + "</ns4:LinearRing>"
        + "</ns4:exterior>"
        + "</ns4:Polygon>"
        + "</Contains>"
        + "</ns5:Filter>";
  }

  public static String getCrossesXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Crosses>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:LineString srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:coordinates decimal=\".\" cs=\",\" ts=\" \">-10.0,30.0 30.0,30.0 30.0,10.0 -10.0,10.0 </ns4:coordinates>"
        + "</ns4:LineString>"
        + "</Crosses>"
        + "</ns5:Filter>";
  }

  public static String getDisjointXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Disjoint>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Polygon srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:exterior>"
        + "<ns4:LinearRing>"
        + "<ns4:coordinates decimal=\".\" cs=\",\" ts=\" \">-10.0,30.0 30.0,30.0 30.0,10.0 -10.0,10.0 -10.0,30.0 </ns4:coordinates>"
        + "</ns4:LinearRing>"
        + "</ns4:exterior>"
        + "</ns4:Polygon>"
        + "</Disjoint>"
        + "</ns5:Filter>";
  }

  public static String getDWithinXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<DWithin>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Polygon srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:exterior>"
        + "<ns4:LinearRing>"
        + "<ns4:coordinates decimal=\".\" cs=\",\" ts=\" \">-10.0,30.0 30.0,30.0 30.0,10.0 -10.0,10.0 -10.0,30.0 </ns4:coordinates>"
        + "</ns4:LinearRing>"
        + "</ns4:exterior>"
        + "</ns4:Polygon>"
        + "<Distance uom=\"METERS\">1000.0</Distance>"
        + "</DWithin>"
        + "</ns5:Filter>";
  }

  public static String getDWithinPointXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<DWithin>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Point srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:coordinates>-10.0,30.0</ns4:coordinates>"
        + "</ns4:Point>"
        + "<Distance uom=\"METERS\">1000.0</Distance>"
        + "</DWithin>"
        + "</ns5:Filter>";
  }

  public static String getIntersectsXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Intersects>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Polygon srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:exterior>"
        + "<ns4:LinearRing>"
        + "<ns4:coordinates decimal=\".\" cs=\",\" ts=\" \">-10.0,30.0 30.0,30.0 30.0,10.0 -10.0,10.0 -10.0,30.0 </ns4:coordinates>"
        + "</ns4:LinearRing>"
        + "</ns4:exterior>"
        + "</ns4:Polygon>"
        + "</Intersects>"
        + "</ns5:Filter>";
  }

  public static String getIntersectsWithEnvelopeLonLatXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Intersects>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Envelope srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:lowerCorner>10.0 -10.0</ns4:lowerCorner>"
        + "<ns4:upperCorner>30.0 30.0</ns4:upperCorner>"
        + "</ns4:Envelope>"
        + "</Intersects>"
        + "</ns5:Filter>";
  }

  public static String getIntersectsLonLatXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Intersects>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Polygon srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:exterior>"
        + "<ns4:LinearRing>"
        + "<ns4:coordinates decimal=\".\" cs=\",\" ts=\" \">30.0,-10.0 30.0,30.0 10.0,30.0 10.0,-10.0 30.0,-10.0 </ns4:coordinates>"
        + "</ns4:LinearRing>"
        + "</ns4:exterior>"
        + "</ns4:Polygon>"
        + "</Intersects>"
        + "</ns5:Filter>";
  }

  public static String getIntersectsWithEnvelopeXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Intersects>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Envelope srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:lowerCorner>-10.0 10.0</ns4:lowerCorner>"
        + "<ns4:upperCorner>30.0 30.0</ns4:upperCorner>"
        + "</ns4:Envelope>"
        + "</Intersects>"
        + "</ns5:Filter>";
  }

  public static String getNotIntersectsXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Not><Intersects>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Polygon srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:exterior>"
        + "<ns4:LinearRing>"
        + "<ns4:coordinates decimal=\".\" cs=\",\" ts=\" \">-10.0,30.0 30.0,30.0 30.0,10.0 -10.0,10.0 -10.0,30.0 </ns4:coordinates>"
        + "</ns4:LinearRing>"
        + "</ns4:exterior>"
        + "</ns4:Polygon>"
        + "</Intersects></Not>"
        + "</ns5:Filter>";
  }

  public static String getBboxXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<BBOX>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Envelope srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:lowerCorner>10.0 -10.0</ns4:lowerCorner>"
        + "<ns4:upperCorner>30.0 30.0</ns4:upperCorner>"
        + "</ns4:Envelope>"
        + "</BBOX>"
        + "</ns5:Filter>";
  }

  public static String getNotBboxXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Not><BBOX>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Envelope srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:lowerCorner>10.0 -10.0</ns4:lowerCorner>"
        + "<ns4:upperCorner>30.0 30.0</ns4:upperCorner>"
        + "</ns4:Envelope>"
        + "</BBOX></Not>"
        + "</ns5:Filter>";
  }

  public static String getOverlapsXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Overlaps>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Polygon srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:exterior>"
        + "<ns4:LinearRing>"
        + "<ns4:coordinates decimal=\".\" cs=\",\" ts=\" \">-10.0,30.0 30.0,30.0 30.0,10.0 -10.0,10.0 -10.0,30.0 </ns4:coordinates>"
        + "</ns4:LinearRing>"
        + "</ns4:exterior>"
        + "</ns4:Polygon>"
        + "</Overlaps>"
        + "</ns5:Filter>";
  }

  public static String getTouchesXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Touches>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Polygon srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:exterior>"
        + "<ns4:LinearRing>"
        + "<ns4:coordinates decimal=\".\" cs=\",\" ts=\" \">-10.0,30.0 30.0,30.0 30.0,10.0 -10.0,10.0 -10.0,30.0 </ns4:coordinates>"
        + "</ns4:LinearRing>"
        + "</ns4:exterior>"
        + "</ns4:Polygon>"
        + "</Touches>"
        + "</ns5:Filter>";
  }

  public static String getWithinXmlFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ns5:Filter xmlns:ns2=\"http://www.opengis.net/ows/1.1\" xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ns4=\"http://www.opengis.net/gml/3.2\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
        + "<Within>"
        + "<ValueReference>geom</ValueReference>"
        + "<ns4:Polygon srsName=\"urn:ogc:def:crs:EPSG::4326\">"
        + "<ns4:exterior>"
        + "<ns4:LinearRing>"
        + "<ns4:coordinates decimal=\".\" cs=\",\" ts=\" \">-10.0,30.0 30.0,30.0 30.0,10.0 -10.0,10.0 -10.0,30.0 </ns4:coordinates>"
        + "</ns4:LinearRing>"
        + "</ns4:exterior>"
        + "</ns4:Polygon>"
        + "</Within>"
        + "</ns5:Filter>";
  }

  static String getPropertyIsEqualToFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ogc:Filter xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ogc=\"http://www.opengis.net/ogc\">"
        + "<PropertyIsEqualTo>"
        + "<ValueReference>mockFeatureProperty</ValueReference>"
        + "<Literal>Literal</Literal>"
        + "</PropertyIsEqualTo>"
        + "</ogc:Filter>";
  }

  static String getPropertyIsBetweenFilter() {
    return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
        + "<ogc:Filter xmlns=\"http://www.opengis.net/fes/2.0\" xmlns:ogc=\"http://www.opengis.net/ogc\">"
        + "<PropertyIsBetween>"
        + "<ValueReference>mockFeatureProperty</ValueReference>"
        + "<LowerBoundary>"
        + "<Literal>100</Literal>"
        + "</LowerBoundary>"
        + "<UpperBoundary>"
        + "<Literal>200</Literal>"
        + "</UpperBoundary>"
        + "</PropertyIsBetween>"
        + "</ogc:Filter>";
  }
}
