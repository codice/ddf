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
package ddf.catalog.security.operation.plugin;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Operation;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.AccessPlugin;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.SubjectOperations;
import ddf.security.permission.CollectionPermission;
import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.impl.KeyValueCollectionPermissionImpl;
import java.util.Map;
import java.util.Set;

/**
 * Security-based plugin that check the pre create/update/delete/query request to make sure the
 * subject has the appropriate attributes to make the request.
 */
public class OperationPlugin implements AccessPlugin {

  private SubjectOperations subjectOperations;

  @Override
  public CreateRequest processPreCreate(CreateRequest input) throws StopProcessingException {
    checkOperation(input);
    return input;
  }

  @Override
  public UpdateRequest processPreUpdate(
      UpdateRequest input, Map<String, Metacard> existingMetacards) throws StopProcessingException {
    checkOperation(input);
    return input;
  }

  @Override
  public DeleteRequest processPreDelete(DeleteRequest input) throws StopProcessingException {
    checkOperation(input);
    return input;
  }

  @Override
  public DeleteResponse processPostDelete(DeleteResponse input) throws StopProcessingException {
    return input;
  }

  @Override
  public QueryRequest processPreQuery(QueryRequest input) throws StopProcessingException {
    checkOperation(input);
    return input;
  }

  @Override
  public QueryResponse processPostQuery(QueryResponse input) throws StopProcessingException {
    return input;
  }

  @Override
  public ResourceRequest processPreResource(ResourceRequest input) throws StopProcessingException {
    checkOperation(input);
    return input;
  }

  @Override
  public ResourceResponse processPostResource(ResourceResponse input, Metacard metacard)
      throws StopProcessingException {
    return input;
  }

  /**
   * checkOperation will throw a StopProcessingException if the operation is not permitted based on
   * the the subjects attributes and the operations property "operation.security"
   *
   * @param operation The operation to check
   * @throws StopProcessingException
   */
  private void checkOperation(Operation operation) throws StopProcessingException {
    if (!operation.hasProperties()
        || !operation.containsPropertyName(PolicyPlugin.OPERATION_SECURITY)) {
      return;
    }

    Object securityAssertion = operation.getPropertyValue(SecurityConstants.SECURITY_SUBJECT);
    Subject subject;
    if (securityAssertion instanceof Subject) {
      subject = (Subject) securityAssertion;
    } else {
      throw new StopProcessingException(
          "Unable to filter contents of current message, no user Subject available.");
    }

    Map<String, Set<String>> perms =
        (Map<String, Set<String>>) operation.getPropertyValue(PolicyPlugin.OPERATION_SECURITY);
    KeyValueCollectionPermission securityPermission =
        new KeyValueCollectionPermissionImpl(CollectionPermission.READ_ACTION, perms);

    if (!subject.isPermitted(securityPermission)) {
      String userName = "UNKNOWN";
      if (subjectOperations != null) {
        userName = subjectOperations.getName(subject, userName);
      }
      throw new StopProcessingException(
          "User " + userName + " does not have the required attributes " + perms);
    }
  }

  public void setSubjectOperations(SubjectOperations subjectOperations) {
    this.subjectOperations = subjectOperations;
  }
}
