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
package ddf.catalog.registry.identification;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBElement;

import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.FileBackedOutputStream;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.PreIngestPlugin;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.registry.common.RegistryConstants;
import ddf.catalog.registry.common.metacard.RegistryObjectMetacardType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;

public class IdentificationPlugin implements PreIngestPlugin {

    private Parser parser;

    private ParserConfigurator configurator;

    @Override
    public CreateRequest process(CreateRequest input)
            throws PluginExecutionException, StopProcessingException {
        List<Metacard> metacards = input.getMetacards();

        for (Metacard metacard : metacards) {
            Set<String> metacardTags = metacard.getTags();
            if (metacardTags.contains(RegistryConstants.REGISTRY_TAG)) {
                setMetacardExtID(metacard);
            }
        }
        return input;
    }

    @Override
    public UpdateRequest process(UpdateRequest input)
            throws PluginExecutionException, StopProcessingException {
        return input;
    }

    @Override
    public DeleteRequest process(DeleteRequest input)
            throws PluginExecutionException, StopProcessingException {
        return input;
    }

    private void setMetacardExtID(Metacard metacard) throws StopProcessingException {

        boolean extOriginFound = false;
        String metacardID = metacard.getId();
        String metadata = metacard.getMetadata();
        String registryID = metacard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID)
                .getValue()
                .toString();

        InputStream inputStream = new ByteArrayInputStream(metadata.getBytes(Charsets.UTF_8));
        FileBackedOutputStream outputStream = new FileBackedOutputStream(1000000);

        try {
            JAXBElement<RegistryObjectType> registryObjectTypeJAXBElement = parser.unmarshal(
                    configurator,
                    JAXBElement.class,
                    inputStream);

            if (registryObjectTypeJAXBElement != null) {
                RegistryObjectType registryObjectType = registryObjectTypeJAXBElement.getValue();

                if (registryObjectType != null) {

                    List<ExternalIdentifierType> extIdList = new ArrayList<>();

                    //check if external ids are already present
                    if (registryObjectType.isSetExternalIdentifier()) {
                        List<ExternalIdentifierType> currentExtIdList =
                                registryObjectType.getExternalIdentifier();

                        for (ExternalIdentifierType extId : currentExtIdList) {
                            extId.setRegistryObject(registryID);
                            if (extId.getId()
                                    .equals(RegistryConstants.REGISTRY_MCARD_LOCAL_ID)) {
                                //update local id
                                extId.setValue(metacardID);
                            } else if (extId.getId()
                                    .equals(RegistryConstants.REGISTRY_MCARD_ORIGIN_ID)) {
                                extOriginFound = true;
                            }
                            extIdList.add(extId);
                        }

                        if (!extOriginFound) {
                            ExternalIdentifierType originExtId = new ExternalIdentifierType();
                            originExtId.setId(RegistryConstants.REGISTRY_MCARD_ORIGIN_ID);
                            originExtId.setRegistryObject(registryID);
                            originExtId.setIdentificationScheme(RegistryConstants.REGISTRY_METACARD_ID_CLASS);
                            originExtId.setValue(metacardID);

                            extIdList.add(originExtId);
                        }

                    } else {
                        //create both ids
                        extIdList = new ArrayList<>(2);

                        ExternalIdentifierType localExtId = new ExternalIdentifierType();
                        localExtId.setId(RegistryConstants.REGISTRY_MCARD_LOCAL_ID);
                        localExtId.setRegistryObject(registryID);
                        localExtId.setIdentificationScheme(RegistryConstants.REGISTRY_METACARD_ID_CLASS);
                        localExtId.setValue(metacardID);

                        ExternalIdentifierType originExtId = new ExternalIdentifierType();
                        originExtId.setId(RegistryConstants.REGISTRY_MCARD_ORIGIN_ID);
                        originExtId.setRegistryObject(registryID);
                        originExtId.setIdentificationScheme(RegistryConstants.REGISTRY_METACARD_ID_CLASS);
                        originExtId.setValue(metacardID);

                        extIdList.add(localExtId);
                        extIdList.add(originExtId);

                    }

                    registryObjectType.setExternalIdentifier(extIdList);
                    registryObjectTypeJAXBElement.setValue(registryObjectType);
                    parser.marshal(configurator, registryObjectTypeJAXBElement, outputStream);

                    String xml = CharStreams.toString(outputStream.asByteSource()
                            .asCharSource(Charsets.UTF_8)
                            .openStream());
                    metacard.setAttribute(new AttributeImpl(Metacard.METADATA, xml));
                }
            }

        } catch (ParserException | IOException e) {
            throw new StopProcessingException(
                    "Unable to access Registry Metadata. Parser exception caught");
        }
    }

    public void setParser(Parser parser) {

        this.configurator =
                parser.configureParser(Arrays.asList(RegistryObjectType.class.getPackage()
                                .getName(),
                        net.opengis.ogc.ObjectFactory.class.getPackage()
                                .getName(),
                        net.opengis.gml.v_3_1_1.ObjectFactory.class.getPackage()
                                .getName()),
                        this.getClass()
                                .getClassLoader());

        this.parser = parser;
    }
}
