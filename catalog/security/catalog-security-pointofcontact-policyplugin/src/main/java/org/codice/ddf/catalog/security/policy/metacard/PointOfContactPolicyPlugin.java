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
package org.codice.ddf.catalog.security.policy.metacard;

import static ddf.catalog.Constants.OPERATION_TRANSACTION_KEY;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import ddf.security.permission.Permissions;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Security-based plugin that adds a policy if the point-of-contact field changed on preUpdate. */
public class PointOfContactPolicyPlugin implements PolicyPlugin {
  private static final String[] PERMISSION_STRING = {
    "read-only=Cannot update the point-of-contact field"
  };

  private static final Map<String, Set<String>> PERMISSION_MAP =
      Permissions.parsePermissionsFromString(PERMISSION_STRING);

  private static final Logger LOGGER = LoggerFactory.getLogger(PointOfContactPolicyPlugin.class);

  @Override
  public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreUpdate(Metacard newMetacard, Map<String, Serializable> properties)
      throws StopProcessingException {
    // If it's not a resource metacard, don't apply the policy.
    if (!newMetacard.getTags().isEmpty() && !newMetacard.getTags().contains("resource")) {
      return new PolicyResponseImpl();
    }

    List<Metacard> previousStateMetacards =
        ((OperationTransaction) properties.get(OPERATION_TRANSACTION_KEY))
            .getPreviousStateMetacards();

    Metacard previous;
    previous =
        previousStateMetacards.stream()
            .filter(x -> x.getId().equals(newMetacard.getId()))
            .findFirst()
            .orElse(null);

    return pointOfContactChanged(newMetacard, previous)
        ? new PolicyResponseImpl(null, PERMISSION_MAP)
        : new PolicyResponseImpl();
  }

  private boolean pointOfContactChanged(Metacard newMetacard, @Nullable Metacard previousMetacard) {
    if (previousMetacard == null) {
      LOGGER.debug("Cannot locate metacard {} for update.", newMetacard.getId());
      return false;
    }

    Attribute newPointOfContact = newMetacard.getAttribute(Metacard.POINT_OF_CONTACT);
    Attribute oldPointOfContact = previousMetacard.getAttribute(Metacard.POINT_OF_CONTACT);

    if (newPointOfContact != null && oldPointOfContact != null) {
      return !newPointOfContact.getValue().equals(oldPointOfContact.getValue());
    }

    // Return true if only one of them is null
    return newPointOfContact != oldPointOfContact;
  }

  @Override
  public PolicyResponse processPreDelete(
      List<Metacard> metacards, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostDelete(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreQuery(Query query, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreResource(ResourceRequest resourceRequest)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }
}
