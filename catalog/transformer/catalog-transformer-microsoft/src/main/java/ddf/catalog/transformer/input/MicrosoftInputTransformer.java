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
package ddf.catalog.transformer.input;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.FileBackedOutputStream;

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardEnricher;

public class MicrosoftInputTransformer implements InputTransformer {
    private static final Logger LOGGER = LoggerFactory.getLogger(MicrosoftInputTransformer.class);

    private static int fileThreshold = 1000000;

    private MetacardEnricher clavinMetacardEnricher;

    private InputTransformer tikaInputTransformer;

    public void setClavinMetacardEnricher(MetacardEnricher clavinMetacardEnricher) {
        this.clavinMetacardEnricher = clavinMetacardEnricher;
    }

    public void setTikaInputTransformer(InputTransformer tikaInputTransformer) {
        this.tikaInputTransformer = tikaInputTransformer;
    }

    public MicrosoftInputTransformer() {
    }

    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String id)
            throws IOException, CatalogTransformerException {
        Metacard metacard = null;

        try (FileBackedOutputStream fbos = new FileBackedOutputStream(fileThreshold)) {
            IOUtils.copy(input, fbos);

            try (InputStream tikaCopy = fbos.asByteSource().openStream()) {
                metacard = tikaInputTransformer.transform(tikaCopy);
            }

            try (InputStream enricherCopy = fbos.asByteSource().openStream()) {
                clavinMetacardEnricher.enrich(metacard, enricherCopy);
            }
        }
        return metacard;
    }

}
