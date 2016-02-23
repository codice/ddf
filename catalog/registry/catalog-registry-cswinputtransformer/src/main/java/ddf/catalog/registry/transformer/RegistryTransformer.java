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
 **/
package ddf.catalog.registry.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.registry.common.metacard.RegistryMetacardImpl;
import ddf.catalog.registry.common.metacard.RegistryObjectMetacardType;
import ddf.catalog.registry.common.metacard.RegistryServiceMetacardType;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transform.MetacardTransformer;

public class RegistryTransformer implements InputTransformer, MetacardTransformer {

    private static final Logger LOGGER = LoggerFactory.getLogger(RegistryTransformer.class);

    private final Gml3ToWkt gml3ToWkt;

    private final XStream xstream;

    public RegistryTransformer(Gml3ToWkt gml3ToWkt) {
        this.gml3ToWkt = gml3ToWkt;

        xstream = new XStream(new Xpp3Driver());
        xstream.setClassLoader(this.getClass()
                .getClassLoader());
        xstream.registerConverter(new RegistryServiceConverter(gml3ToWkt));
        xstream.alias("rim:Service", Metacard.class);

    }

    @Override
    public Metacard transform(InputStream inputStream)
            throws IOException, CatalogTransformerException {
        return transform(inputStream, null);
    }

    @Override
    public Metacard transform(InputStream inputStream, String id)
            throws IOException, CatalogTransformerException {

        RegistryMetacardImpl metacard;
        String xml = IOUtils.toString(inputStream);
        IOUtils.closeQuietly(inputStream);

        try {
            metacard = (RegistryMetacardImpl) xstream.fromXML(xml);
        } catch (XStreamException e) {
            throw new CatalogTransformerException(
                    "Unable to transform from CSW RIM Service Record to Metacard.",
                    e);
        }

        if (metacard == null) {
            throw new CatalogTransformerException(
                    "Unable to transform from CSW RIM Service Record to Metacard.");
        } else if (StringUtils.isNotEmpty(id)) {
            metacard.setAttribute(Metacard.ID, id);
        }

        metacard.setAttribute(Metacard.METADATA, xml);
        metacard.setTags(Collections.singleton(RegistryObjectMetacardType.REGISTRY_TAG));
        metacard.setContentTypeName(RegistryServiceMetacardType.SERVICE_REGISTRY_METACARD_TYPE_NAME);

        return metacard;
    }

    @Override
    public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
            throws CatalogTransformerException {
        if (metacard.getContentTypeName()
                .startsWith(RegistryObjectMetacardType.REGISTRY_METACARD_TYPE_NAME)) {
            String metadata = metacard.getMetadata();
            return new BinaryContentImpl(IOUtils.toInputStream(metadata));
        } else {
            throw new CatalogTransformerException(
                    "Can't transform metacard of content type " + metacard.getContentTypeName()
                            + " to csw-ebrim xml");
        }
    }

}
