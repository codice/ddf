/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.extensiblemetacard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.data.MetacardTypeUnregistrationException;
import ddf.catalog.data.QualifiedMetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.QualifiedMetacardTypeImpl;
import ddf.catalog.data.metacardtype.MetacardTypeRegistryImpl;
import ddf.catalog.data.types.Core;
import java.util.HashSet;
import java.util.Set;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.InvalidSyntaxException;

public class MetacardTypeRegistryTest {

  public static final long SAMPLE_A_FREQUENCY = 14000000;

  public static final long SAMPLE_A_MIN_FREQUENCY = 10000000;

  public static final long SAMPLE_A_MAX_FREQUENCY = 20000000;

  public static final int SAMPLE_A_ANGLE = 180;

  public static final String DEFAULT_TITLE = "myTitle";

  public static final String DEFAULT_ID = "myId";

  public static final String DEFAULT_VERSION = "myVersion";

  public static final String DEFAULT_TYPE = "myType";

  public static final byte[] DEFAULT_BYTES = {8};

  // Constants for Sample Metacard A defined below
  private static final String ANGLE_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A = "angle";

  private static final String MAX_FREQUENCY_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A = "max-frequency";

  private static final String MIN_FREQUENCY_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A = "min-frequency";

  private static final String FREQUENCY_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A = "frequency";

  private static final int NUM_ATTRIBUTES_SAMPLE_METACARD_TYPE_A = 6;

  private static final String SAMPLE_A_METACARD_TYPE_NAME = "MetacardTypeA";

  // Constants for Sample Metacard B defined below
  private static final String NUMBER_REVIEWERS_ATTRIBUTE_KEY = "number-reviewers";

  private static final String PRECISE_HEIGHT_METERS_ATTRIBUTE_KEY = "precise-height-meters";

  private static final String REVIEWED_ATTRIBUTE_KEY = "reviewed";

  private static final String PRECISE_LENGTH_METERS_ATTRIBUTE_KEY = "precise-length-meters";

  private static final String DESCRIPTION_ATTRIBUTE_KEY = "description";

  private static final String ROWS_ATTRIBUTE_KEY = "rows";

  private static final String COLUMNS_ATTRIBUTE_KEY = "columns";

  private static final String SAMPLE_B_METACARD_TYPE_NAME = "MetacardTypeB";

  private static final int NUM_ATTRIBUTES_SAMPLE_METACARD_TYPE_B = 9;

  private static final String SAMPLE_B_METACARD_TYPE_NAMESPACE = "sample.b.namespace";

  private static final String QUALIFIED_METACARD_TYPE_NAMESPACE_BAD = "namespace.bad";

  private static final String QUALIFIED_METACARD_TYPE_NAME_BAD = "qmt-bad";

  private static final String QUALIFIED_METACARD_TYPE_NAME_3 = "qmt3";

  private static final String QUALIFIED_METACARD_TYPE_NAME_2 = "qmt2";

  private static final String METADATA_ATTRIBUTE_DESCRIPTOR_NAME = "metadata";

  private static final String GEO_ATTRIBUTE_DESCRIPTOR_NAME = "geo";

  private static final int QUALIFIED_METACARD_TYPE_DESCRIPTORS_SIZE = 2;

  private static final String QUALIFIED_METACARD_TYPE_NAMESPACE_1 = "ddf.test.namespace";

  private static final String QUALIFIED_METACARD_TYPE_NAME_1 = "qmt1";

  private static final String DEFAULT_DESCRIPTION = "sample description";

  private static final int DEFAULT_ROWS = 100;

  private static final int DEFAULT_COLUMNS = 5;

  private static final String DEFAULT_MODIFIED_DATE = "2012-09-01T00:09:19.368+0000";

  private static final String DEFAULT_CREATED_DATE = "2012-08-01T00:09:19.368+0000";

  private static final String DEFAULT_URI = "http://example.com";

  private static final Object DEFAULT_EXPIRATION_DATE = "2013-09-01T00:09:19.368+0000";

  private static final Object DEFAULT_EFFECTIVE_DATE = "2012-08-15T00:09:19.368+0000";

  private static MetacardTypeRegistry mtr;

  private static HashSet<AttributeDescriptor> qmtAttributes;

  @BeforeClass
  public static void setupMetacardTypeRegistry() throws Exception {
    mtr = MetacardTypeRegistryImpl.getInstance();

    qmtAttributes = new HashSet<AttributeDescriptor>();
    AttributeDescriptor ad1 =
        new AttributeDescriptorImpl(
            GEO_ATTRIBUTE_DESCRIPTOR_NAME, true, true, false, false, BasicTypes.GEO_TYPE);
    qmtAttributes.add(ad1);
    AttributeDescriptor ad2 =
        new AttributeDescriptorImpl(
            METADATA_ATTRIBUTE_DESCRIPTOR_NAME, true, true, false, false, BasicTypes.XML_TYPE);
    qmtAttributes.add(ad2);

    QualifiedMetacardTypeImpl qmt1 =
        new QualifiedMetacardTypeImpl(
            QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
    mtr.register(qmt1);

    QualifiedMetacardTypeImpl qmt2 =
        new QualifiedMetacardTypeImpl(
            QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_2, qmtAttributes);
    mtr.register(qmt2);

    QualifiedMetacardTypeImpl qmt3 =
        new QualifiedMetacardTypeImpl(
            QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE,
            QUALIFIED_METACARD_TYPE_NAME_3,
            qmtAttributes);
    mtr.register(qmt3);
  }

  private void assertOnExpectedMetacardTypeFields(QualifiedMetacardType qmtResult) {
    Set<AttributeDescriptor> attributeDescriptors = qmtResult.getAttributeDescriptors();
    assertNotNull(attributeDescriptors);
    assertEquals(QUALIFIED_METACARD_TYPE_DESCRIPTORS_SIZE, attributeDescriptors.size());

    AttributeDescriptor geoAD = qmtResult.getAttributeDescriptor(GEO_ATTRIBUTE_DESCRIPTOR_NAME);
    assertNotNull(geoAD);
    assertEquals(GEO_ATTRIBUTE_DESCRIPTOR_NAME, geoAD.getName());

    AttributeDescriptor metadataAD =
        qmtResult.getAttributeDescriptor(METADATA_ATTRIBUTE_DESCRIPTOR_NAME);
    assertNotNull(metadataAD);
    assertEquals(METADATA_ATTRIBUTE_DESCRIPTOR_NAME, metadataAD.getName());
  }

  @Test
  public void testLookupMetacardType() {
    QualifiedMetacardType qmtResult =
        mtr.lookup("ddf.test.namespace", QUALIFIED_METACARD_TYPE_NAME_1);
    assertNotNull(qmtResult);
    assertEquals(QUALIFIED_METACARD_TYPE_NAME_1, qmtResult.getName());
    assertEquals(QUALIFIED_METACARD_TYPE_NAMESPACE_1, qmtResult.getNamespace());

    assertOnExpectedMetacardTypeFields(qmtResult);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLookupMetacardTypeNullNamespace() {
    mtr.lookup(null, QUALIFIED_METACARD_TYPE_NAME_3);
  }

  @Test
  public void testLookupMetacardTypeEmptyNamespace() {
    QualifiedMetacardType qmtResult = mtr.lookup("", QUALIFIED_METACARD_TYPE_NAME_3);
    assertNotNull(qmtResult);
    assertEquals(QUALIFIED_METACARD_TYPE_NAME_3, qmtResult.getName());
    assertEquals("", qmtResult.getNamespace());

    assertOnExpectedMetacardTypeFields(qmtResult);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLookupMetacardTypeNullName() {
    mtr.lookup(QUALIFIED_METACARD_TYPE_NAMESPACE_1, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testLookupMetacardTypeEmptyName() {
    mtr.lookup(QUALIFIED_METACARD_TYPE_NAMESPACE_1, "");
  }

  @Test
  public void testLookupMetacardTypeCantFindName() {
    QualifiedMetacardType qmt =
        mtr.lookup(QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_BAD);
    assertNull(qmt);
  }

  @Test
  public void testLookupMetacardTypeCantFindNamespace() {
    QualifiedMetacardType qmt =
        mtr.lookup(QUALIFIED_METACARD_TYPE_NAMESPACE_BAD, QUALIFIED_METACARD_TYPE_NAME_3);
    assertNull(qmt);
  }

  @Test
  public void testNoNamespaceLookup() {
    QualifiedMetacardType qmt = mtr.lookup(QUALIFIED_METACARD_TYPE_NAME_3);
    assertNotNull(qmt);
    assertEquals(QUALIFIED_METACARD_TYPE_NAME_3, qmt.getName());
    assertEquals(QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, qmt.getNamespace());
    assertOnExpectedMetacardTypeFields(qmt);
  }

  @Test
  public void testNoNamespaceLookupCantFindName() {
    QualifiedMetacardType qmt = mtr.lookup(QUALIFIED_METACARD_TYPE_NAME_BAD);
    assertNull(qmt);
  }

  @Test
  public void testNoNamespaceLookupMatchingNameMismatchingNamespace() {
    QualifiedMetacardType qmt = mtr.lookup(QUALIFIED_METACARD_TYPE_NAME_2);
    assertNull(qmt);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoNamespaceLookupEmptyName() {
    mtr.lookup("");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNoNamespaceLookupNullName() {
    mtr.lookup(null);
  }

  @Test
  public void registerMetacardType()
      throws InvalidSyntaxException, IllegalArgumentException, MetacardTypeUnregistrationException {
    assertEquals(4, mtr.getRegisteredTypes().size());
    mtr.register(sampleMetacardTypeA());
    mtr.register(sampleMetacardTypeB());
    assertEquals(6, mtr.getRegisteredTypes().size());

    QualifiedMetacardType metacardTypeAFromRegistry =
        mtr.lookup(
            QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, SAMPLE_A_METACARD_TYPE_NAME);
    assertNotNull(metacardTypeAFromRegistry);
    assertEquals(SAMPLE_A_METACARD_TYPE_NAME, metacardTypeAFromRegistry.getName());
    assertEquals(
        QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE,
        metacardTypeAFromRegistry.getNamespace());
    assertOnSampleMetacardTypeAExpectedAttributes(metacardTypeAFromRegistry);

    QualifiedMetacardType metacardTypeBFromRegistry =
        mtr.lookup(SAMPLE_B_METACARD_TYPE_NAMESPACE, SAMPLE_B_METACARD_TYPE_NAME);
    assertNotNull(metacardTypeBFromRegistry);
    assertEquals(SAMPLE_B_METACARD_TYPE_NAME, metacardTypeBFromRegistry.getName());
    assertEquals(SAMPLE_B_METACARD_TYPE_NAMESPACE, metacardTypeBFromRegistry.getNamespace());
    assertOnSampleMetacardTypeBExcpectedAttributes(metacardTypeBFromRegistry);

    mtr.unregister(sampleMetacardTypeA());
    mtr.unregister(sampleMetacardTypeB());
  }

  @Test
  public void registerMetacardTypeNullNamespace()
      throws IllegalArgumentException, MetacardTypeUnregistrationException {
    QualifiedMetacardType qmt =
        new QualifiedMetacardTypeImpl(null, QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
    mtr.register(qmt);
    QualifiedMetacardType metacardTypeFromReg =
        mtr.lookup(
            QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, QUALIFIED_METACARD_TYPE_NAME_1);
    assertNotNull(metacardTypeFromReg);
    assertEquals(
        QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, metacardTypeFromReg.getNamespace());
    assertEquals(QUALIFIED_METACARD_TYPE_NAME_1, metacardTypeFromReg.getName());
    mtr.unregister(qmt);
  }

  @Test
  public void registerMetacardTypeEmptyNamepsace()
      throws IllegalArgumentException, MetacardTypeUnregistrationException {
    QualifiedMetacardType qmt =
        new QualifiedMetacardTypeImpl("", QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
    mtr.register(qmt);
    QualifiedMetacardType metacardTypeFromReg =
        mtr.lookup(
            QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, QUALIFIED_METACARD_TYPE_NAME_1);
    assertNotNull(metacardTypeFromReg);
    assertEquals(
        QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, metacardTypeFromReg.getNamespace());
    assertEquals(QUALIFIED_METACARD_TYPE_NAME_1, metacardTypeFromReg.getName());
    mtr.unregister(qmt);
  }

  @Test(expected = IllegalArgumentException.class)
  public void registerMetacardTypeNull() {
    mtr.register(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void registerMetacardTypeNullName() {
    QualifiedMetacardType qmt =
        new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, null, qmtAttributes);
    mtr.register(qmt);
  }

  @Test(expected = IllegalArgumentException.class)
  public void registerMetacardTypeEmptyName() {

    QualifiedMetacardType qmt =
        new QualifiedMetacardTypeImpl(QUALIFIED_METACARD_TYPE_NAMESPACE_1, "", qmtAttributes);
    mtr.register(qmt);
  }

  @Test
  public void testRegisteredTypes() {
    Set<QualifiedMetacardType> registeredTypes = mtr.getRegisteredTypes();

    assertEquals(4, registeredTypes.size());

    QualifiedMetacardTypeImpl test0 =
        new QualifiedMetacardTypeImpl(
            QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
    assertTrue(registeredTypes.contains(test0));

    QualifiedMetacardTypeImpl test1 =
        new QualifiedMetacardTypeImpl(
            QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_2, qmtAttributes);
    assertTrue(registeredTypes.contains(test1));

    QualifiedMetacardTypeImpl test2 =
        new QualifiedMetacardTypeImpl(
            QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE,
            QUALIFIED_METACARD_TYPE_NAME_3,
            qmtAttributes);
    assertTrue(registeredTypes.contains(test2));
  }

  @Test
  public void testUnregister()
      throws IllegalArgumentException, MetacardTypeUnregistrationException {
    assertEquals(4, mtr.getRegisteredTypes().size());
    mtr.register(sampleMetacardTypeA());
    mtr.register(sampleMetacardTypeB());
    assertEquals(6, mtr.getRegisteredTypes().size());

    QualifiedMetacardType metacardTypeAFromRegistry =
        mtr.lookup(
            QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE, SAMPLE_A_METACARD_TYPE_NAME);
    assertNotNull(metacardTypeAFromRegistry);
    assertEquals(SAMPLE_A_METACARD_TYPE_NAME, metacardTypeAFromRegistry.getName());
    assertEquals(
        QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE,
        metacardTypeAFromRegistry.getNamespace());
    assertOnSampleMetacardTypeAExpectedAttributes(metacardTypeAFromRegistry);

    QualifiedMetacardType metacardTypeBFromRegistry =
        mtr.lookup(SAMPLE_B_METACARD_TYPE_NAMESPACE, SAMPLE_B_METACARD_TYPE_NAME);
    assertNotNull(metacardTypeBFromRegistry);
    assertEquals(SAMPLE_B_METACARD_TYPE_NAME, metacardTypeBFromRegistry.getName());
    assertEquals(SAMPLE_B_METACARD_TYPE_NAMESPACE, metacardTypeBFromRegistry.getNamespace());
    assertOnSampleMetacardTypeBExcpectedAttributes(metacardTypeBFromRegistry);

    mtr.unregister(sampleMetacardTypeA());

    assertEquals(5, mtr.getRegisteredTypes().size());
    assertTrue(mtr.getRegisteredTypes().contains(sampleMetacardTypeB()));
    assertFalse(mtr.getRegisteredTypes().contains(sampleMetacardTypeA()));

    QualifiedMetacardTypeImpl qmt1 =
        new QualifiedMetacardTypeImpl(
            QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_1, qmtAttributes);
    assertTrue(mtr.getRegisteredTypes().contains(qmt1));

    QualifiedMetacardTypeImpl qmt2 =
        new QualifiedMetacardTypeImpl(
            QUALIFIED_METACARD_TYPE_NAMESPACE_1, QUALIFIED_METACARD_TYPE_NAME_2, qmtAttributes);
    assertTrue(mtr.getRegisteredTypes().contains(qmt2));

    QualifiedMetacardTypeImpl qmt3 =
        new QualifiedMetacardTypeImpl(
            QualifiedMetacardType.DEFAULT_METACARD_TYPE_NAMESPACE,
            QUALIFIED_METACARD_TYPE_NAME_3,
            qmtAttributes);
    assertTrue(mtr.getRegisteredTypes().contains(qmt3));

    mtr.unregister(sampleMetacardTypeB());
  }

  private QualifiedMetacardType sampleMetacardTypeA() {
    Set<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
    descriptors.add(
        new AttributeDescriptorImpl(
            FREQUENCY_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.LONG_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            MIN_FREQUENCY_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.LONG_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            MAX_FREQUENCY_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.LONG_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            ANGLE_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.INTEGER_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            Core.ID,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            Core.TITLE,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));

    return new QualifiedMetacardTypeImpl("", SAMPLE_A_METACARD_TYPE_NAME, descriptors);
  }

  private void assertOnSampleMetacardTypeAExpectedAttributes(QualifiedMetacardType qmtResult) {
    Set<AttributeDescriptor> attributeDescriptors = qmtResult.getAttributeDescriptors();
    assertNotNull(attributeDescriptors);
    assertEquals(NUM_ATTRIBUTES_SAMPLE_METACARD_TYPE_A, attributeDescriptors.size());

    AttributeDescriptor angle =
        qmtResult.getAttributeDescriptor(ANGLE_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A);
    assertNotNull(angle);
    assertEquals(ANGLE_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A, angle.getName());

    AttributeDescriptor maxFreq =
        qmtResult.getAttributeDescriptor(MAX_FREQUENCY_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A);
    assertNotNull(maxFreq);
    assertEquals(MAX_FREQUENCY_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A, maxFreq.getName());

    AttributeDescriptor minFreq =
        qmtResult.getAttributeDescriptor(MIN_FREQUENCY_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A);
    assertNotNull(minFreq);
    assertEquals(MIN_FREQUENCY_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A, minFreq.getName());

    AttributeDescriptor freq =
        qmtResult.getAttributeDescriptor(FREQUENCY_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A);
    assertNotNull(freq);
    assertEquals(FREQUENCY_ATTRIBUTE_NAME_SAMPLE_METACARD_TYPE_A, freq.getName());

    AttributeDescriptor metacardId = qmtResult.getAttributeDescriptor(Core.ID);
    assertNotNull(metacardId);
    assertEquals(Core.ID, metacardId.getName());

    AttributeDescriptor metacardTitle = qmtResult.getAttributeDescriptor(Core.TITLE);
    assertNotNull(metacardTitle);
    assertEquals(Core.TITLE, metacardTitle.getName());
  }

  private QualifiedMetacardType sampleMetacardTypeB() {
    Set<AttributeDescriptor> descriptors = new HashSet<AttributeDescriptor>();
    descriptors.add(
        new AttributeDescriptorImpl(
            COLUMNS_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.INTEGER_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            ROWS_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.INTEGER_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            DESCRIPTION_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            Core.ID,
            true /* indexed */,
            true /* stored */,
            false /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            Core.TITLE,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.STRING_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            REVIEWED_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.BOOLEAN_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            PRECISE_LENGTH_METERS_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.DOUBLE_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            PRECISE_HEIGHT_METERS_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.FLOAT_TYPE));
    descriptors.add(
        new AttributeDescriptorImpl(
            NUMBER_REVIEWERS_ATTRIBUTE_KEY,
            true /* indexed */,
            true /* stored */,
            true /* tokenized */,
            false /* multivalued */,
            BasicTypes.SHORT_TYPE));

    return new QualifiedMetacardTypeImpl(
        SAMPLE_B_METACARD_TYPE_NAMESPACE, SAMPLE_B_METACARD_TYPE_NAME, descriptors);
  }

  private void assertOnSampleMetacardTypeBExcpectedAttributes(QualifiedMetacardType qmt) {
    Set<AttributeDescriptor> attributeDescriptors = qmt.getAttributeDescriptors();
    assertNotNull(attributeDescriptors);
    assertEquals(NUM_ATTRIBUTES_SAMPLE_METACARD_TYPE_B, attributeDescriptors.size());

    AttributeDescriptor preciseHeight =
        qmt.getAttributeDescriptor(PRECISE_HEIGHT_METERS_ATTRIBUTE_KEY);
    assertNotNull(preciseHeight);
    assertEquals(PRECISE_HEIGHT_METERS_ATTRIBUTE_KEY, preciseHeight.getName());

    AttributeDescriptor reviewed = qmt.getAttributeDescriptor(REVIEWED_ATTRIBUTE_KEY);
    assertNotNull(reviewed);
    assertEquals(REVIEWED_ATTRIBUTE_KEY, reviewed.getName());

    AttributeDescriptor preciseLength =
        qmt.getAttributeDescriptor(PRECISE_LENGTH_METERS_ATTRIBUTE_KEY);
    assertNotNull(preciseLength);
    assertEquals(PRECISE_LENGTH_METERS_ATTRIBUTE_KEY, preciseLength.getName());

    AttributeDescriptor description = qmt.getAttributeDescriptor(DESCRIPTION_ATTRIBUTE_KEY);
    assertNotNull(description);
    assertEquals(DESCRIPTION_ATTRIBUTE_KEY, description.getName());

    AttributeDescriptor rows = qmt.getAttributeDescriptor(ROWS_ATTRIBUTE_KEY);
    assertNotNull(rows);
    assertEquals(ROWS_ATTRIBUTE_KEY, rows.getName());

    AttributeDescriptor columns = qmt.getAttributeDescriptor(COLUMNS_ATTRIBUTE_KEY);
    assertNotNull(columns);
    assertEquals(COLUMNS_ATTRIBUTE_KEY, columns.getName());

    AttributeDescriptor numReviewers = qmt.getAttributeDescriptor(NUMBER_REVIEWERS_ATTRIBUTE_KEY);
    assertNotNull(numReviewers);
    assertEquals(NUMBER_REVIEWERS_ATTRIBUTE_KEY, numReviewers.getName());

    AttributeDescriptor metacardId = qmt.getAttributeDescriptor(Core.ID);
    assertNotNull(metacardId);
    assertEquals(Core.ID, metacardId.getName());

    AttributeDescriptor metacardTitle = qmt.getAttributeDescriptor(Core.TITLE);
    assertNotNull(metacardTitle);
    assertEquals(Core.TITLE, metacardTitle.getName());
  }
}
