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
package org.codice.ui.admin.sources.config.test;

import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.PING_TIMEOUT;
import static org.codice.ui.admin.sources.config.SourceConfigurationHandlerImpl.VALID_URL_TEST_ID;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.buildMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;

import org.codice.ui.admin.sources.config.SourceConfiguration;
import org.codice.ui.admin.wizard.api.test.TestMethod;
import org.codice.ui.admin.wizard.api.test.TestReport;

import com.google.common.collect.ImmutableMap;

public class ValidUrlTestMethod extends TestMethod<SourceConfiguration> {

    private static final String DESCRIPTION = "Attempts to connect to a given hostname and port";

    private static final String HOSTNAME = "hostname";

    private static final String PORT = "port";

    private static final Map<String, String> REQUIRED_FIELDS = ImmutableMap.of(HOSTNAME,
            "The hostname to attempt to connect to.",
            PORT,
            "The port to use to connect.");

    private static final String VERIFIED_URL = "verifiedUrl";

    private static final String CANNOT_CONNECT = "cannotConnect";

    private static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(VERIFIED_URL,
            "Connected to hostname and port.");

    private static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(CANNOT_CONNECT,
            "Unable to connect to hostname and port.");

    public ValidUrlTestMethod() {
        super(VALID_URL_TEST_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                null,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public TestReport test(SourceConfiguration configuration) {
        try (Socket connection = new Socket()) {
            connection.connect(new InetSocketAddress(configuration.sourceHostName(),
                    configuration.sourcePort()), PING_TIMEOUT);
            connection.close();
            return new TestReport(buildMessage(SUCCESS, "Was able to reach source successfully"));
        } catch (IOException e) {
            return new TestReport(buildMessage(FAILURE,
                    "Unable to reach specified hostname and port."));
        }
    }
}
