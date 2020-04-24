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
package ddf.security.pdp.realm.xacml.processor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import ddf.security.audit.SecurityLogger;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributeValueType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.AttributesType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.DecisionType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ObjectFactory;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.RequestType;
import oasis.names.tc.xacml._3_0.core.schema.wd_17.ResponseType;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.parser.xml.XmlParser;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XacmlClientTest {
  private static final Logger LOGGER = LoggerFactory.getLogger(XacmlClientTest.class);

  private static final String ROLE_CLAIM =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

  private static final String STRING_DATA_TYPE = "http://www.w3.org/2001/XMLSchema#string";

  private static final String ACTION_CATEGORY =
      "urn:oasis:names:tc:xacml:3.0:attribute-category:action";

  private static final String ACTION_ID = "urn:oasis:names:tc:xacml:1.0:action:action-id";

  private static final String SUBJECT_CATEGORY =
      "urn:oasis:names:tc:xacml:1.0:subject-category:access-subject";

  private static final String SUBJECT_ID = "urn:oasis:names:tc:xacml:1.0:subject:subject-id";

  private static final String PERMISSIONS_CATEGORY = "http://example.com/category";

  private static final String CITIZENSHIP_ATTRIBUTE =
      "http://www.opm.gov/feddata/CountryOfCitizenship";

  private static final String RELATIVE_POLICIES_DIR = "src/test/resources";

  private static final String TEMP_DIR_NAME = "tempDir";

  private static final String TEST_CREATION_DIR = "target/temp";

  private static final String QUERY_ACTION = "query";

  private static final String TEST_USER_1 = "testuser1";

  private static final String TEST_USER_2 = "testuser2";

  private static final String ROLE = "user";

  private static final String POLICY_FILE = "query-policy.xml";

  private static final String US_COUNTRY = "USA";

  private static String projectHome;

  @Rule public TemporaryFolder folder = new TemporaryFolder();

  private File tempDir;

  @BeforeClass
  public static void init() {
    try {
      projectHome = new File(".").getCanonicalFile().getPath();
      LOGGER.debug("projectHome: {}", projectHome);
    } catch (IOException e) {
      LOGGER.debug(e.getMessage(), e);
    }
  }

  @Test
  public void testEvaluateroleuseractionquerycitizenshipUS() throws Exception {
    LOGGER.debug("\n\n\n##### testEvaluate_role_user_action_query_citizenship_US");

    // Setup
    File destDir = folder.newFolder(TEMP_DIR_NAME);
    LOGGER.debug("Making directory: {}", destDir.getPath());
    if (destDir.mkdir()) {
      File srcFile =
          new File(
              projectHome + File.separator + RELATIVE_POLICIES_DIR + File.separator + POLICY_FILE);
      FileUtils.copyFileToDirectory(srcFile, destDir);

      RequestType xacmlRequestType = new RequestType();
      xacmlRequestType.setCombinedDecision(false);
      xacmlRequestType.setReturnPolicyIdList(false);

      AttributesType actionAttributes = new AttributesType();
      actionAttributes.setCategory(ACTION_CATEGORY);
      AttributeType actionAttribute = new AttributeType();
      actionAttribute.setAttributeId(ACTION_ID);
      actionAttribute.setIncludeInResult(false);
      AttributeValueType actionValue = new AttributeValueType();
      actionValue.setDataType(STRING_DATA_TYPE);
      actionValue.getContent().add(QUERY_ACTION);
      actionAttribute.getAttributeValue().add(actionValue);
      actionAttributes.getAttribute().add(actionAttribute);

      AttributesType subjectAttributes = new AttributesType();
      subjectAttributes.setCategory(SUBJECT_CATEGORY);
      AttributeType subjectAttribute = new AttributeType();
      subjectAttribute.setAttributeId(SUBJECT_ID);
      subjectAttribute.setIncludeInResult(false);
      AttributeValueType subjectValue = new AttributeValueType();
      subjectValue.setDataType(STRING_DATA_TYPE);
      subjectValue.getContent().add(TEST_USER_1);
      subjectAttribute.getAttributeValue().add(subjectValue);
      subjectAttributes.getAttribute().add(subjectAttribute);

      AttributeType roleAttribute = new AttributeType();
      roleAttribute.setAttributeId(ROLE_CLAIM);
      roleAttribute.setIncludeInResult(false);
      AttributeValueType roleValue = new AttributeValueType();
      roleValue.setDataType(STRING_DATA_TYPE);
      roleValue.getContent().add(ROLE);
      roleAttribute.getAttributeValue().add(roleValue);
      subjectAttributes.getAttribute().add(roleAttribute);

      AttributesType categoryAttributes = new AttributesType();
      categoryAttributes.setCategory(PERMISSIONS_CATEGORY);
      AttributeType citizenshipAttribute = new AttributeType();
      citizenshipAttribute.setAttributeId(CITIZENSHIP_ATTRIBUTE);
      citizenshipAttribute.setIncludeInResult(false);
      AttributeValueType citizenshipValue = new AttributeValueType();
      citizenshipValue.setDataType(STRING_DATA_TYPE);
      citizenshipValue.getContent().add(US_COUNTRY);
      citizenshipAttribute.getAttributeValue().add(citizenshipValue);
      categoryAttributes.getAttribute().add(citizenshipAttribute);

      xacmlRequestType.getAttributes().add(actionAttributes);
      xacmlRequestType.getAttributes().add(subjectAttributes);
      xacmlRequestType.getAttributes().add(categoryAttributes);

      XacmlClient pdp =
          new XacmlClient(destDir.getCanonicalPath(), new XmlParser(), mock(SecurityLogger.class));

      // Perform Test
      ResponseType xacmlResponse = pdp.evaluate(xacmlRequestType);

      // Verify
      JAXBContext jaxbContext = JAXBContext.newInstance(ResponseType.class);
      Marshaller marshaller = jaxbContext.createMarshaller();
      ObjectFactory objectFactory = new ObjectFactory();
      Writer writer = new StringWriter();
      marshaller.marshal(objectFactory.createResponse(xacmlResponse), writer);
      LOGGER.debug("\nXACML 3.0 Response:\n{}", writer.toString());
      assertEquals(xacmlResponse.getResult().get(0).getDecision(), DecisionType.PERMIT);

      // Cleanup
      LOGGER.debug("Deleting directory: {}", destDir);
      FileUtils.deleteDirectory(destDir);
    } else {
      LOGGER.debug("Could not create directory: {}", destDir);
    }
  }

  @Test
  public void testEvaluateroleuseractionquerycitizenshipCA() throws Exception {
    LOGGER.debug("\n\n\n##### testEvaluate_role_user_action_query_citizenship_CA");

    final String country = "CA";

    testSetup();

    RequestType xacmlRequestType = new RequestType();
    xacmlRequestType.setCombinedDecision(false);
    xacmlRequestType.setReturnPolicyIdList(false);

    AttributesType actionAttributes = new AttributesType();
    actionAttributes.setCategory(ACTION_CATEGORY);
    AttributeType actionAttribute = new AttributeType();
    actionAttribute.setAttributeId(ACTION_ID);
    actionAttribute.setIncludeInResult(false);
    AttributeValueType actionValue = new AttributeValueType();
    actionValue.setDataType(STRING_DATA_TYPE);
    actionValue.getContent().add(QUERY_ACTION);
    actionAttribute.getAttributeValue().add(actionValue);
    actionAttributes.getAttribute().add(actionAttribute);

    AttributesType subjectAttributes = new AttributesType();
    subjectAttributes.setCategory(SUBJECT_CATEGORY);
    AttributeType subjectAttribute = new AttributeType();
    subjectAttribute.setAttributeId(SUBJECT_ID);
    subjectAttribute.setIncludeInResult(false);
    AttributeValueType subjectValue = new AttributeValueType();
    subjectValue.setDataType(STRING_DATA_TYPE);
    subjectValue.getContent().add(TEST_USER_2);
    subjectAttribute.getAttributeValue().add(subjectValue);
    subjectAttributes.getAttribute().add(subjectAttribute);

    AttributeType roleAttribute = new AttributeType();
    roleAttribute.setAttributeId(ROLE_CLAIM);
    roleAttribute.setIncludeInResult(false);
    AttributeValueType roleValue = new AttributeValueType();
    roleValue.setDataType(STRING_DATA_TYPE);
    roleValue.getContent().add(ROLE);
    roleAttribute.getAttributeValue().add(roleValue);
    subjectAttributes.getAttribute().add(roleAttribute);

    AttributesType categoryAttributes = new AttributesType();
    categoryAttributes.setCategory(PERMISSIONS_CATEGORY);
    AttributeType citizenshipAttribute = new AttributeType();
    citizenshipAttribute.setAttributeId(CITIZENSHIP_ATTRIBUTE);
    citizenshipAttribute.setIncludeInResult(false);
    AttributeValueType citizenshipValue = new AttributeValueType();
    citizenshipValue.setDataType(STRING_DATA_TYPE);
    citizenshipValue.getContent().add(country);
    citizenshipAttribute.getAttributeValue().add(citizenshipValue);
    categoryAttributes.getAttribute().add(citizenshipAttribute);

    xacmlRequestType.getAttributes().add(actionAttributes);
    xacmlRequestType.getAttributes().add(subjectAttributes);
    xacmlRequestType.getAttributes().add(categoryAttributes);

    XacmlClient pdp =
        new XacmlClient(tempDir.getCanonicalPath(), new XmlParser(), mock(SecurityLogger.class));

    // Perform Test
    ResponseType xacmlResponse = pdp.evaluate(xacmlRequestType);

    // Verify
    JAXBContext jaxbContext = JAXBContext.newInstance(ResponseType.class);
    Marshaller marshaller = jaxbContext.createMarshaller();
    ObjectFactory objectFactory = new ObjectFactory();
    Writer writer = new StringWriter();
    marshaller.marshal(objectFactory.createResponse(xacmlResponse), writer);
    LOGGER.debug("\nXACML 3.0 Response:\n{}", writer.toString());
    assertEquals(xacmlResponse.getResult().get(0).getDecision(), DecisionType.DENY);
  }

  @Test
  public void testWrapperpoliciesdirectorydoesnotexist() throws PdpException, IOException {
    LOGGER.debug("\n\n\n##### testXACMLWrapper_policies_directory_does_not_exist");

    // Perform Test on new directory
    // Expect directory to be created
    new XacmlClient(TEST_CREATION_DIR, new XmlParser(), mock(SecurityLogger.class));

    // Delete the directory that was just created
    FileUtils.forceDelete(new File(TEST_CREATION_DIR));
  }

  @Test
  /** No longer expect an exception thrown here since we can start with an empty directory */
  public void testWrapperpoliciesdirectoryexistsandisempty() throws Exception {
    LOGGER.debug("\n\n\n##### testXACMLWrapper_policies_directory_exists_and_is_empty");

    // Setup
    File dir = folder.newFolder(TEMP_DIR_NAME);
    LOGGER.debug("Making directory: {}", dir.getPath());
    if (dir.mkdir()) {
      assertTrue(dir.isDirectory());
      assertTrue(isDirEmpty(dir));

      // Perform Test
      new XacmlClient(dir.getCanonicalPath(), new XmlParser(), mock(SecurityLogger.class));

      // Cleanup
      LOGGER.debug("Deleting directory: {}", dir.getPath());
      FileUtils.deleteDirectory(dir);
    } else {
      LOGGER.debug("Could not create directory: {}", dir.getPath());
    }
  }

  @Test
  public void testWrapperpoliciesdirectorypolicyadded() throws Exception {
    LOGGER.debug("\n\n\n##### testXACMLWrapper_policies_directory_policy_added");

    File policyDir = folder.newFolder("tempDir");

    XacmlClient.defaultPollingIntervalInSeconds = 1;
    // Perform Test
    XacmlClient pdp =
        new XacmlClient(policyDir.getCanonicalPath(), new XmlParser(), mock(SecurityLogger.class));

    File srcFile =
        new File(
            projectHome + File.separator + RELATIVE_POLICIES_DIR + File.separator + POLICY_FILE);
    FileUtils.copyFileToDirectory(srcFile, policyDir);

    Thread.sleep(2000);

    RequestType xacmlRequestType = new RequestType();
    xacmlRequestType.setCombinedDecision(false);
    xacmlRequestType.setReturnPolicyIdList(false);

    AttributesType actionAttributes = new AttributesType();
    actionAttributes.setCategory(ACTION_CATEGORY);
    AttributeType actionAttribute = new AttributeType();
    actionAttribute.setAttributeId(ACTION_ID);
    actionAttribute.setIncludeInResult(false);
    AttributeValueType actionValue = new AttributeValueType();
    actionValue.setDataType(STRING_DATA_TYPE);
    actionValue.getContent().add(QUERY_ACTION);
    actionAttribute.getAttributeValue().add(actionValue);
    actionAttributes.getAttribute().add(actionAttribute);

    AttributesType subjectAttributes = new AttributesType();
    subjectAttributes.setCategory(SUBJECT_CATEGORY);
    AttributeType subjectAttribute = new AttributeType();
    subjectAttribute.setAttributeId(SUBJECT_ID);
    subjectAttribute.setIncludeInResult(false);
    AttributeValueType subjectValue = new AttributeValueType();
    subjectValue.setDataType(STRING_DATA_TYPE);
    subjectValue.getContent().add(TEST_USER_1);
    subjectAttribute.getAttributeValue().add(subjectValue);
    subjectAttributes.getAttribute().add(subjectAttribute);

    AttributeType roleAttribute = new AttributeType();
    roleAttribute.setAttributeId(ROLE_CLAIM);
    roleAttribute.setIncludeInResult(false);
    AttributeValueType roleValue = new AttributeValueType();
    roleValue.setDataType(STRING_DATA_TYPE);
    roleValue.getContent().add(ROLE);
    roleAttribute.getAttributeValue().add(roleValue);
    subjectAttributes.getAttribute().add(roleAttribute);

    AttributesType categoryAttributes = new AttributesType();
    categoryAttributes.setCategory(PERMISSIONS_CATEGORY);
    AttributeType citizenshipAttribute = new AttributeType();
    citizenshipAttribute.setAttributeId(CITIZENSHIP_ATTRIBUTE);
    citizenshipAttribute.setIncludeInResult(false);
    AttributeValueType citizenshipValue = new AttributeValueType();
    citizenshipValue.setDataType(STRING_DATA_TYPE);
    citizenshipValue.getContent().add(US_COUNTRY);
    citizenshipAttribute.getAttributeValue().add(citizenshipValue);
    categoryAttributes.getAttribute().add(citizenshipAttribute);

    xacmlRequestType.getAttributes().add(actionAttributes);
    xacmlRequestType.getAttributes().add(subjectAttributes);
    xacmlRequestType.getAttributes().add(categoryAttributes);

    // Perform Test
    ResponseType xacmlResponse = pdp.evaluate(xacmlRequestType);

    // Verify - The policy was loaded to allow a permit decision
    JAXBContext jaxbContext = JAXBContext.newInstance(ResponseType.class);
    Marshaller marshaller = jaxbContext.createMarshaller();
    ObjectFactory objectFactory = new ObjectFactory();
    Writer writer = new StringWriter();
    marshaller.marshal(objectFactory.createResponse(xacmlResponse), writer);
    LOGGER.debug("\nXACML 3.0 Response:\n{}", writer.toString());
    assertEquals(xacmlResponse.getResult().get(0).getDecision(), DecisionType.PERMIT);

    FileUtils.deleteDirectory(policyDir);
  }

  @Test(expected = IllegalStateException.class)
  public void testIllegalStateException() throws Exception {
    LOGGER.debug("\n\n\n##### testExecption");

    File policyDir = folder.newFolder("tempDir");

    // Perform Test
    XacmlClient pdp =
        new XacmlClient(policyDir.getCanonicalPath(), null, mock(SecurityLogger.class));

    File srcFile =
        new File(
            projectHome + File.separator + RELATIVE_POLICIES_DIR + File.separator + POLICY_FILE);
    FileUtils.copyFileToDirectory(srcFile, policyDir);

    RequestType xacmlRequestType = new RequestType();
    xacmlRequestType.setCombinedDecision(false);
    xacmlRequestType.setReturnPolicyIdList(false);

    AttributesType actionAttributes = new AttributesType();
    actionAttributes.setCategory(ACTION_CATEGORY);
    AttributeType actionAttribute = new AttributeType();
    actionAttribute.setAttributeId(ACTION_ID);
    actionAttribute.setIncludeInResult(false);
    AttributeValueType actionValue = new AttributeValueType();
    actionValue.setDataType(STRING_DATA_TYPE);
    actionValue.getContent().add(QUERY_ACTION);
    actionAttribute.getAttributeValue().add(actionValue);
    actionAttributes.getAttribute().add(actionAttribute);

    AttributesType subjectAttributes = new AttributesType();
    subjectAttributes.setCategory(SUBJECT_CATEGORY);
    AttributeType subjectAttribute = new AttributeType();
    subjectAttribute.setAttributeId(SUBJECT_ID);
    subjectAttribute.setIncludeInResult(false);
    AttributeValueType subjectValue = new AttributeValueType();
    subjectValue.setDataType(STRING_DATA_TYPE);
    subjectValue.getContent().add(TEST_USER_1);
    subjectAttribute.getAttributeValue().add(subjectValue);
    subjectAttributes.getAttribute().add(subjectAttribute);

    AttributeType roleAttribute = new AttributeType();
    roleAttribute.setAttributeId(ROLE_CLAIM);
    roleAttribute.setIncludeInResult(false);
    AttributeValueType roleValue = new AttributeValueType();
    roleValue.setDataType(STRING_DATA_TYPE);
    roleValue.getContent().add(ROLE);
    roleAttribute.getAttributeValue().add(roleValue);
    subjectAttributes.getAttribute().add(roleAttribute);

    AttributesType categoryAttributes = new AttributesType();
    categoryAttributes.setCategory(PERMISSIONS_CATEGORY);
    AttributeType citizenshipAttribute = new AttributeType();
    citizenshipAttribute.setAttributeId(CITIZENSHIP_ATTRIBUTE);
    citizenshipAttribute.setIncludeInResult(false);
    AttributeValueType citizenshipValue = new AttributeValueType();
    citizenshipValue.setDataType(STRING_DATA_TYPE);
    citizenshipValue.getContent().add(US_COUNTRY);
    citizenshipAttribute.getAttributeValue().add(citizenshipValue);
    categoryAttributes.getAttribute().add(citizenshipAttribute);

    xacmlRequestType.getAttributes().add(actionAttributes);
    xacmlRequestType.getAttributes().add(subjectAttributes);
    xacmlRequestType.getAttributes().add(categoryAttributes);

    // Perform Test
    pdp.evaluate(xacmlRequestType);
  }

  @After
  public void cleanup() throws IOException {
    if (tempDir != null && tempDir.exists()) {
      LOGGER.debug("Deleting directory: {}", tempDir);
      FileUtils.deleteDirectory(tempDir);
    }
  }

  private void testSetup() throws IOException {
    // Setup
    tempDir = folder.newFolder(TEMP_DIR_NAME);
    LOGGER.debug("Making directory: {}", tempDir.getPath());
    boolean dirExists = true;
    if (!tempDir.exists()) {
      dirExists = tempDir.mkdir();
    }
    if (dirExists) {
      File srcFile =
          new File(
              projectHome + File.separator + RELATIVE_POLICIES_DIR + File.separator + POLICY_FILE);
      FileUtils.copyFileToDirectory(srcFile, tempDir);
    }
  }

  private boolean isDirEmpty(File dir) {
    return ((null != dir)
        && (dir.isDirectory())
        && (null != dir.listFiles(getXmlFilenameFilter()))
        && (dir.listFiles(getXmlFilenameFilter()).length == 0));
  }

  private FilenameFilter getXmlFilenameFilter() {
    FilenameFilter filter =
        new FilenameFilter() {
          @Override
          public boolean accept(File dir, String name) {
            return name.toLowerCase().endsWith(".xml");
          }
        };

    return filter;
  }
}
