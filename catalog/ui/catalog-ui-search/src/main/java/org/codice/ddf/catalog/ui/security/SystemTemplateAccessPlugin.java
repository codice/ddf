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
package org.codice.ddf.catalog.ui.security;

import static org.codice.ddf.catalog.ui.security.Constants.SYSTEM_TEMPLATE;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.StopProcessingException;
import java.io.Serializable;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.security.common.Security;

public class SystemTemplateAccessPlugin implements AccessPlugin {
  private final EndpointUtil util;

  private final Supplier<Boolean> subjectHasAdmin;

  public SystemTemplateAccessPlugin(EndpointUtil util) {
    this(util, () -> Security.getInstance().javaSubjectHasAdminRole());
  }

  public SystemTemplateAccessPlugin(EndpointUtil util, Supplier<Boolean> subjectHasAdmin) {
    this.util = util;
    this.subjectHasAdmin = subjectHasAdmin;
  }

  /**
   * Only an admin can create system templates. This is an extra layer of protection in case the
   * policy for shareable metacards changes or malicious users try to use a different endpoint.
   *
   * @param input the {@link CreateRequest} to process.
   * @return the same {@link CreateRequest} if none of the metacards to create are considered system
   *     templates, or if the current subject is an admin.
   * @throws StopProcessingException if any of the metacards to create are system templates and the
   *     current subject is not an admin.
   */
  @Override
  public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
    if (subjectHasAdmin.get()) {
      return input;
    }
    if (input
        .getMetacards()
        .stream()
        .map(Metacard::getTags)
        .flatMap(Set::stream)
        .anyMatch(SYSTEM_TEMPLATE::equals)) {
      throw new StopProcessingException("System templates can only be created by an admin");
    }
    return input;
  }

  /**
   * No one is allowed to update system templates.
   *
   * @param input the {@link UpdateRequest} to process.
   * @param existingMetacards the Map of {@link Metacard}s that currently exist.
   * @return the same {@link UpdateRequest} if no system templates are targets for the update.
   * @throws StopProcessingException if any of the metacards targeted for the update are system
   *     templates.
   */
  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest input, Map<String, Metacard> existingMetacards) throws StopProcessingException {
    Set<String> systemTemplateIds =
        util.getMetacardsByFilter(SYSTEM_TEMPLATE)
            .values()
            .stream()
            .map(Result::getMetacard)
            .map(Metacard::getId)
            .collect(Collectors.toSet());
    if (existingMetacards.keySet().stream().anyMatch(systemTemplateIds::contains)) {
      throw new StopProcessingException("System templates cannot be updated");
    }
    return input;
  }

  /**
   * Only an admin can delete system templates. This is an extra layer of protection in case the
   * policy for shareable metacards changes or malicious users try to use a different endpoint.
   *
   * @param input the {@link DeleteRequest} to process.
   * @return the same {@link DeleteRequest} if none of the metacard IDs point to system templates,
   *     or if the current subject is an admin.
   * @throws StopProcessingException if any of the metacard IDs on the {@link DeleteRequest} point
   *     to system templates and the current subject is not an admin.
   */
  @Override
  public DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException {
    if (subjectHasAdmin.get()) {
      return input;
    }
    if (!Metacard.ID.equals(input.getAttributeName())) {
      return input;
    }
    Set<? extends Serializable> systemTemplateIds =
        util.getMetacardsByFilter(SYSTEM_TEMPLATE)
            .values()
            .stream()
            .map(Result::getMetacard)
            .map(Metacard::getId)
            .collect(Collectors.toSet());
    if (input.getAttributeValues().stream().anyMatch(systemTemplateIds::contains)) {
      throw new StopProcessingException("System templates cannot be deleted");
    }
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
}
