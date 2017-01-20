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
package org.codice.ddf.admin.api.handler;

import java.util.List;
import java.util.Optional;

import org.codice.ddf.admin.api.config.Configuration;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.CapabilitiesReport;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.Report;

/**
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */
public abstract class DefaultConfigurationHandler<S extends Configuration> implements ConfigurationHandler<S> {

    public abstract List<ProbeMethod> getProbeMethods();

    public abstract List<TestMethod> getTestMethods();

    public abstract List<PersistMethod> getPersistMethods();

    @Override
    public ProbeReport probe(String probeId, S configuration) {
        if (getProbeMethods() == null) {
            return getNoProbeFoundReport(probeId);
        }

        Optional<ProbeMethod> probeMethod = getProbeMethods().stream()
                .filter(method -> method.id()
                        .equals(probeId))
                .findFirst();

        return probeMethod.isPresent() ?
                probeMethod.get().probe(configuration) :
                getNoProbeFoundReport(probeId);
    }

    @Override
    public Report test(String testId, S configuration) {
        if (getTestMethods() == null) {
            return getNoTestFoundReport(testId);
        }

        Optional<TestMethod> testMethod = getTestMethods().stream()
                .filter(method -> method.id()
                        .equals(testId))
                .findFirst();

        return testMethod.isPresent() ?
                testMethod.get().test(configuration) :
                getNoTestFoundReport(testId);
    }

    @Override
    public Report persist(String persistId, S configuration) {

        if (getPersistMethods() == null) {
            return getNoTestFoundReport(persistId);
        }

        Optional<PersistMethod> persistMethod = getPersistMethods().stream()
                .filter(method -> method.id()
                        .equals(persistId))
                .findFirst();

        return persistMethod.isPresent() ?
                persistMethod.get().persist(configuration) :
                getNoTestFoundReport(persistId);
    }

    public Report getNoTestFoundReport(String badId){
        return new Report(ConfigurationMessage.buildMessage(ConfigurationMessage.MessageType.FAILURE,
                ConfigurationMessage.NO_METHOD_FOUND,
                "Unknown method id: \"" + (badId == null ? "null" : badId + "\".")));
    }

    public ProbeReport getNoProbeFoundReport(String badId){
        return new ProbeReport(ConfigurationMessage.buildMessage(ConfigurationMessage.MessageType.FAILURE, ConfigurationMessage.NO_METHOD_FOUND,
                "Unknown probe id \"" + (badId == null ? "null" : badId) + "\"."));
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        return new CapabilitiesReport(getConfigurationType().configTypeName(),
                getConfigurationHandlerId(),
                getTestMethods(),
                getProbeMethods(),
                getPersistMethods());
    }
}
