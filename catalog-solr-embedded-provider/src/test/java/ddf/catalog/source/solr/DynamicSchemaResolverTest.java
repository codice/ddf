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
package ddf.catalog.source.solr;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.solr.common.SolrInputDocument;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;

public class DynamicSchemaResolverTest {
    /**
     * Verify that when a metacard type has attribute descriptors that inherit from AttributeDescriptorImpl, the attribute
     * descriptors are recreated as AttributeDescriptorsImpls before serialization into the solr cache.
     */
    @Test
    public void testAddFields() throws Exception {
        // Setup
        String metacardTypeName = "states";
        Set<AttributeDescriptor> addtributeDescriptors = new HashSet<AttributeDescriptor>(1);
        String propertyName = "title";
        String name = metacardTypeName + "." + propertyName;
        boolean indexed = true;
        boolean stored = true;
        boolean tokenized = false;
        boolean multiValued = false;
        addtributeDescriptors.add(new TestAttributeDescriptorImpl(name, propertyName, indexed, stored, tokenized, multiValued, BasicTypes.OBJECT_TYPE));
        Serializable mockValue = mock(Serializable.class);
        Attribute mockAttribute = mock(Attribute.class); 
        when(mockAttribute.getValue()).thenReturn(mockValue);
        Metacard mockMetacard = mock(Metacard.class, RETURNS_DEEP_STUBS);
        when(mockMetacard.getMetacardType().getName()).thenReturn(metacardTypeName);
        when(mockMetacard.getMetacardType().getAttributeDescriptors()).thenReturn(addtributeDescriptors);
        when(mockMetacard.getAttribute(name)).thenReturn(mockAttribute);
        ArgumentCaptor<byte[]> metacardTypeBytes = ArgumentCaptor.forClass(byte[].class);
        SolrInputDocument mockSolrInputDocument = mock(SolrInputDocument.class);
        DynamicSchemaResolver resolver = new DynamicSchemaResolver();
        
        // Perform Test
        resolver.addFields(mockMetacard, mockSolrInputDocument);
        
        // Verify: Verify that TestAttributeDescritorImpl has been recreated as a AttributeDescriptorImpl.
        verify(mockSolrInputDocument).addField(eq(SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME), metacardTypeBytes.capture());
        byte[] serializedMetacardType = metacardTypeBytes.getValue();
        MetacardType metacardType = deserializeMetacardType(serializedMetacardType);
        for(AttributeDescriptor attributeDescriptor : metacardType.getAttributeDescriptors()) {
            assertThat(attributeDescriptor.getClass().getName(), is(AttributeDescriptorImpl.class.getName()));
        }      
    }
    
    private MetacardType deserializeMetacardType(byte[] serializedMetacardType) throws ClassNotFoundException, IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) serializedMetacardType);
        ObjectInputStream in = new ObjectInputStream(bais);
        MetacardType metacardType = (MetacardType) in.readObject();
        IOUtils.closeQuietly(bais);
        IOUtils.closeQuietly(in);
        return metacardType;
    }
}

