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
package org.codice.ui.admin.sources.config;

import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.PING_TIMEOUT;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.buildMessage;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Optional;

import org.codice.ui.admin.wizard.api.ConfigurationMessage;

public class SourceUtils {

    public static Optional<ConfigurationMessage> endpointIsReachable(SourceConfiguration config) {
        try {
            URLConnection urlConnection =
                    (new URL(config.endpointUrl())).openConnection();
            urlConnection.setConnectTimeout(PING_TIMEOUT);
            urlConnection.connect();
        } catch (MalformedURLException | IllegalArgumentException e) {
            return Optional.of(buildMessage(FAILURE, "URL is improperly formatted."));
        } catch (Exception e) {
            Optional.of(buildMessage(FAILURE, "Unable to reach specified URL."));
        }
        return Optional.empty();
    }

}
