/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.idp.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Function;

import org.codice.ddf.configuration.SystemBaseUrl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.action.Action;
import ddf.action.ActionProvider;
import ddf.action.impl.ActionImpl;
import ddf.security.encryption.EncryptionService;

public class IdpLogoutActionProvider implements ActionProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(IdpLogoutActionProvider.class);

    private static final String ID = "security.logout.idp";

    private static final String TITLE = "Identity Provider Logout";

    private static final String DESCRIPTION = "Logging out of the Identity Provider(IDP) realm will logout all external accounts signed in to that Identity Provider.";

    EncryptionService encryptionService;

    // TODO (RAP) - Add javadoc
    @Override
    public <T> Action getAction(T var) {

        URL logoutUrl = null;
        if (var instanceof Function) {
            try {
                @SuppressWarnings("unchecked")
                String nameId = ((Function<String, String>) var).apply("idp");

                String nameIdTimestamp = nameId + "\n" + System.currentTimeMillis();
                nameIdTimestamp = encryptionService.encrypt(nameIdTimestamp);
                logoutUrl = new URL(new SystemBaseUrl().constructUrl(
                        "/saml/logout/start" +  "?NameId=" + nameId + "&NameIdTimestamp" + nameIdTimestamp, true));

            } catch (MalformedURLException e) {
                LOGGER.info("Unable to resolve URL: {}",
                        new SystemBaseUrl().constructUrl("/logout/local"));
            } catch (ClassCastException e) {
                LOGGER.debug("Unable to cast parameter to Function<String, String>, {}", var, e);
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

}
