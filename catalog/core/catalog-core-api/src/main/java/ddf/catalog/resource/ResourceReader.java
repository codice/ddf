/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.resource;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.util.Describable;

/**
 * 
 * A ResourceReader is used to obtain a {@link Resource}. 
 * 
 * For example the {@link URLResourceReader} obtains a {@link Resource} based on
 * supported {@link URI} schemes.
 * 
 * @author michael.menousek@lmco.com
 */
public interface ResourceReader extends Describable {

    /**
     * Retrieves a {@link Resource} based on a {@link URI} and provided arguments.
     * 
     * @param uri A {@link URI} that defines what {@link Resource} to retrieve and how to do it.
     * @param arguments Any additional arguments that should be passed to the ResourceReader.
     * 
     * @return A {@link ResourceResponse} containing the retrieved {@link Resource}.
     * 
     * @throws {@link IOException} thrown typically when there is an issue retrieving the file
     * @throws {@link ResourceNotFoundException} thrown when the {@link Resource} could not be found
     * @throws {@link ResourceNotSupportedException} thrown when the {@link Resource} is not supported by the
     *             ResourceReader
     */
    public ResourceResponse retrieveResource(URI uri, Map<String, Serializable> arguments) throws IOException,
	    ResourceNotFoundException, ResourceNotSupportedException;

    /**
     * Returns a set of {@link URI} schemes that the ResourceReader can accept
     * when doing a {@link Resource} lookup. Custom schemes can be created for a
     * ResourceReader to support.
     * 
     * @return {@link Set} of supported schemes
     */
    public Set<String> getSupportedSchemes();

    /**
     * Obtain a set of all options supported by this ResourceReader.  
     * Options are used to obtain the {@link Resource} in a unique way.  
     * 
     * @param metacard
     * @return {@link Set} of all options that this ResourceReader supports.  
     * 		This will be an empty set if no options are supported. 
     */
    public Set<String> getOptions(Metacard metacard);
}
