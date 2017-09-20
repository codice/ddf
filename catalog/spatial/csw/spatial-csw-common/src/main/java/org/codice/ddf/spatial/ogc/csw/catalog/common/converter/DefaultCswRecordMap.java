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
package org.codice.ddf.spatial.ogc.csw.catalog.common.converter;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Topic;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdConstants;
import org.xml.sax.helpers.NamespaceSupport;

/**
 * DefaultCswRecordMap defines the system map between CSW Records and a framework Metacard. It
 * provides functions to retrieve the mapped values in either direction, and to retrieve metacard
 * fields for a CSW field as a {@link QName} or just a local name as a {@link String}. If a mapped
 * value isn't found, then the input value is used as the mapped value.
 */
public class DefaultCswRecordMap {
  private static final Map<QName, String> CSW_RECORD_QNAME_MAPPING;

  private static final Map<String, String> CSW_RECORD_LOCAL_NAME_MAPPING;

  private static final Map<String, List<QName>> METACARD_MAPPING;

  private static final Map<String, String> PREFIX_TO_URI_MAPPING;

  private static final DefaultCswRecordMap MAPPING;

  static {
    Map<String, String> localNameMap = new CaseInsensitiveMap();

    localNameMap.put(CswConstants.ANY_TEXT, Metacard.ANY_TEXT);
    localNameMap.put(CswConstants.CSW_NO_PREFIX_MODIFIED, Core.MODIFIED);
    localNameMap.put(CswConstants.CSW_NO_PREFIX_CREATED, Core.CREATED);
    localNameMap.put(CswConstants.CSW_TITLE, Core.TITLE);
    localNameMap.put(CswConstants.CSW_NO_PREFIX_TITLE, Core.TITLE);
    localNameMap.put(CswConstants.CSW_ALTERNATIVE, Core.TITLE);
    localNameMap.put(CswConstants.CSW_FORMAT, Media.FORMAT);
    localNameMap.put(CswConstants.CSW_TYPE, Metacard.CONTENT_TYPE);
    localNameMap.put(CswConstants.CSW_IDENTIFIER, Core.ID);
    localNameMap.put(CswConstants.CSW_BIBLIOGRAPHIC_CITATION, Core.ID);
    localNameMap.put(CswConstants.CSW_SOURCE, Core.RESOURCE_URI);
    localNameMap.put(CswConstants.CSW_LANGUAGE, Core.LANGUAGE);
    localNameMap.put(CswConstants.CSW_CREATED, Core.CREATED);
    localNameMap.put(CswConstants.CSW_MODIFIED, Core.MODIFIED);
    localNameMap.put(CswConstants.CSW_DATE, Core.MODIFIED);
    localNameMap.put(CswConstants.CSW_DATE_SUBMITTED, Core.MODIFIED);
    localNameMap.put(CswConstants.CSW_ISSUED, Core.MODIFIED);
    localNameMap.put(CswConstants.CSW_DATE_ACCEPTED, Metacard.EFFECTIVE);
    localNameMap.put(CswConstants.CSW_DATE_COPYRIGHTED, Metacard.EFFECTIVE);
    localNameMap.put(CswConstants.CSW_VALID, Core.EXPIRATION);
    localNameMap.put(CswConstants.CSW_PUBLISHER, Contact.PUBLISHER_NAME);
    localNameMap.put(CswConstants.CSW_CONTRIBUTOR, Contact.CONTRIBUTOR_NAME);
    localNameMap.put(CswConstants.CSW_CREATOR, Contact.CREATOR_NAME);
    localNameMap.put(CswConstants.CSW_RELATION, Core.RESOURCE_DOWNLOAD_URL);
    localNameMap.put(CswConstants.CSW_TABLE_OF_CONTENTS, Core.DESCRIPTION);
    localNameMap.put(CswConstants.CSW_ABSTRACT, Core.DESCRIPTION);
    localNameMap.put(CswConstants.CSW_DESCRIPTION, Core.DESCRIPTION);
    localNameMap.put(CswConstants.CSW_SUBJECT, Topic.CATEGORY);
    localNameMap.put(CswConstants.OWS_BOUNDING_BOX, Core.LOCATION);
    localNameMap.put(CswConstants.CSW_REFERENCES, Core.THUMBNAIL);

    /* GMD TYPES */
    localNameMap.put(GmdConstants.GMD_REVISION_DATE, Core.MODIFIED);
    localNameMap.put(GmdConstants.GMD_ALTERNATE_TITLE, Core.TITLE);
    localNameMap.put(GmdConstants.GMD_CREATION_DATE, Core.CREATED);
    localNameMap.put(GmdConstants.GMD_PUBLICATION_DATE, Metacard.EFFECTIVE);
    localNameMap.put(GmdConstants.GMD_ORGANIZATION_NAME, Contact.POINT_OF_CONTACT_NAME);
    localNameMap.put(GmdConstants.GMD_FORMAT, Media.FORMAT);
    localNameMap.put(GmdConstants.GMD_MODIFIED, Core.MODIFIED);
    localNameMap.put(GmdConstants.GMD_TYPE, Core.DATATYPE);
    localNameMap.put(GmdConstants.GMD_TYPE, Metacard.CONTENT_TYPE);

    CSW_RECORD_LOCAL_NAME_MAPPING = Collections.unmodifiableMap(localNameMap);

    Map<QName, String> qNameMap = new HashMap<>();
    qNameMap.put(CswConstants.CSW_IDENTIFIER_QNAME, Core.ID);
    qNameMap.put(CswConstants.CSW_BIBLIOGRAPHIC_CITATION_QNAME, Core.ID);
    qNameMap.put(CswConstants.CSW_SOURCE_QNAME, Core.RESOURCE_URI);
    qNameMap.put(CswConstants.CSW_LANGUAGE_QNAME, Core.LANGUAGE);
    qNameMap.put(CswConstants.CSW_TITLE_QNAME, Core.TITLE);
    qNameMap.put(CswConstants.CSW_ALTERNATIVE_QNAME, Core.TITLE);
    qNameMap.put(CswConstants.CSW_FORMAT_QNAME, Media.FORMAT);
    qNameMap.put(CswConstants.CSW_TYPE_QNAME, Metacard.CONTENT_TYPE);
    qNameMap.put(CswConstants.CSW_DATE_QNAME, Core.MODIFIED);
    qNameMap.put(CswConstants.CSW_MODIFIED_QNAME, Core.MODIFIED);
    qNameMap.put(CswConstants.CSW_CREATED_QNAME, Core.CREATED);
    qNameMap.put(CswConstants.CSW_DATE_ACCEPTED_QNAME, Metacard.EFFECTIVE);
    qNameMap.put(CswConstants.CSW_DATE_COPYRIGHTED_QNAME, Metacard.EFFECTIVE);
    qNameMap.put(CswConstants.CSW_DATE_SUBMITTED_QNAME, Core.MODIFIED);
    qNameMap.put(CswConstants.CSW_ISSUED_QNAME, Core.MODIFIED);
    qNameMap.put(CswConstants.CSW_VALID_QNAME, Core.EXPIRATION);
    qNameMap.put(CswConstants.CSW_PUBLISHER_QNAME, Contact.PUBLISHER_NAME);
    qNameMap.put(CswConstants.CSW_CONTRIBUTOR_QNAME, Contact.CONTRIBUTOR_NAME);
    qNameMap.put(CswConstants.CSW_CREATOR_QNAME, Contact.CREATOR_NAME);
    qNameMap.put(CswConstants.CSW_RELATION_QNAME, Core.RESOURCE_DOWNLOAD_URL);
    qNameMap.put(CswConstants.CSW_ABSTRACT_QNAME, Core.DESCRIPTION);
    qNameMap.put(CswConstants.CSW_TABLE_OF_CONTENTS_QNAME, Core.DESCRIPTION);
    qNameMap.put(CswConstants.CSW_DESCRIPTION_QNAME, Core.DESCRIPTION);
    qNameMap.put(CswConstants.CSW_SUBJECT_QNAME, Topic.CATEGORY);
    qNameMap.put(CswConstants.OWS_BOUNDING_BOX_QNAME, Core.LOCATION);
    qNameMap.put(CswConstants.CSW_REFERENCES_QNAME, Core.THUMBNAIL);

    /* GMD TYPES */
    qNameMap.put(GmdConstants.GMD_REVISION_DATE_QNAME, Core.MODIFIED);
    qNameMap.put(GmdConstants.GMD_ALTERNATE_TITLE_QNAME, Core.TITLE);
    qNameMap.put(GmdConstants.GMD_CREATION_DATE_QNAME, Core.CREATED);
    qNameMap.put(GmdConstants.GMD_PUBLICATION_DATE_QNAME, Metacard.EFFECTIVE);
    qNameMap.put(GmdConstants.GMD_ORGANIZATION_NAME_QNAME, Contact.POINT_OF_CONTACT_NAME);
    qNameMap.put(GmdConstants.GMD_FORMAT_QNAME, Media.FORMAT_VERSION);
    qNameMap.put(GmdConstants.GMD_MODIFIED_QNAME, Core.MODIFIED);
    qNameMap.put(GmdConstants.GMD_TYPE_QNAME, Core.DATATYPE);
    qNameMap.put(GmdConstants.GMD_TYPE_QNAME, Metacard.CONTENT_TYPE);

    CSW_RECORD_QNAME_MAPPING = Collections.unmodifiableMap(qNameMap);

    Map<String, List<QName>> metacardMap = new HashMap<>();
    metacardMap.put(
        Core.ID,
        Arrays.asList(
            CswConstants.CSW_IDENTIFIER_QNAME, CswConstants.CSW_BIBLIOGRAPHIC_CITATION_QNAME));

    metacardMap.put(
        Core.TITLE,
        Arrays.asList(
            CswConstants.CSW_TITLE_QNAME,
            CswConstants.CSW_ALTERNATIVE_QNAME,
            GmdConstants.GMD_ALTERNATE_TITLE_QNAME));

    metacardMap.put(Media.FORMAT, Arrays.asList(CswConstants.CSW_FORMAT_QNAME));

    metacardMap.put(
        Core.MODIFIED,
        Arrays.asList(
            CswConstants.CSW_DATE_QNAME,
            CswConstants.CSW_MODIFIED_QNAME,
            CswConstants.CSW_DATE_SUBMITTED_QNAME,
            CswConstants.CSW_ISSUED_QNAME,
            GmdConstants.GMD_REVISION_DATE_QNAME));

    metacardMap.put(
        Core.CREATED,
        Arrays.asList(CswConstants.CSW_CREATED_QNAME, GmdConstants.GMD_CREATION_DATE_QNAME));

    metacardMap.put(
        Metacard.EFFECTIVE,
        Arrays.asList(
            CswConstants.CSW_DATE_ACCEPTED_QNAME,
            CswConstants.CSW_DATE_COPYRIGHTED_QNAME,
            GmdConstants.GMD_PUBLICATION_DATE_QNAME));

    metacardMap.put(
        Contact.POINT_OF_CONTACT_NAME, Arrays.asList(GmdConstants.GMD_ORGANIZATION_NAME_QNAME));

    metacardMap.put(
        Core.LANGUAGE,
        Arrays.asList(GmdConstants.GMD_RESOURCE_LANGUAGE_QNAME, GmdConstants.GMD_LANGUAGE_QNAME));

    metacardMap.put(Media.FORMAT, Arrays.asList(GmdConstants.GMD_FORMAT_QNAME));

    metacardMap.put(Core.DATATYPE, Arrays.asList(GmdConstants.GMD_TYPE_QNAME));

    metacardMap.put(Core.EXPIRATION, Arrays.asList(CswConstants.CSW_VALID_QNAME));

    metacardMap.put(Core.RESOURCE_URI, Arrays.asList(CswConstants.CSW_SOURCE_QNAME));

    metacardMap.put(Core.LANGUAGE, Arrays.asList(CswConstants.CSW_LANGUAGE_QNAME));

    metacardMap.put(Contact.CREATOR_NAME, Arrays.asList(CswConstants.CSW_CREATOR_QNAME));

    metacardMap.put(Contact.PUBLISHER_NAME, Arrays.asList(CswConstants.CSW_PUBLISHER_QNAME));

    metacardMap.put(Contact.CONTRIBUTOR_NAME, Arrays.asList(CswConstants.CSW_CONTRIBUTOR_QNAME));

    metacardMap.put(Core.RESOURCE_DOWNLOAD_URL, Arrays.asList(CswConstants.CSW_RELATION_QNAME));

    metacardMap.put(
        Core.DESCRIPTION,
        Arrays.asList(
            CswConstants.CSW_ABSTRACT_QNAME,
            CswConstants.CSW_TABLE_OF_CONTENTS_QNAME,
            CswConstants.CSW_DESCRIPTION_QNAME,
            GmdConstants.GMD_ABSTRACT_QNAME));

    metacardMap.put(Core.LOCATION, Arrays.asList(CswConstants.OWS_BOUNDING_BOX_QNAME));

    metacardMap.put(Core.THUMBNAIL, Arrays.asList(CswConstants.CSW_REFERENCES_QNAME));

    metacardMap.put(Core.RESOURCE_URI, Arrays.asList(CswConstants.CSW_SOURCE_QNAME));

    metacardMap.put(Topic.CATEGORY, Arrays.asList(CswConstants.CSW_SUBJECT_QNAME));

    metacardMap.put(Metacard.CONTENT_TYPE, Arrays.asList(CswConstants.CSW_TYPE_QNAME));

    metacardMap.put(
        Metacard.POINT_OF_CONTACT, Arrays.asList(GmdConstants.GMD_ORGANIZATION_NAME_QNAME));

    metacardMap.put(Media.FORMAT, Arrays.asList(GmdConstants.GMD_FORMAT_QNAME));

    metacardMap.put(Core.DATATYPE, Arrays.asList(GmdConstants.GMD_TYPE_QNAME));

    METACARD_MAPPING = Collections.unmodifiableMap(metacardMap);

    Map<String, String> prefixMapping = new HashMap<>();

    prefixMapping.put(XMLConstants.XML_NS_PREFIX, XMLConstants.XML_NS_URI);
    prefixMapping.put(
        CswConstants.XML_SCHEMA_INSTANCE_NAMESPACE_PREFIX,
        XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
    prefixMapping.put(CswConstants.XML_SCHEMA_NAMESPACE_PREFIX, XMLConstants.W3C_XML_SCHEMA_NS_URI);
    prefixMapping.put(CswConstants.OWS_NAMESPACE_PREFIX, CswConstants.OWS_NAMESPACE);
    prefixMapping.put(CswConstants.CSW_NAMESPACE_PREFIX, CswConstants.CSW_OUTPUT_SCHEMA);
    prefixMapping.put(CswConstants.DUBLIN_CORE_NAMESPACE_PREFIX, CswConstants.DUBLIN_CORE_SCHEMA);
    prefixMapping.put(
        CswConstants.DUBLIN_CORE_TERMS_NAMESPACE_PREFIX, CswConstants.DUBLIN_CORE_TERMS_SCHEMA);

    PREFIX_TO_URI_MAPPING = Collections.unmodifiableMap(prefixMapping);

    MAPPING = new DefaultCswRecordMap();
  }

  private DefaultCswRecordMap() {}

  public static DefaultCswRecordMap getDefaultCswRecordMap() {
    return MAPPING;
  }

  /** NOTE: This is a {@link CaseInsensitiveMap}. */
  public static Map<String, String> getCswToMetacardAttributeNames() {
    return CSW_RECORD_LOCAL_NAME_MAPPING;
  }

  public static String getDefaultMetacardFieldFor(QName cswField) {
    if (CSW_RECORD_QNAME_MAPPING.containsKey(cswField)) {
      return CSW_RECORD_QNAME_MAPPING.get(cswField);
    }

    return getDefaultMetacardFieldFor(cswField.getLocalPart());
  }

  public static boolean hasDefaultMetacardFieldFor(QName cswField) {
    return CSW_RECORD_QNAME_MAPPING.containsKey(cswField);
  }

  public static boolean hasDefaultMetacardFieldFor(String cswField) {
    return CSW_RECORD_LOCAL_NAME_MAPPING.containsKey(cswField);
  }

  public static String getDefaultMetacardFieldFor(String cswField) {
    if (CSW_RECORD_LOCAL_NAME_MAPPING.containsKey(cswField)) {
      return CSW_RECORD_LOCAL_NAME_MAPPING.get(cswField);
    }

    return cswField;
  }

  public static boolean hasDefaultMetacardFieldForPrefixedString(String name) {
    return hasDefaultMetacardFieldForPrefixedString(name, null);
  }

  public static boolean hasDefaultMetacardFieldForPrefixedString(
      String propertyName, NamespaceSupport namespaceSupport) {
    if (propertyName.contains(":") && !isXpathPropertyName(propertyName)) {
      String prefix = propertyName.substring(0, propertyName.indexOf(":"));
      String localName = propertyName.substring(propertyName.indexOf(":") + 1);
      if (namespaceSupport != null && namespaceSupport.getURI(prefix) != null) {
        String uri = namespaceSupport.getURI(prefix);
        QName qname = new QName(uri, localName, prefix);
        return hasDefaultMetacardFieldFor(qname);
      } else {
        return hasDefaultMetacardFieldFor(localName);
      }
    } else {
      return hasDefaultMetacardFieldFor(propertyName);
    }
  }

  private static boolean isXpathPropertyName(String propertyName) {
    return propertyName.contains("/") || propertyName.contains("@");
  }

  public static String getDefaultMetacardFieldForPrefixedString(String name) {
    return getDefaultMetacardFieldForPrefixedString(name, null);
  }

  public static String getDefaultMetacardFieldForPrefixedString(
      String propertyName, NamespaceSupport namespaceSupport) {
    String name;

    if (propertyName.contains(":") && !isXpathPropertyName(propertyName)) {
      String prefix = propertyName.substring(0, propertyName.indexOf(":"));
      String localName = propertyName.substring(propertyName.indexOf(":") + 1);
      if (namespaceSupport != null && namespaceSupport.getURI(prefix) != null) {
        String uri = namespaceSupport.getURI(prefix);
        QName qname = new QName(uri, localName, prefix);
        name = getDefaultMetacardFieldFor(qname);
      } else {
        name = getDefaultMetacardFieldFor(localName);
      }
    } else {
      name = getDefaultMetacardFieldFor(propertyName);
    }
    return name;
  }

  public static List<QName> getCswFieldsFor(String metacardField) {
    if (METACARD_MAPPING.containsKey(metacardField)) {
      return METACARD_MAPPING.get(metacardField);
    }
    return Arrays.asList(new QName(metacardField));
  }

  public static Map<String, String> getPrefixToUriMapping() {
    return PREFIX_TO_URI_MAPPING;
  }
}
