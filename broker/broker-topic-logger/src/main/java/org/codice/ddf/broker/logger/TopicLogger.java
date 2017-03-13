/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.broker.logger;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TopicLogger extends RouteBuilder {
    private static final Logger LOGGER = LoggerFactory.getLogger(TopicLogger.class);

    private String name = "example.topic";

    private long samplePeriod = 3600L;

    private boolean showHeaders;

    public TopicLogger(CamelContext camelContext) throws Exception {
        setContext(camelContext);
    }

    @Override
    public void configure() throws Exception {

        fromF("sjms:topic:%s", name).routeId(
                name + " period=" + samplePeriod + " headers=" + showHeaders)
                .toF("log:topic-logger?groupInterval=%d", samplePeriod * 1000)
                .sample(samplePeriod, TimeUnit.SECONDS)
                .toF("log:topic-logger?showHeaders=%s&maxChars=100000",
                        String.valueOf(showHeaders));

        LOGGER.info("Starting route: {}", toString());
    }

    public void start() {
        try {
            getContext().addRoutes(this);
        } catch (Exception e) {
            LOGGER.error("Could not start route: {}", toString(), e);
        }
    }

    public void stop(int code) {
        try {
            getContext().removeRouteDefinitions(getRouteCollection().getRoutes());
        } catch (Exception e) {
            LOGGER.error("Could not stop route: {}", toString(), e);
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public void update(Map<String, Object> properties) throws Exception {
        setName(properties.get("name")
                .toString());
        setShowHeaders((Boolean) properties.get("showHeaders"));
        setSamplePeriod((Long) properties.get("samplePeriod"));
        stop(0);
        configure();
        start();
    }

    public void setSamplePeriod(long samplePeriod) {
        this.samplePeriod = samplePeriod;
    }

    public void setShowHeaders(boolean showHeaders) {
        this.showHeaders = showHeaders;
    }
}
