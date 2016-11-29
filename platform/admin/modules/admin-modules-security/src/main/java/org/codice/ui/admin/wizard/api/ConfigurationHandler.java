package org.codice.ui.admin.wizard.api;

import java.util.List;

import org.codice.ui.admin.wizard.config.Configuration;

public interface ConfigurationHandler<S extends Configuration> {

    /**
     * Used to search the system for information relative to the configuration and type of probing.
     * Really bad return type here, sorry. Not sure what to return yets
     * @param probeId - id of the probe to be used for searching
     * @param configuration - information used for probing
     * @return dont fire me...
     */
    S probe(String probeId, S configuration);

    /**
     * Returns a list of error messages resulting from the testing. If empty, assume testing was successful
     * @param testId - Id of the test to perform
     * @param configuration - Configuration with properties that will be used for testing
     * @return Error messages resulting from testing.
     */
    List<ConfigurationTestMessage> test(String testId, S configuration);

    /**
     * Persists the configuration to the according bundles and services. Returns a list of error messages resulting from persisting
     * @param configuration - Configuration to persist
     * @return Error messages resulting from persisting
     */
    List<ConfigurationTestMessage> persist(S configuration);

    /**
     * UUID of this configuration handler
     * @return - uuid
     */
    String getConfigurationHandlerId();
}
