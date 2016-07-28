/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.impl;

import java.io.Serializable;
import java.util.Map;

import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import ddf.catalog.Constants;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;

public class TransformOperations {
    private FrameworkProperties frameworkProperties;

    public TransformOperations(FrameworkProperties frameworkProperties) {
        this.frameworkProperties = frameworkProperties;
    }

    //
    // Delegate methods
    //
    BinaryContent transform(Metacard metacard, String transformerId,
            Map<String, Serializable> requestProperties) throws CatalogTransformerException {

        ServiceReference[] refs;
        try {
            // TODO replace shortname with id
            refs = frameworkProperties.getBundleContext()
                    .getServiceReferences(MetacardTransformer.class.getName(),
                            "(|" + "(" + Constants.SERVICE_SHORTNAME + "=" + transformerId + ")"
                                    + "(" + Constants.SERVICE_ID + "=" + transformerId + ")" + ")");
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid transformer shortName: " + transformerId,
                    e);
        }
        if (refs == null || refs.length == 0) {
            throw new IllegalArgumentException("Transformer " + transformerId + " not found");
        } else {
            MetacardTransformer transformer =
                    (MetacardTransformer) frameworkProperties.getBundleContext()
                            .getService(refs[0]);
            if (metacard != null) {
                return transformer.transform(metacard, requestProperties);
            } else {
                throw new IllegalArgumentException("Metacard is null.");
            }
        }
    }

    BinaryContent transform(SourceResponse response, String transformerId,
            Map<String, Serializable> requestProperties) throws CatalogTransformerException {

        ServiceReference[] refs;
        try {
            refs = frameworkProperties.getBundleContext()
                    .getServiceReferences(QueryResponseTransformer.class.getName(),
                            "(|" + "(" + Constants.SERVICE_SHORTNAME + "=" + transformerId + ")"
                                    + "(" + Constants.SERVICE_ID + "=" + transformerId + ")" + ")");
        } catch (InvalidSyntaxException e) {
            throw new IllegalArgumentException("Invalid transformer id: " + transformerId, e);
        }

        if (refs == null || refs.length == 0) {
            throw new IllegalArgumentException("Transformer " + transformerId + " not found");
        } else {
            QueryResponseTransformer transformer =
                    (QueryResponseTransformer) frameworkProperties.getBundleContext()
                            .getService(refs[0]);
            if (response != null) {
                return transformer.transform(response, requestProperties);
            } else {
                throw new IllegalArgumentException("QueryResponse is null.");
            }
        }
    }
}
