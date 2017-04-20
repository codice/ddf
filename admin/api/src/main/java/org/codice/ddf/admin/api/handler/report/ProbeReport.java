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

package org.codice.ddf.admin.api.handler.report;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.handler.ConfigurationMessage;

/**
 * A special {@link Report} with an additional {@link Map} of information about results from a
 * {@link org.codice.ddf.admin.api.handler.ConfigurationHandler#probe(String, org.codice.ddf.admin.api.config.Configuration)} call.
 */
public class ProbeReport extends Report {

    private Map<String, Object> probeResults = new HashMap<>();

    public ProbeReport() {
        super();
    }

    public ProbeReport(List<ConfigurationMessage> messages) {
        super(messages);
    }

    public ProbeReport(ConfigurationMessage... messages) {
        super(Arrays.asList(messages));
    }

    public static final ProbeReport createProbeReport(Map<String, String> successTypes,
            Map<String, String> failureTypes, Map<String, String> warningTypes, String result) {
        return new ProbeReport(Report.createReport(successTypes, failureTypes, warningTypes, result).messages());
    }

    public static final ProbeReport createProbeReport(Map<String, String> successTypes,
            Map<String, String> failureTypes, Map<String, String> warningTypes, List<String> results) {
        return new ProbeReport(Report.createReport(successTypes, failureTypes, warningTypes, results).messages());
    }

    //Setters
    public ProbeReport probeResult(String key, Object value) {
        if(value != null) {
            probeResults.put(key, value);
        }
        return this;
    }

    public ProbeReport probeResults(Map<String, Object> results) {
        probeResults.putAll(results);
        return this;
    }

    public Map<String, Object> getProbeResults() {
        return probeResults;
    }

    public ProbeReport messages(ConfigurationMessage message) {
        super.messages(message);
        return this;
    }
}
