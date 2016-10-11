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
package org.codice.ddf.catalog.plugin.metacard;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.catalog.plugin.metacard.util.AttributeHelper;
import org.codice.ddf.catalog.plugin.metacard.util.KeyValueParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PreAuthorizationPlugin;
import ddf.catalog.plugin.StopProcessingException;

/**
 * Conditional attribute adjustments for metacards based on IP, hostname, scheme, and context path.
 */
public class MetacardIngestNetworkPlugin implements PreAuthorizationPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardIngestNetworkPlugin.class);

    private static final String CLIENT_INFO_KEY = "client-info";

    private final KeyValueParser keyValueParser;

    private final AttributeHelper attributeHelper;

    private MetacardCondition metacardCondition;

    private List<String> newAttributes;

    public String getCriteriaKey() {
        return metacardCondition.getCriteriaKey();
    }

    public void setCriteriaKey(String criteriaKey) {
        metacardCondition.setCriteriaKey(criteriaKey);
    }

    public String getExpectedValue() {
        return metacardCondition.getExpectedValue();
    }

    public void setExpectedValue(String expectedValue) {
        metacardCondition.setExpectedValue(expectedValue);
    }

    public List<String> getNewAttributes() {
        return newAttributes;
    }

    public void setNewAttributes(List<String> newAttributes) {
        this.newAttributes = newAttributes;
    }

    /**
     * Default constructor.
     */
    public MetacardIngestNetworkPlugin(KeyValueParser keyValueParser,
            AttributeHelper attributeHelper) {
        this(keyValueParser, attributeHelper, new MetacardCondition());
    }

    /**
     * Extended constructor with a {@link MetacardCondition}.
     */
    public MetacardIngestNetworkPlugin(KeyValueParser keyValueParser,
            AttributeHelper attributeHelper, MetacardCondition metacardCondition) {
        this.keyValueParser = keyValueParser;
        this.attributeHelper = attributeHelper;
        this.metacardCondition = metacardCondition;
    }

    @Override
    public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
        apply(input.getMetacards(), getClientInfoProperties());
        return input;
    }

    //region - Passthrough  methods

    @Override
    public UpdateRequest processPreUpdate(UpdateRequest input,
            Map<String, Metacard> existingMetacards) throws StopProcessingException {
        return input;
    }

    @Override
    public DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException {
        return input;
    }

    @Override
    public DeleteResponse processPostDelete(DeleteResponse input) throws StopProcessingException {
        return input;
    }

    @Override
    public QueryRequest processPreQuery(QueryRequest input) throws StopProcessingException {
        return input;
    }

    @Override
    public QueryResponse processPostQuery(QueryResponse input) throws StopProcessingException {
        return input;
    }

    @Override
    public ResourceRequest processPreResource(ResourceRequest input)
            throws StopProcessingException {
        return input;
    }

    @Override
    public ResourceResponse processPostResource(ResourceResponse input, Metacard metacard)
            throws StopProcessingException {
        return input;
    }

    //endregion

    private void apply(List<Metacard> metacards, Map<String, Serializable> criteria) {
        if (metacardCondition.applies(criteria)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Metacard condition was true; expected {} of {} equals {}",
                        metacardCondition.getCriteriaKey(),
                        metacardCondition.getExpectedValue(),
                        criteria.get(metacardCondition.getCriteriaKey()));
            }
            Map<String, String> parsedAttributes = keyValueParser.parsePairsToMap(newAttributes);
            attributeHelper.applyNewAttributes(metacards, parsedAttributes);
        }
    }

    private Map<String, Serializable> getClientInfoProperties() throws StopProcessingException {
        Object info = ThreadContext.get(CLIENT_INFO_KEY);
        if (info == null || !(info instanceof Map)) {
            throw new StopProcessingException("Client-info map was not found or is not a valid map");
        }

        return (Map<String, Serializable>) info;
    }
}
