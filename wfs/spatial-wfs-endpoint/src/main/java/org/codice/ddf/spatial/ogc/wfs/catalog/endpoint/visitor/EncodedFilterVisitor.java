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
package org.codice.ddf.spatial.ogc.wfs.catalog.endpoint.visitor;

import java.util.ArrayList;
import java.util.List;

import ogc.schema.opengis.Traverser;
import ogc.schema.opengis.TraversingVisitor;
import ogc.schema.opengis.Visitor;
import ogc.schema.opengis.filter.v_1_0_0.DistanceBufferType;
import ogc.schema.opengis.filter.v_1_0_0.DistanceType;
import ogc.schema.opengis.filter.v_1_0_0.FeatureIdType;

import org.apache.commons.lang.StringUtils;
import org.geotools.styling.UomOgcMapping;
import org.opengis.filter.Filter;

import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterBuilder;

public class EncodedFilterVisitor extends TraversingVisitor {

    private FilterBuilder builder;

    private List<Filter> featureIdFilters = new ArrayList<Filter>();

    public EncodedFilterVisitor(Traverser traverser, Visitor visitor, FilterBuilder builder) {
        super(traverser, visitor);
        this.builder = builder;
    }

    @Override
    public void visit(DistanceBufferType dbt) {
        if (dbt.isSetDistance()) {
            DistanceType distanceType = dbt.getDistance();

            if ((!UomOgcMapping.FOOT.name().equals(distanceType.getUnits()))
                    && (!UomOgcMapping.METRE.name().equals(distanceType.getUnits()))) {
                throw new UnsupportedOperationException(
                        "Units for Distance elements must be \"FOOT\" or \"METRE\".");
            }

        }
    }

    public void visit(FeatureIdType featureIdType) {
        if (!StringUtils.isEmpty(featureIdType.getFid())) {
            featureIdFilters.add(builder.attribute(Metacard.ID).is().text(featureIdType.getFid()));
        }
    }

    public List<Filter> getFeatureIdFilters() {
        return featureIdFilters;
    }
}
