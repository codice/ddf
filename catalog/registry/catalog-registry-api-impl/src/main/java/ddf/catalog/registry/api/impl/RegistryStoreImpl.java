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
package ddf.catalog.registry.api.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.ParserConfigurator;
import org.codice.ddf.parser.ParserException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.source.AbstractCswSource;
import org.osgi.framework.BundleContext;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.FileBackedOutputStream;
import com.thoughtworks.xstream.converters.Converter;

import ddf.catalog.Constants;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.registry.api.RegistryStore;
import ddf.catalog.registry.api.metacard.RegistryObjectMetacardType;
import ddf.catalog.registry.common.RegistryConstants;
import ddf.catalog.source.IngestException;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ExternalIdentifierType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.RegistryObjectType;

public class RegistryStoreImpl extends AbstractCswSource implements RegistryStore {

    public static final String PUSH_ALLOWED_PROPERTY = "pushAllowed";

    public static final String PULL_ALLOWED_PROPERTY = "pullAllowed";

    private boolean pushAllowed = false;

    private boolean pullAllowed = false;

    private Parser parser;

    private ParserConfigurator marshalConfigurator;

    private ParserConfigurator unmarshalConfigurator;

    public RegistryStoreImpl(BundleContext context, CswSourceConfiguration cswSourceConfiguration,
            Converter provider, SecureCxfClientFactory factory) {
        super(context, cswSourceConfiguration, provider, factory);
    }

    public RegistryStoreImpl() {
        super();
    }

    @Override
    protected Map<String, Consumer<Object>> getAdditionalConsumers() {
        Map<String, Consumer<Object>> map = new HashMap<>();
        map.put(PUSH_ALLOWED_PROPERTY, value -> setPushAllowed((Boolean) value));
        map.put(PULL_ALLOWED_PROPERTY, value -> setPullAllowed((Boolean) value));
        return map;
    }

    @Override
    public boolean isPushAllowed() {
        return pushAllowed;
    }

    @Override
    public boolean isPullAllowed() {
        return pullAllowed;
    }

    @Override
    public UpdateResponse update(UpdateRequest request) throws IngestException {
        Map<String, Metacard> updatedMetacards = new HashMap<>();
        Map<String, Metacard> origMetacards = new HashMap<>();

        for (Map.Entry<Serializable, Metacard> entry : request.getUpdates()) {

            updatedMetacards.put(getRegistryId(entry.getValue()), entry.getValue());
        }

        OperationTransaction opTrans =
                (OperationTransaction) request.getPropertyValue(Constants.OPERATION_TRANSACTION_KEY);

        for (Metacard mcard : opTrans.getPreviousStateMetacards()) {
            origMetacards.put(getRegistryId(mcard), mcard);
        }

        //update the new metacards with the id from the orig so that they can be found on the remote system
        try {
            for (Map.Entry<String, Metacard> entry : updatedMetacards.entrySet()) {
                setMetacardExtID(entry.getValue(),
                        origMetacards.get(entry.getKey())
                                .getId());
            }
        } catch (ParserException | IOException e) {
            throw new IngestException("Could not update metacards id", e);
        }

        return super.update(request);
    }

    public void setPushAllowed(boolean pushAllowed) {
        this.pushAllowed = pushAllowed;
    }

    public void setPullAllowed(boolean pullAllowed) {
        this.pullAllowed = pullAllowed;
    }

    private String getRegistryId(Metacard mcard) {
        Attribute attr = mcard.getAttribute(RegistryObjectMetacardType.REGISTRY_ID);
        if (attr != null && attr.getValue() instanceof String) {
            return (String) attr.getValue();
        }
        return null;
    }

    private void setMetacardExtID(Metacard metacard, String newId)
            throws ParserException, IOException {

        String metadata = metacard.getMetadata();

        InputStream inputStream = new ByteArrayInputStream(metadata.getBytes(Charsets.UTF_8));
        FileBackedOutputStream outputStream = new FileBackedOutputStream(1000000);

        JAXBElement<RegistryObjectType> registryObjectTypeJAXBElement = parser.unmarshal(
                unmarshalConfigurator,
                JAXBElement.class,
                inputStream);

        if (registryObjectTypeJAXBElement != null) {
            RegistryObjectType registryObjectType = registryObjectTypeJAXBElement.getValue();

            if (registryObjectType != null) {
                List<ExternalIdentifierType> currentExtIdList =
                        registryObjectType.getExternalIdentifier();
                for (ExternalIdentifierType extId : currentExtIdList) {
                    if (extId.getId()
                            .equals(RegistryConstants.REGISTRY_MCARD_LOCAL_ID)) {
                        extId.setValue(newId);
                        break;
                    }
                }

                registryObjectTypeJAXBElement.setValue(registryObjectType);
                parser.marshal(marshalConfigurator, registryObjectTypeJAXBElement, outputStream);

                String xml = CharStreams.toString(outputStream.asByteSource()
                        .asCharSource(Charsets.UTF_8)
                        .openStream());
                metacard.setAttribute(new AttributeImpl(Metacard.METADATA, xml));
            }
        }
    }

    public void setParser(Parser parser) {

        this.unmarshalConfigurator =
                parser.configureParser(Arrays.asList(RegistryObjectType.class.getPackage()
                                .getName(),
                        net.opengis.ogc.ObjectFactory.class.getPackage()
                                .getName(),
                        net.opengis.gml.v_3_1_1.ObjectFactory.class.getPackage()
                                .getName()),
                        this.getClass()
                                .getClassLoader());
        this.marshalConfigurator =
                parser.configureParser(Arrays.asList(RegistryObjectType.class.getPackage()
                                .getName(),
                        net.opengis.ogc.ObjectFactory.class.getPackage()
                                .getName(),
                        net.opengis.gml.v_3_1_1.ObjectFactory.class.getPackage()
                                .getName()),
                        this.getClass()
                                .getClassLoader());
        this.marshalConfigurator.addProperty(Marshaller.JAXB_FRAGMENT, true);
        this.parser = parser;
    }
}
