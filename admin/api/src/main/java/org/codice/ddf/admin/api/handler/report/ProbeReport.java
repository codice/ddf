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

public class ProbeReport extends TestReport {

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

    public ProbeReport addProbeResult(String key, Object value) {
        probeResults.put(key, value);
        return this;
    }

    public ProbeReport addProbeResults(Map<String, Object> results) {
        probeResults.putAll(probeResults);
        return this;
    }

    public Map<String, Object> getProbeResults() {
        return probeResults;
    }
}
