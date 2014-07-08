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
import net.opengis.ows.v_1_1_0.ValueType;

import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.WfsConstants.COMPARISON_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.WfsConstants.CONFORMANCE_CONSTRAINTS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.WfsConstants.SPATIAL_OPERATORS;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.WfsConstants.TEMPORAL_OPERATORS;

public class MockWfsServer {

    public static FilterCapabilities getFilterCapabilities() {
        FilterCapabilities capabilities = new FilterCapabilities();
        ConformanceType conformance = new ConformanceType();
        for (CONFORMANCE_CONSTRAINTS constraint : CONFORMANCE_CONSTRAINTS.values()) {
            DomainType domain = new DomainType();
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

        List<QName> qnames = Arrays.asList(new QName(WfsConstants.GML_3_2_NAMESPACE, "Box"),
                new QName(WfsConstants.GML_3_2_NAMESPACE, "Envelope"));
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
        List<QName> timeQNames = Arrays.asList(new QName(WfsConstants.GML_3_2_NAMESPACE,
                "TimePeriod"), new QName(WfsConstants.GML_3_2_NAMESPACE, "TimeInstant"));
        for (QName qName : timeQNames) {
            TemporalOperand operand = new TemporalOperand();
            operand.setName(qName);
            temporalOperands.getTemporalOperand().add(operand);
        }
        temporal.setTemporalOperands(temporalOperands);
        capabilities.setTemporalCapabilities(temporal);

        return capabilities;
    }

}
