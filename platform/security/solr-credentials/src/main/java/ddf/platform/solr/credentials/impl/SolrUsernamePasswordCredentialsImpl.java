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
package ddf.platform.solr.credentials.impl;

import com.sun.istack.internal.Nullable;
import ddf.platform.solr.credentials.api.SolrUsernamePasswordCredentials;


/**
 * A class for collecting username and password for authentication to Solr.
 */

public class SolrUsernamePasswordCredentialsImpl implements SolrUsernamePasswordCredentials {

  private String password;
  private String username;

  @Override
  public synchronized @Nullable String getUsername() {
    return username;
  }

  @Override
  public synchronized void setUsername(String username) {
    this.username = username;
  }

  @Override
  public synchronized @Nullable String getPassword() {
    return password;
  }

  @Override
  public synchronized void setPassword(String password) {
    this.password = password;
  }
}
