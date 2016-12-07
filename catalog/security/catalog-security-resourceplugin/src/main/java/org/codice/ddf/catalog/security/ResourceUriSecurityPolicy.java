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
package org.codice.ddf.catalog.security;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;

/**
 * Restricts how resource URIs are updated and created.
 * There are security risk to allowing users to update URIs as well as allowing users to
 * pass in a URI when the metacard is created.
 */
public class ResourceUriPolicy implements PolicyPlugin {

    private String[] updatePermissions;


    @Override
    public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {



        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreUpdate(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {

        // If incomming metacard has resource URI different than current URI in the catalog,
        // add ______ to the _______ collection in the policy reponse.

        PolicyResponse

        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreDelete(List<Metacard> metacards,
            Map<String, Serializable> properties) throws StopProcessingException {

        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostDelete(Metacard input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreQuery(Query query, Map<String, Serializable> properties)
            throws StopProcessingException {

        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPreResource(ResourceRequest resourceRequest)
            throws StopProcessingException {

        return new PolicyResponseImpl();
    }

    @Override
    public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
            throws StopProcessingException {
        return new PolicyResponseImpl();
    }


    }
}
