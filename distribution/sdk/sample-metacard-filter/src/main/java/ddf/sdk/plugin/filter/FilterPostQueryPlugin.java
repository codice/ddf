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
 **/
package ddf.sdk.plugin.filter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PostQueryPlugin;
import ddf.catalog.plugin.StopProcessingException;

public class FilterPostQueryPlugin implements PostQueryPlugin {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(FilterPostQueryPlugin.class.getName());

    @Override
    public QueryResponse process(QueryResponse queryResponse)
            throws PluginExecutionException, StopProcessingException {
        Metacard metacard;
        for (Result result : queryResponse.getResults()) {
            metacard = result.getMetacard();
            // You should parse your metacard here then add security attributes

            HashMap<String, List<String>> securityFinalMap = new HashMap<String, List<String>>();
            securityFinalMap.put(Metacard.POINT_OF_CONTACT, Arrays.asList("admin"));
            metacard.setAttribute(new AttributeImpl(Metacard.SECURITY, securityFinalMap));
        }

        return queryResponse;
    }
}
