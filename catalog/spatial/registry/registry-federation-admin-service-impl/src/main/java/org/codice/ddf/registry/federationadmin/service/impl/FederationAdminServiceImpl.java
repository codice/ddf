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
package org.codice.ddf.registry.federationadmin.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.xml.bind.Marshaller;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.registry.federationadmin.service.FederationAdminException;
import org.codice.ddf.registry.federationadmin.service.FederationAdminService;
import org.codice.ddf.security.common.Security;
import org.geotools.filter.FilterFactoryImpl;
import org.opengis.filter.FilterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.transform.InputTransformer;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryPackageType;

public class FederationAdminServiceImpl implements FederationAdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FederationAdminServiceImpl.class);

    private static final FilterFactory FILTER_FACTORY = new FilterFactoryImpl();

    private static final ObjectFactory RIM_FACTORY = new ObjectFactory();

    private CatalogFramework catalogFramework;

    private InputTransformer registryTransformer;

    private Parser parser;

    private ParserConfigurator configurator;

    private FilterBuilder filterBuilder;

    private final Security security;

    public FederationAdminServiceImpl() {
        this(Security.getInstance());
    }

    FederationAdminServiceImpl(Security security) {
        this.security = security;
    }

    public void init() {

    }

    public void setCatalogFramework(CatalogFramework catalogFramework) {
        this.catalogFramework = catalogFramework;
    }

    public void setFilterBuilder(FilterBuilder filterBuilder) {
        this.filterBuilder = filterBuilder;
    }

    public void setParser(Parser parser) {
        this.configurator =
                parser.configureParser(Arrays.asList(RegistryObjectType.class.getPackage()
                                .getName(),
                        net.opengis.ogc.ObjectFactory.class.getPackage()
                                .getName(),
                        net.opengis.gml.v_3_1_1.ObjectFactory.class.getPackage()
                                .getName()), FederationAdminServiceImpl.class.getClassLoader());
        this.configurator.addProperty(Marshaller.JAXB_FRAGMENT, true);
        this.parser = parser;
    }

    public void setRegistryTransformer(InputTransformer inputTransformer) {
        this.registryTransformer = inputTransformer;
    }

    @Override
    public String addRegistryEntry(String xml) throws FederationAdminException {
        return null;
    }

    @Override
    public String addRegistryEntry(String xml, Set<String> destinations)
            throws FederationAdminException {
        return null;
    }

    @Override
    public String addRegistryEntry(Metacard metacard) throws FederationAdminException {
        return null;
    }

    @Override
    public String addRegistryEntry(Metacard metacard, Set<String> destinations)
            throws FederationAdminException {
        return null;
    }

    @Override
    public void deleteRegistryEntriesByRegistryIds(List<String> registryIds)
            throws FederationAdminException {

    }

    @Override
    public void deleteRegistryEntriesByRegistryIds(List<String> registryIds,
            Set<String> destinations) throws FederationAdminException {

    }

    @Override
    public void deleteRegistryEntriesByMetacardIds(List<String> metacardIds)
            throws FederationAdminException {

    }

    @Override
    public void deleteRegistryEntriesByMetacardIds(List<String> metacardIds,
            Set<String> destinations) throws FederationAdminException {

    }

    @Override
    public void updateRegistryEntry(Metacard metacard) throws FederationAdminException {

    }

    @Override
    public void updateRegistryEntry(Metacard metacard, Set<String> destinations)
            throws FederationAdminException {

    }

    @Override
    public void updateRegistryEntry(String xml) throws FederationAdminException {

    }

    @Override
    public void updateRegistryEntry(String xml, Set<String> destinations)
            throws FederationAdminException {

    }

    @Override
    public List<Metacard> getRegistryMetacards() throws FederationAdminException {
        return new ArrayList<>();
    }

    @Override
    public List<Metacard> getLocalRegistryMetacards() throws FederationAdminException {
        return new ArrayList<>();
    }

    @Override
    public List<Metacard> getRegistryMetacardsByRegistryIds(List<String> ids)
            throws FederationAdminException {
        return new ArrayList<>();
    }

    @Override
    public List<Metacard> getLocalRegistryMetacardsByRegistryIds(List<String> ids)
            throws FederationAdminException {
        return new ArrayList<>();
    }

    @Override
    public List<RegistryPackageType> getLocalRegistryObjects() throws FederationAdminException {
        return new ArrayList<>();
    }

    @Override
    public List<RegistryPackageType> getRegistryObjects() throws FederationAdminException {
        return new ArrayList<>();
    }

    @Override
    public RegistryPackageType getRegistryObjectByMetacardId(String metacardId)
            throws FederationAdminException {
        return null;
    }

    @Override
    public RegistryPackageType getRegistryObjectByMetacardId(String metacardId,
            List<String> sourceIds) throws FederationAdminException {
        return null;
    }

    public void refreshRegistrySubscriptions() throws FederationAdminException {

    }
}
