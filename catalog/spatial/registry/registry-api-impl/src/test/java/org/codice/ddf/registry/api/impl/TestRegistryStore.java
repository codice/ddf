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
package org.codice.ddf.registry.api.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;

import org.codice.ddf.cxf.SecureCxfClientFactory;
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.common.metacard.RegistryObjectMetacardType;
import org.codice.ddf.registry.schemabindings.helper.MetacardMarshaller;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import com.thoughtworks.xstream.converters.Converter;

import ddf.catalog.Constants;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.filter.FilterAdapter;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.OperationTransaction;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.OperationTransactionImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.operation.impl.SourceResponseImpl;
import ddf.catalog.operation.impl.UpdateRequestImpl;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.security.encryption.EncryptionService;
import net.opengis.cat.csw.v_2_0_2.TransactionResponseType;
import net.opengis.cat.csw.v_2_0_2.TransactionSummaryType;

public class TestRegistryStore {

    private RegistryStoreImpl registryStore;

    private RegistryStoreImplTest registryStoreImplTest;

    private Parser parser;

    private BundleContext context;

    private Converter provider;

    private CswSourceConfiguration configuration;

    private SecureCxfClientFactory factory;

    private TransformerManager transformer;

    private EncryptionService encryptionService;

    private ConfigurationAdmin configAdmin;

    private FilterAdapter filterAdapter;

    private Configuration config;

    private Filter filter;

    @Before
    public void setup() {
        parser = new XmlParser();
        context = mock(BundleContext.class);
        provider = mock(Converter.class);
        configuration = mock(CswSourceConfiguration.class);
        factory = mock(SecureCxfClientFactory.class);
        transformer = mock(TransformerManager.class);
        encryptionService = mock(EncryptionService.class);
        registryStore = new RegistryStoreImpl(context,
                configuration,
                provider,
                factory,
                encryptionService);
        registryStore.setMetacardMarshaller(new MetacardMarshaller(parser));
        registryStore.setFilterBuilder(new GeotoolsFilterBuilder());
        registryStore.setSchemaTransformerManager(transformer);
        configAdmin = mock(ConfigurationAdmin.class);
        filterAdapter = mock(FilterAdapter.class);
        config = mock(Configuration.class);
        filter = mock(Filter.class);
    }

    @Test
    public void testUpdate() throws Exception {
        registryStore = new RegistryStoreImpl(context,
                configuration,
                provider,
                factory,
                encryptionService) {
            @Override
            public SourceResponse query(QueryRequest queryRequest)
                    throws UnsupportedQueryException {
                List<Result> results = new ArrayList<>();
                results.add(new ResultImpl(getDefaultMetacard()));
                return new SourceResponseImpl(queryRequest, results);
            }

            @Override
            protected void validateOperation() {

            }
        };
        registryStore.setMetacardMarshaller(new MetacardMarshaller(parser));
        registryStore.setFilterBuilder(new GeotoolsFilterBuilder());
        registryStore.setSchemaTransformerManager(transformer);

        Csw csw = mock(Csw.class);
        TransactionResponseType responseType = mock(TransactionResponseType.class);
        TransactionSummaryType tst = new TransactionSummaryType();
        tst.setTotalUpdated(new BigInteger("1"));
        when(responseType.getTransactionSummary()).thenReturn(tst);
        when(factory.getClientForSubject(any())).thenReturn(csw);
        when(csw.transaction(any())).thenReturn(responseType);
        when(transformer.getTransformerIdForSchema(any())).thenReturn(null);
        UpdateRequestImpl request = new UpdateRequestImpl("testId", getDefaultMetacard());
        MetacardImpl updatedMcard = getDefaultMetacard();
        updatedMcard.setId("newTestId");
        OperationTransactionImpl opTrans =
                new OperationTransactionImpl(OperationTransaction.OperationType.UPDATE,
                        Collections.singletonList(updatedMcard));
        request.getProperties()
                .put(Constants.OPERATION_TRANSACTION_KEY, opTrans);
        registryStore.update(request);
        assertThat(request.getUpdates()
                .get(0)
                .getValue()
                .getMetadata()
                .contains("value=\"newTestId\""), is(true));
    }

    private MetacardImpl getDefaultMetacard() {
        InputStream inputStream = getClass().getResourceAsStream("/csw-full-registry-package.xml");
        BufferedReader buffer = new BufferedReader(new InputStreamReader(inputStream));
        String xml = buffer.lines()
                .collect(Collectors.joining("\n"));
        MetacardImpl mcard = new MetacardImpl(new RegistryObjectMetacardType());
        mcard.setId("testId");
        mcard.setTags(Collections.singleton(RegistryConstants.REGISTRY_TAG));
        mcard.setAttribute(RegistryObjectMetacardType.REGISTRY_ID, "registryId");
        mcard.setContentTypeName(RegistryConstants.REGISTRY_NODE_METACARD_TYPE_NAME);
        mcard.setMetadata(xml);
        mcard.setTitle("testRegistryMetacard");
        return mcard;
    }

    @Test
    public void testSetAvailable() throws Exception {
        setupRegistryStoreImplTest();

        registryStoreImplTest.registryInfoQuery();

        assertThat(registryStoreImplTest.getRegistryId(), is("registryId"));
        assertThat(registryStoreImplTest.getRemoteName(), is("testRegistryMetacard"));
    }

    @Test
    public void testQuery() throws Exception {
        setupRegistryStoreImplTest();

        QueryRequest testRequest = new QueryRequestImpl(new QueryImpl(filter));
        SourceResponse answer = registryStoreImplTest.query(testRequest);
        List<Result> testResults = answer.getResults();

        assertThat(testResults.size(), is(1));
        assertThat(testResults.get(0)
                .getMetacard()
                .getTitle(), is("testRegistryMetacard"));
    }

    private void setupRegistryStoreImplTest() throws Exception{
        registryStoreImplTest = new RegistryStoreImplTest();

        registryStoreImplTest.setFilterBuilder(new GeotoolsFilterBuilder());
        registryStoreImplTest.setFilterAdapter(filterAdapter);
        registryStoreImplTest.setConfigAdmin(configAdmin);
        registryStoreImplTest.setRegistryId("registryId");

        when(filterAdapter.adapt(any(), any())).thenReturn(true);
        when(configAdmin.getConfiguration(any())).thenReturn(config);
        when(config.getProperties()).thenReturn(new Hashtable<>());
    }

    private class RegistryStoreImplTest extends RegistryStoreImpl {

        RegistryStoreImplTest() {
            super(encryptionService);
        }

        @Override
        public SourceResponse queryResponse(QueryRequest request) {
            List<Result> results = new ArrayList<>();
            results.add(new ResultImpl(getDefaultMetacard()));
            return new SourceResponseImpl(request, results);
        }
    }
}
