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
package org.codice.ddf.catalog.transformer.html;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.jknack.handlebars.ValueResolver;

import ddf.catalog.data.Metacard;

public class MetacardValueResolver implements ValueResolver {


    private static final Logger LOGGER = LoggerFactory.getLogger(MetacardValueResolver.class);

    @Override
    public Object resolve(Object context, String name) {

        LOGGER.debug("Resolving {} for {}", context.getClass().getName(), name);
        if (context instanceof Metacard) {
            if ("geometry".equals(name)) {
                return ((Metacard) context).getLocation();
            } else if ("properties".equals(name)) {
                return new MetacardWrapper((Metacard) context);                
            }
        } else if(context instanceof MetacardWrapper) {
            Metacard metacard = ((MetacardWrapper) context).getMetacard();

            if ("thumbnail".equals(name)) {
                byte[] bytes = metacard.getThumbnail();
                return DatatypeConverter.printBase64Binary(bytes);
            } else if ("source-id".equals(name)) {
                return metacard.getSourceId();
            } else if ("type".equals(name)) {
                return metacard.getMetacardType().getName();
            } else if (metacard.getAttribute(name) != null) {
                return metacard.getAttribute(name).getValue();
            }
        }

        return UNRESOLVED;
    }

    @Override
    public Set<Entry<String, Object>> propertySet(Object context) {
        return Collections.emptySet();
    }

    private class MetacardWrapper {
        Metacard metacard;
        
        MetacardWrapper(Metacard metacard) {
            this.metacard = metacard;
        }
        
        Metacard getMetacard() {
            return metacard;
        }
    }
}
