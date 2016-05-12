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
 **/
package ddf.common.test;

import java.security.Principal;

import org.apache.karaf.features.FeaturesService;
import org.apache.karaf.itests.KarafTestSupport;
import org.apache.karaf.jaas.boot.principal.RolePrincipal;
import org.apache.karaf.shell.api.console.SessionFactory;
import org.osgi.framework.BundleContext;

/**
 * Class that provides access to the Karaf Console.
 * <p>
 * Note: This class is needed to expose the protected methods provided by {@link KarafTestSupport}.
 * Since we already extend from our own base class, out test classes cannot extend from this one
 * to access its protected methods.
 */
public class KarafConsole extends KarafTestSupport {

    private static final RolePrincipal[] DEFAULT_ROLES =
            {new RolePrincipal("admin"), new RolePrincipal("group"), new RolePrincipal("manager"),
                    new RolePrincipal("viewer"), new RolePrincipal(
                    "system-admin"), new RolePrincipal("systembundles")};

    /**
     * Karaf console constructor.
     *
     * @param bundleContext bundle context to use when using the console. Cannot be {@code null}.
     */
    public KarafConsole(BundleContext bundleContext, FeaturesService featuresService,
            SessionFactory sessionFactory) {
        super();
        this.bundleContext = bundleContext;
        this.featureService = featuresService;
        this.sessionFactory = sessionFactory;
    }

    /**
     * Runs a shell command and returns output as a String. Commands have a default timeout of
     * 10 seconds.
     *
     * @param command    command to execute. Cannot be {@code null}.
     * @param timeout    command timeout in milliseconds
     * @param principals principals (e.g. RolePrincipal objects) to run the command under
     *                   (optional)
     * @return command output. Can be empty but not {@code null}.
     */
    public String runCommand(String command, long timeout, Principal... principals) {
        return executeCommand(command, timeout, false, principals);
    }

    /**
     * Runs a shell command and returns output as a String. Commands have a default timeout of
     * 10 seconds.
     *
     * @param command    command to execute. Cannot be {@code null}.
     * @param principals principals (e.g. RolePrincipal objects) to run the command under
     *                   (optional)
     * @return command output. Can be empty but not {@code null}.
     */
    public String runCommand(String command, Principal... principals) {
        return executeCommand(command, principals);
    }

    /**
     * Runs a shell command and returns output as a String. Commands have a default timeout of
     * 10 seconds. Uses the DEFAULT_ROLES to execute the command as an administrator.
     *
     * @param command command to execute. Cannot be {@code null}.
     * @return command output. Can be empty but not {@code null}.
     */
    public String runCommand(String command) {
        return executeCommand(command, DEFAULT_ROLES);
    }
}
