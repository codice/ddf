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
package org.codice.ddf.admin.sources.impl.probe;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.federation.SourceConfiguration;
import org.codice.ddf.admin.api.handler.SourceConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;

import com.google.common.collect.ImmutableMap;

public class GetConfigHandlersProbeMethod extends ProbeMethod<SourceConfiguration> {

    public static final String GET_CONFIG_HANDLER_ID = "config-handlers";
    public static final String DESCRIPTION = "Retrieves a list of all running configuration handlers.";

    public static final String SRC_CONFIG_HNDLRS = "sourceConfigurationHandlers";
    public static final String CONFIG_HANDLER_ID = "id";
    public static final String DISPLAY_NAME_KEY = "name";

    public static final Map<String, String> RETURN_TYPES = ImmutableMap.of(SRC_CONFIG_HNDLRS, "List of configuration handlers with keys: " + CONFIG_HANDLER_ID + " and " + DISPLAY_NAME_KEY);

    private List<SourceConfigurationHandler> handlers;

    public GetConfigHandlersProbeMethod(List<SourceConfigurationHandler> handlers) {
        super(GET_CONFIG_HANDLER_ID,
                DESCRIPTION,
                null,
                null,
                null,
                null,
                null,
                RETURN_TYPES);
        this.handlers = handlers;
    }

    @Override
    public ProbeReport probe(SourceConfiguration configuration) {
        List<ImmutableMap> collect = handlers.stream()
                .map(handler -> ImmutableMap.builder()
                        .put(CONFIG_HANDLER_ID, handler.getConfigurationHandlerId())
                        .put(DISPLAY_NAME_KEY, handler.getSourceDisplayName())
                        .build())
                .collect(Collectors.toList());

        return new ProbeReport().probeResult(SRC_CONFIG_HNDLRS, collect);
    }
}
