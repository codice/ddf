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
package org.codice.ddf.security.idp.binding.post;

import java.util.Map;

import org.codice.ddf.security.idp.binding.api.Binding;
import org.codice.ddf.security.idp.binding.api.RequestDecoder;
import org.codice.ddf.security.idp.binding.api.ResponseCreator;
import org.codice.ddf.security.idp.binding.api.Validator;
import org.opensaml.saml2.metadata.EntityDescriptor;

import ddf.security.samlp.SystemCrypto;
import ddf.security.samlp.impl.EntityInformation;

public class PostBinding implements Binding {

    private final PostRequestDecoder decoder;

    private final PostResponseCreator creator;

    private final PostValidator validator;

    public PostBinding(SystemCrypto systemCrypto, Map<String, EntityInformation> serviceProviders) {
        decoder = new PostRequestDecoder();
        creator = new PostResponseCreator(systemCrypto, serviceProviders);
        validator = new PostValidator(systemCrypto, serviceProviders);
    }

    @Override
    public RequestDecoder decoder() {
        return decoder;
    }

    @Override
    public ResponseCreator creator() {
        return creator;
    }

    @Override
    public Validator validator() {
        return validator;
    }
}
