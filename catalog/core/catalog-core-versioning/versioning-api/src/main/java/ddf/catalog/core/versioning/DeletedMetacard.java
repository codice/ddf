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
package ddf.catalog.core.versioning;

import java.util.function.Function;

import ddf.catalog.data.Metacard;

/**
 * Experimental. Subject to change.
 * <br/>
 * Represents a currently soft deleted metacard.
 */
public interface DeletedMetacard extends Metacard {
    String PREFIX = "metacard.deleted";

    Function<String, String> PREFIXER = s -> String.format("%s.%s", PREFIX, s);

    String DELETED_TAG = "deleted";

    String DELETED_BY = PREFIXER.apply("deleted-by");

    String DELETION_OF_ID = PREFIXER.apply("id");

    String LAST_VERSION_ID = PREFIXER.apply("version");

}
