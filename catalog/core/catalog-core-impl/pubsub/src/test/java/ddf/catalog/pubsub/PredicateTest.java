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
package ddf.catalog.pubsub;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.pubsub.criteria.contenttype.ContentTypeEvaluationCriteriaImpl;
import ddf.catalog.pubsub.criteria.contenttype.ContentTypeEvaluator;
import ddf.catalog.pubsub.criteria.contextual.ContextualEvaluator;
import ddf.catalog.pubsub.criteria.contextual.ContextualTokenizer;
import ddf.catalog.pubsub.criteria.geospatial.GeospatialEvaluationCriteria;
import ddf.catalog.pubsub.criteria.geospatial.GeospatialEvaluationCriteriaImpl;
import ddf.catalog.pubsub.criteria.geospatial.GeospatialEvaluator;
import ddf.catalog.pubsub.criteria.temporal.TemporalEvaluationCriteria;
import ddf.catalog.pubsub.criteria.temporal.TemporalEvaluationCriteriaImpl;
import ddf.catalog.pubsub.criteria.temporal.TemporalEvaluator;
import ddf.catalog.pubsub.internal.PubSubConstants;
import ddf.catalog.pubsub.internal.SubscriptionFilterVisitor;
import ddf.catalog.pubsub.predicate.ContentTypePredicate;
import ddf.catalog.pubsub.predicate.GeospatialPredicate;
import ddf.catalog.pubsub.predicate.Predicate;
import ddf.measure.Distance;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.lucene.store.Directory;
import org.geotools.filter.FilterTransformer;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.opengis.filter.Filter;
import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredicateTest {

  private static final Logger LOGGER = LoggerFactory.getLogger(PredicateTest.class);

  private static final double EQUATORIAL_RADIUS_IN_METERS = 6378137.0;

  private static final double NM_PER_DEG_LAT = 60.0;

  private static final double METERS_PER_NM = 1852.0;

  private static final String METADATA_FORMAT =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n"
          + "<Resource xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
          + "  <identifier qualifier=\"http://metadata.abc.com/mdr/ns/MDR/0.1/MDR.owl#URI\" value=\"http://www.abc.com/news/May2004/n05172004_200405174.html\"/>\r\n"
          + "  <title classification=\"U\" ownerProducer=\"AUS GBR USA\">%s</title>\r\n"
          + "  <creator classification=\"U\" ownerProducer=\"AUS GBR USA\">\r\n"
          + "    <Person>\r\n"
          + "      <name>Donna Miffles</name>\r\n"
          + "      <surname>Cat</surname>\r\n"
          + "      <affiliation>American Forces Press Service</affiliation>\r\n"
          + "    </Person>\r\n"
          + "  </creator>\r\n"
          + "  <subjectCoverage>\r\n"
          + "    <Subject>\r\n"
          + "      <keyword value=\"exercise\"/>\r\n"
          + "      <category qualifier=\"SubjectCoverageQualifier\" code=\"nitf\" label=\"nitf\"/>\r\n"
          + "    </Subject>\r\n"
          + "  </subjectCoverage>\r\n"
          + "  <security classification=\"U\" ownerProducer=\"USA\"/>\r\n"
          + "</Resource>";

  private static final String LEADING_TERM = "%stest";

  private static final String TRAILING_TERM = "test%s";

  private static final String EMBEDDED_TERM = "test%smarker";

  private static final String EMBEDDED_TERM_REVERSED = "marker%stest";

  @Test
  public void testContentTypeEvaluation() throws IOException {

    String methodName = "testContentTypeEvaluation";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    // test input that has type1:version1 matches
    // test input that has type1:version2 doesn't match
    // TODO: tests input that has type2:version1 doesn't match
    // TODO: test input that has type1:""
    // TODO: test input that has "":""
    // TODO: test input that has "":version1
    // TODO: test UNKNOWN for entire contentType String

    MetacardImpl metacard = new MetacardImpl();
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());

    ContentTypePredicate ctp = new ContentTypePredicate("type1", "version1");
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "type1,version1");
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    // Above Pulled from PubSubProviderImpl

    Event testEvent = new Event("topic", properties);
    assertTrue(ctp.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "type1,version2");
    Event testEvent1 = new Event("topic1", properties);
    assertFalse(ctp.matches(testEvent1));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, ",version2");
    testEvent1 = new Event("topic1", properties);
    assertFalse(ctp.matches(testEvent1));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "type1,");
    testEvent1 = new Event("topic1", properties);
    assertFalse(ctp.matches(testEvent1));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);

    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, ",");
    testEvent1 = new Event("topic1", properties);
    assertFalse(ctp.matches(testEvent1));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);

    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "UNKNOWN");
    testEvent1 = new Event("topic1", properties);
    assertFalse(ctp.matches(testEvent1));

    // TODO: test input type1:version1 matches
    // TODO: test input type1:someversion matches
    // TODO: test input type2:version1 doesn't match
    ContentTypePredicate ctp2 = new ContentTypePredicate("type1", null);
    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "type1,version1");
    Event testEvent2 = new Event("topic", properties);
    assertTrue(ctp2.matches(testEvent2));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);

    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "type1,someversion");
    Event testEvent3 = new Event("topic", properties);
    assertTrue(ctp2.matches(testEvent3));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);

    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "type2,someversion");
    Event testEvent4 = new Event("topic", properties);
    assertFalse(ctp2.matches(testEvent4));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testContentTypeEvaluationNullMetadata() throws IOException {
    String methodName = "testContentTypeEvaluationNullMetadata";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    ContentTypePredicate ctp = new ContentTypePredicate("type1", "version1");
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "type1,version1");
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    // No Contextual map added to properties containing indexed metadata

    Event testEvent = new Event("topic", properties);
    assertTrue(ctp.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testContentTypeEvaluationNullOperation() throws IOException {
    String methodName = "testContentTypeEvaluationNullOperation";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());

    ContentTypePredicate ctp = new ContentTypePredicate("type1", "version1");
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "type1,version1");
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, null);
    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);

    Event testEvent = new Event("topic", properties);
    assertTrue(ctp.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testContentTypeFilter() throws Exception {
    String methodName = "testContentTypeFilter";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());

    String type = "type_1";
    String version = "v1";

    List<MockTypeVersionsExtension> extensions = new ArrayList<>();
    MockTypeVersionsExtension ext1 = new MockTypeVersionsExtension();
    List<String> ext1Versions = ext1.getVersions();
    ext1Versions.add(version);
    ext1.setExtensionTypeName(type);
    extensions.add(ext1);

    MockQuery query = new MockQuery();
    query.addTypeFilter(extensions);

    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
    Predicate pred = (Predicate) query.getFilter().accept(visitor, null);

    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type + "," + version);
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    // Below Pulled from PubSubProviderImpl
    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    // Above Pulled from PubSubProviderImpl

    Event testEvent = new Event("topic", properties);
    assertTrue(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type + "," + "unmatching_version");
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "unmatchingtype" + "," + version);
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(
        PubSubConstants.HEADER_CONTENT_TYPE_KEY, "unmatchingtype" + "," + "unmatchingversion");
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  private Map<String, Object> constructContextualMap(MetacardImpl metacard) throws IOException {
    Directory index = ContextualEvaluator.buildIndex(metacard.getMetadata());
    Map<String, Object> contextualMap = new HashMap<>();
    contextualMap.put("DEFAULT_INDEX", index);
    contextualMap.put("METADATA", metacard.getMetadata());
    return contextualMap;
  }

  @Test
  public void testContentTypeFilterTypeOnly() throws Exception {
    String methodName = "testContentTypeFilterTypeOnly";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());

    String type1 = "type_1";
    List<MockTypeVersionsExtension> extensions = new ArrayList<>();
    MockTypeVersionsExtension ext1 = new MockTypeVersionsExtension();
    ext1.setExtensionTypeName(type1);
    extensions.add(ext1);

    MockQuery query = new MockQuery();
    query.addTypeFilter(extensions);

    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
    ContentTypePredicate pred = (ContentTypePredicate) query.getFilter().accept(visitor, null);
    assertEquals(type1, pred.getType());
    assertNull(pred.getVersion());

    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    // Above Pulled from PubSubProviderImpl

    // handle null case
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, null);
    Event testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    // handle content type
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type1 + ",");
    testEvent = new Event("topic", properties);
    assertTrue(pred.matches(testEvent));

    // handle content version that matches content type
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "," + type1);
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type1 + "," + "random_version");
    testEvent = new Event("topic", properties);
    assertTrue(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(
        PubSubConstants.HEADER_CONTENT_TYPE_KEY, "unmatchingtype" + "," + "random_version");
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "," + "unmatchingversion"); // Invalid
    // input
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testMultipleContentTypes() throws IOException {
    String methodName = "testMultipleContentTypes";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    String type1 = "type1";
    String version1 = "version1";
    String type2 = "type2";
    String version2 = "version2";
    String type3 = "type3";
    String version4 = "version4";

    MetacardImpl metacard = new MetacardImpl();
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());

    List<MockTypeVersionsExtension> extensions =
        createContentTypeVersionList("type1,version1|type2,version2|type3,|,version4");

    MockQuery query = new MockQuery();
    query.addTypeFilter(extensions);

    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
    Predicate pred = (Predicate) query.getFilter().accept(visitor, null);
    LOGGER.debug("Resulting Predicate: {}", pred);

    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    // Above Pulled from PubSubProviderImpl

    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type1 + "," + version1);
    Event testEvent = new Event("topic", properties);
    assertTrue(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type2 + "," + version2);
    testEvent = new Event("topic", properties);
    assertTrue(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type2 + "," + version1);
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type1 + "," + version2);
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type3 + "," + "random_version");
    testEvent = new Event("topic", properties);
    assertTrue(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "random_type" + "," + "random_version");
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "random_type" + "," + version4);
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testContentTypesWildcard() throws IOException {
    String methodName = "testContentTypesWildcard";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    String type1 = "type1*";
    String type1Input = "type1";
    String version1 = "version1";
    String type2 = "type2";
    String version2 = "version2*";
    String version2Input = "version2abc";
    String type3 = "ty*pe3";
    String type3Input = "type3";
    String version4 = "version4";

    MetacardImpl metacard = new MetacardImpl();
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());

    List<MockTypeVersionsExtension> extensions = new ArrayList<>();
    MockTypeVersionsExtension ext1 = new MockTypeVersionsExtension();
    List<String> ext1Versions = ext1.getVersions();
    ext1Versions.add(version1);
    ext1.setExtensionTypeName(type1);
    extensions.add(ext1);

    MockTypeVersionsExtension ext2 = new MockTypeVersionsExtension();
    List<String> ext2Versions = ext2.getVersions();
    ext2Versions.add(version2);
    ext2.setExtensionTypeName(type2);
    extensions.add(ext2);

    // No version
    MockTypeVersionsExtension ext3 = new MockTypeVersionsExtension();
    ext3.setExtensionTypeName(type3);
    extensions.add(ext3);

    // No type, only version(s)
    MockTypeVersionsExtension ext4 = new MockTypeVersionsExtension();
    List<String> ext4Versions = ext4.getVersions();
    ext4Versions.add(version4);
    extensions.add(ext4);

    MockQuery query = new MockQuery();
    query.addTypeFilter(extensions);

    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
    Predicate pred = (Predicate) query.getFilter().accept(visitor, null);
    LOGGER.debug("Resulting Predicate: {}", pred);

    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    // Above Pulled from PubSubProviderImpl

    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type1Input + "," + version1);
    Event testEvent = new Event("topic", properties);
    assertTrue(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type2 + "," + version2Input);
    testEvent = new Event("topic", properties);
    assertTrue(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type2 + "," + version1);
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type1 + "," + version2);
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type3Input + "," + "random_version");
    testEvent = new Event("topic", properties);
    assertTrue(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "random_type" + "," + "random_version");
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "random_type" + "," + version4);
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testTemporalEffective() throws Exception {
    testTemporalAttribute(Metacard.EFFECTIVE);
  }

  @Test
  public void testTemporalModified() throws Exception {
    testTemporalAttribute(Core.MODIFIED);
  }

  @Test
  public void testTemporalCreated() throws Exception {
    testTemporalAttribute(Core.CREATED);
  }

  @Test
  public void testTemporalExpiration() throws Exception {
    testTemporalAttribute(Core.EXPIRATION);
  }

  @Test
  public void testTemporalMetacardCreated() throws Exception {
    testTemporalAttribute(Core.METACARD_CREATED);
  }

  @Test
  public void testTemporalMetacardModified() throws Exception {
    testTemporalAttribute(Core.METACARD_MODIFIED);
  }

  private void testTemporalAttribute(String temporalAttr) throws Exception {
    String methodName = "testTemporal: " + temporalAttr;
    LOGGER.debug("***************  START: {}  *****************", methodName);

    MockQuery query = new MockQuery();

    DatatypeFactory df = DatatypeFactory.newInstance();
    XMLGregorianCalendar start = df.newXMLGregorianCalendarDate(2011, 10, 25, 0);
    XMLGregorianCalendar end = df.newXMLGregorianCalendarDate(2011, 10, 27, 0);
    query.addTemporalFilter(start, end, temporalAttr);

    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
    Predicate pred = (Predicate) query.getFilter().accept(visitor, null);
    LOGGER.debug("Resulting Predicate: {}", pred);

    Filter filter = query.getFilter();
    FilterTransformer transform = new FilterTransformer();
    transform.setIndentation(2);
    String filterXml = transform.transform(filter);
    LOGGER.debug(filterXml);

    // input that passes temporal
    LOGGER.debug("\npass temporal.\n");
    MetacardImpl metacard = new MetacardImpl();
    metacard.setCreatedDate(new Date());
    metacard.setExpirationDate(new Date());
    metacard.setModifiedDate(new Date());
    metacard.setAttribute(Core.METACARD_CREATED, new Date());
    metacard.setAttribute(Core.METACARD_MODIFIED, new Date());
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());

    XMLGregorianCalendar cal = df.newXMLGregorianCalendarDate(2011, 10, 26, 0);
    Date dateAttr = cal.toGregorianCalendar().getTime();
    metacard.setAttribute(temporalAttr, dateAttr);

    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    // Above Pulled from PubSubProviderImpl

    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    Event testEvent = new Event("topic", properties);
    boolean b = pred.matches(testEvent);
    assertTrue(b);

    // input that fails temporal
    LOGGER.debug("\nfail temporal.  fail content type.\n");
    XMLGregorianCalendar cal1 = df.newXMLGregorianCalendarDate(2012, 10, 30, 0); // time out of
    // range
    Date dateAttr1 = cal1.toGregorianCalendar().getTime();
    metacard.setAttribute(temporalAttr, dateAttr1);
    LOGGER.debug("metacard date: {}", metacard.getAttribute(temporalAttr));

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testTemporalNullMetadata() throws Exception {
    String methodName = "testTemporalNullMetadata";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    MockQuery query = new MockQuery();

    DatatypeFactory df = DatatypeFactory.newInstance();
    XMLGregorianCalendar start = df.newXMLGregorianCalendarDate(2011, 10, 25, 0);
    XMLGregorianCalendar end = df.newXMLGregorianCalendarDate(2011, 10, 27, 0);
    query.addTemporalFilter(start, end, Metacard.EFFECTIVE);

    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
    Predicate pred = (Predicate) query.getFilter().accept(visitor, null);
    LOGGER.debug("Resulting Predicate: {}", pred);

    Filter filter = query.getFilter();
    FilterTransformer transform = new FilterTransformer();
    transform.setIndentation(2);
    String filterXml = transform.transform(filter);
    LOGGER.debug(filterXml);

    // input that passes temporal
    LOGGER.debug("\npass temporal.\n");
    MetacardImpl metacard = new MetacardImpl();
    metacard.setCreatedDate(new Date());
    metacard.setExpirationDate(new Date());
    metacard.setModifiedDate(new Date());

    XMLGregorianCalendar cal = df.newXMLGregorianCalendarDate(2011, 10, 26, 0);
    Date effectiveDate = cal.toGregorianCalendar().getTime();
    metacard.setEffectiveDate(effectiveDate);

    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    // no contextual map containing indexed contextual data

    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    Event testEvent = new Event("topic", properties);
    boolean b = pred.matches(testEvent);
    assertTrue(b);

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testTemporalNullOperation() throws Exception {
    String methodName = "testTemporalNullOperation";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    MockQuery query = new MockQuery();

    DatatypeFactory df = DatatypeFactory.newInstance();
    XMLGregorianCalendar start = df.newXMLGregorianCalendarDate(2011, 10, 25, 0);
    XMLGregorianCalendar end = df.newXMLGregorianCalendarDate(2011, 10, 27, 0);
    query.addTemporalFilter(start, end, Metacard.EFFECTIVE);

    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
    Predicate pred = (Predicate) query.getFilter().accept(visitor, null);
    LOGGER.debug("Resulting Predicate: {}", pred);

    Filter filter = query.getFilter();
    FilterTransformer transform = new FilterTransformer();
    transform.setIndentation(2);
    String filterXml = transform.transform(filter);
    LOGGER.debug(filterXml);

    // input that passes temporal
    LOGGER.debug("\npass temporal.\n");
    MetacardImpl metacard = new MetacardImpl();
    metacard.setCreatedDate(new Date());
    metacard.setExpirationDate(new Date());
    metacard.setModifiedDate(new Date());
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());

    XMLGregorianCalendar cal = df.newXMLGregorianCalendarDate(2011, 10, 26, 0);
    Date effectiveDate = cal.toGregorianCalendar().getTime();
    metacard.setEffectiveDate(effectiveDate);

    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, null);
    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);

    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    Event testEvent = new Event("topic", properties);
    boolean b = pred.matches(testEvent);
    assertTrue(b);

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testSpatial() throws Exception {
    String methodName = "testSpatial";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    MockQuery query = new MockQuery();

    // XMLGregorianCalendar start = df.newXMLGregorianCalendarDate( 2011, 10, 25, 0 );
    // XMLGregorianCalendar end = df.newXMLGregorianCalendarDate( 2011, 10, 27, 0 );
    // query.addTemporalFilter( start, end, Metacard.EFFECTIVE );
    String geometryWkt = "POINT(44.5 34.5)";
    Double inputRadius = 66672.0;
    String linearUnit = Distance.LinearUnit.METER.name();
    String spatialType = "POINT_RADIUS";
    query.addSpatialFilter(geometryWkt, inputRadius, linearUnit, spatialType);

    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
    Predicate pred = (Predicate) query.getFilter().accept(visitor, null);
    LOGGER.debug("Resulting Predicate: {}", pred);

    Filter filter = query.getFilter();
    FilterTransformer transform = new FilterTransformer();
    transform.setIndentation(2);
    String filterXml = transform.transform(filter);
    LOGGER.debug(filterXml);
  }

  @Test
  public void testMultipleCriteriaWithContentTypes() throws Exception {
    String methodName = "testMultipleCriteriaWithContentTypes";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    MockQuery query = new MockQuery();

    DatatypeFactory df = DatatypeFactory.newInstance();
    XMLGregorianCalendar start = df.newXMLGregorianCalendarDate(2011, 10, 25, 0);
    XMLGregorianCalendar end = df.newXMLGregorianCalendarDate(2011, 10, 27, 0);
    query.addTemporalFilter(start, end, Metacard.EFFECTIVE);

    // create content type criteria
    String version1 = "version1";
    String type1 = "type1";

    List<MockTypeVersionsExtension> extensions = new ArrayList<>();
    MockTypeVersionsExtension ext1 = new MockTypeVersionsExtension();
    List<String> ext1Versions = ext1.getVersions();
    ext1Versions.add(version1);
    ext1.setExtensionTypeName(type1);
    extensions.add(ext1);

    query.addTypeFilter(extensions);

    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
    Predicate pred = (Predicate) query.getFilter().accept(visitor, null);
    LOGGER.debug("Resulting Predicate: {}", pred);

    Filter filter = query.getFilter();
    FilterTransformer transform = new FilterTransformer();
    transform.setIndentation(2);
    String filterXml = transform.transform(filter);
    LOGGER.debug(filterXml);

    // input that passes both temporal and content type
    LOGGER.debug("\npass temporal and pass content type.\n");
    MetacardImpl metacard = new MetacardImpl();
    metacard.setCreatedDate(new Date());
    metacard.setExpirationDate(new Date());
    metacard.setModifiedDate(new Date());
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());

    XMLGregorianCalendar cal = df.newXMLGregorianCalendarDate(2011, 10, 26, 0);
    Date effectiveDate = cal.toGregorianCalendar().getTime();
    metacard.setEffectiveDate(effectiveDate);

    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    // Above Pulled from PubSubProviderImpl

    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type1 + "," + version1);
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    Event testEvent = new Event("topic", properties);
    boolean b = pred.matches(testEvent);
    assertTrue(b);

    // input that fails both temporal and content type
    LOGGER.debug("\nfail temporal.  fail content type.\n");
    XMLGregorianCalendar cal1 = df.newXMLGregorianCalendarDate(2012, 10, 30, 0); // time out of
    // range
    Date effectiveDate1 = cal1.toGregorianCalendar().getTime();
    metacard.setEffectiveDate(effectiveDate1);
    LOGGER.debug("metacard date: {}", metacard.getEffectiveDate());

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "invalid_type" + "," + version1);
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    // input that passes temporal and fails content type
    LOGGER.debug("\npass temporal.  fail content type\n");
    XMLGregorianCalendar cal2 = df.newXMLGregorianCalendarDate(2011, 10, 26, 0); // time in
    // range
    Date effectiveDate2 = cal2.toGregorianCalendar().getTime();
    metacard.setEffectiveDate(effectiveDate2);
    LOGGER.debug("metacard date: {}", metacard.getEffectiveDate());

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "invalid_type" + "," + version1);
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    // input that fails temporal and passes content type
    LOGGER.debug("\nfail temporal.  pass content type\n");
    XMLGregorianCalendar cal3 = df.newXMLGregorianCalendarDate(2012, 10, 26, 0); // time out of
    // range
    Date effectiveDate3 = cal3.toGregorianCalendar().getTime();
    metacard.setEffectiveDate(effectiveDate3);
    LOGGER.debug("metacard date: {}", metacard.getEffectiveDate());

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type1 + "," + version1);
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    // multiple content types
    LOGGER.debug("\nTesting multiple content types.\n");

    String type2 = "type2";
    String version2 = "version2";
    MockTypeVersionsExtension ext2 = new MockTypeVersionsExtension();
    List<String> ext2Versions = ext2.getVersions();
    ext2Versions.add(version2);
    ext2.setExtensionTypeName(type2);
    extensions.add(ext2);

    // No version
    String type3 = "type3";
    MockTypeVersionsExtension ext3 = new MockTypeVersionsExtension();
    ext3.setExtensionTypeName(type3);
    extensions.add(ext3);

    MockQuery query2 = new MockQuery();
    query2.addTemporalFilter(start, end, Metacard.EFFECTIVE);
    query2.addTypeFilter(extensions);
    SubscriptionFilterVisitor visitor1 = new SubscriptionFilterVisitor();
    Predicate pred1 = (Predicate) query2.getFilter().accept(visitor1, null);
    LOGGER.debug("resulting predicate: " + pred1);

    // Create metacard for input
    // time and contentType match
    XMLGregorianCalendar cal4 = df.newXMLGregorianCalendarDate(2011, 10, 26, 0); // time in
    // range
    Date effectiveDate4 = cal4.toGregorianCalendar().getTime();
    metacard.setEffectiveDate(effectiveDate4);
    LOGGER.debug("metacard date: {}", metacard.getEffectiveDate());

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type1 + "," + version1);
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    testEvent = new Event("topic", properties);
    assertTrue(pred1.matches(testEvent));

    // time and contentType match against content type 3 with any version
    XMLGregorianCalendar cal5 = df.newXMLGregorianCalendarDate(2011, 10, 26, 0); // time in
    // range
    Date effectiveDate5 = cal5.toGregorianCalendar().getTime();
    metacard.setEffectiveDate(effectiveDate5);
    LOGGER.debug("metacard date: {}", metacard.getEffectiveDate());

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type3 + "," + "random_version");
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    testEvent = new Event("topic", properties);
    assertTrue(pred1.matches(testEvent));

    // time matches and contentType matches type2
    XMLGregorianCalendar cal6 = df.newXMLGregorianCalendarDate(2011, 10, 26, 0); // time in
    // range
    Date effectiveDate6 = cal6.toGregorianCalendar().getTime();
    metacard.setEffectiveDate(effectiveDate6);
    LOGGER.debug("metacard date: {}", metacard.getEffectiveDate());

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type2 + "," + version2);
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    testEvent = new Event("topic", properties);
    assertTrue(pred1.matches(testEvent));

    // time matches and content type doesn't match
    XMLGregorianCalendar cal7 = df.newXMLGregorianCalendarDate(2011, 10, 26, 0); // time in
    // range
    Date effectiveDate7 = cal7.toGregorianCalendar().getTime();
    metacard.setEffectiveDate(effectiveDate7);
    LOGGER.debug("metacard date: {}", metacard.getEffectiveDate());

    properties.clear();
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, type2 + "," + version1);
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    testEvent = new Event("topic", properties);
    assertFalse(pred1.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testMultipleCriteria() throws Exception {
    String methodName = "testMultipleCriteria";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    // test with temporal, spatial, and entry
    MockQuery query = new MockQuery();

    DatatypeFactory df = DatatypeFactory.newInstance();
    XMLGregorianCalendar start = df.newXMLGregorianCalendarDate(2011, 10, 25, 0);
    XMLGregorianCalendar end = df.newXMLGregorianCalendarDate(2011, 10, 27, 0);
    query.addTemporalFilter(start, end, Metacard.MODIFIED);

    String wkt = "POLYGON((0 10, 0 0, 10 0, 10 10, 0 10))";
    query.addSpatialFilter(wkt, 0.0, "Meter", "CONTAINS");

    // create entry criteria
    String catalogId = "ABC123";
    query.addEntryFilter(catalogId);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setLocation("POINT(5 5)");
    metacard.setId(catalogId);
    metacard.setCreatedDate(new Date());
    metacard.setExpirationDate(new Date());
    metacard.setEffectiveDate(new Date());
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());

    XMLGregorianCalendar cal = df.newXMLGregorianCalendarDate(2011, 10, 26, 0);
    Date modifiedDate = cal.toGregorianCalendar().getTime();
    metacard.setModifiedDate(modifiedDate);

    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_ID_KEY, metacard.getId());
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    // Above Pulled from PubSubProviderImpl

    Event testEvent = new Event("topic", properties);

    // input passes temporal, id, and geo
    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
    Predicate pred = (Predicate) query.getFilter().accept(visitor, null);
    LOGGER.debug("Resulting Predicate: {}", pred);

    Filter filter = query.getFilter();
    FilterTransformer transform = new FilterTransformer();
    transform.setIndentation(2);
    String filterXml = transform.transform(filter);
    LOGGER.debug(filterXml);

    assertTrue(pred.matches(testEvent));

    // input passes temporal, id, but fails geo
    metacard.setLocation("POINT(5 50)"); // geo out of range
    properties.clear();
    properties.put(PubSubConstants.HEADER_ID_KEY, metacard.getId());
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    // Below Pulled from PubSubProviderImpl
    contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    // Above Pulled from PubSubProviderImpl

    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    // input passes geo, and id, but fails temporal
    metacard.setLocation("POINT(5 5)");
    XMLGregorianCalendar cal1 = df.newXMLGregorianCalendarDate(2011, 10, 28, 0);
    Date modifiedDate1 = cal1.toGregorianCalendar().getTime();
    metacard.setModifiedDate(modifiedDate1); // date out of range
    properties.clear();
    properties.put(PubSubConstants.HEADER_ID_KEY, metacard.getId());
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    // Below Pulled from PubSubProviderImpl
    contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    // Above Pulled from PubSubProviderImpl

    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    // input passes temporal, geo, but fails id
    XMLGregorianCalendar cal2 = df.newXMLGregorianCalendarDate(2011, 10, 26, 0);
    Date modifiedDate2 = cal2.toGregorianCalendar().getTime();
    metacard.setModifiedDate(modifiedDate2);
    metacard.setId("invalid_id"); // bad id
    properties.clear();
    properties.put(PubSubConstants.HEADER_ID_KEY, metacard.getId());
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    // Below Pulled from PubSubProviderImpl
    contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    // Above Pulled from PubSubProviderImpl

    testEvent = new Event("topic", properties);
    assertFalse(pred.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testContextualQuery() throws Exception {
    String methodName = "testContextualQuery";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    String searchPhrase = "serengeti event";

    MockQuery query = new MockQuery();
    query.addContextualFilter(searchPhrase, null);

    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
    Predicate predicate = (Predicate) query.getFilter().accept(visitor, null);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("ABC123");
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_ID_KEY, metacard.getId());
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    // Above Pulled from PubSubProviderImpl

    Event testEvent = new Event("topic", properties);
    assertTrue(predicate.matches(testEvent));

    contextualMap.clear();
    properties.clear();
    metacard.setMetadata(TestDataLibrary.getDogEntry());
    Directory index1 = ContextualEvaluator.buildIndex(metacard.getMetadata());
    contextualMap.put("DEFAULT_INDEX", index1);
    contextualMap.put("METADATA", metacard.getMetadata());
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);
    properties.put(PubSubConstants.HEADER_ID_KEY, metacard.getId());
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    testEvent = new Event("topic", properties);
    assertFalse(predicate.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testCaseSensitiveContextualQuery() throws Exception {
    String methodName = "testCaseSensitiveContextualQuery";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    String searchPhrase = "laZy BROwn foX";
    Predicate predicate = getPredicate(searchPhrase, true);

    String metadata = String.format(METADATA_FORMAT, "laZy BROwn foX");
    Event testEvent = getEvent(metadata);
    assertTrue(predicate.matches(testEvent));

    metadata = String.format(METADATA_FORMAT, "lazy brown fox");
    testEvent = getEvent(metadata);
    assertFalse(predicate.matches(testEvent));

    metadata = String.format(METADATA_FORMAT, "laZy bROwn foX");
    testEvent = getEvent(metadata);
    assertFalse(predicate.matches(testEvent));

    metadata = String.format(METADATA_FORMAT, "laZyBROwn foX");
    testEvent = getEvent(metadata);
    assertFalse(predicate.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testContextualQuerySpecialMatch() throws IOException {
    for (String term : Arrays.asList(LEADING_TERM, TRAILING_TERM, EMBEDDED_TERM)) {
      for (Character specialChar : ContextualTokenizer.SPECIAL_CHARACTERS_SET) {
        String phrase = String.format(term, specialChar);
        String metadata = String.format(METADATA_FORMAT, StringEscapeUtils.escapeXml(phrase));
        Predicate predicate = getPredicate("\"" + phrase + "\"");
        Event testEvent = getEvent(metadata);
        assertThat(phrase + " not matched", predicate.matches(testEvent), is(equalTo(true)));
      }
    }
  }

  @Test
  public void testContextualQuerySpecialNotMatch() throws IOException {
    for (String term : Arrays.asList(LEADING_TERM, TRAILING_TERM, EMBEDDED_TERM)) {
      for (Character specialChar : ContextualTokenizer.SPECIAL_CHARACTERS_SET) {
        String phrase = String.format(term, specialChar);
        for (Character differentSpecialChar : ContextualTokenizer.SPECIAL_CHARACTERS_SET) {
          if (specialChar != differentSpecialChar) {
            String metadata =
                String.format(
                    METADATA_FORMAT,
                    StringEscapeUtils.escapeXml(String.format(term, differentSpecialChar)));
            Predicate predicate = getPredicate("\"" + phrase + "\"");
            Event testEvent = getEvent(metadata);
            assertThat(phrase + " matched", predicate.matches(testEvent), is(equalTo(false)));
          }
        }
      }
    }
  }

  @Test
  public void testContextualQuerySurroundedByWildcards() throws Exception {
    Predicate predicate = getPredicate("*test*");
    for (String term : Arrays.asList(LEADING_TERM, TRAILING_TERM, EMBEDDED_TERM)) {
      for (Character specialChar : ContextualTokenizer.SPECIAL_CHARACTERS_SET) {
        String phrase = String.format(term, specialChar);
        String metadata = String.format(METADATA_FORMAT, StringEscapeUtils.escapeXml(phrase));
        Event testEvent = getEvent(metadata);
        assertThat(phrase + " not matched", predicate.matches(testEvent), is(equalTo(true)));
      }
    }
  }

  @Test
  public void testContextualQueryLeadingWildcard() throws Exception {
    Predicate predicate = getPredicate("*test");
    for (String term : Arrays.asList(LEADING_TERM, EMBEDDED_TERM_REVERSED)) {
      for (Character specialChar : ContextualTokenizer.SPECIAL_CHARACTERS_SET) {
        String phrase = String.format(term, specialChar);
        String metadata = String.format(METADATA_FORMAT, StringEscapeUtils.escapeXml(phrase));
        Event testEvent = getEvent(metadata);
        assertThat(phrase + " not matched", predicate.matches(testEvent), is(equalTo(true)));
      }
    }
    for (String term : Arrays.asList(TRAILING_TERM, EMBEDDED_TERM)) {
      for (Character specialChar : ContextualTokenizer.SPECIAL_CHARACTERS_SET) {
        String phrase = String.format(term, specialChar);
        String metadata = String.format(METADATA_FORMAT, StringEscapeUtils.escapeXml(phrase));
        Event testEvent = getEvent(metadata);
        assertThat(phrase + " matched", predicate.matches(testEvent), is(equalTo(false)));
      }
    }
  }

  @Test
  public void testContextualQueryTrailingWildcard() throws Exception {
    Predicate predicate = getPredicate("test*");
    for (String term : Arrays.asList(TRAILING_TERM, EMBEDDED_TERM)) {
      for (Character specialChar : ContextualTokenizer.SPECIAL_CHARACTERS_SET) {
        String phrase = String.format(term, specialChar);
        String metadata = String.format(METADATA_FORMAT, StringEscapeUtils.escapeXml(phrase));
        Event testEvent = getEvent(metadata);
        assertThat(phrase + " not matched", predicate.matches(testEvent), is(equalTo(true)));
      }
    }
    for (String term : Arrays.asList(LEADING_TERM, EMBEDDED_TERM_REVERSED)) {
      for (Character specialChar : ContextualTokenizer.SPECIAL_CHARACTERS_SET) {
        String phrase = String.format(term, specialChar);
        String metadata = String.format(METADATA_FORMAT, StringEscapeUtils.escapeXml(phrase));
        Event testEvent = getEvent(metadata);
        assertThat(phrase + " matched", predicate.matches(testEvent), is(equalTo(false)));
      }
    }
  }

  @Test
  public void testContextualQueryNullMetadata() throws Exception {
    String methodName = "testContextualQueryNullMetadata";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    String searchPhrase = "serengeti event";

    MockQuery query = new MockQuery();
    query.addContextualFilter(searchPhrase, null);

    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();
    Predicate predicate = (Predicate) query.getFilter().accept(visitor, null);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("ABC123");
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_ID_KEY, metacard.getId());
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    // No contextual map containing indexed metadata added to properties

    Event testEvent = new Event("topic", properties);
    assertFalse(predicate.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testTemporalEvaluator() throws Exception {
    LOGGER.debug(
        "**************************  START: testTemporalEvaluator()  ***********************");

    Calendar calendar = Calendar.getInstance();
    Date start = calendar.getTime();
    Date end = null;
    calendar.setTime(start);
    Date date = calendar.getTime();
    TemporalEvaluationCriteria tec = new TemporalEvaluationCriteriaImpl(end, start, date);
    boolean status = TemporalEvaluator.evaluate(tec);

    assertTrue(status);

    LOGGER.debug(
        "**************************  END: testTemporalEvaluator()  ***********************");
  }

  @Test
  public void testGeospatialEvaluatorOverlaps() throws Exception {
    LOGGER.debug(
        "**************************  START: testGeospatialEvaluator_Overlaps()  ***********************");

    // WKT specifies points in LON LAT order
    String geometryWkt = "POLYGON ((40 34, 40 33, 44.5 33, 44.5 34, 40 34))";
    String operation = "overlaps";
    double distance = 0.0000001;
    GeospatialPredicate predicate = new GeospatialPredicate(geometryWkt, operation, distance);
    Geometry geoCriteria = predicate.getGeoCriteria();

    String geospatialXml =
        "<gml:Polygon xmlns:gml=\"http://www.opengis.net/gml\" gml:id=\"BGE-1\">\n"
            + "    <gml:exterior>\n"
            + "        <gml:LinearRing>\n"
            + "            <gml:pos>34.0 44.0</gml:pos>\n"
            + "            <gml:pos>33.0 44.0</gml:pos>\n"
            + "            <gml:pos>33.0 45.0</gml:pos>\n"
            + "            <gml:pos>34.0 45.0</gml:pos>\n"
            + "            <gml:pos>34.0 44.0</gml:pos>\n"
            + "        </gml:LinearRing>\n"
            + "    </gml:exterior>\n"
            + "</gml:Polygon>";

    Geometry input = GeospatialEvaluator.buildGeometry(geospatialXml);
    LOGGER.debug("input.toText() = {}", input.toText());
    GeospatialEvaluationCriteria gec =
        new GeospatialEvaluationCriteriaImpl(geoCriteria, operation, input, distance);
    boolean status = GeospatialEvaluator.evaluate(gec);

    assertTrue(status);

    LOGGER.debug(
        "**************************  END: testGeospatialEvaluator_Overlaps()  ***********************");
  }

  @Test
  public void testGeospatialPredicateNullMetadata() throws IOException {
    String methodName = "testGeospatialPredicateNullMetadata()";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    String geometryWkt = "POLYGON ((40 34, 40 33, 44.5 33, 44.5 34, 40 34))";
    String operation = "overlaps";
    double distance = 0.0000001;
    GeospatialPredicate predicate = new GeospatialPredicate(geometryWkt, operation, distance);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setLocation("POINT (41 34)");

    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "type1,version1");
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    // No Contextual map added to properties containing indexed metadata

    Event testEvent = new Event("topic", properties);
    assertTrue(predicate.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testGeospatialPredicateNullOperation() throws IOException {
    String methodName = "testGeospatialPredicateNullOperation";
    LOGGER.debug("***************  START: {}  *****************", methodName);

    String geometryWkt = "POLYGON ((40 34, 40 33, 44.5 33, 44.5 34, 40 34))";
    String operation = "overlaps";
    double distance = 0.0000001;
    GeospatialPredicate predicate = new GeospatialPredicate(geometryWkt, operation, distance);

    MetacardImpl metacard = new MetacardImpl();
    metacard.setLocation("POINT (41 34)");
    metacard.setMetadata(TestDataLibrary.getCatAndDogEntry());

    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_CONTENT_TYPE_KEY, "type1,version1");
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, null);
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);

    Event testEvent = new Event("topic", properties);
    assertTrue(predicate.matches(testEvent));

    LOGGER.debug("***************  END: {}  *****************", methodName);
  }

  @Test
  public void testGeospatialEvaluatorPointRadiusNotContains() throws Exception {
    LOGGER.debug(
        "**************************  START: testGeospatialEvaluator_PointRadius_NotContains()  ***********************");

    // WKT specifies points in LON LAT order
    String geometryWkt = "POINT (44.5 34.5)";
    String operation = "point_radius";
    double distance = 500.0;
    double radiusInDegrees = (distance * 180.0) / (Math.PI * EQUATORIAL_RADIUS_IN_METERS);
    GeospatialPredicate predicate =
        new GeospatialPredicate(geometryWkt, operation, radiusInDegrees);
    Geometry geoCriteria = predicate.getGeoCriteria();
    LOGGER.debug("geoCriteria.toText() {}", geoCriteria.toText());

    String geospatialXml =
        "<gml:Polygon xmlns:gml=\"http://www.opengis.net/gml\" gml:id=\"BGE-1\">\n"
            + "    <gml:exterior>\n"
            + "        <gml:LinearRing>\n"
            + "            <gml:pos>24.0 22.0</gml:pos>\n"
            + "            <gml:pos>23.0 22.0</gml:pos>\n"
            + "            <gml:pos>23.0 24.0</gml:pos>\n"
            + "            <gml:pos>24.0 24.0</gml:pos>\n"
            + "            <gml:pos>24.0 22.0</gml:pos>\n"
            + "        </gml:LinearRing>\n"
            + "    </gml:exterior>\n"
            + "</gml:Polygon>";

    Geometry input = GeospatialEvaluator.buildGeometry(geospatialXml);
    LOGGER.debug("input.toText() = {}", input.toText());
    GeospatialEvaluationCriteria gec =
        new GeospatialEvaluationCriteriaImpl(geoCriteria, operation, input, radiusInDegrees);
    boolean status = GeospatialEvaluator.evaluate(gec);

    assertFalse(status);

    LOGGER.debug(
        "**************************  END: testGeospatialEvaluator_PointRadius_NotContains()  ***********************");
  }

  @Test
  public void testGeospatialEvaluatorPointRadiusContains() throws Exception {
    LOGGER.debug(
        "**************************  START: testGeospatialEvaluator_PointRadius_Contains()  ***********************");

    // WKT specifies points in LON LAT order
    String geometryWkt = "POINT (44.5 34.5)";
    String operation = "point_radius";
    double distance = 0.6 * NM_PER_DEG_LAT * METERS_PER_NM; // 0.6 degrees
    // latitude in
    // meters
    double radiusInDegrees = (distance * 180.0) / (Math.PI * EQUATORIAL_RADIUS_IN_METERS);
    LOGGER.debug("distance (in meters) = " + distance + ",   radiusInDegrees = " + radiusInDegrees);
    GeospatialPredicate predicate =
        new GeospatialPredicate(geometryWkt, operation, radiusInDegrees);
    Geometry geoCriteria = predicate.getGeoCriteria();
    LOGGER.debug("geoCriteria.toText() = {}", geoCriteria.toText());

    String geospatialXml =
        "<gml:Polygon xmlns:gml=\"http://www.opengis.net/gml\" gml:id=\"BGE-1\">\n"
            + "    <gml:exterior>\n"
            + "        <gml:LinearRing>\n"
            + "            <gml:pos>34.0 44.0</gml:pos>\n"
            + "            <gml:pos>33.0 44.0</gml:pos>\n"
            + "            <gml:pos>33.0 45.0</gml:pos>\n"
            + "            <gml:pos>34.0 45.0</gml:pos>\n"
            + "            <gml:pos>34.0 44.0</gml:pos>\n"
            + "        </gml:LinearRing>\n"
            + "    </gml:exterior>\n"
            + "</gml:Polygon>";

    Geometry input = GeospatialEvaluator.buildGeometry(geospatialXml);
    LOGGER.debug("input.toText() = {}", input.toText());
    GeospatialEvaluationCriteria gec =
        new GeospatialEvaluationCriteriaImpl(geoCriteria, operation, input, radiusInDegrees);
    boolean status = GeospatialEvaluator.evaluate(gec);

    assertTrue(status);

    LOGGER.debug(
        "**************************  END: testGeospatialEvaluator_PointRadius_Contains()  ***********************");
  }

  @Test
  public void testSpatialAndTemporalAnd2EntryPredicates() throws Exception {
    // LOGGER.debug("**************************  START:
    // testSpatial_And_Temporal_And_2_EntryPredicates()  ***********************");
    //
    // Calendar calendar = Calendar.getInstance();
    // Date start = calendar.getTime();
    // calendar.add(Calendar.DAY_OF_YEAR, 1); // add one day to the date/calendar
    // Date end = calendar.getTime();
    //
    // TemporalCriteria temporalCriteria = new TemporalCriteria(start, end, TimeType.EFFECTIVE);
    //
    // String geometryWKT = "POLYGON ((0 10, 0 30, 20 30, 20 10, 0 10))";
    // int maxNeighborResults = 0;
    // SpatialCriteria spatialCriteria = new SpatialCriteria(geometryWKT, new
    // Integer(maxNeighborResults), null, SpatialType.CONTAINS);
    //
    // EntryCriteria entryCriteria1 = new EntryCriteria("a47238c1db7e44ba7f00000100a8654b");
    //
    // EntryCriteria entryCriteria2 = new EntryCriteria("a47238c1db7e44ba7f00000100a8ffff");
    //
    // FilterNode entries = new BinaryOperationImpl(entryCriteria1, entryCriteria2,
    // LogicalOperator.OR, true);
    // FilterNode temporal = new BinaryOperationImpl(temporalCriteria, entries,
    // LogicalOperator.AND, true);
    // FilterNode root = new BinaryOperationImpl(spatialCriteria, temporal, LogicalOperator.AND,
    // true);
    //
    // MockSubscription subscription = new MockSubscription(null, root, new
    // MockDeliveryMethod());
    //
    // PubSubProviderImpl pubSub = new PubSubProviderImpl(new MockBundle().getBundleContext(),
    // new MockEventAdmin());
    // Predicate finalPredicate = pubSub.createFinalPredicate(subscription);
    // LOGGER.debug("finalPredicate:\n" + finalPredicate.toString());
    //
    // LOGGER.debug("**************************  END:
    // testSpatial_And_Temporal_And_2_EntryPredicates()  ***********************");
  }

  @Test
  public void testSpatialOrTemporalAnd2EntryPredicates() throws Exception {
    // LOGGER.debug("**************************  START:
    // testSpatial_Or_Temporal_And_2_EntryPredicates()  ***********************");
    //
    // Calendar calendar = Calendar.getInstance();
    // Date start = calendar.getTime();
    // calendar.add(Calendar.DAY_OF_YEAR, 1); // add one day to the date/calendar
    // Date end = calendar.getTime();
    //
    // TemporalCriteria temporalCriteria = new TemporalCriteria(start, end, TimeType.EFFECTIVE);
    //
    // String geometryWKT = "POLYGON ((0 10, 0 30, 20 30, 20 10, 0 10))";
    // int maxNeighborResults = 0;
    // SpatialCriteria spatialCriteria = new SpatialCriteria(geometryWKT, new
    // Integer(maxNeighborResults), null, SpatialType.CONTAINS);
    //
    // EntryCriteria entryCriteria1 = new EntryCriteria("a47238c1db7e44ba7f00000100a8654b");
    //
    // EntryCriteria entryCriteria2 = new EntryCriteria("a47238c1db7e44ba7f00000100a8ffff");
    //
    // FilterNode entries = new BinaryOperationImpl(entryCriteria1, entryCriteria2,
    // LogicalOperator.OR, true);
    // FilterNode temporal = new BinaryOperationImpl(temporalCriteria, entries,
    // LogicalOperator.AND, true);
    // FilterNode root = new BinaryOperationImpl(spatialCriteria, temporal, LogicalOperator.OR,
    // true);
    //
    // MockSubscription subscription = new MockSubscription(null, root, new
    // MockDeliveryMethod());
    //
    // PubSubProviderImpl pubSub = new PubSubProviderImpl(new MockBundle().getBundleContext(),
    // new MockEventAdmin());
    // Predicate finalPredicate = pubSub.createFinalPredicate(subscription);
    // LOGGER.debug("finalPredicate:\n" + finalPredicate.toString());
    //
    // LOGGER.debug("**************************  END:
    // testSpatial_Or_Temporal_And_2_EntryPredicates()  ***********************");
  }

  @Test
  public void testContentTypeEvaluatorOnlyTypeMatch() throws Exception {
    LOGGER.debug(
        "**************************  START: testContentTypeEvaluator_OnlyType_Match()  ***********************");

    // Match on "nitf", all versions
    ContentTypePredicate predicate = new ContentTypePredicate("nitf", null);

    String inputContentType = "nitf,v20";

    ContentTypeEvaluationCriteriaImpl ctec =
        new ContentTypeEvaluationCriteriaImpl(predicate, inputContentType);

    boolean status = ContentTypeEvaluator.evaluate(ctec);
    assertTrue(status);

    LOGGER.debug(
        "**************************  END: testContentTypeEvaluator_OnlyType_Match()  ***********************");
  }

  @Test
  public void testContentTypeEvaluatorOnlyTypeNoMatch() throws Exception {
    LOGGER.debug(
        "**************************  START: testContentTypeEvaluator_OnlyType_NoMatch()  ***********************");

    // Match on "nitf", all versions
    ContentTypePredicate predicate = new ContentTypePredicate("nitf", null);

    String inputContentType = "video,v20";

    ContentTypeEvaluationCriteriaImpl ctec =
        new ContentTypeEvaluationCriteriaImpl(predicate, inputContentType);

    boolean status = ContentTypeEvaluator.evaluate(ctec);
    assertFalse(status);

    LOGGER.debug(
        "**************************  END: testContentTypeEvaluator_OnlyType_NoMatch()  ***********************");
  }

  @Test
  public void testContentTypeEvaluatorTypeAndVersionMatch() throws Exception {
    LOGGER.debug(
        "**************************  START: testContentTypeEvaluator_TypeAndVersion_Match()  ***********************");

    // Match on "nitf, v20"
    ContentTypePredicate predicate = new ContentTypePredicate("nitf", "v20");

    String inputContentType = "nitf,v20";

    ContentTypeEvaluationCriteriaImpl ctec =
        new ContentTypeEvaluationCriteriaImpl(predicate, inputContentType);

    boolean status = ContentTypeEvaluator.evaluate(ctec);
    assertTrue(status);

    LOGGER.debug(
        "**************************  END: testContentTypeEvaluator_TypeAndVersion_Match()  ***********************");
  }

  @Test
  public void testContentTypeEvaluatorTypeAndVersionTypeMismatch() throws Exception {
    LOGGER.debug(
        "**************************  START: testContentTypeEvaluator_TypeAndVersion_TypeMismatch()  ***********************");

    // Match on "nitf, v20"
    ContentTypePredicate predicate = new ContentTypePredicate("nitf", "v20");

    String inputContentType = "video,v20";

    ContentTypeEvaluationCriteriaImpl ctec =
        new ContentTypeEvaluationCriteriaImpl(predicate, inputContentType);

    boolean status = ContentTypeEvaluator.evaluate(ctec);
    assertFalse(status);

    LOGGER.debug(
        "**************************  END: testContentTypeEvaluator_TypeAndVersion_TypeMismatch()  ***********************");
  }

  @Test
  public void testContentTypeEvaluatorTypeAndVersionVersionMismatch() throws Exception {
    LOGGER.debug(
        "**************************  START: testContentTypeEvaluator_TypeAndVersion_VersionMismatch()  ***********************");

    // Match on "nitf, v20"
    ContentTypePredicate predicate = new ContentTypePredicate("nitf", "v20");

    String inputContentType = "nitf,v20_Army";

    ContentTypeEvaluationCriteriaImpl ctec =
        new ContentTypeEvaluationCriteriaImpl(predicate, inputContentType);

    boolean status = ContentTypeEvaluator.evaluate(ctec);
    assertFalse(status);

    LOGGER.debug(
        "**************************  END: testContentTypeEvaluator_TypeAndVersion_VersionMismatch()  ***********************");
  }

  /*
   * DEBUG - for testing CaseSensitiveStandardAnalyzer
   *
   * @Test public void testAnalyzers() throws Exception { String[] strings = {
   * "The QUICK brown Fox jumped over the laZy dogs" };
   *
   * for (int i = 0; i < strings.length; i++) { analyze( "contents", strings[i] ); } } END DEBUG
   */

  ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

  /**
   * @param typesVersions List of types and versions where each type/version pairs are pipe
   *     delimited and each type/version is comma-delimited
   */
  private List<MockTypeVersionsExtension> createContentTypeVersionList(String typesVersions) {
    List<MockTypeVersionsExtension> extensions = new ArrayList<>();

    LOGGER.debug("typesVersions = {}", typesVersions);
    String[] typeVersionPairs = typesVersions.split("\\|");
    for (String typeVersionPair : typeVersionPairs) {
      LOGGER.debug("typeVersionPair = {}", typeVersionPair);
      MockTypeVersionsExtension ext = null;
      String[] pair = typeVersionPair.split(",");
      LOGGER.debug("pair.length = {}", pair.length);
      String type = pair[0];
      String version = null;
      if (pair.length == 2) {
        version = pair[1];
      }

      if (type != null && !type.isEmpty()) {
        ext = new MockTypeVersionsExtension();
        ext.setExtensionTypeName(type);
      }

      if (version != null && !version.isEmpty()) {
        if (ext == null) {
          ext = new MockTypeVersionsExtension();
        }
        List<String> extVersions = ext.getVersions();
        extVersions.add(version);
      }

      if (ext != null) {
        extensions.add(ext);
      }
    }

    return extensions;
  }

  private Predicate getPredicate(String searchPhrase) {
    return getPredicate(searchPhrase, null, false);
  }

  private Predicate getPredicate(String searchPhrase, boolean caseSensitive) {
    return getPredicate(searchPhrase, null, caseSensitive);
  }

  private Predicate getPredicate(
      String searchPhrase, String textPathSections, boolean caseSensitive) {
    MockQuery query = new MockQuery();
    query.addContextualFilter(searchPhrase, textPathSections, caseSensitive);

    SubscriptionFilterVisitor visitor = new SubscriptionFilterVisitor();

    return (Predicate) query.getFilter().accept(visitor, null);
  }

  private Event getEvent(String metadata) throws IOException {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setId("ABC123");
    metacard.setMetadata(metadata);
    HashMap<String, Object> properties = new HashMap<>();
    properties.put(PubSubConstants.HEADER_ID_KEY, metacard.getId());
    properties.put(PubSubConstants.HEADER_ENTRY_KEY, metacard);
    properties.put(PubSubConstants.HEADER_OPERATION_KEY, PubSubConstants.CREATE);

    Map<String, Object> contextualMap = constructContextualMap(metacard);
    properties.put(PubSubConstants.HEADER_CONTEXTUAL_KEY, contextualMap);

    return new Event("topic", properties);
  }

  // private static void analyze( String fieldName, String text ) throws IOException
  // {
  // System.out.println("Analzying \"" + text + "\"");
  // for (int i = 0; i < analyzers.length; i++)
  // {
  // Analyzer analyzer = analyzers[i];
  // System.out.println("\t" + analyzer.getClass().getName() + ":");
  // System.out.print("\t\t");
  //
  // String term = "";
  // TokenStream ts = analyzer.tokenStream( fieldName, new StringReader( text ) );
  // TermAttribute termAttribute = (TermAttribute) ts.getAttribute(TermAttribute.class);
  // termAttribute = (TermAttribute) ts.getAttribute(TermAttribute.class);
  // while (ts.incrementToken())
  // {
  // term = termAttribute.term();
  // //System.out.println("token: " + term);
  // System.out.print("[" + term + "] ");
  // }
  // System.out.println("\n");
  // }
  // }

}
