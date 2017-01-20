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

import org.codice.ddf.admin.api.config.sources.SourceConfiguration;
/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */

/**
 * A marker interface that signifies this {@link ConfigurationHandler} can handle a {@link SourceConfiguration}.
 *
 * @param <S> the {@link org.codice.ddf.admin.api.config.Configuration} this {@link SourceConfigurationHandler} can handle.
 */
public interface SourceConfigurationHandler<S extends SourceConfiguration>
        extends ConfigurationHandler<S> {

}
