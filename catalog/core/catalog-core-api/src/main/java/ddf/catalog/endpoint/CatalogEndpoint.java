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
package ddf.catalog.endpoint;

import java.util.Map;

public interface CatalogEndpoint {
    String BINDING_TYPE_KEY = "bindingType";
    String DESCRIPTION_KEY = "description";
    String ID_KEY = "id";
    String NAME_KEY = "name";
    String URL_KEY = "url";
    String VERSION_KEY = "version";

    Map<String, String> getEndpointProperties();
}
