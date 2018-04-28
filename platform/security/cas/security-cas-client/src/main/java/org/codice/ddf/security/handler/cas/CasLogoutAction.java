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
package org.codice.ddf.security.handler.cas;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import java.net.MalformedURLException;
import java.net.URL;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CasLogoutAction implements ActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(CasLogoutAction.class);

  private static final String ID = "security.logout.cas";

  private static final String TITLE = "CAS Logout";

  private static final String DESCRIPTION = "Logging out of CAS";

  private static URL logoutUrl = null;

  static {
    try {
      logoutUrl = new URL(SystemBaseUrl.constructUrl("/cas/logout", true));
    } catch (MalformedURLException e) {
      LOGGER.info("Unable to resolve URL: {}", e);
    }
  }

  @Override
  public <T> Action getAction(T subject) {
    return new ActionImpl(ID, TITLE, DESCRIPTION, logoutUrl);
  }

  @Override
  public String getId() {
    return ID;
  }
}
