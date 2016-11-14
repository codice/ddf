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

import static org.apache.commons.lang.BooleanUtils.isFalse;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.catalog.plugin.metacard.util.AttributeFactory;
import org.codice.ddf.catalog.plugin.metacard.util.KeyValueParser;
import org.codice.ddf.catalog.plugin.metacard.util.MetacardServices;
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

    private final MetacardServices metacardServices;

    private final AttributeFactory attributeFactory;

    private final MetacardCondition metacardCondition;

    private List<String> newAttributes;

    /**
     * Default constructor.
     */
    public MetacardIngestNetworkPlugin(KeyValueParser keyValueParser,
            MetacardServices metacardServices) {
        this(keyValueParser, metacardServices, new AttributeFactory(), new MetacardCondition());
    }

    /**
     * Extended constructor with an {@link AttributeFactory} and {@link MetacardCondition} to provide
     * more fine-grained control.
     */
    public MetacardIngestNetworkPlugin(KeyValueParser keyValueParser,
            MetacardServices metacardServices, AttributeFactory attributeFactory,
            MetacardCondition metacardCondition) {
        this.keyValueParser = keyValueParser;
        this.metacardServices = metacardServices;
        this.attributeFactory = attributeFactory;
        this.metacardCondition = metacardCondition;
    }

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
            metacardServices.setAttributesIfAbsent(metacards, parsedAttributes, attributeFactory);
        }
    }

    private Map<String, Serializable> getClientInfoProperties() throws StopProcessingException {
        Object info = ThreadContext.get(CLIENT_INFO_KEY);
        if (isFalse(info instanceof Map)) {
            throw new StopProcessingException("Client-info map was not found or is not a valid map");
        }

        return (Map<String, Serializable>) info;
    }
}
