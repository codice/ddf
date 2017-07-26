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
package org.codice.ddf.migration;

/**
 * Base interface that must be implemented by all bundles or
 * features that need to export and/or import system related
 * settings (e.g. bundle specific Java properties, XML or JSON
 * configuration files) during system migration. The information
 * exported must allow the new system to have the same configuration
 * as the original system. Only bundle or feature specific settings
 * need be handled. All configurations stored in OSGi's
 * {@code ConfigurationAdmin} will automatically be migrated and do
 * not need to be managed by implementors of this class.
 * <p>
 * Also, any other data not related to the system's configuration
 * and settings (e.g., data stored in Solr) should be handled by a
 * different service that implements the {@link DataMigratable}
 * interface, not by implementors of this class.
 * <p>
 * <p>
 * <b>
 * This code is experimental. While this interface is functional
 * and tested, it may change or be removed in a future version of the
 * library.
 * </b>
 * </p>
 */
public interface ConfigurationMigratable extends Migratable {
}
