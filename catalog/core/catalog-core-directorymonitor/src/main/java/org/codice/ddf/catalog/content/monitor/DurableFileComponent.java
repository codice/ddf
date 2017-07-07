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
import org.apache.commons.lang.StringUtils;

public class DurableFileComponent extends GenericFileComponent<EventfulFileWrapper> {
    Boolean isDav;

    String remaining;

    @Override
    protected GenericFileEndpoint<EventfulFileWrapper> buildFileEndpoint(String uri,
            String remaining, Map parameters) throws Exception {
        // the starting directory must be a static (not containing dynamic expressions)
        if (StringHelper.hasStartToken(remaining, "simple")) {
            throw new IllegalArgumentException("Invalid directory: " + remaining
                    + ". Dynamic expressions with ${ } placeholders is not allowed."
                    + " Use the fileName option to set the dynamic expression.");
        }
        if (StringUtils.isEmpty(remaining)) {
            throw new IllegalArgumentException("Location to monitor must be specified");
        }

        this.remaining = remaining;
        String davParam = String.valueOf(parameters.get("isDav"));
        isDav = Boolean.valueOf(davParam);
        parameters.remove("isDav");

        GenericFileConfiguration config = new GenericFileConfiguration();
        File file = new File(remaining);
        if (isDav) {
            file = new File("");
        }
        config.setDirectory(file.getCanonicalPath());
        DurableFileEndpoint result = new DurableFileEndpoint(uri, this);
        result.setFile(file);
        result.setConfiguration(config);

        return result;
    }

    public void setIsDav(String paramDav) {
        isDav = Boolean.valueOf(paramDav);
    }

    @Override
    protected void afterPropertiesSet(GenericFileEndpoint endpoint) throws Exception {

    }
}
