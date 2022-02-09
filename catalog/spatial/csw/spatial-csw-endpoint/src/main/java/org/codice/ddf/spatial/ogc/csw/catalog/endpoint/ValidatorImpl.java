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

import ddf.catalog.transform.QueryFilterTransformerProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;
import net.opengis.cat.csw.v_2_0_2.QueryType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api.CswXmlValidator;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.api.TransformerManager;

/** Validator provides methods to validate Requests for CSW 2.0.2 */
public class ValidatorImpl implements CswXmlValidator {

  private static final List<String> ELEMENT_NAMES = Arrays.asList("brief", "summary", "full");

  private static final String OCTET_STREAM_OUTPUT_SCHEMA =
      "http://www.iana.org/assignments/media-types/application/octet-stream";

  private static final String DEFAULT_OUTPUT_FORMAT = MediaType.APPLICATION_XML;

  private QueryFilterTransformerProvider queryFilterTransformerProvider;

  @Override
  public void validateFullyQualifiedTypes(List<QName> types) throws CswException {
    for (QName type : types) {
      if (StringUtils.isBlank(type.getNamespaceURI())) {
        throw new CswException(
            "Unqualified type name: '" + type.getLocalPart() + "'",
            CswConstants.INVALID_PARAMETER_VALUE,
            null);
      }
    }
  }

  /**
   * Verifies that if types are passed, then they exist.
   *
   * @param types List of QNames representing types
   * @param version the specified version of the types
   */
  void validateTypes(List<QName> types, String version) throws CswException {
    if (types == null || types.size() == 0) {
      // No type at all is valid, just return
      return;
    }

    for (QName type : types) {
      if (!queryFilterTransformerProvider.getTransformer(type).isPresent()) {
        throw createUnknownTypeException(type.toString());
      }
    }
  }

  /**
   * Verifies that if the ElementName or ElementSetName is passed, that they are valid and mutually
   * exclusive according to the OpenGIS CSW spec.
   *
   * @param query QueryType to be validated
   */
  void validateElementNames(QueryType query) throws CswException {

    if (query.isSetElementSetName() && query.isSetElementName()) {
      throw new CswException(
          "ElementSetName and ElementName must be mutually exclusive",
          CswConstants.INVALID_PARAMETER_VALUE,
          "ElementName");
    } else if (query.isSetElementName() && query.getElementName().size() > 0) {

      for (QName elementName : query.getElementName()) {
        String elementNameString = elementName.getLocalPart();
        if (!ELEMENT_NAMES.contains(elementNameString)) {
          throw new CswException(
              "Unknown ElementName " + elementNameString,
              CswConstants.INVALID_PARAMETER_VALUE,
              "ElementName");
        }
      }
    } else if (query.isSetElementSetName() && query.getElementSetName().getValue() == null) {
      throw new CswException(
          "Unknown ElementSetName", CswConstants.INVALID_PARAMETER_VALUE, "ElementSetName");
    }
  }

  @Override
  public void validateOutputSchema(String schema, TransformerManager schemaTransformerManager)
      throws CswException {
    if (schema == null
        || schemaTransformerManager.getTransformerBySchema(schema) != null
        || schema.equals(OCTET_STREAM_OUTPUT_SCHEMA)) {
      return;
    }
    throw createUnknownSchemaException(schema);
  }

  @Override
  public void validateVersion(String versions) throws CswException {
    if (!versions.contains(CswConstants.VERSION_2_0_2)) {
      throw new CswException(
          "Version(s) "
              + versions
              + " is not supported, we currently support version "
              + CswConstants.VERSION_2_0_2,
          CswConstants.VERSION_NEGOTIATION_FAILED,
          null);
    }
  }

  @Override
  public void validateOutputFormat(String format, TransformerManager mimeTypeTransformerManager)
      throws CswException {
    if (!StringUtils.isEmpty(format)) {
      if (!(DEFAULT_OUTPUT_FORMAT.equals(format)
              || MediaType.APPLICATION_OCTET_STREAM.equals(format))
          && mimeTypeTransformerManager != null
          && !mimeTypeTransformerManager.getAvailableMimeTypes().contains(format)) {
        throw new CswException(
            "Invalid output format '" + format + "'",
            CswConstants.INVALID_PARAMETER_VALUE,
            "outputformat");
      }
    }
  }

  @Override
  public void validateSchemaLanguage(String schemaLanguage) throws CswException {
    if (!StringUtils.isEmpty(schemaLanguage)) {
      if (!CswConstants.VALID_SCHEMA_LANGUAGES.contains(schemaLanguage)) {
        throw new CswException(
            "Invalid schema language '" + schemaLanguage + "'",
            CswConstants.INVALID_PARAMETER_VALUE,
            "schemaLanguage");
      }
    }
  }

  /**
   * Validates TypeName to namspace uri mapping in query request.
   *
   * @param typeNames this can be a comma separated list of types which can be prefixed with
   *     prefixes. example csw:Record
   * @param namespaces the namespace parameter from the request example
   *     NAMESPACE=xmlns(csw=http://www.opengis.net/cat/csw/2.0.2)
   * @param namespacePrefixToUriMappings map of namespace prefixes to namespace uri example key=csw
   *     value=http://www.opengis.net/cat/csw/2.0.2
   * @throws CswException
   */
  public void validateTypeNameToNamespaceMappings(
      String typeNames, String namespaces, Map<String, String> namespacePrefixToUriMappings)
      throws CswException {

    // No typeName in query.
    if (StringUtils.isBlank(typeNames)) {
      return;
    }

    String[] types = typeNames.split(CswConstants.COMMA);
    String prefix = null;

    for (String type : types) {
      if (type.contains(CswConstants.NAMESPACE_DELIMITER)) {
        // Get the prefix. For example in csw:Record, get csw.
        prefix = type.split(CswConstants.NAMESPACE_DELIMITER)[0];
      } else {
        prefix = "";
      }

      // if the prefix does not map to a provided namespace, throw an exception.
      if (!namespacePrefixToUriMappings.containsKey(prefix)) {
        throw new CswException(
            "Unable to map ["
                + type
                + "] to one of the following namespaces ["
                + namespaces
                + "].");
      }
    }
  }

  private CswException createUnknownTypeException(final String type) {
    return new CswException(
        "The type '" + type + "' is not known to this service.",
        CswConstants.INVALID_PARAMETER_VALUE,
        null);
  }

  private CswException createUnknownSchemaException(final String schema) {
    return new CswException(
        "The schema '" + schema + "' is not known to this service.",
        CswConstants.INVALID_PARAMETER_VALUE,
        "OutputSchema");
  }

  @Override
  public void setQueryFilterTransformerProvider(
      QueryFilterTransformerProvider queryFilterTransformerHelper) {
    this.queryFilterTransformerProvider = queryFilterTransformerHelper;
  }
}
