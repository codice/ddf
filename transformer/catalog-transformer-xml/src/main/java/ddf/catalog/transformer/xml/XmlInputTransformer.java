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
package ddf.catalog.transformer.xml;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.helpers.DefaultValidationEventHandler;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transformer.xml.adapter.MetacardTypeAdapter;

public class XmlInputTransformer extends AbstractXmlTransformer implements InputTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlInputTransformer.class);

    private static final String FAILED_TRANSFORMATION = "Failed Transformation.  Could not create Metacard from XML.";

    private List<MetacardType> metacardTypes;

    @Override
    public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
        return transform(input, null);
    }

    @Override
    public Metacard transform(InputStream input, String id) throws IOException,
        CatalogTransformerException {
        Metacard metacard = null;
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        try {

            Unmarshaller unmarshaller = CONTEXT.createUnmarshaller();
            unmarshaller.setAdapter(MetacardTypeAdapter.class, new MetacardTypeAdapter(
                    metacardTypes));
            unmarshaller.setEventHandler(new DefaultValidationEventHandler());
            try {
                metacard = (Metacard) unmarshaller.unmarshal(input);
                if (!StringUtils.isEmpty(id)) {
                    metacard.setAttribute(new AttributeImpl(Metacard.ID, id));
                }
            } catch (RuntimeException e) {
                // JAXB likes to throw RuntimeExceptions and we don't want to
                // bubble those up.
                LOGGER.debug("Caught RuntimeException during JAXB Transformation", e);
                throw new CatalogTransformerException(FAILED_TRANSFORMATION, e);
            }
        } catch (JAXBException e) {
            LOGGER.debug("Caught JAXBException during JAXB Transformation");
            throw new CatalogTransformerException(FAILED_TRANSFORMATION, e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
            IOUtils.closeQuietly(input);
        }

        if (metacard == null) {
            throw new CatalogTransformerException(FAILED_TRANSFORMATION);
        }

        return metacard;
    }

    /**
     * @param metacardTypes
     *            the metacardTypes to set
     */
    public void setMetacardTypes(List<MetacardType> metacardTypes) {
        this.metacardTypes = metacardTypes;
    }

}
