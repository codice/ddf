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
package ddf.catalog.source.solr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Validation;
import ddf.catalog.source.solr.json.MetacardTypeMapperFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import lux.Config;
import lux.xml.SaxonDocBuilder;
import lux.xml.XmlReader;
import lux.xml.tinybin.TinyBinary;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.tree.tiny.TinyDocumentImpl;
import net.sf.saxon.tree.tiny.TinyTree;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrRequest.METHOD;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.codehaus.stax2.XMLInputFactory2;
import org.codice.solr.client.solrj.SolrClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class tries to resolve all user given field names to their corresponding dynamic Solr index
 * field name. This class takes most of its logic directly from the configured Solr schema.xml. For
 * instance, the suffixes enumerated in this class are directly copied from the schema.xml.
 *
 * @since 0.2.0
 */
public class DynamicSchemaResolver {

  private static final String LUX_XML_FIELD_NAME = "lux_xml";

  private static final String SCORE_FIELD_NAME = "score";

  private static final int TOKEN_MAXIMUM_BYTES = 32766;

  static final String PHONETICS_FEATURE = "phonetics";

  static final char FIRST_CHAR_OF_SUFFIX = '_';

  private static final String COULD_NOT_READ_METACARD_TYPE_MESSAGE = "Could not read MetacardType.";

  private static final String FIELDS_KEY = "fields";

  private static final String COULD_NOT_SERIALIZE_OBJECT_MESSAGE = "Could not serialize object";

  private static final XMLInputFactory XML_INPUT_FACTORY;

  static final int FIVE_MEGABYTES = 5 * 1024 * 1024;

  private static final String METADATA_SIZE_LIMIT = "metadata.size.limit";

  private static final String SOLR_CLOUD_VERSION_FIELD = "_version_";

  private static final String COULD_NOT_UPDATE_CACHE_FOR_FIELD_NAMES =
      "Could not update cache for field names.";

  private static final List<String> PRIVATE_SOLR_FIELDS =
      Arrays.asList(
          SOLR_CLOUD_VERSION_FIELD,
          SchemaFields.METACARD_TYPE_FIELD_NAME,
          SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME,
          LUX_XML_FIELD_NAME,
          SCORE_FIELD_NAME);

  private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSchemaResolver.class);

  private Function<TinyTree, TinyBinary> tinyBinaryFunction;

  private static int metadataMaximumBytes;

  private static final ObjectMapper METACARD_TYPE_MAPPER =
      MetacardTypeMapperFactory.newObjectMapper();

  static {
    ClassLoader tccl = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(DynamicSchemaResolver.class.getClassLoader());

      XML_INPUT_FACTORY = XMLInputFactory2.newInstance();
      XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.FALSE);
      XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
      XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
      XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
      XML_INPUT_FACTORY.setProperty(
          XMLInputFactory.SUPPORT_DTD,
          Boolean.FALSE); // This disables DTDs entirely for that factory
    } finally {
      Thread.currentThread().setContextClassLoader(tccl);
    }
  }

  Set<String> fieldsCache = new HashSet<>();

  private Set<String> anyTextFieldsCache = new HashSet<>();

  private SchemaFields schemaFields;

  private Cache<String, MetacardType> metacardTypesCache =
      CacheBuilder.newBuilder().maximumSize(4096).initialCapacity(64).build();

  private Cache<String, byte[]> metacardTypeNameToSerialCache =
      CacheBuilder.newBuilder().maximumSize(4096).initialCapacity(64).build();

  private Processor processor = new Processor(new Config());

  public DynamicSchemaResolver(
      List<String> additionalFields, Function<TinyTree, TinyBinary> tinyBinaryFunction) {
    this(additionalFields);
    this.tinyBinaryFunction = tinyBinaryFunction;
  }

  public DynamicSchemaResolver(List<String> additionalFields) {
    this.tinyBinaryFunction = this::newTinyBinary;
    this.schemaFields = new SchemaFields();
    metadataMaximumBytes = getMetadataSizeLimit();
    fieldsCache.add(Metacard.ID + SchemaFields.TEXT_SUFFIX);
    fieldsCache.add(Metacard.ID + SchemaFields.TEXT_SUFFIX + SchemaFields.TOKENIZED);
    fieldsCache.add(
        Metacard.ID + SchemaFields.TEXT_SUFFIX + SchemaFields.TOKENIZED + SchemaFields.HAS_CASE);
    fieldsCache.add(Metacard.TAGS + SchemaFields.TEXT_SUFFIX);

    anyTextFieldsCache.add(Metacard.METADATA + SchemaFields.TEXT_SUFFIX);

    fieldsCache.add(Validation.VALIDATION_ERRORS + SchemaFields.TEXT_SUFFIX);
    fieldsCache.add(Validation.VALIDATION_WARNINGS + SchemaFields.TEXT_SUFFIX);

    fieldsCache.add(SchemaFields.METACARD_TYPE_FIELD_NAME);
    fieldsCache.add(SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME);

    addAdditionalFields(this, additionalFields);
  }

  public DynamicSchemaResolver() {
    this(Collections.emptyList());
  }

  public static void addAdditionalFields(
      DynamicSchemaResolver dynamicSchemaResolver, List<String> additionalFields) {
    additionalFields
        .stream()
        .filter(StringUtils::isNotBlank)
        .forEach(field -> dynamicSchemaResolver.fieldsCache.add(field));
  }

  @SuppressWarnings("WeakerAccess" /* access needed by blueprint */)
  public void addMetacardType(MetacardType metacardType) {
    metacardType.getAttributeDescriptors().forEach(this::addToFieldsCache);

    metacardType
        .getAttributeDescriptors()
        .stream()
        .filter(descriptor -> BasicTypes.STRING_TYPE.equals(descriptor.getType()))
        .map(stringDescriptor -> stringDescriptor.getName() + SchemaFields.TEXT_SUFFIX)
        .forEach(fieldName -> anyTextFieldsCache.add(fieldName));
  }

  /**
   * Adds the fields that are already in Solr to the cache. This method should be called once the
   * SolrClient is up to ensure the cache is synchronized with Solr.
   *
   * @param client the SolrClient we are working with
   * @throws SolrServerException if a Solr server exception occurs
   * @throws SolrException if a Solr exception occurs
   * @throws IOException if an I/O exception occurs
   */
  void addFieldsFromClient(SolrClient client) throws SolrServerException, IOException {
    if (client == null) {
      LOGGER.debug("Solr client is null, could not add fields to cache.");
      return;
    }

    SolrQuery query = new SolrQuery();

    // numterms=0 means retrieve everything (regular or dynamic fields)
    query.add("numterms", "0");

    /*
     * Adding this request handler allows us to query the schema dynamically. The name of the
     * request handler is provided by the schema.xml. If the name is changed in the schema.xml,
     * then this value must be changed as well.
     */
    query.setRequestHandler("/admin/luke");

    QueryResponse response;
    try {
      response = client.query(query, METHOD.POST);
    } catch (SolrServerException | SolrException | IOException e) {
      LOGGER.info(DynamicSchemaResolver.COULD_NOT_UPDATE_CACHE_FOR_FIELD_NAMES);
      LOGGER.debug(DynamicSchemaResolver.COULD_NOT_UPDATE_CACHE_FOR_FIELD_NAMES, e);
      throw e;
    }
    NamedList<?> fields = (SimpleOrderedMap<?>) (response.getResponse().get(FIELDS_KEY));

    if (fields == null) {
      return;
    }

    for (Entry<String, ?> e : fields) {
      String key = e.getKey();
      fieldsCache.add(key);
      if (key.endsWith(SchemaFields.TEXT_SUFFIX)) {
        anyTextFieldsCache.add(key);
      }
    }
  }

  /** Adds the fields of the Metacard into the {@link SolrInputDocument} */
  void addFields(Metacard metacard, SolrInputDocument solrInputDocument)
      throws MetacardCreationException {
    MetacardType schema = metacard.getMetacardType();

    // TODO: register these metacard types when a new one is seen

    for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
      if (metacard.getAttribute(ad.getName()) != null) {
        List<Serializable> attributeValues = metacard.getAttribute(ad.getName()).getValues();

        if (CollectionUtils.isNotEmpty(attributeValues) && attributeValues.get(0) != null) {
          AttributeFormat format = ad.getType().getAttributeFormat();
          String formatIndexName = ad.getName() + getFieldSuffix(format);

          if (AttributeFormat.XML.equals(format)
              && solrInputDocument.getFieldValue(
                      formatIndexName + getSpecialIndexSuffix(AttributeFormat.STRING))
                  == null) {
            List<String> parsedTexts = parseTextFrom(attributeValues);

            // parsedTexts => *_txt_tokenized
            String specialStringIndexName =
                ad.getName()
                    + getFieldSuffix(AttributeFormat.STRING)
                    + getSpecialIndexSuffix(AttributeFormat.STRING);
            solrInputDocument.addField(specialStringIndexName, parsedTexts);
          } else if (AttributeFormat.STRING.equals(format)
              && solrInputDocument.getFieldValue(
                      ad.getName() + getFieldSuffix(AttributeFormat.STRING))
                  == null) {
            List<Serializable> truncatedValues =
                attributeValues
                    .stream()
                    .map(value -> value != null ? truncateAsUTF8(value.toString()) : value)
                    .collect(Collectors.toList());
            // *_txt
            solrInputDocument.addField(
                ad.getName() + getFieldSuffix(AttributeFormat.STRING), truncatedValues);

            // *_txt_tokenized
            solrInputDocument.addField(
                ad.getName()
                    + getFieldSuffix(AttributeFormat.STRING)
                    + getSpecialIndexSuffix(AttributeFormat.STRING),
                attributeValues);
          } else if (AttributeFormat.OBJECT.equals(format)) {
            ByteArrayOutputStream byteArrayOS = new ByteArrayOutputStream();
            List<Serializable> byteArrays = new ArrayList<>();

            try (ObjectOutputStream out = new ObjectOutputStream(byteArrayOS)) {
              for (Serializable serializable : attributeValues) {
                out.writeObject(serializable);
                byteArrays.add(byteArrayOS.toByteArray());
                out.reset();
              }
            } catch (IOException e) {
              throw new MetacardCreationException(COULD_NOT_SERIALIZE_OBJECT_MESSAGE, e);
            }

            attributeValues = byteArrays;
          }

          if (AttributeFormat.GEOMETRY.equals(format)
              && solrInputDocument.getFieldValue(formatIndexName + SchemaFields.SORT_SUFFIX)
                  == null) {
            solrInputDocument.addField(
                formatIndexName + SchemaFields.SORT_SUFFIX, createCenterPoint(attributeValues));
          }

          // Prevent adding a field already on document
          if (solrInputDocument.getFieldValue(formatIndexName) == null) {
            solrInputDocument.addField(formatIndexName, attributeValues);
          } else {
            LOGGER.trace("Skipping adding field already found on document ({})", formatIndexName);
          }
        }
      }
    }

    if (!ConfigurationStore.getInstance().isDisableTextPath()
        && StringUtils.isNotBlank(metacard.getMetadata())) {
      String metadata = metacard.getMetadata();
      if (metadata.getBytes().length < metadataMaximumBytes) {
        try {
          byte[] luxXml = createTinyBinary(metadata);
          solrInputDocument.addField(LUX_XML_FIELD_NAME, luxXml);
        } catch (XMLStreamException | SaxonApiException | IOException | RuntimeException e) {
          LOGGER.debug(
              "Unable to parse metadata field.  XPath support unavailable for metacard {}",
              metacard.getId(),
              e);
        }
      } else {
        LOGGER.debug(
            "Can't create binary data from metadata larger than metadata size limit. ID: {}",
            metacard.getId());
      }
    }

    /*
     * Lastly the metacardType must be added to the solr document. These are internal fields
     */
    String schemaName = String.format("%s#%s", schema.getName(), schema.hashCode());
    solrInputDocument.addField(SchemaFields.METACARD_TYPE_FIELD_NAME, schemaName);
    byte[] metacardTypeBytes = metacardTypeNameToSerialCache.getIfPresent(schemaName);

    if (metacardTypeBytes == null) {
      MetacardType coreMetacardType =
          new MetacardTypeImpl(
              schema.getName(), convertAttributeDescriptors(schema.getAttributeDescriptors()));

      metacardTypesCache.put(schemaName, coreMetacardType);

      metacardTypeBytes = serialize(coreMetacardType);
      metacardTypeNameToSerialCache.put(schemaName, metacardTypeBytes);

      addToFieldsCache(coreMetacardType.getAttributeDescriptors());
    }

    solrInputDocument.addField(SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME, metacardTypeBytes);
  }

  /*
   * Truncation that takes multibyte UTF-8 characters and surrogate pairs into consideration.
   * https://stackoverflow.com/questions/119328/how-do-i-truncate-a-java-string-to-fit-in-a-given-number-of-bytes-once-utf-8-en
   */
  private String truncateAsUTF8(String value) {
    int b = 0;
    int skip;
    for (int i = 0; i < value.length(); i = i + 1 + skip) {
      char c = value.charAt(i);

      skip = 0;
      int more;
      if (c <= 0x007f) {
        more = 1;
      } else if (c <= 0x07FF) {
        more = 2;
      } else if (c <= 0xd7ff) {
        more = 3;
      } else if (c <= 0xDFFF) {
        more = 4;
        skip = 1;
      } else {
        more = 3;
      }

      if (b + more > TOKEN_MAXIMUM_BYTES) {
        return value.substring(0, i);
      }
      b += more;
    }
    return value;
  }

  private String createCenterPoint(List<Serializable> values) {
    WKTReader reader = new WKTReader(GEOMETRY_FACTORY);
    List<Geometry> geometries = new ArrayList<>();

    for (Serializable serializable : values) {
      String wkt = serializable.toString();
      try {
        geometries.add(reader.read(wkt));
      } catch (ParseException e) {
        LOGGER.debug("Failed to read WKT, skipping: {}", wkt, e);
      }
    }

    if (geometries.isEmpty()) {
      return null;
    }

    Point centerPoint;
    if (geometries.size() > 1) {
      GeometryCollection geoCollection =
          GEOMETRY_FACTORY.createGeometryCollection(geometries.toArray(new Geometry[0]));

      centerPoint = geoCollection.getCentroid();
    } else {
      centerPoint = geometries.get(0).getCentroid();
    }

    if (centerPoint == null || centerPoint.isEmpty()) {
      return null;
    }

    return centerPoint.getY() + "," + centerPoint.getX();
  }

  private byte[] createTinyBinary(String xml)
      throws XMLStreamException, SaxonApiException, IOException {
    SaxonDocBuilder builder = new SaxonDocBuilder(processor);

    XmlReader xmlReader = new XmlReader();
    xmlReader.addHandler(builder);
    xmlReader.setStripNamespaces(true);
    xmlReader.read(IOUtils.toInputStream(xml, Charset.defaultCharset().name()));

    XdmNode node = builder.getDocument();

    TinyTree tinyTree = ((TinyDocumentImpl) node.getUnderlyingNode()).getTree();
    TinyBinary tinyBinary = tinyBinaryFunction.apply(tinyTree);

    return tinyBinary.getBytes();
  }

  private TinyBinary newTinyBinary(TinyTree tinyTree) {
    return new TinyBinary(tinyTree, StandardCharsets.UTF_8);
  }

  /**
   * Returns the best approximation as to what {@link AttributeFormat} this Solr Field is.
   *
   * @param solrFieldName name of the Solr field
   * @return the {@link AttributeFormat} associated with the Solr field
   */
  public AttributeFormat getType(String solrFieldName) {

    String suffix = "";
    int lastIndexOfUndercore = solrFieldName.lastIndexOf(FIRST_CHAR_OF_SUFFIX);

    if (lastIndexOfUndercore != -1) {
      suffix = solrFieldName.substring(lastIndexOfUndercore);
    }

    return schemaFields.getFormat(suffix);
  }

  List<Serializable> getDocValues(String solrFieldName, Collection<Object> docValues) {
    ArrayList<Serializable> values = new ArrayList<>();
    for (Object docValue : docValues) {
      values.add(getDocValue(solrFieldName, docValue));
    }
    return values;
  }

  @SuppressWarnings(
      "squid:S2093" /* try-with-resource will throw IOException with InputStream and we do not care to get that exception */)
  private Serializable getDocValue(String solrFieldName, Object docValue) {

    AttributeFormat format = getType(solrFieldName);

    if (AttributeFormat.SHORT.equals(format)) {
      /*
       * We have inside knowledge that user-given short objects are stored as Integers in
       * Solr. You cannot cast an int to a short, so the workaround is to parse it. This
       * should not lead to any loss of information because the value was originally a short.
       */
      return Short.parseShort(docValue.toString());
    } else if (AttributeFormat.OBJECT.equals(format)) {

      ByteArrayInputStream bais = null;
      ObjectInputStream in = null;

      try {
        bais = new ByteArrayInputStream((byte[]) docValue);
        in = new ObjectInputStream(bais);
        return (Serializable) in.readObject();
      } catch (IOException e) {
        LOGGER.info("IO exception loading input document", e);
      } catch (ClassNotFoundException e) {
        LOGGER.info("Could not create object to return.", e);
        // TODO which exception to throw?
      } finally {
        IOUtils.closeQuietly(bais);
        IOUtils.closeQuietly(in);
      }

      return null;
    } else {
      return ((Serializable) docValue);
    }
  }

  /**
   * PRE-CONDITION is that fieldname cannot be null.
   *
   * <p>The convention is that we add a suffix starting with an underscore, so if we find the last
   * underscore, then we can return the original field name.
   *
   * @param solrFieldName Solr index field name
   * @return the original field name
   */
  String resolveFieldName(String solrFieldName) {
    int lastIndexOfUndercore = solrFieldName.lastIndexOf(FIRST_CHAR_OF_SUFFIX);

    if (lastIndexOfUndercore != -1) {
      return solrFieldName.substring(0, lastIndexOfUndercore);
    }
    return solrFieldName;
  }

  boolean isPrivateField(String solrFieldName) {
    return PRIVATE_SOLR_FIELDS.contains(solrFieldName);
  }

  /**
   * Attempts to resolve the name of a field without being given an {@link AttributeFormat}
   *
   * @param field user given field name
   * @return a list of possible Solr field names that match the given field. If none are found, then
   *     an empty list is returned
   */
  List<String> getAnonymousField(String field) {
    ArrayList<String> list = new ArrayList<>();

    for (AttributeFormat format : AttributeFormat.values()) {
      String fullFieldName = field + schemaFields.getFieldSuffix(format);

      if (fieldsCache.contains(fullFieldName)) {
        list.add(fullFieldName);
      }
    }

    return list;
  }

  /**
   * Attempts to find the fieldName for the given propertyName value.
   *
   * @param propertyName property name provided by user
   * @param format {@link AttributeFormat} that describes the type of {@link
   *     ddf.catalog.data.Attribute} the field is
   * @param isSearchedAsExactValue specifies if any special index suffixes need to be added to the
   *     field
   * @return the proper schema field name. If a schema name cannot be found in cache, returns a
   *     schema field name that matches the dynamic field type formatting.
   */
  String getField(
      String propertyName,
      AttributeFormat format,
      boolean isSearchedAsExactValue,
      Map<String, Serializable> enabledFeatures) {

    if (Metacard.ANY_GEO.equals(propertyName)) {
      return Metacard.GEOGRAPHY + "_geo_index";
    }

    final String fieldSuffix = getFieldSuffix(format);
    String fieldName =
        propertyName
            + fieldSuffix
            + (isSearchedAsExactValue ? "" : getSpecialIndexSuffix(format, enabledFeatures));

    if (fieldsCache.contains(fieldName)) {
      return fieldName;
    }

    switch (format) {
      case DOUBLE:
      case LONG:
      case INTEGER:
      case SHORT:
      case FLOAT:
        return findAnyMatchingNumericalField(propertyName, fieldSuffix);
      default:
        break;
    }

    LOGGER.debug(
        "Could not find exact schema field name for [{}], attempting to search with [{}]",
        propertyName,
        fieldName);

    return fieldName;
  }

  private String getFieldSuffix(AttributeFormat format) {
    return schemaFields.getFieldSuffix(format);
  }

  @SuppressWarnings(
      "squid:S2093" /* try-with-resource will throw IOException with InputStream and we do not care to get that exception */)
  public MetacardType getMetacardType(SolrDocument doc) throws MetacardCreationException {
    String mTypeFieldName = doc.getFirstValue(SchemaFields.METACARD_TYPE_FIELD_NAME).toString();

    MetacardType cachedMetacardType = metacardTypesCache.getIfPresent(mTypeFieldName);

    if (cachedMetacardType != null) {
      return cachedMetacardType;
    }

    byte[] bytes = (byte[]) doc.getFirstValue(SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME);
    try {
      cachedMetacardType = METACARD_TYPE_MAPPER.readValue(bytes, MetacardType.class);
    } catch (IOException e) {
      LOGGER.info("IO exception loading cached metacard type", e);
      throw new MetacardCreationException(COULD_NOT_READ_METACARD_TYPE_MESSAGE);
    }

    metacardTypeNameToSerialCache.put(mTypeFieldName, bytes);
    metacardTypesCache.put(mTypeFieldName, cachedMetacardType);
    addToFieldsCache(cachedMetacardType.getAttributeDescriptors());

    return cachedMetacardType;
  }

  String getCaseSensitiveField(
      String mappedPropertyName, Map<String, Serializable> enabledFeatures) {
    if (isPhoneticsEnabled(enabledFeatures)
        && mappedPropertyName.endsWith(SchemaFields.PHONETICS)) {
      return mappedPropertyName;
    }
    // TODO We can check if this field really does exist
    return mappedPropertyName + SchemaFields.HAS_CASE;
  }

  private String getSpecialIndexSuffix(AttributeFormat format) {
    return getSpecialIndexSuffix(format, Collections.emptyMap());
  }

  String getSpecialIndexSuffix(AttributeFormat format, Map<String, Serializable> enabledFeatures) {

    switch (format) {
      case STRING:
        if (isPhoneticsEnabled(enabledFeatures)) {
          return SchemaFields.PHONETICS;
        }
        return SchemaFields.TOKENIZED;
      case GEOMETRY:
        return SchemaFields.INDEXED;
      case XML:
        return SchemaFields.TEXT_PATH;
      default:
        break;
    }

    return "";
  }

  private boolean isPhoneticsEnabled(Map<String, Serializable> enabledFeatures) {
    Boolean phoneticsEnabled = false;
    Object phoneticsValue = enabledFeatures.get(PHONETICS_FEATURE);
    if (phoneticsValue != null) {
      phoneticsEnabled = (Boolean) phoneticsValue;
    }
    return phoneticsEnabled;
  }

  private void addToFieldsCache(Set<AttributeDescriptor> descriptors) {
    for (AttributeDescriptor ad : descriptors) {
      addToFieldsCache(ad);
    }
  }

  private void addToFieldsCache(AttributeDescriptor descriptor) {

    AttributeFormat format = descriptor.getType().getAttributeFormat();

    fieldsCache.add(descriptor.getName() + schemaFields.getFieldSuffix(format));

    if (!getSpecialIndexSuffix(format).equals("")) {
      fieldsCache.add(
          descriptor.getName()
              + schemaFields.getFieldSuffix(format)
              + getSpecialIndexSuffix(format));
    }

    if (format.equals(AttributeFormat.STRING)) {
      fieldsCache.add(
          descriptor.getName()
              + schemaFields.getFieldSuffix(format)
              + getSpecialIndexSuffix(format)
              + SchemaFields.HAS_CASE);
      fieldsCache.add(
          descriptor.getName() + schemaFields.getFieldSuffix(format) + SchemaFields.PHONETICS);
      anyTextFieldsCache.add(descriptor.getName() + schemaFields.getFieldSuffix(format));
    }

    if (format.equals(AttributeFormat.XML)) {
      fieldsCache.add(descriptor.getName() + SchemaFields.TEXT_SUFFIX + SchemaFields.TOKENIZED);
      fieldsCache.add(
          descriptor.getName()
              + SchemaFields.TEXT_SUFFIX
              + SchemaFields.TOKENIZED
              + SchemaFields.HAS_CASE);
      fieldsCache.add(
          descriptor.getName()
              + schemaFields.getFieldSuffix(format)
              + getSpecialIndexSuffix(format));
    }
  }

  private byte[] serialize(MetacardType anywhereMType) throws MetacardCreationException {
    try {
      return METACARD_TYPE_MAPPER.writeValueAsBytes(anywhereMType);
    } catch (JsonProcessingException e) {
      throw new MetacardCreationException(COULD_NOT_READ_METACARD_TYPE_MESSAGE, e);
    }
  }

  private String findAnyMatchingNumericalField(String propertyName, String fieldSuffix) {

    if (fieldsCache.contains(propertyName + SchemaFields.DOUBLE_SUFFIX)) {
      return propertyName + SchemaFields.DOUBLE_SUFFIX;
    }
    if (fieldsCache.contains(propertyName + SchemaFields.FLOAT_SUFFIX)) {
      return propertyName + SchemaFields.FLOAT_SUFFIX;
    }
    if (fieldsCache.contains(propertyName + SchemaFields.INTEGER_SUFFIX)) {
      return propertyName + SchemaFields.INTEGER_SUFFIX;
    }
    if (fieldsCache.contains(propertyName + SchemaFields.LONG_SUFFIX)) {
      return propertyName + SchemaFields.LONG_SUFFIX;
    }
    if (fieldsCache.contains(propertyName + SchemaFields.SHORT_SUFFIX)) {
      return propertyName + SchemaFields.SHORT_SUFFIX;
    }

    LOGGER.debug(
        "Did not find any numerical schema fields for property [{}]. Replacing with property [{}{}]",
        propertyName,
        propertyName,
        fieldSuffix);
    return propertyName + fieldSuffix;
  }

  /**
   * Given xml as a string, this method will parse out element text and CDATA text. It separates
   * each by one space character.
   *
   * @param xmlDatas List of XML as {@code String}
   * @return parsed CDATA and element text
   */
  @SuppressWarnings(
      "squid:S2093" /* try-with-resource will throw IOException with InputStream and we do not care to get that exception */)
  private List<String> parseTextFrom(List<Serializable> xmlDatas) {

    StringBuilder builder = new StringBuilder();
    List<String> parsedTexts = new ArrayList<>();
    XMLStreamReader xmlStreamReader = null;
    StringReader sr = null;
    long starttime = System.currentTimeMillis();

    try {
      for (Serializable xmlData : xmlDatas) {
        // xml parser does not handle leading whitespace
        sr = new StringReader(xmlData.toString());
        xmlStreamReader = XML_INPUT_FACTORY.createXMLStreamReader(sr);

        while (xmlStreamReader.hasNext()) {
          int event = xmlStreamReader.next();

          if (event == XMLStreamConstants.CHARACTERS || event == XMLStreamConstants.CDATA) {

            String text = xmlStreamReader.getText();

            if (StringUtils.isNotBlank(text)) {
              builder.append(" ").append(text.trim());
            }
          }
          if (event == XMLStreamConstants.START_ELEMENT) {
            for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {

              String text = xmlStreamReader.getAttributeValue(i);

              if (StringUtils.isNotBlank(text)) {
                builder.append(" ").append(text.trim());
              }
            }
          }
        }
        parsedTexts.add(builder.toString());
        builder.setLength(0);
      }
    } catch (XMLStreamException e1) {
      LOGGER.info(
          "Failure occurred in parsing the xml data. No data has been stored or indexed.", e1);
    } finally {
      IOUtils.closeQuietly(sr);
      if (xmlStreamReader != null) {
        try {
          xmlStreamReader.close();
        } catch (XMLStreamException e) {
          LOGGER.debug("Exception closing XMLStreamReader", e);
        }
      }
    }
    long endTime = System.currentTimeMillis();

    LOGGER.debug("Parsing took {} ms", endTime - starttime);

    return parsedTexts;
  }

  private Set<AttributeDescriptor> convertAttributeDescriptors(
      Set<AttributeDescriptor> attributeDescriptors) {
    Set<AttributeDescriptor> newAttributeDescriptors = new HashSet<>(attributeDescriptors.size());

    for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
      String name = attributeDescriptor.getName();
      boolean isIndexed = attributeDescriptor.isIndexed();
      boolean isStored = attributeDescriptor.isStored();
      boolean isTokenized = attributeDescriptor.isTokenized();
      boolean isMultiValued = attributeDescriptor.isMultiValued();
      AttributeType<?> attributeType = attributeDescriptor.getType();
      newAttributeDescriptors.add(
          new AttributeDescriptorImpl(
              name, isIndexed, isStored, isTokenized, isMultiValued, attributeType));
    }

    return newAttributeDescriptors;
  }

  String getSortKey(String field) {
    if (field.endsWith(SchemaFields.GEO_SUFFIX)) {
      field = field + SchemaFields.SORT_SUFFIX;
    }
    return field;
  }

  Stream<String> anyTextFields() {
    return anyTextFieldsCache.stream();
  }

  /**
   * Get the metadata size limit from custom.system.properties. Defaults to {@link #FIVE_MEGABYTES}.
   *
   * @return Integer metadata size limit in bytes
   */
  @VisibleForTesting
  static Integer getMetadataSizeLimit() {
    String sizeLimit =
        AccessController.doPrivileged(
            (PrivilegedAction<String>)
                () -> System.getProperty(METADATA_SIZE_LIMIT, String.valueOf(FIVE_MEGABYTES)));
    if (StringUtils.isNumeric(sizeLimit)) {
      try {
        return Integer.parseInt(sizeLimit);
      } catch (NumberFormatException e) {
        LOGGER.info("Metadata size limit set in system properties is out of range: {}", sizeLimit);
      }
    } else {
      LOGGER.info("User set metadata size limit is not numeric: {}", sizeLimit);
    }
    return FIVE_MEGABYTES;
  }
}
