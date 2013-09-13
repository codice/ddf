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
package ddf.content.operation;

import ddf.content.data.ContentItem;

/**
 * Defines a Delete Response object which contains response information that should be returned on a
 * {@link DeleteRequest} operation.
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 */
public interface DeleteResponse extends Response<DeleteRequest> {

    /**
     * Returns status of the deletion of the file.
     * 
     * @return <code>true</code> if the file was deleted; <code>false</code> otherwise
     */
    public boolean isFileDeleted();

    public ContentItem getContentItem();
}
