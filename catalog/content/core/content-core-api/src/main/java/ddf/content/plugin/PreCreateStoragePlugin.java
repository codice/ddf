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
package ddf.content.plugin;

import ddf.content.operation.CreateRequest;

public interface PreCreateStoragePlugin {

    /**
     * Processes the {@link CreateRequest}.
     *
     * @param input the {@link CreateRequest} to process
     * @return the value of the processed {@link CreateRequest} to pass to the next
     * {@link PreCreateStoragePlugin}, or if this is the last {@link PreCreateStoragePlugin} to be called
     * @throws PluginExecutionException thrown when an error occurs during processing
     */
    public CreateRequest process(CreateRequest input) throws PluginExecutionException;

}
