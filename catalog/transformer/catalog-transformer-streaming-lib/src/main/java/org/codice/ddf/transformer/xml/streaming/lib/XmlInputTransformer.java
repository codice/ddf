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
package org.codice.ddf.transformer.xml.streaming.lib;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.codice.ddf.transformer.xml.streaming.SaxEventHandler;
import org.codice.ddf.transformer.xml.streaming.SaxEventHandlerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.util.Describable;

/**
 * A {@link InputTransformer} that can be configured to parse any XML into a {@link Metacard}
 * It is configured through {@link XmlInputTransformer#setSaxEventHandlerConfiguration(List)} and {@link XmlInputTransformer#getMetacardType()}
 * {@inheritDoc}
 */
public class XmlInputTransformer implements InputTransformer, Describable {

    private static final Logger LOGGER = LoggerFactory.getLogger(XmlInputTransformer.class);

    /*
     * The Describable attributes that can be used to describe this (specific configuration of) transformer
     */
    private String id = "DEFAULT_ID";

    private String title = "DEFAULT_TITLE";

    private String description = "DEFAULT_DESCRIPTION";

    private String version = "DEFAULT_VERSION";

    private String organization = "DEFAULT_ORGANIZATION";

    /*
     * List of all SaxEventHandlerFactories, used in creating the corresponding, properly configured SaxEventHandlerDelegate.
     * Set using its setter, generally through a blueprint.xml
     */
    private List<SaxEventHandlerFactory> saxEventHandlerFactories;

    /*
     * List of all SaxEventHandlerFactories, used in creating the corresponding, properly configured SaxEventHandlerDelegate.
     * Used to filter the saxEventHandlerFactories
     * Set using its setter, generally through a blueprint.xml
     */
    private List<String> saxEventHandlerConfiguration;

    /**
     * Method to create a new {@link SaxEventHandlerDelegate}, configured to parse a metacard
     * according to {@link XmlInputTransformer#saxEventHandlerConfiguration} and {@link XmlInputTransformer#metacardType}
     *
     * @return a new SaxEventHandlerDelegate
     */
    private SaxEventHandlerDelegate create() {

        /*
         * Gets new instances of each SaxEventHandler denoted in saxEventHandlerConfiguration
         */
        List<SaxEventHandler> filteredSaxEventHandlers = saxEventHandlerFactories.stream()
                .filter(p -> saxEventHandlerConfiguration.contains(p.getId()))
                .map(SaxEventHandlerFactory::getNewSaxEventHandler)
                .collect(Collectors.toList());
        /*
         * Pass all the new handlers to configure and create a new SaxEventHandlerDelegate and sets
         * the metacardType
         */
        return new SaxEventHandlerDelegate(filteredSaxEventHandlers).setMetacardType(getMetacardType());

    }

    /**
     * Takes in an XML {@link InputStream} and returns a populated {@link Metacard}
     * The Metacard is populated with all attributes that have been parsed by the {@link SaxEventHandler}s
     * declared in {@link XmlInputTransformer#saxEventHandlerConfiguration}s
     *
     * @param inputStream an XML input stream to be turned into a Metacard
     * @return a populated Metacard
     * @throws CatalogTransformerException
     * @throws IOException
     */
    public Metacard transform(InputStream inputStream) throws CatalogTransformerException {
        if (inputStream == null) {
            throw new CatalogTransformerException();
        }

        /*
         * Create the necessary new SaxEventHandlerDelegate
         */
        SaxEventHandlerDelegate delegate = create();
        /*
         * Split the input stream, so that we can use it for parsing as well as read it into the Metacard.METADATA attribute
         */
        try (OutputStream baos = new ByteArrayOutputStream();
                OutputStream outputStream = new BufferedOutputStream(baos);

                InputStream teeInputStream = new BufferedInputStream(delegate.getMetadataStream(
                        inputStream,
                        outputStream))) {


        /*
         * Read the input stream into the metacard - where all the magic happens
         */
            Metacard metacard = delegate.read(teeInputStream);

        /*
         * Read the metadata from the split input stream and set it on the Metacard.METADATA attribute.
         * However, if the metadata is null or empty, throw an exception - we can't return a metacard
         * with no metadata
         */
            outputStream.flush();
            String metadata = baos.toString();
            if (metadata.isEmpty()) {
                throw new CatalogTransformerException(
                        "Metadata is empty from output stream. Could not properly parse metacard.");
            }
            metacard.setAttribute(new AttributeImpl(Metacard.METADATA, metadata));

            return metacard;
        } catch (IOException e) {
            LOGGER.warn("IO Exception during parsing", e);
            throw new CatalogTransformerException(
                    "Could not finish transforming metacard because of IOException",
                    e);
        }

    }

    /**
     * Takes in an XML {@link InputStream} and an ID and returns a populated {@link Metacard}
     * The Metacard is populated with all attributes that have been parsed by the {@link SaxEventHandler}s
     * declared in {@link XmlInputTransformer#saxEventHandlerConfiguration}s and with the specific ID
     *
     * @param inputStream an XML input stream to be turned into a Metacard.
     * @param id          the attribute value for the {@link Metacard#ID} attribute that should be set in
     *                    the generated {@link Metacard}
     * @return a populated Metacard
     * @throws CatalogTransformerException
     * @throws IOException
     */

    public Metacard transform(InputStream inputStream, String id)
            throws CatalogTransformerException, IOException {
        Metacard metacard = transform(inputStream);
        metacard.setAttribute(new AttributeImpl(Metacard.ID, id));
        return metacard;
    }

    /**
     * Setter to set the list of all {@link SaxEventHandlerFactory}s. Usually called from a blueprint,
     * and the blueprint will keep the factories updated
     *
     * @param saxEventHandlerFactories a list of all SaxEventHandlerFactories (usually a list of all
     *                                 factories that are available as services)
     */
    public void setSaxEventHandlerFactories(List<SaxEventHandlerFactory> saxEventHandlerFactories) {
        this.saxEventHandlerFactories = saxEventHandlerFactories;
    }

    /**
     * Setter to set the configuration of SaxEventHandlers used to parse metacards
     *
     * @param saxEventHandlerConfiguration a list of SaxEventHandlerFactory ids
     */
    public void setSaxEventHandlerConfiguration(List<String> saxEventHandlerConfiguration) {
        this.saxEventHandlerConfiguration = saxEventHandlerConfiguration;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getOrganization() {
        return organization;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    /**
     * Defines and returns a {@link DynamicMetacardType} based on component Sax Event Handler Factories
     * and what attributes they populate
     *
     * @return a DynamicMetacardType that describes the type of metacard that is created in this transformer
     */

    public MetacardType getMetacardType() {
        Set<AttributeDescriptor> attributeDescriptors = new HashSet<>();

        if (saxEventHandlerConfiguration != null && saxEventHandlerFactories != null) {
            List<SaxEventHandlerFactory> filteredSaxEventHandlerFactories =
                    saxEventHandlerFactories.stream()
                            .filter(p -> saxEventHandlerConfiguration.contains(p.getId()))
                            .collect(Collectors.toList());

            for (SaxEventHandlerFactory factory : filteredSaxEventHandlerFactories) {
                attributeDescriptors.addAll(factory.getSupportedAttributeDescriptors());
            }
        }
        attributeDescriptors.addAll(BasicTypes.BASIC_METACARD.getAttributeDescriptors());
        return new DynamicMetacardType(attributeDescriptors, id);
    }

}

class DynamicMetacardType implements MetacardType {

    Set<AttributeDescriptor> attributeDescriptors;

    String name;

    public DynamicMetacardType(Set<AttributeDescriptor> attDesc, String name) {
        this.attributeDescriptors = attDesc;
        this.name = name + ".metacard";
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Set<AttributeDescriptor> getAttributeDescriptors() {
        return attributeDescriptors;
    }

    @Override
    public AttributeDescriptor getAttributeDescriptor(String attributeName) {
        return attributeDescriptors.stream()
                .filter(p -> p.getName()
                        .equals(attributeName))
                .findFirst()
                .orElse(null);
    }
}
