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

import org.codice.ddf.admin.api.handler.Configuration;
import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import spark.servlet.SparkApplication;

public class ConfigurationHandlerRouter implements SparkApplication {

    public static final String APPLICATION_JSON = "application/json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationHandlerRouter.class);

    public static String contextPath;

    private Map<String, ConfigurationHandler> handlers = new HashMap<>();

    private Gson getGsonParser() {
        RuntimeTypeAdapterFactory rtaf = RuntimeTypeAdapterFactory.of(Configuration.class,
                "configurationType");
        handlers.keySet().forEach(key -> rtaf.registerSubtype(handlers.get(key).getConfigClass(), key));
        return new GsonBuilder().registerTypeAdapterFactory(rtaf)
                .create();
    }

    public ConfigurationHandler getConfigurationHandler(
            List<ConfigurationHandler> configurationHandlers, String configurationId) {
        Optional<ConfigurationHandler> foundConfigHandler = configurationHandlers.stream()
                .filter(handler -> handler.getConfigurationHandlerId()
                        .equals(configurationId))
                .findFirst();
        return foundConfigHandler.isPresent() ? foundConfigHandler.get() : null;
    }

    private String toJson(Object body) {
        return getGsonParser().toJson(body);
    }

    @Override
    public void init() {

        post("/test/:configHandlerId/:testId", (req, res) -> {
            Configuration config = getGsonParser().fromJson(req.body(), Configuration.class);
            TestReport testResults = getConfigurationHandler(new ArrayList<>(handlers.values()),
                    req.params("configHandlerId")).test(req.params("testId"), config);

            if (testResults.containsUnsuccessfulMessages()) {
                res.status(400);
            }

            return testResults;
        }, this::toJson);

        post("/persist/:configHandlerId/:persistId", (req, res) -> {
            Configuration config = getGsonParser().fromJson(req.body(), Configuration.class);
            // TODO: tbatie - 11/29/16 - Check if configurationHandler is running before testing
            TestReport results = getConfigurationHandler(new ArrayList<>(handlers.values()),
                    req.params("configHandlerId")).persist(config, req.params("persistId"));

            if (results.containsUnsuccessfulMessages()) {
                res.status(400);
            }

            return results;
        }, this::toJson);

        post("/probe/:configHandlerId/:probeId", (req, res) -> {
            Configuration config = getGsonParser().fromJson(req.body(), Configuration.class);
            ProbeReport report = getConfigurationHandler(new ArrayList<>(handlers.values()),
                    req.params("configHandlerId")).probe(req.params("probeId"), config);

            if (report.containsUnsuccessfulMessages()) {
                res.status(400);
            }

            return report;
        }, this::toJson);

        get("/capabilities",
                (req, res) ->
                    handlers.values().stream().map(handler -> handler.getCapabilities()).collect(Collectors.toList()),
                this::toJson);

        get("/capabilities/:configHandlerId",
                (req, res) ->
                    getConfigurationHandler(new ArrayList<>(handlers.values()), req.params("configHandlerId")).getCapabilities(),
                this::toJson);

        get("/configurations/:configHandlerId",
                (req, res) -> getConfigurationHandler(new ArrayList<>(handlers.values()),
                        req.params("configHandlerId")).getConfigurations(),
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

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }

    public void setConfigurationHandlers(List<ConfigurationHandler> configurationHandlers) {
        configurationHandlers.forEach(handler -> handlers.put(handler.getConfigurationHandlerId(), handler));
    }

    public void registerConfigType(ConfigurationHandler handler) {
        handlers.put(handler.getConfigurationHandlerId(), handler);
    }

}
