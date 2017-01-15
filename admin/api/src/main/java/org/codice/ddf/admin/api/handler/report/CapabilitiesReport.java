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

import java.util.List;

import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;

public class CapabilitiesReport {

    private String configurationType;

    private String configurationHandlerId;

    private List<TestMethod> testMethods;

    private List<ProbeMethod> probeMethods;

    private List<PersistMethod> persistMethods;

    public CapabilitiesReport(String configurationType, String configurationHandlerId,
            List<TestMethod> testMethods) {
        this.configurationType = configurationType;
        this.configurationHandlerId = configurationHandlerId;
        this.testMethods = testMethods;
    }

    public CapabilitiesReport(String configurationType, String configurationHandlerId,
            List<TestMethod> testMethods, List<ProbeMethod> probeMethods,
            List<PersistMethod> persistMethods) {
        this.configurationType = configurationType;
        this.configurationHandlerId = configurationHandlerId;
        this.testMethods = testMethods;
        this.probeMethods = probeMethods;
        this.persistMethods = persistMethods;
    }

    public String getConfigurationType() {
        return configurationType;
    }

    public String getConfigurationHandlerId() {
        return configurationHandlerId;
    }

    public List<TestMethod> getTestMethods() {
        return testMethods;
    }

    public List<ProbeMethod> getProbeMethods() {
        return probeMethods;
    }

    public List<PersistMethod> getPersistMethods() {
        return persistMethods;
    }
}
