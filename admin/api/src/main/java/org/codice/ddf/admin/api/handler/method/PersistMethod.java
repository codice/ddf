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

package org.codice.ddf.admin.api.handler.method;

import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.Configuration;
import org.codice.ddf.admin.api.handler.report.Report;

/**
 * <b> This code is experimental. While this class is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 */

/**
 * A {@link PersistMethod} is a method that is used to persist a {@link Configuration}.
 *
 * @param <S> the {@link Configuration} type to persist.
 */
public abstract class PersistMethod<S extends Configuration> extends ConfigurationHandlerMethod {
    public PersistMethod(String id, String description, List<String> requiredFields,
            List<String> optionalFields, Map<String, String> successTypes,
            Map<String, String> failureTypes, Map<String, String> warningTypes) {
        super(id,
                description,
                requiredFields,
                optionalFields,
                successTypes,
                failureTypes,
                warningTypes);
    }

    public abstract Report persist(S configuration);

}
