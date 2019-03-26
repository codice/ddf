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
package org.codice.ddf.spatial.ogc.csw.catalog.common.source;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.security.sts.client.configuration.STSClientConfiguration;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.AbstractRecordType;
import net.opengis.cat.csw.v_2_0_2.BriefRecordType;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.RecordType;
import net.opengis.cat.csw.v_2_0_2.SearchResultsType;
import net.opengis.cat.csw.v_2_0_2.SummaryRecordType;
import net.opengis.cat.csw.v_2_0_2.dc.elements.SimpleLiteral;
import net.opengis.filter.v_1_1_0.ComparisonOperatorType;
import net.opengis.filter.v_1_1_0.ComparisonOperatorsType;
import net.opengis.filter.v_1_1_0.FilterCapabilities;
import net.opengis.filter.v_1_1_0.LogicalOperators;
import net.opengis.filter.v_1_1_0.ScalarCapabilitiesType;
import net.opengis.ows.v_1_0_0.DomainType;
import net.opengis.ows.v_1_0_0.Operation;
import net.opengis.ows.v_1_0_0.OperationsMetadata;
import net.opengis.ows.v_1_0_0.ServiceIdentification;
import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.spatial.ogc.catalog.MetadataTransformer;
import org.codice.ddf.spatial.ogc.catalog.common.AvailabilityTask;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Before;
import org.junit.BeforeClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestCswSourceBase {
  private static final Logger LOGGER = LoggerFactory.getLogger(TestCswSourceBase.class);

  protected static final String ID = "MyCswSource";

  protected static final String URL = "http://www.example.com/csw";

  protected static final String USERNAME = "myUsername";

  protected static final String PASSWORD = "myPass";

  protected static final String CERT_ALIAS = "testCert";

  protected static final String KEYSTORE_PATH = "/path/to/keystore";

  protected static final String SSL_PROTOCOL = "TLSv1.2";

  protected static final Integer CONNECTION_TIMEOUT = 11;

  protected static final Integer RECEIVE_TIMEOUT = 12;

  protected static final String OUTPUT_SCHEMA = "outputSchema";

  protected static final String QUERY_TYPE_NAME = "queryTypeName";

  protected static final String QUERY_TYPE_NAMESPACE = "http://example.com/namespace";

  protected static final String IDENTIFIER_MAPPING = "idMapping";

  protected static final String COORDINATE_ORDER = "LAT_LON";

  protected static final String CREATED_DATE = "createdProperty";

  protected static final String EFFECTIVE_DATE = "effectiveProperty";

  protected static final String MODIFIED_DATE = "modifiedProperty";

  protected static final String CONTENT_TYPE = "contentTypeProperty";

  protected static String[] metacardMappings;

  protected static final Integer POLL_INTERVAL = 100;

  private static final List<String> JAXB_ELEMENT_CLASS_NAMES =
      ImmutableList.of(GetRecordsType.class.getName());

  private static final Map<String, String> JAXB_ELEMENT_CLASS_MAP =
      ImmutableMap.of(
          GetRecordsType.class.getName(),
          new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.GET_RECORDS).toString());

  protected final GeotoolsFilterBuilder builder = new GeotoolsFilterBuilder();

  protected Csw mockCsw = mock(Csw.class);

  protected BundleContext mockContext = mock(BundleContext.class);

  protected AvailabilityTask mockAvailabilityTask = mock(AvailabilityTask.class);

  protected List<MetacardType> mockRegistry = new ArrayList<>();

  protected String getRecordsControlXml202 =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<GetRecords resultType=\"results\" outputFormat=\"application/xml\""
          + "    outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\""
          + "    maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\""
          + "    xmlns=\"http://www.opengis.net/cat/csw/2.0.2\""
          + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\""
          + "    xmlns:ogc=\"http://www.opengis.net/ogc\">"
          + "    <Query typeNames=\"csw:Record\">"
          + "        <ElementSetName>full</ElementSetName>"
          + "        <Constraint version=\"1.1.0\">"
          + "            <ogc:Filter>"
          + "                <ogc:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\" matchCase=\"false\">"
          + "                    <ogc:PropertyName>"
          + CswConstants.ANY_TEXT
          + "                    </ogc:PropertyName>"
          + "                    <ogc:Literal>*th*e</ogc:Literal>"
          + "                </ogc:PropertyIsLike>"
          + "            </ogc:Filter>"
          + "        </Constraint>"
          + "    </Query>"
          + "</GetRecords>";

  protected String getRecordsControlXml202ContentTypeMappedToFormat =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
          + "<GetRecords resultType=\"results\" outputFormat=\"application/xml\""
          + "    outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\""
          + "    maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\""
          + "    xmlns=\"http://www.opengis.net/cat/csw/2.0.2\""
          + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\""
          + "    xmlns:ogc=\"http://www.opengis.net/ogc\">"
          + "    <Query typeNames=\"csw:Record\">"
          + "<ElementSetName>full</ElementSetName>"
          + "        <Constraint version=\"1.1.0\">"
          + "            <ogc:Filter>"
          + "                <ogc:PropertyIsEqualTo matchCase=\"true\">"
          + "                    <ogc:PropertyName>"
          + CswConstants.CSW_FORMAT
          + "                    </ogc:PropertyName>"
          + "                    <ogc:Literal>myContentType</ogc:Literal>"
          + "                </ogc:PropertyIsEqualTo>"
          + "            </ogc:Filter>"
          + "        </Constraint>"
          + "    </Query>"
          + "</GetRecords>";

  protected String getRecordsControlXml202ConteTypeAndVersion =
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
          + "<csw:GetRecords xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
          + "    xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:gml=\"http://www.opengis.net/gml\"\n"
          + "    xmlns:ns4=\"http://www.w3.org/1999/xlink\" xmlns:ns5=\"http://www.w3.org/2001/SMIL20/\"\n"
          + "    xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:ows=\"http://www.opengis.net/ows\"\n"
          + "    xmlns:dct=\"http://purl.org/dc/terms/\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\"\n"
          + "    resultType=\"results\" outputFormat=\"application/xml\"\n"
          + "    outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\" maxRecords=\"10\"\n"
          + "    service=\"CSW\" version=\"2.0.2\">\n"
          + "    <csw:Query typeNames=\"csw:Record\">\n"
          + "        <csw:ElementSetName>full</csw:ElementSetName>\n"
          + "        <csw:Constraint version=\"1.1.0\">\n"
          + "            <ogc:Filter>\n"
          + "                <ogc:And>\n"
          + "                    <ogc:PropertyIsEqualTo matchCase=\"true\">\n"
          + "                        <ogc:PropertyName>"
          + CswConstants.CSW_FORMAT
          + "</ogc:PropertyName>\n"
          + "                        <ogc:Literal>myContentType</ogc:Literal>\n"
          + "                    </ogc:PropertyIsEqualTo>\n"
          + "                    <ogc:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\" matchCase=\"false\">\n"
          + "                        <ogc:PropertyName>metadata-content-type-version</ogc:PropertyName>\n"
          + "                        <ogc:Literal>2.0</ogc:Literal>\n"
          + "                    </ogc:PropertyIsLike>\n"
          + "                </ogc:And>\n"
          + "            </ogc:Filter>\n"
          + "        </csw:Constraint>\n"
          + "    </csw:Query>\n"
          + "</csw:GetRecords>";

  @BeforeClass
  public static void init() {
    // The magic setting - besides ignoring whitespace, this setting configures XMLUnit to
    // treat namespaces that are equivalent but that use different prefixes to be considered
    // a recoverable difference, hence XMLUnit's diff will mark the docs as similar if this is
    // their only difference.
    XMLUnit.setIgnoreWhitespace(true);
    XMLUnit.setIgnoreAttributeOrder(true);
    XMLUnit.setIgnoreComments(true);

    metacardMappings =
        new String[] {
          Core.ID + "=" + IDENTIFIER_MAPPING,
          Core.CREATED + "=" + CREATED_DATE,
          Metacard.EFFECTIVE + "=" + EFFECTIVE_DATE,
          Core.MODIFIED + "=" + MODIFIED_DATE,
          Metacard.CONTENT_TYPE + "=" + CONTENT_TYPE
        };
  }

  @Before
  public void setUp() {
    ServiceReference ref = mock(ServiceReference.class);
    ServiceReference[] serviceRefs = new ServiceReference[] {ref};
    try {
      when(mockContext.getServiceReferences(eq(MetadataTransformer.class.getName()), anyString()))
          .thenReturn(serviceRefs);
      when(mockContext.getServiceReferences(
              eq(STSClientConfiguration.class.getName()), anyString()))
          .thenReturn(null);
    } catch (InvalidSyntaxException e) {
      LOGGER.error(e.getMessage(), e);
    }

    MetadataTransformer transformer = mock(MetadataTransformer.class);

    // Just return same Metacard that was passed in
    when(mockContext.getService(any(ServiceReference.class))).thenReturn(transformer);
    try {
      when(transformer.transform(any(Metacard.class)))
          .thenAnswer(invocation -> invocation.getArguments()[0]);
    } catch (CatalogTransformerException e) {
      LOGGER.error(e.getMessage(), e);
    }

    when(mockAvailabilityTask.isAvailable()).thenReturn(true);
  }

  protected Set<ContentType> generateContentType(List<String> names) {
    Set<ContentType> contentTypes = new HashSet<>();

    for (String name : names) {
      ContentType ct = new ContentTypeImpl(name, "");
      contentTypes.add(ct);
    }
    return contentTypes;
  }

  protected Csw createMockCsw() throws CswException {
    Csw mockCsw = mock(Csw.class);
    InputStream stream = getClass().getResourceAsStream("/getCapabilities.xml");
    CapabilitiesType capabilities = parseXml(stream);
    when(mockCsw.getCapabilities(any(GetCapabilitiesRequest.class))).thenReturn(capabilities);

    CswRecordCollection collection = generateCswCollection("/getBriefRecordsResponse.xml");
    when(mockCsw.getRecords(any(GetRecordsType.class))).thenReturn(collection);
    return mockCsw;
  }

  protected CswRecordCollection generateCswCollection(String file) {
    InputStream stream = getClass().getResourceAsStream(file);
    GetRecordsResponseType recordsResponse = parseXml(stream);

    GetRecordsResponseType records = new GetRecordsResponseType();
    recordsResponse.copyTo(records);
    List<Metacard> cswRecords = new LinkedList<>();
    for (JAXBElement<? extends AbstractRecordType> rec :
        records.getSearchResults().getAbstractRecord()) {
      MetacardImpl metacard = new MetacardImpl();
      cswRecords.add(metacard);
      if (rec.getValue() instanceof BriefRecordType) {
        BriefRecordType record = (BriefRecordType) rec.getValue();
        metacard.setId(record.getIdentifier().get(0).getValue().getContent().get(0));
        if (!CollectionUtils.isEmpty(record.getType().getContent())) {
          metacard.setContentTypeName(record.getType().getContent().get(0));
        }
      } else if (rec.getValue() instanceof SummaryRecordType) {
        SummaryRecordType record = (SummaryRecordType) rec.getValue();
        metacard.setId(record.getIdentifier().get(0).getValue().getContent().get(0));
        if (!CollectionUtils.isEmpty(record.getType().getContent())) {

          metacard.setContentTypeName(record.getType().getContent().get(0));
        }
      } else if (rec.getValue() instanceof RecordType) {
        RecordType record = (RecordType) rec.getValue();
        for (JAXBElement<SimpleLiteral> jb : record.getDCElement()) {
          if ("identifier".equals(jb.getName().getLocalPart())) {
            metacard.setId(jb.getValue().getContent().get(0));
          }
          if ("type".equals(jb.getName().getLocalPart())
              && !CollectionUtils.isEmpty(jb.getValue().getContent())) {
            metacard.setContentTypeName(jb.getValue().getContent().get(0));
          }
        }
      }
    }
    CswRecordCollection collection = new CswRecordCollection();
    collection.setCswRecords(cswRecords);
    collection.setNumberOfRecordsMatched(
        records.getSearchResults().getNumberOfRecordsMatched().intValue());
    collection.setNumberOfRecordsReturned(
        records.getSearchResults().getNumberOfRecordsReturned().intValue());
    return collection;
  }

  @SuppressWarnings("unchecked")
  protected <T> T parseXml(InputStream stream) {
    JAXBElement<T> jaxb;
    try {
      JAXBContext jc =
          JAXBContext.newInstance("net.opengis.cat.csw.v_2_0_2:net.opengis.gml.v_3_1_1");
      Unmarshaller u = jc.createUnmarshaller();

      Object o = u.unmarshal(stream);
      jaxb = (JAXBElement<T>) o;
    } catch (JAXBException e) {
      throw new AssertionError("failed to parse xml", e);
    }

    return jaxb.getValue();
  }

  protected String getGetRecordsTypeAsXml(GetRecordsType getRecordsType) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

    CswJAXBElementProvider<GetRecordsType> getRecordsTypeProvider = new CswJAXBElementProvider<>();
    getRecordsTypeProvider.setMarshallAsJaxbElement(true);
    getRecordsTypeProvider.setJaxbElementClassNames(JAXB_ELEMENT_CLASS_NAMES);
    getRecordsTypeProvider.setJaxbElementClassMap(JAXB_ELEMENT_CLASS_MAP);
    getRecordsTypeProvider.writeTo(
        getRecordsType,
        GenericType.class,
        GetRecordsType.class.getAnnotations(),
        MediaType.APPLICATION_XML_TYPE,
        new MultivaluedHashMap<>(0),
        outputStream);
    return outputStream.toString();
  }

  protected void configureMockCsw() throws CswException {
    configureMockCsw(0, 0L, CswConstants.VERSION_2_0_2);
  }

  protected void configureMockCsw(int numRecordsReturned, long numRecordsMatched, String cswVersion)
      throws CswException {

    ServiceIdentification mockServiceIdentification = mock(ServiceIdentification.class);
    when(mockServiceIdentification.getAbstract()).thenReturn("myDescription");
    CapabilitiesType mockCapabilities = mock(CapabilitiesType.class);

    when(mockCapabilities.getVersion()).thenReturn(cswVersion);
    when(mockCapabilities.getServiceIdentification()).thenReturn(mockServiceIdentification);
    when(mockCapabilities.getServiceIdentification()).thenReturn(mockServiceIdentification);
    when(mockCsw.getCapabilities(any(GetCapabilitiesRequest.class))).thenReturn(mockCapabilities);

    FilterCapabilities mockFilterCapabilities = mock(FilterCapabilities.class);
    when(mockCapabilities.getFilterCapabilities()).thenReturn(mockFilterCapabilities);

    List<ComparisonOperatorType> comparisonOpsList = new ArrayList<>();
    comparisonOpsList.add(ComparisonOperatorType.EQUAL_TO);
    comparisonOpsList.add(ComparisonOperatorType.LIKE);
    comparisonOpsList.add(ComparisonOperatorType.NOT_EQUAL_TO);
    comparisonOpsList.add(ComparisonOperatorType.GREATER_THAN);
    comparisonOpsList.add(ComparisonOperatorType.GREATER_THAN_EQUAL_TO);
    comparisonOpsList.add(ComparisonOperatorType.LESS_THAN);
    comparisonOpsList.add(ComparisonOperatorType.LESS_THAN_EQUAL_TO);
    comparisonOpsList.add(ComparisonOperatorType.BETWEEN);
    comparisonOpsList.add(ComparisonOperatorType.NULL_CHECK);

    ComparisonOperatorsType comparisonOps = new ComparisonOperatorsType();
    comparisonOps.setComparisonOperator(comparisonOpsList);

    ScalarCapabilitiesType mockScalarCapabilities = mock(ScalarCapabilitiesType.class);
    when(mockScalarCapabilities.getLogicalOperators()).thenReturn(mock(LogicalOperators.class));
    mockScalarCapabilities.setComparisonOperators(comparisonOps);
    when(mockScalarCapabilities.getComparisonOperators()).thenReturn(comparisonOps);
    when(mockFilterCapabilities.getScalarCapabilities()).thenReturn(mockScalarCapabilities);

    List<DomainType> getRecordsParameters = new ArrayList<>();
    DomainType typeName = new DomainType();
    typeName.setName(CswConstants.TYPE_NAME_PARAMETER);
    typeName.setValue(Collections.singletonList("csw:Record"));
    getRecordsParameters.add(typeName);
    DomainType typeNames = new DomainType();
    typeNames.setName(CswConstants.TYPE_NAMES_PARAMETER);
    getRecordsParameters.add(typeNames);
    DomainType getRecordsOutputSchema = new DomainType();
    getRecordsOutputSchema.setName(CswConstants.OUTPUT_SCHEMA_PARAMETER);
    getRecordsOutputSchema.getValue().add(CswConstants.CSW_OUTPUT_SCHEMA);
    getRecordsParameters.add(getRecordsOutputSchema);
    DomainType constraintLang = new DomainType();
    constraintLang.setName(CswConstants.CONSTRAINT_LANGUAGE_PARAMETER);
    constraintLang.setValue(Collections.singletonList(CswConstants.CONSTRAINT_LANGUAGE_FILTER));
    getRecordsParameters.add(constraintLang);
    DomainType outputFormat = new DomainType();
    outputFormat.setName(CswConstants.OUTPUT_FORMAT_PARAMETER);
    getRecordsParameters.add(outputFormat);
    DomainType resultType = new DomainType();
    resultType.setName(CswConstants.RESULT_TYPE_PARAMETER);
    getRecordsParameters.add(resultType);
    DomainType elementSetName = new DomainType();
    elementSetName.setName(CswConstants.ELEMENT_SET_NAME_PARAMETER);
    getRecordsParameters.add(elementSetName);

    List<DomainType> getRecordByIdParameters = new ArrayList<>();
    DomainType getRecordByIdOutputSchema = new DomainType();
    getRecordByIdOutputSchema.setName(CswConstants.OUTPUT_SCHEMA_PARAMETER);
    List<String> outputSchemas = new ArrayList<>();
    outputSchemas.add(CswConstants.CSW_OUTPUT_SCHEMA);
    getRecordByIdOutputSchema.setValue(outputSchemas);
    getRecordByIdParameters.add(getRecordByIdOutputSchema);

    Operation getRecords = new Operation();
    getRecords.setName(CswConstants.GET_RECORDS);
    getRecords.setParameter(getRecordsParameters);
    Operation getRecordById = new Operation();
    getRecordById.setName(CswConstants.GET_RECORD_BY_ID);
    getRecordById.setParameter(getRecordByIdParameters);
    List<Operation> operations = new ArrayList<>(2);
    operations.add(getRecords);
    operations.add(getRecordById);

    OperationsMetadata mockOperationsMetadata = mock(OperationsMetadata.class);
    mockOperationsMetadata.setOperation(operations);
    when(mockCapabilities.getOperationsMetadata()).thenReturn(mockOperationsMetadata);
    when(mockOperationsMetadata.getOperation()).thenReturn(operations);

    if (numRecordsReturned > 0) {
      List<Metacard> metacards = new ArrayList<>(numRecordsReturned);
      for (int i = 1; i <= numRecordsReturned; i++) {
        String id = "ID_" + String.valueOf(i);
        MetacardImpl metacard = new MetacardImpl();
        metacard.setId(id);
        metacard.setContentTypeName("myContentType");
        metacards.add(metacard);
      }

      SearchResultsType searchResults = mock(SearchResultsType.class);
      when(searchResults.getNumberOfRecordsMatched())
          .thenReturn(BigInteger.valueOf(numRecordsMatched));
      when(searchResults.getNumberOfRecordsReturned())
          .thenReturn(BigInteger.valueOf(numRecordsReturned));

      CswRecordCollection mockCswRecordCollection = mock(CswRecordCollection.class);

      when(mockCswRecordCollection.getCswRecords()).thenReturn(metacards);

      when(mockCswRecordCollection.getNumberOfRecordsMatched()).thenReturn(numRecordsMatched);
      when(mockCswRecordCollection.getNumberOfRecordsReturned())
          .thenReturn((long) numRecordsReturned);
      when(mockCsw.getRecords(any(GetRecordsType.class))).thenReturn(mockCswRecordCollection);
    }
  }

  protected void setupMockContextForMetacardTypeRegistrationAndUnregistration(
      List<String> contentTypes) {
    ServiceRegistration<?> mockRegisteredMetacardType =
        (ServiceRegistration<?>) mock(ServiceRegistration.class);
    doReturn(mockRegisteredMetacardType)
        .when(mockContext)
        .registerService(eq(MetacardType.class.getName()), any(MetacardType.class), any());
    ServiceReference<?> mockServiceReference = (ServiceReference<?>) mock(ServiceReference.class);
    doReturn(mockServiceReference).when(mockRegisteredMetacardType).getReference();
    when(mockServiceReference.getProperty(eq(Metacard.CONTENT_TYPE))).thenReturn(contentTypes);
  }

  protected List<String> getDefaultContentTypes() {
    return new LinkedList<>(Collections.singletonList("myContentType"));
  }
}
