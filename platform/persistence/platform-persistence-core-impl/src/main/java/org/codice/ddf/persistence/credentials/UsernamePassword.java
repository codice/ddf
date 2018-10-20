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

public class UsernamePassword {

  private String password;
  private String username;

  public void update() {
    //    ConfigurationStore.getInstance().setUsername(username);
    //    ConfigurationStore.getInstance().setPassword(password);
  }

  @SuppressWarnings("unused")
  public String getUsername() {
    // return password;
    return HttpSolrClientFactory.getUsername();
  }

  @SuppressWarnings("unused")
  public void setUsername(String username) {
    // this.username = username;
    // update();
    HttpSolrClientFactory.setUsername(username);
  }

  @SuppressWarnings("unused")
  public String getPassword() {
    // return password;
    return HttpSolrClientFactory.getUsername();
  }

  @SuppressWarnings("unused")
  public void setPassword(String password) {
    HttpSolrClientFactory.setPassword(password);
    //    this.password = password;
    //    update();
  }
}
