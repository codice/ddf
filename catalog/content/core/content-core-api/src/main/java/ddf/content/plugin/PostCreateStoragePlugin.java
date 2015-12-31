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
package ddf.content.plugin;

import ddf.content.operation.CreateResponse;

public interface PostCreateStoragePlugin {

    /**
     * Processes the {@link CreateResponse}.
     *
     * @param input the {@link CreateResponse} to process
     * @return the value of the processed {@link CreateResponse} to pass to the next
     * {@link PostCreateStoragePlugin}, or if this is the last {@link PostCreateStoragePlugin} to be called
     * @throws PluginExecutionException thrown when an error occurs during processing
     */
    public CreateResponse process(CreateResponse input) throws PluginExecutionException;

}
