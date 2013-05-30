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

package ddf.catalog.data;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

public class QualifiedMetacardTypeTest {

    private static final String QUALIFIED_METACARD_TYPE_NAME_3 = "qmt3";
    private static final String QUALIFIED_METACARD_TYPE_NAME_2 = "qmt2";
    private static final String METADATA_ATTRIBUTE_DESCRIPTOR_NAME = "metadata";
    private static final String GEO_ATTRIBUTE_DESCRIPTOR_NAME = "geo";
    private static final String QUALIFIED_METACARD_TYPE_NAMESPACE_2 = "ddf.test.namespace.2";
    private static final String QUALIFIED_METACARD_TYPE_NAMESPACE_1 = "ddf.test.namespace";
    private static final String QUALIFIED_METACARD_TYPE_NAME_1 = "qmt1";
    private static Set<AttributeDescriptor> qmtAttributes;
    
    @BeforeClass
    public static void prepareTest(){
	qmtAttributes = new HashSet<AttributeDescriptor>();

	AttributeDescriptor ad1 = new AttributeDescriptorImpl(GEO_ATTRIBUTE_DESCRIPTOR_NAME, true, true, false, false, BasicTypes.GEO_TYPE);
	qmtAttributes.add(ad1);
	AttributeDescriptor ad2 = new AttributeDescriptorImpl(METADATA_ATTRIBUTE_DESCRIPTOR_NAME, true, true, false, false, BasicTypes.XML_TYPE);
	qmtAttributes.add(ad2);
    }
    
    @Test
    public void testEquals(){
	QualifiedMetacardTypeImpl qmt1 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
	QualifiedMetacardTypeImpl qmt2 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
	
	assertTrue(qmt1.equals(qmt2));
	assertTrue(qmt2.equals(qmt1));
	assertEquals(qmt1.hashCode(), qmt2.hashCode());
    }
    
    @Test
    public void testDifferentNames(){
	QualifiedMetacardTypeImpl qmt1 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
	QualifiedMetacardTypeImpl qmt2 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_2, qmtAttributes);
	
	assertTrue(!qmt1.equals(qmt2));
	assertTrue(!qmt2.equals(qmt1));
	assertTrue(qmt1.hashCode() != qmt2.hashCode());
    }
    
    @Test
    public void testDifferentNamespaces(){
	QualifiedMetacardTypeImpl qmt1 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
	QualifiedMetacardTypeImpl qmt2 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_2, QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
	
	assertTrue(!qmt1.equals(qmt2));
	assertTrue(!qmt2.equals(qmt1));
	assertTrue(qmt1.hashCode() != qmt2.hashCode());
    }
    
    @Test
    public void testDifferentNamesAndNamespaces(){
	QualifiedMetacardTypeImpl qmt1 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
	QualifiedMetacardTypeImpl qmt2 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_2, QUALIFIED_METACARD_TYPE_NAME_2, qmtAttributes);
	
	assertTrue(!qmt1.equals(qmt2));
	assertTrue(!qmt2.equals(qmt1));
	assertTrue(qmt1.hashCode() != qmt2.hashCode());
    }    
    
    /**
     * Tests that two QMTs are equal if they have the same name and namespace, regardless of the attributes they have
     */
    @Test
    public void testDifferentAttributes(){
	QualifiedMetacardTypeImpl qmt1 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
	QualifiedMetacardTypeImpl qmt2 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_1, null);
	
	assertTrue(!qmt1.equals(qmt2));
	assertTrue(!qmt2.equals(qmt1));
	assertTrue(qmt1.hashCode() != qmt2.hashCode());
    }
    
    @Test
    public void testDefaultNamespace(){
	MetacardType mt1 = new MetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAME_3, qmtAttributes);
	QualifiedMetacardTypeImpl qmt1 = new QualifiedMetacardTypeImpl(QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, mt1);
	MetacardType mt2 = new MetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAME_3, qmtAttributes);
	QualifiedMetacardTypeImpl qmt2 = new QualifiedMetacardTypeImpl(null, mt2);
	
	assertTrue(qmt1.equals(qmt2));
	assertTrue(qmt2.equals(qmt1));
	assertTrue(qmt1.hashCode() == qmt2.hashCode());
    }
    
    @Test
    public void testDefaultNamespaceNotEqual(){
	MetacardType mt1 = new MetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAME_3, qmtAttributes);
	QualifiedMetacardTypeImpl qmt1 = new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_2, mt1);
	MetacardType mt2 = new MetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAME_3, qmtAttributes);
	QualifiedMetacardTypeImpl qmt2 = new QualifiedMetacardTypeImpl(null, mt2);
	
	assertTrue(!qmt1.equals(qmt2));
	assertTrue(!qmt2.equals(qmt1));
	assertTrue(qmt1.hashCode() != qmt2.hashCode());
    }
    
    @Test
    public void testEqualsQualifiedMetacardTypeAndMetacardTypeImpl(){
	MetacardType mt1 = new MetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAME_3, qmtAttributes);
	QualifiedMetacardTypeImpl qmt1 = new QualifiedMetacardTypeImpl(QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, mt1);
	MetacardType mt2 = new MetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAME_3, qmtAttributes);
	
	assertTrue(qmt1.equals(mt2));
	assertTrue(mt2.equals(qmt1));
    }
    
    
}
