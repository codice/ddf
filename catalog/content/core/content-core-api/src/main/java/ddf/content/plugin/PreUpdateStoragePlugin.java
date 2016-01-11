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

import ddf.content.operation.UpdateRequest;

/**
 * Services implementing this interface are called immediately before an item is updated in the
 * content repository.
 */
public interface PreUpdateStoragePlugin {
    /**
     * Processes the {@link UpdateRequest}.
     *
     * @param input the {@code UpdateRequest} to process
     * @return the processed {@code UpdateRequest} to pass to the next {@link PreUpdateStoragePlugin}
     * @throws PluginExecutionException if an error occurs during processing
     */
    UpdateRequest process(UpdateRequest input) throws PluginExecutionException;
}
