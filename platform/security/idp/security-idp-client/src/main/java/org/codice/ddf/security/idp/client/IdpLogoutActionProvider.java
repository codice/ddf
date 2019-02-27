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
package org.codice.ddf.security.idp.client;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.security.SubjectUtils;
import ddf.security.encryption.EncryptionService;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import org.apache.shiro.subject.Subject;
import org.codice.ddf.configuration.SystemBaseUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdpLogoutActionProvider implements ActionProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(IdpLogoutActionProvider.class);

  private static final String ID = "security.logout.idp";

  private static final String TITLE = "Identity Provider Logout";

  private static final String DESCRIPTION =
      "Logging out of the Identity Provider(IDP) realm will logout all external accounts signed in to that Identity Provider.";

  EncryptionService encryptionService;

  /**
   * *
   *
   * @param <T> is a Map<String, Subject>
   * @param realmSubjectMap containing a realm of "idp" with the corresponding subject
   * @return IdpLogoutActionProvider containing the logout url
   */
  public <T> Action getAction(T realmSubjectMap) {
    if (!canHandle(realmSubjectMap)) {
      return null;
    }

    String logoutUrlString = "";
    URL logoutUrl = null;
    if (realmSubjectMap instanceof Map) {
      try {

        @SuppressWarnings("unchecked")
        String nameId =
            SubjectUtils.getName((Subject) ((Map) realmSubjectMap).get("idp"), "You", true);

        String nameIdTimestamp = nameId + "\n" + System.currentTimeMillis();
        nameIdTimestamp = URLEncoder.encode(encryptionService.encrypt(nameIdTimestamp));
        logoutUrlString =
            SystemBaseUrl.EXTERNAL.constructUrl(
                "/saml/logout/request?EncryptedNameIdTime=" + nameIdTimestamp, true);
        logoutUrl = new URL(logoutUrlString);

      } catch (MalformedURLException e) {
        LOGGER.info("Unable to resolve URL: {}", logoutUrlString);
      } catch (ClassCastException e) {
        LOGGER.debug("Unable to cast parameter to Map<String, Subject>, {}", realmSubjectMap, e);
      }
    }
    return new ActionImpl(ID, TITLE, DESCRIPTION, logoutUrl);
  }

  @Override
  public String getId() {
    return ID;
  }

  public void setEncryptionService(EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }

  private <T> boolean canHandle(T subject) {
    return subject instanceof Map;
  }
}
