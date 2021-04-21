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
package org.codice.ddf.catalog.plugin.metacard;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.plugin.PreAuthorizationPlugin;
import ddf.catalog.plugin.StopProcessingException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.shiro.util.ThreadContext;
import org.codice.ddf.catalog.plugin.metacard.util.AttributeFactory;
import org.codice.ddf.catalog.plugin.metacard.util.KeyValueParser;
import org.codice.ddf.catalog.plugin.metacard.util.MetacardServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Conditional attribute adjustments for metacards based on IP, hostname, scheme, and context path.
 *
 * <p>The list of new attributes are parsed into a map whose key is the attribute name and attribute
 * descriptor name, and value is the desired value of that attribute by the admin. If the value in
 * the client-info map (populated by) at key "criteria key" is equal to the "expected value", then
 * the plugin will attempt to add the list of new attributes to the metacards in the given request.
 *
 * <p>Any attribute already set on the metacards cannot be changed. Currently there is no support
 * for appending new values within a multi-valued attribute onto an already existing one. As long as
 * an attribute is not null, it cannot be overwritten.
 *
 * <p>See {@link MetacardCondition} for details on its immutability and volatility.
 */
public class MetacardIngestNetworkPlugin implements PreAuthorizationPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardIngestNetworkPlugin.class);

  private static final String CLIENT_INFO_KEY = "client-info";

  private static final String CRITERIA_KEY = "criteriaKey";

  private static final String EXPECTED_VALUE = "expectedValue";

  private static final String NEW_ATTRIBUTES = "newAttributes";

  private final KeyValueParser keyValueParser;

  private final MetacardServices metacardServices;

  private final AttributeFactory attributeFactory;

  private volatile MetacardCondition metacardCondition;

  private Map<String, Object> initParameters;

  /** Constructor requires all dependencies. */
  public MetacardIngestNetworkPlugin(
      KeyValueParser keyValueParser,
      MetacardServices metacardServices,
      AttributeFactory attributeFactory,
      MetacardCondition metacardCondition) {
    this.keyValueParser = keyValueParser;
    this.metacardServices = metacardServices;
    this.attributeFactory = attributeFactory;
    this.metacardCondition = metacardCondition;

    this.initParameters = new HashMap<>();
  }

  public String getCriteriaKey() {
    return metacardCondition.getCriteriaKey();
  }

  public String getExpectedValue() {
    return metacardCondition.getExpectedValue();
  }

  public List<String> getNewAttributes() {
    return metacardCondition.getNewAttributes();
  }

  public void setCriteriaKey(String criteriaKey) {
    initParameters.put(CRITERIA_KEY, criteriaKey);
  }

  public void setExpectedValue(String expectedValue) {
    initParameters.put(EXPECTED_VALUE, expectedValue);
  }

  public void setNewAttributes(List<String> newAttributes) {
    initParameters.put(NEW_ATTRIBUTES, newAttributes.toArray(new String[newAttributes.size()]));
  }

  /**
   * Method required by the component-managed strategy. Performs set-up based on initial values
   * passed to the service.
   */
  public void init() {
    updateCondition(initParameters);
  }

  /** Method required by the component-managed strategy. */
  public void destroy() {
    // Does nothing as of now, used in blueprint
  }

  /**
   * Atomically update the condition so that ingest rules are not evaluated based on an invalid
   * rule.
   */
  public void updateCondition(Map<String, Object> properties) {

    if (properties != null) {
      String criteriaKey = (String) properties.get(CRITERIA_KEY);
      String expectedValue = (String) properties.get(EXPECTED_VALUE);
      String[] newAttributes = (String[]) properties.get(NEW_ATTRIBUTES);

      metacardCondition =
          new MetacardCondition(
              criteriaKey, expectedValue, Arrays.asList(newAttributes), keyValueParser);
    }
  }

  @Override
  public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
    Object info = ThreadContext.get(CLIENT_INFO_KEY);
    if (!(info instanceof Map)) {
      LOGGER.debug("Client network info was null or not properly formatted");
      return input;
    }
    Map<String, Serializable> clientInfoProperties = (Map<String, Serializable>) info;
    return createNewMetacardsIfConditionApplies(input, clientInfoProperties);
  }

  // region - Passthrough  methods

  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest input, Map<String, Metacard> existingMetacards) throws StopProcessingException {
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
  public ResourceRequest processPreResource(ResourceRequest input) throws StopProcessingException {
    return input;
  }

  @Override
  public ResourceResponse processPostResource(ResourceResponse input, Metacard metacard)
      throws StopProcessingException {
    return input;
  }

  // endregion

  private CreateRequest createNewMetacardsIfConditionApplies(
      CreateRequest request, Map<String, Serializable> criteria) {
    if (metacardCondition.applies(criteria)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            "Metacard condition was true; expected {} of {} equals {}",
            metacardCondition.getCriteriaKey(),
            metacardCondition.getExpectedValue(),
            criteria.get(metacardCondition.getCriteriaKey()));
      }

      List<Metacard> metacards = request.getMetacards();
      List<Metacard> newMetacards =
          metacardServices.setAttributesIfAbsent(
              metacards, metacardCondition.getParsedAttributes(), attributeFactory);

      return new CreateRequestImpl(newMetacards, request.getProperties());
    }

    return request;
  }
}
