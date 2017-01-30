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
package org.codice.ddf.catalog.content.monitor;

import java.io.File;
import java.util.Map;

import org.apache.camel.component.file.GenericFileComponent;
import org.apache.camel.component.file.GenericFileConfiguration;
import org.apache.camel.component.file.GenericFileEndpoint;
import org.apache.camel.util.StringHelper;

public class DurableFileComponent extends GenericFileComponent<EventfulFileWrapper> {
    @Override
    protected GenericFileEndpoint<EventfulFileWrapper> buildFileEndpoint(String uri,
            String remaining, Map parameters) throws Exception {
        // the starting directory must be a static (not containing dynamic expressions)
        if (StringHelper.hasStartToken(remaining, "simple")) {
            throw new IllegalArgumentException("Invalid directory: " + remaining
                    + ". Dynamic expressions with ${ } placeholders is not allowed."
                    + " Use the fileName option to set the dynamic expression.");
        }

        File file = new File(remaining);

        DurableFileEndpoint result = new DurableFileEndpoint(uri, this);
        result.setFile(file);

        GenericFileConfiguration config = new GenericFileConfiguration();
        config.setDirectory(file.getCanonicalPath());
        result.setConfiguration(config);

        return result;
    }

    @Override
    protected void afterPropertiesSet(GenericFileEndpoint endpoint) throws Exception {

    }
}
