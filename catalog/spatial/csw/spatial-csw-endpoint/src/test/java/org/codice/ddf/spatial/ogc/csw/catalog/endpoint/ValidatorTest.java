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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.transform.QueryFilterTransformer;
import ddf.catalog.transform.QueryFilterTransformerProvider;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.ElementSetNameType;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.DescribeRecordRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transformer.TransformerManager;
import org.junit.Before;
import org.junit.Test;

public class ValidatorTest {

  private static final String OCTET_STREAM_OUTPUT_SCHEMA =
      "http://www.iana.org/assignments/media-types/application/octet-stream";

  private static final String VALID_TYPE = "Record";

  private static final String VALID_PREFIX = "csw";

  private static final String VALID_PREFIX_LOCAL_TYPE = VALID_PREFIX + ":" + VALID_TYPE;

  private Validator validator;

  TransformerManager transformerManager;

  List<QName> qNameList;

  @Before
  public void setUp() throws Exception {
    QName[] qname = {new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.CSW_RECORD_LOCAL_NAME)};
    qNameList = Arrays.asList(qname);
    transformerManager = mock(TransformerManager.class);
    validator = new Validator();

    QueryFilterTransformerProvider transformerProvider = mock(QueryFilterTransformerProvider.class);
    QueryFilterTransformer transformer = mock(QueryFilterTransformer.class);
    when(transformerProvider.getTransformer(any(QName.class))).thenReturn(Optional.empty());
    when(transformerProvider.getTransformer(qname[0])).thenReturn(Optional.of(transformer));
    validator.setQueryFilterTransformerProvider(transformerProvider);
  }

  @Test
  public void testValidateFullyQualifiedTypes() throws Exception {
    QName[] qname = {new QName("not blank", "not blank")};
    validator.validateFullyQualifiedTypes(Arrays.asList(qname));
  }

  @Test
  public void testValidateTypes() throws Exception {
    validator.validateTypes(qNameList, "version");
  }

  @Test(expected = CswException.class)
  public void testValidateTypesInvalidType() throws Exception {
    QName[] qname = {new QName("schema", "name")};
    qNameList = Arrays.asList(qname);
    validator.validateTypes(qNameList, "version");
  }

  @Test
  public void testValidateElementNames() throws Exception {

    QueryType query = new QueryType();

    List<QName> elementNameList =
        Arrays.asList(new QName("brief"), new QName("summary"), new QName("full"));
    query.setElementName(elementNameList);
    validator.validateElementNames(query);
  }

  @Test
  public void testValidateOutputSchema() throws Exception {

    validator.validateOutputSchema(null, transformerManager);
    validator.validateOutputSchema(OCTET_STREAM_OUTPUT_SCHEMA, transformerManager);

    String schema = "schema";
    when(transformerManager.getTransformerBySchema(eq(schema))).thenReturn(new Object());
    validator.validateOutputSchema(schema, transformerManager);
  }

  @Test(expected = CswException.class)
  public void testValidateOutputSchemaInvalidOutputSchema() throws Exception {

    String schema = "schema";
    when(transformerManager.getTransformerBySchema(eq(schema))).thenReturn(null);
    validator.validateOutputSchema(schema, transformerManager);
  }

  @Test
  public void testValidateVersion() throws Exception {
    validator.validateVersion(CswConstants.VERSION_2_0_2);
  }

  @Test
  public void testValidateOutputFormat() throws Exception {
    String[] outputFormats = {"an ouputformat"};
    validator.validateOutputFormat(CswConstants.OUTPUT_FORMAT_XML, transformerManager);
    when(transformerManager.getAvailableMimeTypes()).thenReturn(Arrays.asList(outputFormats));
    validator.validateOutputFormat(outputFormats[0], transformerManager);
  }

  @Test
  public void testValidateSchemaLanguage() throws Exception {
    validator.validateSchemaLanguage(null);
    validator.validateSchemaLanguage(CswConstants.VALID_SCHEMA_LANGUAGES.get(0));
  }

  @Test
  public void testValidateTypeNameToNamespaceMappings() throws Exception {
    DescribeRecordRequest drr = createDefaultDescribeRecordRequest(VALID_PREFIX);
    drr.setTypeName(VALID_PREFIX_LOCAL_TYPE);
    drr.setOutputFormat(CswConstants.OUTPUT_FORMAT_XML);
    Map<String, String> namespacePrefixToUriMappings = drr.parseNamespaces(drr.getNamespace());

    validator.validateTypeNameToNamespaceMappings(
        drr.getTypeName(), drr.getNamespace(), namespacePrefixToUriMappings);
  }

  @Test(expected = CswException.class)
  public void testEmptyNamespace() throws CswException {
    List<QName> types = Collections.singletonList(new QName("localname"));
    validator.validateFullyQualifiedTypes(types);
  }

  @Test
  public void testNoTypes() throws CswException {
    validator.validateTypes(Collections.emptyList(), "version");
    validator.validateTypes(null, "version");
  }

  @Test(expected = CswException.class)
  public void testBadElementSetNameCombination() throws CswException {
    QueryType queryType = mock(QueryType.class);
    when(queryType.isSetElementName()).thenReturn(true);
    when(queryType.isSetElementSetName()).thenReturn(true);
    validator.validateElementNames(queryType);
  }

  @Test(expected = CswException.class)
  public void testInvalidElementName() throws CswException {
    QueryType queryType = mock(QueryType.class);
    when(queryType.isSetElementName()).thenReturn(true);
    when(queryType.getElementName())
        .thenReturn(Collections.singletonList(new QName("fake element name")));
    validator.validateElementNames(queryType);
  }

  @Test(expected = CswException.class)
  public void testInvalidElementSetName() throws CswException {
    QueryType queryType = mock(QueryType.class);
    when(queryType.isSetElementSetName()).thenReturn(true);
    when(queryType.getElementSetName()).thenReturn(new ElementSetNameType());
    validator.validateElementNames(queryType);
  }

  @Test(expected = CswException.class)
  public void testInvalidVersion() throws CswException {
    validator.validateVersion("invalid version");
  }

  @Test(expected = CswException.class)
  public void testInvalidOutputFormat() throws CswException {
    validator.validateOutputFormat("invalid format", transformerManager);
  }

  @Test(expected = CswException.class)
  public void testInvalidSchemaLanguage() throws CswException {
    validator.validateSchemaLanguage("invalid schema language");
  }

  /**
   * Creates default DescribeRecordRequest GET request, with no sections specified
   *
   * @return Vanilla DescribeRecordRequest object
   */
  private DescribeRecordRequest createDefaultDescribeRecordRequest(String prefix) {
    DescribeRecordRequest drr = new DescribeRecordRequest();
    drr.setService(CswConstants.CSW);
    drr.setVersion(CswConstants.VERSION_2_0_2);
    drr.setRequest(CswConstants.DESCRIBE_RECORD);
    drr.setNamespace("xmlns(" + prefix + "=" + CswConstants.CSW_OUTPUT_SCHEMA + ")");
    return drr;
  }
}
