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
package org.codice.ddf.itests.common.csw;

public class CswQueryBuilder {

  private String typename = "Record";

  private String typenameNamespace = "http://www.opengis.net/cat/csw/2.0.2";

  private String internalRepresentation;

  public static final String PROPERTY_IS_EQUAL_TO = "PropertyIsEqualTo";

  public static final String PROPERTY_IS_LIKE = "PropertyIsLike";

  public static final String PROPERTY_IS_NULL = "PropertyIsNull";

  public static final String OR = "Or";

  public static final String AND = "And";

  public static final String NOT = "Not";

  public CswQueryBuilder() {
    internalRepresentation = "";
  }

  public String getQuery() {
    return "<csw:GetRecords resultType=\"results\" outputFormat=\"application/xml\" outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\" maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\" xmlns:ns2=\"http://www.opengis.net/ogc\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.w3.org/1999/xlink\" xmlns:ns3=\"http://www.opengis.net/gml\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ows\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
        + "    <csw:Query typeNames=\"ns10:"
        + typename
        + "\" xmlns=\"\" xmlns:ns10=\""
        + typenameNamespace
        + "\">"
        + "        <csw:ElementSetName>full</csw:ElementSetName>"
        + "        <csw:Constraint version=\"1.1.0\">"
        + "            <ns2:Filter>"
        + internalRepresentation
        + "</ns2:Filter>"
        + "        </csw:Constraint>"
        + "    </csw:Query>"
        + "</csw:GetRecords>";
  }

  public String getQuery(String outputFormat, String outputSchema) {
    return "<csw:GetRecords resultType=\"results\" outputFormat=\""
        + outputFormat
        + "\" outputSchema=\""
        + outputSchema
        + "\" startPosition=\"1\" maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\" xmlns:ns2=\"http://www.opengis.net/ogc\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.w3.org/1999/xlink\" xmlns:ns3=\"http://www.opengis.net/gml\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ows\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">"
        + "    <csw:Query typeNames=\"ns10:"
        + typename
        + "\" xmlns=\"\" xmlns:ns10=\""
        + typenameNamespace
        + "\">"
        + "        <csw:ElementSetName>full</csw:ElementSetName>"
        + "        <csw:Constraint version=\"1.1.0\">"
        + "            <ns2:Filter>"
        + internalRepresentation
        + "</ns2:Filter>"
        + "        </csw:Constraint>"
        + "    </csw:Query>"
        + "</csw:GetRecords>";
  }

  public CswQueryBuilder addAttributeFilter(
      String filter, String propertyName, String literalValue) {
    internalRepresentation =
        internalRepresentation
            + "                    <ns2:"
            + filter
            + " wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
            + "                        <ns2:PropertyName>"
            + propertyName
            + "</ns2:PropertyName>"
            + "                        <ns2:Literal>"
            + literalValue
            + "</ns2:Literal>"
            + "                    </ns2:"
            + filter
            + ">";
    return this;
  }

  public CswQueryBuilder addPropertyIsNullAttributeFilter(String propertyName) {
    internalRepresentation =
        internalRepresentation
            + "                    <ns2:PropertyIsNull wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">"
            + "                        <ns2:PropertyName>"
            + propertyName
            + "</ns2:PropertyName>"
            + "                    </ns2:PropertyIsNull>";
    return this;
  }

  public CswQueryBuilder addLogicalOperator(String logicalOperator) throws java.lang.Exception {
    if (!OR.equals(logicalOperator)
        && !AND.equals(logicalOperator)
        && !NOT.equals(logicalOperator)) {
      throw new java.lang.Exception(
          "getCswLogicalWrapper requires \"Or,\" \"And,\" or \"Not\" as the first argument.");
    }
    internalRepresentation =
        "<ns2:" + logicalOperator + ">" + internalRepresentation + "</ns2:" + logicalOperator + ">";
    return this;
  }

  public CswQueryBuilder setTypename(String typename, String namespaceURI) {
    this.typename = typename;
    this.typenameNamespace = namespaceURI;

    return this;
  }

  @Override
  public String toString() {
    return this.getQuery();
  }
}
