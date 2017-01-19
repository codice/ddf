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

import org.codice.ddf.admin.api.config.Configuration;
import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.handler.report.CapabilitiesReport;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.Report;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 *
 * A {@link ConfigurationHandler} is used to  {@link #probe}, {@link #test}, and {@link #persist} a {@link Configuration}
 */
public interface ConfigurationHandler<S extends Configuration> {

    /**
     * Used to discover information. This may be system specific or external to the system.
     *
     * @param probeId A unique key that specifies the probe operation for the {@link ConfigurationHandler} to perform on the {@link Configuration}
     * @param configuration A configuration containing information to be used for discovery of information
     * @return ProbeReport containing the results of probe
     */
    ProbeReport probe(String probeId, S configuration);

    /**
     * Tests a {@link Configuration}. For example, confirming that various {@link Configuration} fields are valid.
     *
     * @param testId A unique key that specifies the test operation for the {@link ConfigurationHandler} to perform on the {@link Configuration}
     * @param configuration Configuration to test
     * @return A {@link Report} containing the results of the operation
     */
    Report test(String testId, S configuration);

    /**
     * Persists a {@link Configuration}.
     *
     * @param persistId A unique key that specifies the persist operation for the {@link ConfigurationHandler} to perform on the {@link Configuration}
     * @param configuration Configuration to persist
     * @return A {@link Report} containing the results of the operation
     */
    Report persist(String persistId, S configuration);

    /**
     * Returns a list of available {@link Configuration}s associated to this {@link ConfigurationHandler}.
     *
     * @return list of {@link Configuration}s
     */
    List<S> getConfigurations();

    CapabilitiesReport getCapabilities();

    /**
     * Unique ID of this configuration handler. The uniqueness of the ID is not enforced.
     *
     * @return  uid
     */
    String getConfigurationHandlerId();

    /**
     * Specifics the type of {@link Configuration} the {@link ConfigurationHandler} can handle. This will by used by the {@link ConfigurationHandlerRouter}identify the configuration as a child class of {@link Configuration}.
     *
     * @return a {@link ConfigurationType}
     */
    ConfigurationType getConfigurationType();
}
