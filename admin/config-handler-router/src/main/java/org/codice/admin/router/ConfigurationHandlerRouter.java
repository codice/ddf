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

package org.codice.admin.router;

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createInvalidFieldMsg;
import static spark.Spark.after;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.Configuration;
import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import spark.servlet.SparkApplication;

public class ConfigurationHandlerRouter implements SparkApplication {

    public static final String CONFIGURATION_TYPE_FIELD = "configurationType";
    public static final String APPLICATION_JSON = "application/json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationHandlerRouter.class);
    private List<ConfigurationHandler> handlers = new ArrayList<>();

    private Gson getGsonParser() {
        RuntimeTypeAdapterFactory rtaf = RuntimeTypeAdapterFactory.of(Configuration.class,
                CONFIGURATION_TYPE_FIELD);
        handlers.stream()
                .map(handler -> handler.getConfigurationType())
                .forEach(configType -> rtaf.registerSubtype(configType.configClass(),
                        configType.configTypeName()));

        return new GsonBuilder().registerTypeAdapterFactory(rtaf)
                .create();
    }

    @Override
    public void init() {
        // TODO: tbatie - 1/16/17 - Comment endpoints
        post("/test/:configHandlerId/:testId", (req, res) -> {
            Report testReport = new Report();
            String configHandlerId = req.params("configHandlerId");
            String testId = req.params("testId");
            ConfigurationHandler configHandler = getConfigurationHandler(configHandlerId);
            if(configHandler == null) {
                res.status(400);
                return testReport.messages(createInvalidFieldMsg("No configuration handler with id of: " + configHandlerId + " found.", configHandlerId));
            }

            Configuration config = getGsonParser().fromJson(req.body(), Configuration.class);
            testReport = configHandler.test(testId, config);

            if (testReport.containsUnsuccessfulMessages()) {
                res.status(400);
            }

            return testReport;
        }, this::toJson);

        post("/persist/:configHandlerId/:persistId", (req, res) -> {
            Report persistReport = new Report();
            String configHandlerId = req.params("configHandlerId");
            String persistId = req.params("persistId");
            ConfigurationHandler configHandler = getConfigurationHandler(configHandlerId);
            if(configHandler == null) {
                res.status(400);
                return persistReport.messages(createInvalidFieldMsg("No configuration handler with id of: " + configHandlerId + " found.", configHandlerId));
            }

            Configuration config = getGsonParser().fromJson(req.body(), Configuration.class);
            persistReport = configHandler.persist(persistId, config);

            if (persistReport.containsUnsuccessfulMessages()) {
                res.status(400);
            }

            return persistReport;
        }, this::toJson);

        post("/probe/:configHandlerId/:probeId", (req, res) -> {
            ProbeReport probeReport = new ProbeReport();
            String configHandlerId = req.params("configHandlerId");
            String probeId = req.params("probeId");
            ConfigurationHandler configHandler = getConfigurationHandler(configHandlerId);

            if(configHandler == null) {
                res.status(400);
                return probeReport.messages(createInvalidFieldMsg("No configuration handler with id of: " + configHandlerId + " found.", configHandlerId));
            }

            Configuration config = getGsonParser().fromJson(req.body(), Configuration.class);
            probeReport = configHandler.probe(probeId, config);

            if (probeReport.containsUnsuccessfulMessages()) {
                res.status(400);
            }

            return probeReport;
        }, this::toJson);

        // TODO: tbatie - 1/15/17 - Need to apply an @Expose to fields, random objects are showing up in the reports
        get("/capabilities",
                (req, res) -> handlers.stream()
                        .map(handler -> handler.getCapabilities())
                        .collect(Collectors.toList()),
                this::toJson);

        get("/capabilities/:configHandlerId",
                (req, res) -> {
                    String configHandlerId = req.params("configHandlerId");
                    ConfigurationHandler configHandler = getConfigurationHandler(configHandlerId);
                    if(configHandler == null) {
                        res.status(400);
                        return new Report(createInvalidFieldMsg("No configuration handler with id of: " + configHandlerId + " found.", configHandlerId));
                    }
                    return configHandler.getCapabilities();
                },
                this::toJson);

        get("/configurations/:configHandlerId",
                (req, res) -> {
                    String configHandlerId = req.params("configHandlerId");
                    ConfigurationHandler configHandler = getConfigurationHandler(configHandlerId);
                    if(configHandler == null) {
                        res.status(400);
                        return new Report(createInvalidFieldMsg("No configuration handler with id of: " + configHandlerId + " found.", configHandlerId));
                    }
                    return configHandler.getConfigurations();
                },
                this::toJson);

        after("/*", (req, res) -> res.type(APPLICATION_JSON));

        exception(Exception.class, (ex, req, res) -> {
            LOGGER.error("Configuration Handler router error: ", ex);
            res.status(500);
            res.type(APPLICATION_JSON);
            res.body(exToJSON(ex));
        });
    }

    private String exToJSON(Exception ex) {
        Map<String, Object> e = new HashMap<>();
        e.put("stackTrace", ex.getStackTrace());
        e.put("cause", ex.toString());
        return new Gson().toJson(e);
    }

    public ConfigurationHandler getConfigurationHandler(String configurationId) {
        Optional<ConfigurationHandler> foundConfigHandler = handlers.stream()
                .filter(handler -> handler.getConfigurationHandlerId()
                        .equals(configurationId))
                .findFirst();

        return foundConfigHandler.isPresent() ? foundConfigHandler.get() : null;
    }

    private String toJson(Object body) {
        return getGsonParser().toJson(body);
    }

    public void setConfigurationHandlers(List<ConfigurationHandler> configurationHandlers) {
        handlers = configurationHandlers;
    }

}
