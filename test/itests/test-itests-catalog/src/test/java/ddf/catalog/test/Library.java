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
package ddf.catalog.test;

/**
 * Helper class containing test data.
 * 
 * @author Phillip Klinefelter
 * @author ddf.isgs@lmco.com
 * 
 */
public final class Library {

    private Library() {
        // static util class
    }

    public static String getSimpleGeoJson() {
        return "{\r\n" + "    \"properties\": {\r\n" + "        \"title\": \"myTitle\",\r\n"
                + "        \"thumbnail\": \"CA==\",\r\n"
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

    public static String getSimpleXml(String uri) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"
                + "<metacard xmlns=\"urn:catalog:metacard\" xmlns:ns2=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns4=\"http://www.w3.org/2001/SMIL20/\" xmlns:ns5=\"http://www.w3.org/2001/SMIL20/Language\">\n"
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
                + "    <base64Binary name=\"thumbnail\">\n" + "        <value>CA==</value>\n"
                + "    </base64Binary>\n" + "    <stringxml name=\"metadata\">\n"
                + "        <value>\n"
                + "            <xml xmlns:ns6=\"urn:catalog:metacard\" xmlns=\"\">text</xml>\n"
                + "        </value>\n" + "    </stringxml>\n"
                + "    <string name=\"metadata-content-type\">\n"
                + "        <value>myType</value>\n" + "    </string>\n"
                + "    <string name=\"resource-uri\">\n" + "        <value>" + uri + "</value>\n"
                + "    </string>\n" + "</metacard>\n";
    }

    public static String getCswQuery(String propertyName, String literalValue) {
        return "<csw:GetRecords resultType=\"results\" outputFormat=\"application/xml\" outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\" maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\" xmlns:ns2=\"http://www.opengis.net/ogc\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.w3.org/1999/xlink\" xmlns:ns3=\"http://www.opengis.net/gml\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ows\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
                + "    <ns10:Query typeNames=\"csw:Record\" xmlns=\"\" xmlns:ns10=\"http://www.opengis.net/cat/csw/2.0.2\">"
                + "        <ns10:ElementSetName>full</ns10:ElementSetName>"
                + "        <ns10:Constraint version=\"1.1.0\">"
                + "            <ns2:Filter>"
                + "                <ns2:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
                + "                    <ns2:PropertyName>" + propertyName + "</ns2:PropertyName>"
                + "                    <ns2:Literal>" + literalValue + "</ns2:Literal>"
                + "                </ns2:PropertyIsLike>"
                + "            </ns2:Filter>"
                + "        </ns10:Constraint>"
                + "    </ns10:Query>"
                + "</csw:GetRecords>";
    }

}
