/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/

package ddf.catalog.extensiblemetacard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeDescriptorImpl;
import ddf.catalog.data.BasicTypes;
import ddf.catalog.data.QualifiedMetacardType;
import ddf.catalog.data.QualifiedMetacardTypeImpl;
import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.data.metacardtype.MetacardTypeRegistryImpl;




public class MetacardTypeRegistryTest {

    private static final String QUALIFIED_METACARD_TYPE_NAMESPACE_BAD = "namespace.bad";
    private static final String QUALIFIED_METACARD_TYPE_NAME_BAD = "qmt-bad";
    private static final String QUALIFIED_METACARD_TYPE_NAME_3 = "qmt3";
    private static final String QUALIFIED_METACARD_TYPE_NAME_2 = "qmt2";
    private static final String INTEGER_ATTRIBUTE_DESCRIPTOR_NAME = "integer";
    private static final String BINARY_ATTRIBUTE_DESCRIPTOR_NAME = "bin";
    private static final String METADATA_ATTRIBUTE_DESCRIPTOR_NAME = "metadata";
    private static final String GEO_ATTRIBUTE_DESCRIPTOR_NAME = "geo";
    private static final int QUALIFIED_METACARD_TYPE_DESCRIPTORS_SIZE = 2;
    private static final String QUALIFIED_METACARD_TYPE_NAMESPACE_1 = "ddf.test.namespace";
    private static final String QUALIFIED_METACARD_TYPE_NAME_1 = "qmt1";
    private static List<QualifiedMetacardType> masterMetacardTypeList;
    
    @BeforeClass
    public static void setupMetacardTypeRegistry(){
	
	masterMetacardTypeList = new ArrayList<QualifiedMetacardType>();
	
	Set<AttributeDescriptor> qmtAttributes = new HashSet<AttributeDescriptor>();

	AttributeDescriptor ad1 = new AttributeDescriptorImpl(GEO_ATTRIBUTE_DESCRIPTOR_NAME, true, true, false, false, BasicTypes.GEO_TYPE);
	qmtAttributes.add(ad1);
	AttributeDescriptor ad2 = new AttributeDescriptorImpl(METADATA_ATTRIBUTE_DESCRIPTOR_NAME, true, true, false, false, BasicTypes.XML_TYPE);
	qmtAttributes.add(ad2);
	
	QualifiedMetacardTypeImpl qmt1 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
	masterMetacardTypeList.add(qmt1);
	
	
	QualifiedMetacardTypeImpl qmt2 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_2, qmtAttributes);
	masterMetacardTypeList.add(qmt2);

	
	QualifiedMetacardTypeImpl qmt3 = new QualifiedMetacardTypeImpl(QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, QUALIFIED_METACARD_TYPE_NAME_3, qmtAttributes);
	masterMetacardTypeList.add(qmt3);
    }
    
    private void assertOnExpectedMetacardTypeFields(QualifiedMetacardType qmtResult) {
        Set<AttributeDescriptor> attributeDescriptors = qmtResult.getAttributeDescriptors();
        assertNotNull(attributeDescriptors);
        assertEquals(QUALIFIED_METACARD_TYPE_DESCRIPTORS_SIZE, attributeDescriptors.size());
        
        AttributeDescriptor geoAD = qmtResult.getAttributeDescriptor(GEO_ATTRIBUTE_DESCRIPTOR_NAME);
        assertNotNull(geoAD);
        assertEquals(GEO_ATTRIBUTE_DESCRIPTOR_NAME, geoAD.getName());
        
        AttributeDescriptor metadataAD = qmtResult.getAttributeDescriptor(METADATA_ATTRIBUTE_DESCRIPTOR_NAME);
        assertNotNull(metadataAD);
        assertEquals(METADATA_ATTRIBUTE_DESCRIPTOR_NAME, metadataAD.getName());
    }

    @Test
    public void testLookupMetacardType(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);

	QualifiedMetacardType qmtResult = registry.lookup("ddf.test.namespace", QUALIFIED_METACARD_TYPE_NAME_1);
	assertNotNull(qmtResult);
	assertEquals(QUALIFIED_METACARD_TYPE_NAME_1, qmtResult.getName());
	assertEquals(QUALIFIED_METACARD_TYPE_NAMESPACE_1, qmtResult.getNamespace());
	
	assertOnExpectedMetacardTypeFields(qmtResult);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testLookupMetacardType_NullNamespace(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	QualifiedMetacardType qmtResult = registry.lookup(null, QUALIFIED_METACARD_TYPE_NAME_3);
//	assertNotNull(qmtResult);
//	assertEquals(QUALIFIED_METACARD_TYPE_NAME_3, qmtResult.getName());
//	assertEquals(DEFAULT_METACARD_TYPE_NAMESPACE_STRING, qmtResult.getNamespace());
//	
//	assertOnExpectedMetacardTypeFields(qmtResult);
    }

    @Test
    public void testLookupMetacardType_EmptyNamespace(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	QualifiedMetacardType qmtResult = registry.lookup("", QUALIFIED_METACARD_TYPE_NAME_3);
	assertNotNull(qmtResult);
	assertEquals(QUALIFIED_METACARD_TYPE_NAME_3, qmtResult.getName());
	assertEquals("", qmtResult.getNamespace());
	
	assertOnExpectedMetacardTypeFields(qmtResult);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testLookupMetacardType_NullName(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	registry.lookup(QUALIFIED_METACARD_TYPE_NAMESPACE_1, null);
	
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testLookupMetacardType_EmptyName(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	registry.lookup(QUALIFIED_METACARD_TYPE_NAMESPACE_1, "");
    }
    
    @Test
    public void testLookupMetacardType_CantFindName(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	QualifiedMetacardType qmt = registry.lookup(QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_BAD);
	assertNull(qmt);
    }

    @Test
    public void testLookupMetacardType_CantFindNamespace(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	QualifiedMetacardType qmt = registry.lookup(QUALIFIED_METACARD_TYPE_NAMESPACE_BAD, QUALIFIED_METACARD_TYPE_NAME_3);
	assertNull(qmt);
    }
    
    @Test
    public void testNoNamespaceLookup(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	QualifiedMetacardType qmt = registry.lookup(QUALIFIED_METACARD_TYPE_NAME_3);
	assertNotNull(qmt);
	assertEquals(QUALIFIED_METACARD_TYPE_NAME_3, qmt.getName());
	assertEquals(QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, qmt.getNamespace());
	assertOnExpectedMetacardTypeFields(qmt);
    }

    @Test
    public void testNoNamespaceLookup_CantFindName(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	QualifiedMetacardType qmt = registry.lookup(QUALIFIED_METACARD_TYPE_NAME_BAD);
	assertNull(qmt);
    }
    
    @Test
    public void testNoNamespaceLookup_MatchingNameMismatchingNamespace(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	QualifiedMetacardType qmt = registry.lookup(QUALIFIED_METACARD_TYPE_NAME_2);
	assertNull(qmt);
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testNoNamespaceLookup_EmptyName(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	registry.lookup("");
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void testNoNamespaceLookup_NullName(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	registry.lookup(null);
    }
    
    @Test
    public void registerMetacardType(){
	//TODO: I should register the service and then work to get the service back out using a getMetacardType
    }
    
    @Test
    public void registerMetacardType_NullNamepsace(){
	//TODO: I should register the service and then work to get the service back out using a getMetacardType.
	// verify that the Namespace is the default namespace
    }

    @Test
    public void registerMetacardType_EmptyNamepsace(){
	//TODO: I should register the service and then work to get the service back out using a getMetacardType.
	// verify that the Namespace is the default namespace
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void registerMetacardType_Null(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	registry.register(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void registerMetacardType_NullName(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	
	QualifiedMetacardType qmt = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, null, masterMetacardTypeList.get(0).getAttributeDescriptors());
	registry.register(qmt);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void registerMetacardType_EmptyName(){
	BundleContext context = mock(BundleContext.class);
	MetacardTypeRegistry registry = MetacardTypeRegistryImpl.getInstance(context, masterMetacardTypeList);
	
	QualifiedMetacardType qmt = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, "", masterMetacardTypeList.get(0).getAttributeDescriptors());
	registry.register(qmt);
    }
    
    @Test
    public void registerMetacardType_WithSourceId(){
	//TODO: I should register the service and then work to get the service back out using a getMetacardType.
	// verify that the service property pulled out contains a source ID property
    }
    
    @Test
    public void registerMetacardType_WithNullSourceId(){
	//TODO: I should register the service and then work to get the service back out using a getMetacardType.
	// verify that everything looks good and that no source ID is specified
    }
    
    @Test
    public void registerMetacardType_WithEmptySourceId(){
	//TODO: I should register the service and then work to get the service back out using a getMetacardType.
	// verify that everything looks good and that no source ID is specified
    }
}
