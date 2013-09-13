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
package ddf.catalog.pubsub;

public class TestDataLibrary {

    public static String getCatAndDogEntry() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n"
                + "<Resource xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "  <identifier qualifier=\"http://metadata.abc.com/mdr/ns/MDR/0.1/MDR.owl#URI\" value=\"http://www.abc.com/news/May2004/n05172004_200405174.html\"/>\r\n"
                + "  <title classification=\"U\" ownerProducer=\"AUS GBR USA\">Serengeti Event dog</title>\r\n"
                + "  <creator classification=\"U\" ownerProducer=\"AUS GBR USA\">\r\n"
                + "    <Person>\r\n"
                + "      <name>Donna Miles</name>\r\n"
                + "      <surname>Cat</surname>\r\n"
                + "      <affiliation>American Forces Press Service</affiliation>\r\n"
                + "    </Person>\r\n"
                + "  </creator>\r\n"
                + "  <subjectCoverage>\r\n"
                + "    <Subject>\r\n"
                + "      <keyword value=\"exercise\"/>\r\n"
                + "      <category qualifier=\"SubjectCoverageQualifier\" code=\"nitf\" label=\"nitf\"/>\r\n"
                + "    </Subject>\r\n" + "  </subjectCoverage>\r\n"
                + "  <security classification=\"U\" ownerProducer=\"USA\"/>\r\n" + "</Resource>";
    }

    public static String getDogEntry() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\r\n"
                + "<Resource xmlns:gml=\"http://www.opengis.net/gml\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\r\n"
                + "  <identifier qualifier=\"http://metadata.abc.com/mdr/ns/MDR/0.1/MDR.owl#URI\" value=\"http://www.abc.com/news/May2004/n05172004_200405174.html\"/>\r\n"
                + "  <title classification=\"U\" ownerProducer=\"AUS GBR USA\">Dog</title>\r\n"
                + "  <creator classification=\"U\" ownerProducer=\"AUS GBR USA\">\r\n"
                + "    <Person>\r\n"
                + "      <name>Donna Miles</name>\r\n"
                + "      <surname>Miles</surname>\r\n"
                + "      <affiliation>American Forces Press Service</affiliation>\r\n"
                + "    </Person>\r\n"
                + "  </creator>\r\n"
                + "  <subjectCoverage>\r\n"
                + "    <Subject>\r\n"
                + "      <keyword value=\"exercise\"/>\r\n"
                + "      <category qualifier=\"SubjectCoverageQualifier\" code=\"nitf\" label=\"nitf\"/>\r\n"
                + "    </Subject>\r\n" + "  </subjectCoverage>\r\n"
                + "  <security classification=\"U\" ownerProducer=\"USA\"/>\r\n" + "</Resource>";
    }

}
