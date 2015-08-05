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
package ddf.catalog.plugin;

import ddf.catalog.event.Subscription;

/**
 * The Interface PreSubscriptionPlugin provides the capability to execute a plugin prior to a
 * {@link Subscription} being created.
 */
public interface PreSubscriptionPlugin {

    /**
     * Processes the {@link Subscription}.
     *
     * @param input
     *            the {@link Subscription} to process
     * @return the value of the processed {@link Subscription} to pass to the next
     *         {@link PreSubscriptionPlugin}, or if this is the last {@link PreSubscriptionPlugin}
     *         to be called
     * @throws PluginExecutionException
     *             thrown when there is an error in processing the {@link Subscription}
     */
    public Subscription process(Subscription input)
            throws PluginExecutionException, StopProcessingException;
}
