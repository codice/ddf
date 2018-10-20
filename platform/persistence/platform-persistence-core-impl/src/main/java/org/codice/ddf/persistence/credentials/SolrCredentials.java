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
package org.codice.ddf.persistence.credentials;

import org.codice.solr.factory.impl.HttpSolrClientFactory;

public class SolrCredentials {

  // FYI. It needs this state to hold default values. Simply pointing the accessors to the
  // static vars on HttpSolrClientFactory doesn't cut the mustard. Everything ends up null.
  private String password;
  private String username;
  private Boolean useBasicAuth;

  @SuppressWarnings("unused")
  public Boolean getUseBasicAuth() {
    return useBasicAuth;
  }

  @SuppressWarnings("unused")
  public void setUseBasicAuth(Boolean useBasicAuth) {
    this.useBasicAuth = useBasicAuth;
    HttpSolrClientFactory.setUseBasicAuth(getUseBasicAuth());
  }

  @SuppressWarnings("unused")
  public String getUsername() {
    return username;
  }

  @SuppressWarnings("unused")
  public void setUsername(String username) {
    this.username = username;
    HttpSolrClientFactory.setUsername(getUsername());
  }

  @SuppressWarnings("unused")
  public String getPassword() {
    return password;
  }

  @SuppressWarnings("unused")
  public void setPassword(String password) {
    this.password = password;
    HttpSolrClientFactory.setPassword(getPassword());
  }
}
