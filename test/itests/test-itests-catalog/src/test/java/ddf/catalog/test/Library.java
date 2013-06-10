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
        return "{\r\n"
                + "    \"properties\": {\r\n"
                + "        \"title\": \"myTitle\",\r\n"
                + "        \"thumbnail\": \"CA==\",\r\n"
                + "        \"created\": \"2012-09-01T00:09:19.368+0000\",\r\n"
                + "        \"metadata-content-type-version\": \"myVersion\",\r\n"
                + "        \"metadata-content-type\": \"myType\",\r\n"
                + "        \"metadata\": \"<xml>text</xml>\",\r\n"
                + "        \"modified\": \"2012-09-01T00:09:19.368+0000\"\r\n"
                + "    },\r\n" 
                + "    \"type\": \"Feature\",\r\n"
                + "    \"geometry\": {\r\n"
                + "        \"type\": \"Point\",\r\n"
                + "        \"coordinates\": [\r\n" 
                + "            30.0,\r\n"
                + "            10.0\r\n" 
                + "        ]\r\n" 
                + "    }\r\n" 
                + "} ";
    }
    
    public static String getSimpleXml() {
        return getSimpleXml("http://example.iana.org/");
    }
    
    public static String getSimpleXml(String filePath) {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" + 
    		"<metacard xmlns=\"urn:catalog:metacard\" xmlns:ns2=\"http://www.opengis.net/gml\" xmlns:ns3=\"http://www.w3.org/1999/xlink\" xmlns:ns4=\"http://www.w3.org/2001/SMIL20/\" xmlns:ns5=\"http://www.w3.org/2001/SMIL20/Language\">\n" + 
    		"    <type>ddf.metacard</type>\n" + 
    		"    <source>ddf.distribution</source>\n" + 
    		"    <string name=\"metadata-content-type-version\">\n" + 
    		"        <value>myVersion</value>\n" + 
    		"    </string>\n" + 
    		"    <string name=\"title\">\n" + 
    		"        <value>myXmlTitle</value>\n" + 
    		"    </string>\n" + 
    		"    <geometry name=\"location\">\n" + 
    		"        <value>\n" + 
    		"            <ns2:Point>\n" + 
    		"                <ns2:pos>30.0 10.0</ns2:pos>\n" + 
    		"            </ns2:Point>\n" + 
    		"        </value>\n" + 
    		"    </geometry>\n" + 
    		"    <dateTime name=\"created\">\n" + 
    		"        <value>2013-04-18T10:50:27.371-07:00</value>\n" + 
    		"    </dateTime>\n" + 
    		"    <dateTime name=\"modified\">\n" + 
    		"        <value>2013-04-18T10:50:27.371-07:00</value>\n" + 
    		"    </dateTime>\n" + 
    		"    <base64Binary name=\"thumbnail\">\n" + 
    		"        <value>CA==</value>\n" + 
    		"    </base64Binary>\n" + 
    		"    <stringxml name=\"metadata\">\n" + 
    		"        <value>\n" + 
    		"            <xml xmlns:ns6=\"urn:catalog:metacard\" xmlns=\"\">text</xml>\n" + 
    		"        </value>\n" + 
    		"    </stringxml>\n" + 
    		"    <string name=\"metadata-content-type\">\n" + 
    		"        <value>myType</value>\n" + 
    		"    </string>\n" + 
    		"    <string name=\"resource-uri\">\n" + 
    		"        <value>" + filePath +"</value>\n" + 
    		"    </string>\n" + 
    		"</metacard>\n";
    }

}
