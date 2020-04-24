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
package org.codice.ddf.catalog.plugin.security.audit;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.Constants;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.Request;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.audit.SecurityLogger;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SecurityAuditPlugin Allows changes to security attributes on a metacard to be audited */
public class SecurityAuditPlugin implements AccessPlugin {

  private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAuditPlugin.class);

  private List<String> auditAttributes = new ArrayList<>();

  private SecurityLogger securityLogger;

  public SecurityAuditPlugin(SecurityLogger securityLogger) {
    this.securityLogger = securityLogger;
  }

  @Override
  public CreateRequest processPreCreate(CreateRequest createRequest)
      throws StopProcessingException {
    return createRequest;
  }

  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest updateRequest, Map<String, Metacard> existingMetacards)
      throws StopProcessingException {
    if (updateRequest == null || !isLocal(updateRequest) || MapUtils.isEmpty(existingMetacards)) {
      return updateRequest;
    }
    for (Map.Entry<Serializable, Metacard> entry : updateRequest.getUpdates()) {
      Metacard updateMetacard = entry.getValue();
      Metacard existingMetacard = existingMetacards.get(entry.getKey().toString());
      Set<AttributeDescriptor> attributeDescriptors =
          updateMetacard.getMetacardType().getAttributeDescriptors();

      for (AttributeDescriptor descriptor : attributeDescriptors) {
        String descriptorName = descriptor.getName();
        if (auditAttributes.contains(descriptorName)) {
          if (hasAttributeValueChanged(
              existingMetacard.getAttribute(descriptorName),
              updateMetacard.getAttribute(descriptorName))) {
            String originalValue = getNonNullAttributeValues(existingMetacard, descriptorName);
            String updateValue = getNonNullAttributeValues(updateMetacard, descriptorName);
            auditMetacardUpdate(descriptorName, updateMetacard.getId(), originalValue, updateValue);
          }
        }
      }
    }

    return updateRequest;
  }

  private boolean hasAttributeValueChanged(Attribute oldAttribute, Attribute newAttribute) {
    if (oldAttribute == null && (newAttribute != null)) {
      // The attribute was added and is not null
      return true;
    } else if (oldAttribute == null || CollectionUtils.isEmpty(oldAttribute.getValues())) {
      // The old attribute is null
      return false;
    } else if (newAttribute == null || CollectionUtils.isEmpty(newAttribute.getValues())) {
      // The attribute has been removed
      return true;
    }
    // The attribute exists in both metacards, check their equality
    return !CollectionUtils.isEqualCollection(oldAttribute.getValues(), newAttribute.getValues());
  }

  private String getNonNullAttributeValues(Metacard metacard, String descriptorName) {
    String value;
    if (metacard.getAttribute(descriptorName) == null
        || metacard.getAttribute(descriptorName).getValues() == null) {
      value = "[NO VALUE]";
    } else {
      value = StringUtils.join(metacard.getAttribute(descriptorName).getValues(), ",");
    }

    return value;
  }

  @Override
  public DeleteRequest processPreDelete(DeleteRequest deleteRequest)
      throws StopProcessingException {
    return deleteRequest;
  }

  @Override
  public DeleteResponse processPostDelete(DeleteResponse deleteResponse)
      throws StopProcessingException {
    return deleteResponse;
  }

  @Override
  public QueryRequest processPreQuery(QueryRequest queryRequest) throws StopProcessingException {
    return queryRequest;
  }

  @Override
  public QueryResponse processPostQuery(QueryResponse queryResponse)
      throws StopProcessingException {
    return queryResponse;
  }

  @Override
  public ResourceRequest processPreResource(ResourceRequest resourceRequest)
      throws StopProcessingException {
    return resourceRequest;
  }

  @Override
  public ResourceResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
      throws StopProcessingException {
    return resourceResponse;
  }

  public void setAuditAttributes(List<String> auditAttributes) {
    securityLogger.audit(
        String.format(
            "Security Audit Plugin configuration changed to audit : %s",
            StringUtils.join(auditAttributes, ",")));
    this.auditAttributes = auditAttributes;
  }

  public void init() {
    securityLogger.audit("Security Audit Plugin started");
  }

  public void destroy() {
    securityLogger.audit("Security Audit Plugin stopped");
  }

  public static boolean isLocal(Request req) {
    return req == null || !req.hasProperties() || isLocal(req.getProperties());
  }

  public static boolean isLocal(Map<String, Serializable> props) {
    return props == null
        || props.get(Constants.LOCAL_DESTINATION_KEY) == null
        || (boolean) props.get(Constants.LOCAL_DESTINATION_KEY);
  }

  @VisibleForTesting
  void auditMetacardUpdate(
      String descriptorName, String metacardId, String originalValue, String updateValue) {
    securityLogger.audit(
        String.format(
            "Attribute %s on metacard %s with value(s) %s was updated to value(s) %s",
            descriptorName, metacardId, originalValue, updateValue));
  }
}
