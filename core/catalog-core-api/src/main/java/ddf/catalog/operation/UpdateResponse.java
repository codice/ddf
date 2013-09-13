/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.operation;

import java.util.List;

/**
 * Further specification of {@link Response} for an {@link Update} request.
 * 
 * @author michael.menousek@lmco.com
 * 
 */
public interface UpdateResponse extends Response<UpdateRequest> {
    /**
     * Get the {@link Update}s that occurred
     * 
     * @return the {@link List} of {@link Update}s that occurred
     */
    public List<Update> getUpdatedMetacards();

}
