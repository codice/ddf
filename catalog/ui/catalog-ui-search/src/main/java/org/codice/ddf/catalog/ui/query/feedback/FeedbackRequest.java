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
package org.codice.ddf.catalog.ui.query.feedback;

public class FeedbackRequest {
  private String username;

  private String email;

  private String authUsername;

  private String workspaceId;

  private String workspaceName;

  private String query;

  private String queryInitiated;

  private String queryResults;

  private String queryStatus;

  private String comments;

  public FeedbackRequest() {}

  /**
   * Username providing feedback.
   *
   * @return username
   */
  public String getUsername() {
    return username;
  }

  /**
   * Sets the username for the user providing feedback.
   *
   * @param username Username from the search UI.
   */
  public void setUsername(String username) {
    this.username = username;
  }

  /**
   * Email for the user providing feedback.
   *
   * @return email
   */
  public String getEmail() {
    return email;
  }

  /**
   * Set the email address for the user providing feedback.
   *
   * @param email User's email address.
   */
  public void setEmail(String email) {
    this.email = email;
  }

  /**
   * The workspace ID that the feedback pertains to.
   *
   * @return workspaceId
   */
  public String getWorkspaceId() {
    return workspaceId;
  }

  /**
   * Set the workspace id from which the feedback comments are being provided.
   *
   * @param workspaceId Specific workspace id
   */
  public void setWorkspaceId(String workspaceId) {
    this.workspaceId = workspaceId;
  }

  /**
   * Workspace name that the feedback pertains to.
   *
   * @return workspaceName
   */
  public String getWorkspaceName() {
    return workspaceName;
  }

  /**
   * Set the workspace id from which the feedback comments are being provided
   *
   * @param workspaceName Friendly name of the workspace as set by the workspace creator
   */
  public void setWorkspaceName(String workspaceName) {
    this.workspaceName = workspaceName;
  }

  /**
   * Authenticated username from the service side receiving feedback.
   *
   * @return authUsername
   */
  public String getAuthUsername() {
    return authUsername;
  }

  /**
   * Set the authenticated username as known by the service receiving the feedback comments.
   *
   * @param authUsername Authenticated username
   */
  public void setAuthUsername(String authUsername) {
    this.authUsername = authUsername;
  }

  /**
   * The catalog query.
   *
   * @return query
   */
  public String getQuery() {
    return query;
  }

  /**
   * Set the specific query that was executed.
   *
   * @param query Query that was executed.
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * The time that the query was initiated.
   *
   * @return queryInitiated
   */
  public String getQueryInitiated() {
    return queryInitiated;
  }

  /**
   * Sets the time that the query was initiated.
   *
   * @param queryInitiated Time of query.
   */
  public void setQueryInitiated(String queryInitiated) {
    this.queryInitiated = queryInitiated;
  }

  /**
   * Results from the query.
   *
   * @return queryResults
   */
  public String getQueryResults() {
    return queryResults;
  }

  /**
   * Sets the query results
   *
   * @param queryResults - Query results from the search criteria provided back to the user.
   */
  public void setQueryResults(String queryResults) {
    this.queryResults = queryResults;
  }

  /**
   * Status of the query as returned to the UI
   *
   * @return queryStatus
   */
  public String getQueryStatus() {
    return queryStatus;
  }

  /**
   * Sets the query status
   *
   * @param queryStatus Query status from the search.
   */
  public void setQueryStatus(String queryStatus) {
    this.queryStatus = queryStatus;
  }

  /**
   * User provided comments about the query.
   *
   * @return comments
   */
  public String getComments() {
    return comments;
  }

  /**
   * Set the user provided comments about the query.
   *
   * @param comments User query comments.
   */
  public void setComments(String comments) {
    this.comments = comments;
  }
}
