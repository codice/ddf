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
 * <p>
 * The list of new attributes are parsed into a map whose key is the attribute name and attribute
 * descriptor name, and value is the desired value of that attribute by the admin. If the value in
 * the client-info map (populated by) at key "criteria key" is equal to the "expected value", then
 * the plugin will attempt to add the list of new attributes to the metacards in the given request.
 * <p>
 * Any attribute already set on the metacards cannot be changed. Currently there is no support for
 * appending new values within a multi-valued attribute onto an already existing one. As long as
 * an attribute is not null, it cannot be overwritten.
 */
public class MetacardIngestNetworkPlugin implements PreAuthorizationPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardIngestNetworkPlugin.class);

    private static final String CLIENT_INFO_KEY = "client-info";

    private final KeyValueParser keyValueParser;

    private final MetacardServices metacardServices;

    private final AttributeFactory attributeFactory;

    private final MetacardCondition metacardCondition;

    private List<String> newAttributes;

    private Map<String, String> parsedAttributes;

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
        this.parsedAttributes = keyValueParser.parsePairsToMap(newAttributes);
    }

    @Override
    public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
        Map<String, Serializable> clientInfoProperties = getClientInfoProperties();
        if (clientInfoProperties == null) {
            return input;
        }
        updateMetacardsIfConditionApplies(input.getMetacards(), getClientInfoProperties());
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

    private void updateMetacardsIfConditionApplies(List<Metacard> metacards,
            Map<String, Serializable> criteria) {
        if (metacardCondition.applies(criteria)) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Metacard condition was true; expected {} of {} equals {}",
                        metacardCondition.getCriteriaKey(),
                        metacardCondition.getExpectedValue(),
                        criteria.get(metacardCondition.getCriteriaKey()));
            }
            metacardServices.setAttributesIfAbsent(metacards, parsedAttributes, attributeFactory);
        }
    }

    private Map<String, Serializable> getClientInfoProperties() throws StopProcessingException {
        Object info = ThreadContext.get(CLIENT_INFO_KEY);
        if (!(info instanceof Map)) {
            return null;
        }

        return (Map<String, Serializable>) info;
    }
}
