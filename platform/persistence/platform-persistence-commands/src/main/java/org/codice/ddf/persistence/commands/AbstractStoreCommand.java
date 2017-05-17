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
package org.codice.ddf.persistence.commands;

import java.io.PrintStream;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.codice.ddf.persistence.PersistenceException;
import org.codice.ddf.persistence.PersistentStore;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract store command that allows store commands to be built off this. Takes care of obtaining
 * the persistent store service, console, and logging.
 */
public abstract class AbstractStoreCommand implements Action {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected PersistentStore persistentStore;

    protected PrintStream console = System.out;

    @Option(name = "Persistence Type", aliases = {"-t",
            "--type"}, required = true, description = "Type of entry in the persistence store to perform the current operation on.\nOptions: attributes, preferences, metacard, saved_query, notification, activity, subscriptions or workspace", multiValued = false)
    protected String type;

    @Option(name = "CQL", aliases = {"-c",
            "--cql"}, required = false, description = "OGC CQL statement to query the persistence store. Not specifying returns all entries. More information on CQL is available at: http://docs.geoserver.org/stable/en/user/tutorials/cql/cql_tutorial.html", multiValued = false)
    protected String cql;

    @Reference
    BundleContext bundleContext;

    @Override
    public Object execute() {

        ServiceReference<PersistentStore> persistentStoreRef =
                bundleContext.getServiceReference(PersistentStore.class);

        try {
            if (persistentStoreRef != null) {
                persistentStore = bundleContext.getService(persistentStoreRef);
                if (PersistentStore.PERSISTENCE_TYPES.contains(type)) {
                    storeCommand();
                } else {
                    console.println("Type passed in was not correct. Must be one of "
                            + PersistentStore.PERSISTENCE_TYPES + ".");
                }
            } else {
                console.println(
                        "Could not obtain reference to Persistent Store service. Cannot perform operation.");
            }
        } catch (PersistenceException pe) {
            console.println(
                    "Encountered an error when trying to perform the command. Check log for more details.");
            logger.debug("Error while performing command.", pe);
        } finally {
            if (persistentStoreRef != null) {
                try {
                    bundleContext.ungetService(persistentStoreRef);
                } catch (IllegalStateException ise) {
                    logger.debug(
                            "Bundle Context was already closed, service reference has been removed.");
                }
            }
        }

        return null;

    }

    /**
     * Calls a command that operates on the Persistent Store service.
     */
    abstract void storeCommand() throws PersistenceException;

}
