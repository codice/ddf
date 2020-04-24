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
package org.codice.ddf.security.servlet.local.logout;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.security.SecurityConstants;
import ddf.security.Subject;
import ddf.security.SubjectOperations;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultLogoutAction implements ActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultLogoutAction.class);

  private static final String ID = "security.logout.default";

  private static final String TITLE = "Local Logout";

  private static final String DESCRIPTION =
      "Logging out of local system. Accounts signed into external identity providers will remain logged in.";

  private static final String USER_PASS_TOKEN_TYPE = "userpass";

  private static final String GUEST_TOKEN_TYPE = "guest";

  private static final String PKI_TOKEN_TYPE = "pki";

  private static URL logoutUrl = null;

  private SubjectOperations subjectOperations;

  static {
    try {
      logoutUrl = new URL(SystemBaseUrl.EXTERNAL.constructUrl("/logout/local"));
    } catch (MalformedURLException e) {
      LOGGER.info(
          "Unable to resolve URL: {}", SystemBaseUrl.EXTERNAL.constructUrl("/logout/local"));
    }
  }

  /**
   * *
   *
   * @param <T> is a Map<String, Subject>
   * @param subjectMap containing the corresponding subject
   * @return OidcLogoutActionProvider containing the logout url
   */
  @Override
  public <T> Action getAction(T subjectMap) {
    if (!canHandle(subjectMap)) {
      return null;
    }

    return new ActionImpl(ID, TITLE, DESCRIPTION, logoutUrl);
  }

  private <T> boolean canHandle(T subjectMap) {
    if (!(subjectMap instanceof Map)) {
      return false;
    }

    Object subject = ((Map) subjectMap).get(SecurityConstants.SECURITY_SUBJECT);
    if (!(subject instanceof Subject)) {
      return false;
    }

    String type = subjectOperations.getType((Subject) subject);
    return type != null
        && (type.equals(GUEST_TOKEN_TYPE)
            || type.equals(USER_PASS_TOKEN_TYPE)
            || type.equals(PKI_TOKEN_TYPE));
  }

  @Override
  public String getId() {
    return ID;
  }

  public void setSubjectOperations(SubjectOperations subjectOperations) {
    this.subjectOperations = subjectOperations;
  }
}
