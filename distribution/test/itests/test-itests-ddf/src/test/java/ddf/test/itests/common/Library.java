/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.test.itests.common;

import java.io.IOException;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrSubstitutor;
import org.apache.tika.io.IOUtils;

import com.google.common.collect.ImmutableMap;

/**
 * Helper class containing test data.
 */
public final class Library {

    public static final String VARIABLE_DELIMETER = "$";

    private Library() {
        // static util class
    }

    public static String getSimpleGeoJson() {
        return "{\r\n" + "    \"properties\": {\r\n" + "        \"title\": \"myTitle\",\r\n"
                + "        \"thumbnail\": \"/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAALAAgDASIAAhEBAxEB/8QAFgABAQEAAAAAAAAAAAAAAAAAAAUH/8QAJBAAAAQFAwUAAAAAAAAAAAAAABETFAEDBBIVAgUGISJRYZP/xAAVAQEBAAAAAAAAAAAAAAAAAAADBf/EABkRAAMAAwAAAAAAAAAAAAAAAAABAwISIv/aAAwDAQACEQMRAD8A3fRvVTHmTS+RjkUfbolC+fUBDhR0zXMN5WUyibqyClrhMj8WdoAtynSGLfJ//9k=\",\r\n"
                + "        \"created\": \"2012-09-01T00:09:19.368+0000\",\r\n"
                + "        \"metadata-content-type-version\": \"myVersion\",\r\n"
                + "        \"metadata-content-type\": \"myType\",\r\n"
                + "        \"metadata\": \"<xml>text</xml>\",\r\n"
                + "        \"modified\": \"2012-09-01T00:09:19.368+0000\"\r\n" + "    },\r\n"
                + "    \"type\": \"Feature\",\r\n" + "    \"geometry\": {\r\n"
                + "        \"type\": \"Point\",\r\n" + "        \"coordinates\": [\r\n"
                + "            30.0,\r\n" + "            10.0\r\n" + "        ]\r\n" + "    }\r\n"
                + "} ";
    }

    public static String getSimpleXml() {
        return getSimpleXml("http://example.iana.org/");
    }

    public static String getSimpleXmlNoDec(String uri) {
        return "<metacard xmlns=\"urn:catalog:metacard\" xmlns:ns2=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns4=\"http://www.w3.org/2001/SMIL20/\" xmlns:ns5=\"http://www.w3.org/2001/SMIL20/Language\">\n"
                + "    <type>ddf.metacard</type>\n" + "    <source>ddf.distribution</source>\n"
                + "    <string name=\"metadata-content-type-version\">\n"
                + "        <value>myVersion</value>\n" + "    </string>\n"
                + "    <string name=\"title\">\n" + "        <value>myXmlTitle</value>\n"
                + "    </string>\n" + "    <geometry name=\"location\">\n" + "        <value>\n"
                + "            <ns2:Point>\n" + "                <ns2:pos>30.0 10.0</ns2:pos>\n"
                + "            </ns2:Point>\n" + "        </value>\n" + "    </geometry>\n"
                + "    <dateTime name=\"created\">\n"
                + "        <value>2013-04-18T10:50:27.371-07:00</value>\n" + "    </dateTime>\n"
                + "    <dateTime name=\"modified\">\n"
                + "        <value>2013-04-18T10:50:27.371-07:00</value>\n" + "    </dateTime>\n"
                + "    <base64Binary name=\"thumbnail\">\n"
                + "        <value>/9j/4AAQSkZJRgABAQAAAQABAAD/2wBDAAYEBQYFBAYGBQYHBwYIChAKCgkJChQODwwQFxQYGBcUFhYaHSUfGhsjHBYWICwgIyYnKSopGR8tMC0oMCUoKSj/2wBDAQcHBwoIChMKChMoGhYaKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCgoKCj/wAARCAALAAgDASIAAhEBAxEB/8QAFgABAQEAAAAAAAAAAAAAAAAAAAUH/8QAJBAAAAQFAwUAAAAAAAAAAAAAABETFAEDBBIVAgUGISJRYZP/xAAVAQEBAAAAAAAAAAAAAAAAAAADBf/EABkRAAMAAwAAAAAAAAAAAAAAAAABAwISIv/aAAwDAQACEQMRAD8A3fRvVTHmTS+RjkUfbolC+fUBDhR0zXMN5WUyibqyClrhMj8WdoAtynSGLfJ//9k=</value>\n"
                + "    </base64Binary>\n" + "    <stringxml name=\"metadata\">\n"
                + "        <value>\n"
                + "            <xml xmlns:ns6=\"urn:catalog:metacard\" xmlns=\"\">text</xml>\n"
                + "        </value>\n" + "    </stringxml>\n"
                + "    <string name=\"metadata-content-type\">\n"
                + "        <value>myType</value>\n" + "    </string>\n"
                + "    <string name=\"resource-uri\">\n" + "        <value>" + uri + "</value>\n"
                + "    </string>\n" + "</metacard>\n";
    }

    public static String getSimpleXml(String uri) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + getSimpleXmlNoDec(uri);
    }

    public static String getCswQuery(String propertyName, String literalValue, String ouputFormat,
            String outputSchema) {

        String schema = "";
        if (StringUtils.isNotBlank(outputSchema)) {
            schema = "outputSchema=\"" + outputSchema + "\" ";
        }

        return "<csw:GetRecords resultType=\"results\" outputFormat=\"" + ouputFormat + "\" "
                + schema
                + "startPosition=\"1\" maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\" xmlns:ns2=\"http://www.opengis.net/ogc\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.w3.org/1999/xlink\" xmlns:ns3=\"http://www.opengis.net/gml\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ows\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
                + "    <ns10:Query typeNames=\"csw:Record\" xmlns=\"\" xmlns:ns10=\"http://www.opengis.net/cat/csw/2.0.2\">"
                + "        <ns10:ElementSetName>full</ns10:ElementSetName>"
                + "        <ns10:Constraint version=\"1.1.0\">" + "            <ns2:Filter>"
                + "                <ns2:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
                + "                    <ns2:PropertyName>" + propertyName + "</ns2:PropertyName>"
                + "                    <ns2:Literal>" + literalValue + "</ns2:Literal>"
                + "                </ns2:PropertyIsLike>" + "            </ns2:Filter>"
                + "        </ns10:Constraint>" + "    </ns10:Query>" + "</csw:GetRecords>";
    }

    public static String getCswSubscription(String propertyName, String literalValue,
            String ouputFormat, String outputSchema, String responseHandler) {

        String schema = "";
        if (StringUtils.isNotBlank(outputSchema)) {
            schema = "outputSchema=\"" + outputSchema + "\" ";
        }

        return "<csw:GetRecords resultType=\"results\" outputFormat=\"" + ouputFormat + "\" "
                + schema
                + "startPosition=\"1\" maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\" xmlns:ns2=\"http://www.opengis.net/ogc\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.w3.org/1999/xlink\" xmlns:ns3=\"http://www.opengis.net/gml\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ows\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
                + "    <csw:ResponseHandler>" + responseHandler + "</csw:ResponseHandler>"
                + "    <ns10:Query typeNames=\"csw:Record\" xmlns=\"\" xmlns:ns10=\"http://www.opengis.net/cat/csw/2.0.2\">"
                + "        <ns10:ElementSetName>full</ns10:ElementSetName>"
                + "        <ns10:Constraint version=\"1.1.0\">" + "            <ns2:Filter>"
                + "                <ns2:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
                + "                    <ns2:PropertyName>" + propertyName + "</ns2:PropertyName>"
                + "                    <ns2:Literal>" + literalValue + "</ns2:Literal>"
                + "                </ns2:PropertyIsLike>" + "            </ns2:Filter>"
                + "        </ns10:Constraint>" + "    </ns10:Query>" + "</csw:GetRecords>";
    }

    public static String getCswQuery(String propertyName, String literalValue) {
        return getCswQuery(propertyName,
                literalValue,
                "application/xml",
                "http://www.opengis.net/cat/csw/2.0.2");
    }

    public static String getCswSubscription(String propertyName, String literalValue,
            String responseHandler) {
        return getCswSubscription(propertyName,
                literalValue,
                "application/xml",
                "http://www.opengis.net/cat/csw/2.0.2",
                responseHandler);
    }

    public static String getCswQueryMetacardXml(String propertyName, String literalValue) {
        return getCswQuery(propertyName, literalValue, "application/xml", "urn:catalog:metacard");
    }

    public static String getCswQueryJson(String propertyName, String literalValue) {
        return getCswQuery(propertyName, literalValue, "application/json", null);
    }

    public static String getCswIngest() {
        return getCswInsert("csw:Record",
                getCswRecord(UUID.randomUUID()
                        .toString()
                        .replaceAll("-", "")));
    }

    public static String getCswRecord(String id) {
        return "        <csw:Record\n" + "            xmlns:ows=\"http://www.opengis.net/ows\"\n"
                + "            xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "            xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "            xmlns:dct=\"http://purl.org/dc/terms/\"\n"
                + "            xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "            <dc:identifier>" + id + "</dc:identifier>\n"
                + "            <dc:title>Aliquam fermentum purus quis arcu</dc:title>\n"
                + "            <dc:type>http://purl.org/dc/dcmitype/Text</dc:type>\n"
                + "            <dc:subject>Hydrography--Dictionaries</dc:subject>\n"
                + "            <dc:format>application/pdf</dc:format>\n"
                + "            <dc:date>2006-05-12</dc:date>\n"
                + "            <dct:abstract>Vestibulum quis ipsum sit amet metus imperdiet vehicula. Nulla scelerisque cursus mi.</dct:abstract>\n"
                + "            <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
                + "                <ows:LowerCorner>44.792 -6.171</ows:LowerCorner>\n"
                + "                <ows:UpperCorner>51.126 -2.228</ows:UpperCorner>\n"
                + "            </ows:BoundingBox>\n" + "        </csw:Record>\n";
    }

    public static String getCswInsert(String typeName, String record) {
        return "<csw:Transaction\n" + "    service=\"CSW\"\n" + "    version=\"2.0.2\"\n"
                + "    verboseResponse=\"true\"\n"
                + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\">\n"
                + "    <csw:Insert typeName=\"" + typeName + "\">\n" + record
                + "    </csw:Insert>\n" + "</csw:Transaction>";
    }

    public static String getRegistryNode() throws IOException {
        return IOUtils.toString(Library.class.getResourceAsStream("/csw-rim-node.xml"));
    }

    public static String getCswRegistryInsert() throws IOException {
        return Library.getCswInsert("rim:RegistryPackage", getRegistryNode());
    }

    public static String getCswRegistryUpdate() throws IOException {
        return "<csw:Transaction\n" + "    service=\"CSW\"\n" + "    version=\"2.0.2\"\n"
                + "    verboseResponse=\"true\"\n"
                + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\">\n"
                + "    <csw:Update typeName=\"rim:RegistryPackage\">\n" + getRegistryNode() + "\n"
                + "    </csw:Update>\n" + "</csw:Transaction>";
    }

    public static String getCswRegistryDelete() throws IOException {
        return "<csw:Transaction service=\"CSW\"\n"
                + "   version=\"2.0.2\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "   xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                + "  <csw:Delete typeName=\"rim:RegistryPackage\" handle=\"something\">\n"
                + "    <csw:Constraint version=\"2.0.0\">\n" + "      <ogc:Filter>\n"
                + "        <ogc:PropertyIsEqualTo>\n"
                + "            <ogc:PropertyName>registry-id</ogc:PropertyName>\n"
                + "            <ogc:Literal>urn:uuid:2014ca7f59ac46f495e32b4a67a51276</ogc:Literal>\n"
                + "        </ogc:PropertyIsEqualTo>\n" + "      </ogc:Filter>\n"
                + "    </csw:Constraint>\n" + "  </csw:Delete>\n" + "</csw:Transaction>";
    }

    public static String getCswFilterDelete() {
        return "<csw:Transaction service=\"CSW\"\n"
                + "   version=\"2.0.2\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "   xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                + "  <csw:Delete typeName=\"csw:Record\" handle=\"something\">\n"
                + "    <csw:Constraint version=\"2.0.0\">\n" + "      <ogc:Filter>\n"
                + "        <ogc:PropertyIsEqualTo>\n"
                + "            <ogc:PropertyName>title</ogc:PropertyName>\n"
                + "            <ogc:Literal>Aliquam fermentum purus quis arcu</ogc:Literal>\n"
                + "        </ogc:PropertyIsEqualTo>\n" + "      </ogc:Filter>\n"
                + "    </csw:Constraint>\n" + "  </csw:Delete>\n" + "</csw:Transaction>";
    }

    public static String getCswCqlDelete() {
        return "<csw:Transaction service=\"CSW\"\n"
                + "   version=\"2.0.2\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "   xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                + "  <csw:Delete typeName=\"csw:Record\" handle=\"something\">\n"
                + "    <csw:Constraint version=\"2.0.0\">\n" + "      <ogc:CqlText>\n"
                + "        title = 'Aliquam fermentum purus quis arcu'\n" + "      </ogc:CqlText>\n"
                + "    </csw:Constraint>\n" + "  </csw:Delete>\n" + "</csw:Transaction>";
    }

    public static String getCswCqlDeleteNone() {
        return "<csw:Transaction service=\"CSW\"\n"
                + "   version=\"2.0.2\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "   xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                + "  <csw:Delete typeName=\"csw:Record\" handle=\"something\">\n"
                + "    <csw:Constraint version=\"2.0.0\">\n" + "      <ogc:CqlText>\n"
                + "        title = 'fake title'\n" + "      </ogc:CqlText>\n"
                + "    </csw:Constraint>\n" + "  </csw:Delete>\n" + "</csw:Transaction>";
    }

    public static String getCombinedCswInsertAndDelete() {
        return "<csw:Transaction\n" + "    service=\"CSW\"\n" + "    version=\"2.0.2\"\n"
                + "    verboseResponse=\"true\"\n"
                + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "    xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                + "    <csw:Delete typeName=\"csw:Record\" handle=\"something\">\n"
                + "      <csw:Constraint version=\"2.0.0\">\n" + "        <ogc:CqlText>\n"
                + "          title = 'Aliquam fermentum purus quis arcu'\n"
                + "        </ogc:CqlText>\n" + "      </csw:Constraint>\n" + "    </csw:Delete>\n"
                + "    <csw:Insert typeName=\"csw:Record\">\n" + "        <csw:Record\n"
                + "            xmlns:ows=\"http://www.opengis.net/ows\"\n"
                + "            xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "            xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "            xmlns:dct=\"http://purl.org/dc/terms/\"\n"
                + "            xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "            <dc:identifier>123</dc:identifier>\n"
                + "            <dc:title>A Different Title</dc:title>\n"
                + "            <dc:type>http://purl.org/dc/dcmitype/Text</dc:type>\n"
                + "            <dc:subject>Hydrography--Dictionaries</dc:subject>\n"
                + "            <dc:format>application/pdf</dc:format>\n"
                + "            <dc:date>2006-05-12</dc:date>\n"
                + "            <dct:abstract>Vestibulum quis ipsum sit amet metus imperdiet vehicula. Nulla scelerisque cursus mi.</dct:abstract>\n"
                + "            <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
                + "                <ows:LowerCorner>44.792 -6.171</ows:LowerCorner>\n"
                + "                <ows:UpperCorner>51.126 -2.228</ows:UpperCorner>\n"
                + "            </ows:BoundingBox>\n" + "        </csw:Record>\n"
                + "    </csw:Insert>\n" + "  </csw:Transaction>";
    }

    public static String getCswUpdateByNewRecord() {
        return "<csw:Transaction\n" + "    service=\"CSW\"\n" + "    version=\"2.0.2\"\n"
                + "    verboseResponse=\"true\"\n"
                + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\">\n" + "    <csw:Update>\n"
                + "        <csw:Record\n" + "            xmlns:ows=\"http://www.opengis.net/ows\"\n"
                + "            xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "            xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                + "            xmlns:dct=\"http://purl.org/dc/terms/\"\n"
                + "            xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
                + "            <dc:identifier>identifier placeholder</dc:identifier>\n"
                + "            <dc:title>Updated Title</dc:title>\n"
                + "            <dc:type>http://purl.org/dc/dcmitype/Text</dc:type>\n"
                + "            <dc:subject>Updated Subject</dc:subject>\n"
                + "            <dc:format>application/pdf</dc:format>\n"
                + "            <dc:date>2015-08-10</dc:date>\n"
                + "            <dct:abstract>Vestibulum quis ipsum sit amet metus imperdiet vehicula. Nulla scelerisque cursus mi.</dct:abstract>\n"
                + "            <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
                + "                <ows:LowerCorner>1.0 2.0</ows:LowerCorner>\n"
                + "                <ows:UpperCorner>3.0 4.0</ows:UpperCorner>\n"
                + "            </ows:BoundingBox>\n" + "        </csw:Record>\n"
                + "    </csw:Update>\n" + "</csw:Transaction>";
    }

    public static String getCswUpdateByFilterConstraint() {
        return "<csw:Transaction\n" + "    service=\"CSW\"\n" + "    version=\"2.0.2\"\n"
                + "    verboseResponse=\"true\"\n"
                + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "    xmlns:ogc=\"http://www.opengis.net/ogc\">\n" + "    <csw:Update>\n"
                + "      <csw:RecordProperty>\n" + "        <csw:Name>title</csw:Name>\n"
                + "        <csw:Value>Updated Title</csw:Value>\n" + "      </csw:RecordProperty>\n"
                + "      <csw:RecordProperty>\n" + "        <csw:Name>date</csw:Name>\n"
                + "        <csw:Value>2015-08-25</csw:Value>\n" + "      </csw:RecordProperty>\n"
                + "      <csw:RecordProperty>\n" + "        <csw:Name>format</csw:Name>\n"
                + "        <csw:Value></csw:Value>\n" + "      </csw:RecordProperty>\n"
                + "      <csw:Constraint version=\"2.0.0\">\n" + "        <ogc:Filter>\n"
                + "          <ogc:PropertyIsEqualTo>\n"
                + "            <ogc:PropertyName>title</ogc:PropertyName>\n"
                + "            <ogc:Literal>Aliquam fermentum purus quis arcu</ogc:Literal>\n"
                + "          </ogc:PropertyIsEqualTo>\n" + "        </ogc:Filter>\n"
                + "      </csw:Constraint>\n" + "    </csw:Update>\n" + "</csw:Transaction>";
    }

    public static String getCswUpdateRemoveAttributesByCqlConstraint() {
        return "<csw:Transaction\n" + "service=\"CSW\"\n" + "version=\"2.0.2\"\n"
                + "  verboseResponse=\"true\"\n"
                + "  xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "  xmlns:ogc=\"http://www.opengis.net/ogc\">\n" + "  <csw:Update>\n"
                + "    <csw:RecordProperty>\n" + "      <csw:Name>title</csw:Name>\n"
                + "    </csw:RecordProperty>\n" + "    <csw:RecordProperty>\n"
                + "      <csw:Name>date</csw:Name>\n" + "    </csw:RecordProperty>\n"
                + "    <csw:RecordProperty>\n" + "      <csw:Name>location</csw:Name>\n"
                + "    </csw:RecordProperty>\n" + "    <csw:Constraint version=\"2.0.0\">\n"
                + "      <ogc:CqlText>\n" + "        title = 'Aliquam fermentum purus quis arcu'\n"
                + "      </ogc:CqlText>\n" + "    </csw:Constraint>\n" + "  </csw:Update>\n"
                + "</csw:Transaction>";
    }

    public static String getGetRecordByIdUrl() {
        return "?service=CSW&version=2.0.2&request=GetRecordById&NAMESPACE=xmlns="
                + "http://www.opengis.net/cat/csw/2.0.2&ElementSetName=full&"
                + "outputFormat=application/xml&outputSchema=http://www.opengis.net/cat/csw/2.0.2&"
                + "id=placeholder_id";
    }

    public static String getGetRecordByIdProductRetrievalUrl() {
        return "?service=CSW&version=2.0.2&request=GetRecordById&NAMESPACE=xmlns="
                + "http://www.opengis.net/cat/csw/2.0.2&"
                + "outputFormat=application/octet-stream&outputSchema="
                + "http://www.iana.org/assignments/media-types/application/octet-stream&"
                + "id=placeholder_id";
    }

    public static String getGetRecordByIdXml() {
        return "<GetRecordById xmlns=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "xmlns:ogc=\"http://www.opengis.net/ogc\"\n"
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "service=\"CSW\" version=\"2.0.2\" outputFormat=\"application/xml\"\n"
                + "outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "xsi:schemaLocation=\"http://www.opengis.net/cat/csw/2.0.2../../../csw/2.0.2/CSW-discovery.xsd\">\n"
                + "  <ElementSetName>full</ElementSetName>\n" + "  <Id>placeholder_id_1</Id>\n"
                + "  <Id>placeholder_id_2</Id>\n" + "</GetRecordById>";
    }

    public static String getGetRecordByIdProductRetrievalXml() {
        return "<GetRecordById xmlns=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                + "xmlns:ogc=\"http://www.opengis.net/ogc\"\n"
                + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "service=\"CSW\" version=\"2.0.2\" outputFormat=\"application/octet-stream\"\n"
                + "outputSchema=\"http://www.iana.org/assignments/media-types/application/octet-stream\">\n"
                + "  <Id>placeholder_id_1</Id>\n" + "</GetRecordById>";
    }

    public static String getCswRecordResponse() throws IOException {
        return IOUtils.toString(Library.class.getResourceAsStream("/get-records-response.xml"));
    }

    public static String getFileContent(String filePath) {

        return getFileContent(filePath, ImmutableMap.<String, String>builder().build());
    }

    /**
     * Variables to be replaced in a resource file should be in the format: $variableName$
     * The variable to replace in the file should also also match the parameter names of the method calling getFileContent.
     *
     * @param filePath
     * @param params
     * @return
     */
    public static String getFileContent(String filePath, ImmutableMap<String, String> params) {

        StrSubstitutor strSubstitutor = new StrSubstitutor(params);

        strSubstitutor.setVariablePrefix(VARIABLE_DELIMETER);
        strSubstitutor.setVariableSuffix(VARIABLE_DELIMETER);
        String fileContent = null;

        try {
            fileContent = org.apache.commons.io.IOUtils.toString(Library.class.getResourceAsStream(
                    filePath), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Failed to read filepath: " + filePath);
        }

        return strSubstitutor.replace(fileContent);
    }
}
