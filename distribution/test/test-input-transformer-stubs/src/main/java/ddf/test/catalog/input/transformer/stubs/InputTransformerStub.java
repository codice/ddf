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
package ddf.test.catalog.input.transformer.stubs;

import java.io.IOException;
import java.io.InputStream;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;

public class InputTransformerStub implements InputTransformer {

    private String title;

    public InputTransformerStub(String title) {

        this.title = title;
    }

    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {

        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String id) throws IOException,
        CatalogTransformerException {

        MetacardImpl metacard = new MetacardImpl();

        metacard.setId(id);

        metacard.setTitle(title);

        metacard.setMetadata("<xml><title>" + title + "</title></xml>");

        return metacard;

    }

}
