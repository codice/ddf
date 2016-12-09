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

package org.codice.ui.admin.wizard;

import static spark.Spark.after;
import static spark.Spark.exception;
import static spark.Spark.get;
import static spark.Spark.post;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.ProbeReport;
import org.codice.ui.admin.wizard.api.TestReport;
import org.codice.ui.admin.wizard.config.Configuration;
import org.codice.ui.admin.wizard.config.RuntimeTypeAdapterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import spark.servlet.SparkApplication;

public class ConfigurationRouter implements SparkApplication {

    public static final String APPLICATION_JSON = "application/json";

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationRouter.class);

    public static String contextPath;

    private Map<String, Class> subtypesToRegister = new HashMap<>();

    private List<ConfigurationHandler> configurationHandlers;

    private Gson getGsonParser() {
        RuntimeTypeAdapterFactory rtaf = RuntimeTypeAdapterFactory.of(Configuration.class,
                "configurationType");
        for(Map.Entry<String, Class> entry : subtypesToRegister.entrySet()) {
            rtaf.registerSubtype(entry.getValue(), entry.getKey());
        }
        return new GsonBuilder().registerTypeAdapterFactory(rtaf).create();
    }

    public static ConfigurationHandler getConfigurationHandler(
            List<ConfigurationHandler> configurationHandlers, String configurationId) {
        Optional<ConfigurationHandler> foundConfigHandler = configurationHandlers.stream()
                .filter(handler -> handler.getConfigurationHandlerId()
                        .equals(configurationId))
                .findFirst();
        return foundConfigHandler.isPresent() ? foundConfigHandler.get() : null;
    }

    @Override
    public void init() {

        post("/test/:configHandlerId/:testId", (req, res) -> {
            Configuration config = getGsonParser().fromJson(req.body(), Configuration.class);
            TestReport testResults = getConfigurationHandler(configurationHandlers,
                    req.params("configHandlerId")).test(req.params("testId"), config);

            if (testResults.containsUnsuccessfulMessages()) {
                res.status(400);
            }

            return testResults;
        }, getGsonParser()::toJson);

        post("/persist/:configHandlerId", (req, res) -> {
            Configuration config = getGsonParser().fromJson(req.body(), Configuration.class);
            // TODO: tbatie - 11/29/16 - Check if configurationHandler is running before testing
            TestReport results = getConfigurationHandler(configurationHandlers,
                    req.params("configHandlerId")).persist(config);

            if (results.containsUnsuccessfulMessages()) {
                res.status(400);
            }

            return results;
        }, getGsonParser()::toJson);

        post("/probe/:configHandlerId/:probeId", (req, res) -> {
            Configuration config = getGsonParser().fromJson(req.body(), Configuration.class);
            ProbeReport report = getConfigurationHandler(configurationHandlers,
                    req.params("configHandlerId")).probe(req.params("probeId"), config);

            if (report.containsUnsuccessfulMessages()) {
                res.status(400);
            }

            return report;
        }, getGsonParser()::toJson);

        get("/capabilities/:configHandlerId",
                (req, res) -> getConfigurationHandler(configurationHandlers,
                        req.params("configHandlerId")).getCapabilities(),
                getGsonParser()::toJson);

        get("/configurations/:configHandlerId",
                (req, res) -> getConfigurationHandler(configurationHandlers,
                        req.params("configHandlerId")).getConfigurations(),
                getGsonParser()::toJson);

        after("/*", (req, res) -> res.type(APPLICATION_JSON));

        exception(Exception.class, (ex, req, res) -> {
            LOGGER.error("Wizard router error: ", ex);
            // TODO: tbatie - 10/26/16 - Remove this on merge
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
        this.configurationHandlers = configurationHandlers;
        configurationHandlers.forEach(this::registerConfigType);
    }

    public void registerConfigType(ConfigurationHandler handler) {
        subtypesToRegister.put((String) handler.getSubtype()
                        .getKey(),
                (Class) handler.getSubtype()
                        .getValue());
    }

}
