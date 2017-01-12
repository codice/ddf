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

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.NO_TEST_FOUND;

import java.util.List;
import java.util.Optional;

import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.CapabilitiesReport;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;

public abstract class DefaultConfigurationHandler<S extends Configuration>
        implements ConfigurationHandler<S> {

    public static final TestReport NO_TEST_FOUND_REPORT = new TestReport(new ConfigurationMessage(
            NO_TEST_FOUND));

    public static final ProbeReport NO_PROBE_FOUND_REPORT =
            new ProbeReport(new ConfigurationMessage(NO_TEST_FOUND));

    public static final TestReport NO_PERSIST_FOUND_REPORT =
            new TestReport(new ConfigurationMessage(NO_TEST_FOUND));

    public abstract List<ProbeMethod> getProbeMethods();

    public abstract List<TestMethod> getTestMethods();

    public abstract List<PersistMethod> getPersistMethods();

    @Override
    public ProbeReport probe(String probeId, S configuration) {
        if (getProbeMethods() == null) {
            return NO_PROBE_FOUND_REPORT;
        }

        Optional<ProbeMethod> probeMethod = getProbeMethods().stream()
                .filter(method -> method.id()
                        .equals(probeId))
                .findFirst();

        return probeMethod.isPresent() ?
                probeMethod.get()
                        .probe(configuration) :
                NO_PROBE_FOUND_REPORT;
    }

    @Override
    public TestReport test(String testId, S configuration) {
        if (getTestMethods() == null) {
            return NO_TEST_FOUND_REPORT;
        }

        Optional<TestMethod> testMethod = getTestMethods().stream()
                .filter(method -> method.id()
                        .equals(testId))
                .findFirst();

        return testMethod.isPresent() ?
                testMethod.get()
                        .test(configuration) :
                NO_TEST_FOUND_REPORT;
    }

    @Override
    public TestReport persist(S configuration, String persistId) {

        if (getPersistMethods() == null) {
            return NO_PERSIST_FOUND_REPORT;
        }

        Optional<PersistMethod> persistMethod = getPersistMethods().stream()
                .filter(method -> method.id()
                        .equals(persistId))
                .findFirst();

        return persistMethod.isPresent() ?
                persistMethod.get()
                        .persist(configuration) :
                NO_PERSIST_FOUND_REPORT;
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        // TODO: tbatie - 1/11/17 - Need to pass configType. Figure out the config type and configuration handler Id difference first before passing the config type
        return new CapabilitiesReport(null,
                getConfigurationHandlerId(),
                getTestMethods(),
                getProbeMethods(),
                getPersistMethods());
    }
}
