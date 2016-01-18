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
package ddf.catalog.transformer.generic.xml.impl;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transformer.generic.xml.SaxEventHandler;
import ddf.catalog.transformer.generic.xml.SaxEventHandlerFactory;
import ddf.catalog.util.Describable;

public class XMLInputTransformer implements InputTransformer, Describable {

    private String id = "DEFAULT_ID";

    private String title = "DEFAULT_TITLE";

    private String description = "DEFAULT_DESCRIPTION";

    private String version = "DEFAULT_VERSION";

    private String organization = "DEFAULT_ORGANIZATION";

    private List<SaxEventHandlerFactory> saxEventHandlerFactories;

    private List<String> saxEventHandlerConfiguration;

    public SaxEventHandlerDelegate create() {

        // Gets new instances of each SaxEventHandler denoted in saxEventHandlerConfiguration
        List<SaxEventHandler> filteredSaxEventHandlerFactories = saxEventHandlerFactories.stream()
                .filter(p -> saxEventHandlerConfiguration.contains(p.getId()))
                .map(SaxEventHandlerFactory::getNewSaxEventHandler).collect(Collectors.toList());
        return new SaxEventHandlerDelegate(filteredSaxEventHandlerFactories);

    }

    public Metacard transform(InputStream inputStream) {
        SaxEventHandlerDelegate delegate = create();
        return delegate.read(inputStream);
    }

    public Metacard transform(InputStream inputStream, String id) {
        Metacard metacard = transform(inputStream);
        metacard.setAttribute(new AttributeImpl(Metacard.ID, id));
        return metacard;
    }

    public void setSaxEventHandlerFactories(List<SaxEventHandlerFactory> saxEventHandlerFactories) {
        this.saxEventHandlerFactories = saxEventHandlerFactories;
    }

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
}
