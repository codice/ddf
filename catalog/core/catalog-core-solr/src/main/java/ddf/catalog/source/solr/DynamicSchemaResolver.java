/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.source.solr;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.codehaus.stax2.XMLInputFactory2;
import org.codice.solr.factory.ConfigurationStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardCreationException;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import lux.Config;
import lux.xml.SaxonDocBuilder;
import lux.xml.XmlReader;
import lux.xml.tinybin.TinyBinary;
import net.sf.saxon.s9api.Processor;
import net.sf.saxon.s9api.SaxonApiException;
import net.sf.saxon.s9api.XdmNode;
import net.sf.saxon.tree.tiny.TinyDocumentImpl;
import net.sf.saxon.tree.tiny.TinyTree;

/**
 * This class tries to resolve all user given field names to their corresponding dynamic Solr index
 * field name. This class takes most of its logic directly from the configured Solr schema.xml. For
 * instance, the suffixes enumerated in this class are directly copied from the schema.xml.
 *
 * @since 0.2.0
 */
public class DynamicSchemaResolver {

    protected static final char FIRST_CHAR_OF_SUFFIX = '_';

    protected static final String COULD_NOT_READ_METACARD_TYPE_MESSAGE =
            "Could not read MetacardType.";

    protected static final String FIELDS_KEY = "fields";

    protected static final String COULD_NOT_SERIALIZE_OBJECT_MESSAGE = "Could not serialize object";

    protected static final XMLInputFactory XML_INPUT_FACTORY;

    private static final String SOLR_CLOUD_VERSION_FIELD = "_version_";

    public static final String LUX_XML_FIELD_NAME = "lux_xml";

    public static final String SCORE_FIELD_NAME = "score";

    private static final List<String> PRIVATE_SOLR_FIELDS = Arrays.asList(SOLR_CLOUD_VERSION_FIELD,
            SchemaFields.METACARD_TYPE_FIELD_NAME,
            SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME,
            LUX_XML_FIELD_NAME,
            SCORE_FIELD_NAME);

    private static final Logger LOGGER = LoggerFactory.getLogger(DynamicSchemaResolver.class);

    static {
        ClassLoader tccl = Thread.currentThread()
                .getContextClassLoader();
        try {
            Thread.currentThread()
                    .setContextClassLoader(DynamicSchemaResolver.class.getClassLoader());

            XML_INPUT_FACTORY = XMLInputFactory2.newInstance();
            XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,
                    Boolean.FALSE);
            XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES,
                    Boolean.FALSE);
            XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE);
            XML_INPUT_FACTORY.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
        } finally {
            Thread.currentThread()
                    .setContextClassLoader(tccl);
        }
    }

    protected Set<String> fieldsCache = new HashSet<>();

    protected Set<String> anyTextFieldsCache = new HashSet<>();

    protected SchemaFields schemaFields;

    protected Map<String, MetacardType> metacardTypesCache = new HashMap<>();

    protected Map<String, byte[]> metacardTypeNameToSerialCache = new HashMap<>();

    private Processor processor = new Processor(new Config());

    public DynamicSchemaResolver() {
        this.schemaFields = new SchemaFields();

        fieldsCache.add(Metacard.ID + SchemaFields.TEXT_SUFFIX);
        fieldsCache.add(Metacard.ID + SchemaFields.TEXT_SUFFIX + SchemaFields.TOKENIZED);
        fieldsCache.add(Metacard.ID + SchemaFields.TEXT_SUFFIX + SchemaFields.TOKENIZED
                + SchemaFields.HAS_CASE);
        fieldsCache.add(Metacard.TAGS + SchemaFields.TEXT_SUFFIX);

        anyTextFieldsCache.add(Metacard.METADATA + SchemaFields.TEXT_SUFFIX);
        Set<String> basicTextAttributes = BasicTypes.BASIC_METACARD.getAttributeDescriptors()
                .stream()
                .filter(descriptor -> BasicTypes.STRING_TYPE.equals(descriptor.getType()))
                .map(stringDescriptor -> stringDescriptor.getName() + SchemaFields.TEXT_SUFFIX)
                .collect(Collectors.toSet());
        anyTextFieldsCache.addAll(basicTextAttributes);
    }

    /**
     * Adds the fields that are already in Solr to the cache. This method should be called
     * once the SolrClient is up to ensure the cache is synchronized with Solr.
     *
     * @param client
     *            the SolrClient we are working with
     */
    public void addFieldsFromClient(SolrClient client) {
        if (client == null) {
            LOGGER.warn("Solr client is null, could not add fields to cache.");
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

        try {
            QueryResponse response = client.query(query);
            for (Entry<String, ?> e : ((SimpleOrderedMap<?>) (response.getResponse()
                    .get(FIELDS_KEY)))) {
                fieldsCache.add(e.getKey());
                if (e.getKey().endsWith(SchemaFields.TEXT_SUFFIX)) {
                    anyTextFieldsCache.add(e.getKey());
                }
            }
        } catch (SolrServerException | SolrException | IOException e) {
            LOGGER.warn("Could not update cache for field names.", e);
        }
    }

    /**
     * Adds the fields of the Metacard into the {@link SolrInputDocument}
     */
    public void addFields(Metacard metacard, SolrInputDocument solrInputDocument)
            throws MetacardCreationException {
        MetacardType schema = metacard.getMetacardType();

        // TODO: register these metacard types when a new one is seen

        for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
            if (metacard.getAttribute(ad.getName()) != null) {
                List<Serializable> attributeValues = metacard.getAttribute(ad.getName())
                        .getValues();

                if (attributeValues != null && attributeValues.size() > 0
                        && attributeValues.get(0) != null) {
                    AttributeFormat format = ad.getType()
                            .getAttributeFormat();
                    String formatIndexName = ad.getName() + getFieldSuffix(format);

                    if (AttributeFormat.XML.equals(format)) {
                        List<String> parsedTexts = parseTextFrom(attributeValues);

                        // text => metadata_txt_ws
                        String whitespaceTokenizedIndexName = ad.getName() + getFieldSuffix(
                                AttributeFormat.STRING) + SchemaFields.WHITESPACE_TEXT_SUFFIX;
                        solrInputDocument.addField(whitespaceTokenizedIndexName, parsedTexts);

                        // text => metadata_txt_ws_has_case
                        String whiteSpaceTokenizedHasCaseIndexName = ad.getName() + getFieldSuffix(
                                AttributeFormat.STRING) + SchemaFields.WHITESPACE_TEXT_SUFFIX
                                + SchemaFields.HAS_CASE;
                        solrInputDocument.addField(whiteSpaceTokenizedHasCaseIndexName,
                                parsedTexts);

                        // text => metadata_txt_tokenized
                        String specialStringIndexName = ad.getName() + getFieldSuffix(
                                AttributeFormat.STRING)
                                + getSpecialIndexSuffix(AttributeFormat.STRING);
                        solrInputDocument.addField(specialStringIndexName, parsedTexts);

                        // text case sensitive
                        solrInputDocument.addField(specialStringIndexName + SchemaFields.HAS_CASE,
                                parsedTexts);
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
                            LOGGER.warn(COULD_NOT_SERIALIZE_OBJECT_MESSAGE, e);
                            throw new MetacardCreationException(COULD_NOT_SERIALIZE_OBJECT_MESSAGE);
                        }

                        attributeValues = byteArrays;
                    }
                    solrInputDocument.addField(formatIndexName, attributeValues);
                    if (!(formatIndexName.endsWith(SchemaFields.BINARY_SUFFIX)
                            || formatIndexName.endsWith(SchemaFields.OBJECT_SUFFIX))) {
                        solrInputDocument.addField(formatIndexName + SchemaFields.SORT_KEY_SUFFIX,
                                attributeValues.get(0));
                    }
                }
            }
        }

        if (!ConfigurationStore.getInstance()
                .isDisableTextPath()) {
            if (StringUtils.isNotBlank(metacard.getMetadata())) {
                try {
                    byte[] luxXml = createTinyBinary(metacard.getMetadata());
                    solrInputDocument.addField(LUX_XML_FIELD_NAME, luxXml);
                } catch (XMLStreamException | SaxonApiException e) {
                    LOGGER.warn("Unable to parse metadata field.  XPath support unavailable for metacard " + metacard.getId());
                }
            }
        }

        /*
         * Lastly the metacardType must be added to the solr document. These are internal fields
         */
        solrInputDocument.addField(SchemaFields.METACARD_TYPE_FIELD_NAME, schema.getName());
        byte[] metacardTypeBytes = metacardTypeNameToSerialCache.get(schema.getName());

        if (metacardTypeBytes == null) {
            MetacardType coreMetacardType = new MetacardTypeImpl(schema.getName(),
                    convertAttributeDescriptors(schema.getAttributeDescriptors()));

            metacardTypesCache.put(schema.getName(), coreMetacardType);

            metacardTypeBytes = serialize(coreMetacardType);
            metacardTypeNameToSerialCache.put(schema.getName(), metacardTypeBytes);

            addToFieldsCache(coreMetacardType.getAttributeDescriptors());
        }

        solrInputDocument.addField(SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME, metacardTypeBytes);
    }

    private byte[] createTinyBinary(String xml) throws XMLStreamException, SaxonApiException {
        SaxonDocBuilder builder = new SaxonDocBuilder(processor);

        XmlReader xmlReader = new XmlReader();
        xmlReader.addHandler(builder);
        xmlReader.setStripNamespaces(true);
        xmlReader.read(IOUtils.toInputStream(xml));

        XdmNode node = builder.getDocument();

        TinyTree tinyTree = ((TinyDocumentImpl) node.getUnderlyingNode()).getTree();
        TinyBinary tinyBinary = new TinyBinary(tinyTree, StandardCharsets.UTF_8);

        return tinyBinary.getBytes();
    }

    /**
     * Returns the best approximation as to what {@link AttributeFormat} this Solr Field is.
     *
     * @param solrFieldName
     *            name of the Solr field
     * @return the {@link AttributeFormat} associated with the Solr field
     */
    public AttributeFormat getType(String solrFieldName) {

        String suffix = "";
        int lastIndexOfUndercore = solrFieldName.lastIndexOf(FIRST_CHAR_OF_SUFFIX);

        if (lastIndexOfUndercore != -1) {
            suffix = solrFieldName.substring(lastIndexOfUndercore, solrFieldName.length());
        }

        return schemaFields.getFormat(suffix);
    }

    public List<Serializable> getDocValues(String solrFieldName, Collection<Object> docValues) {
        ArrayList<Serializable> values = new ArrayList<>();
        Iterator<Object> iterator = docValues.iterator();
        while (iterator.hasNext()) {
            values.add(getDocValue(solrFieldName, iterator.next()));
        }
        return values;
    }

    public Serializable getDocValue(String solrFieldName, Object docValue) {

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
                LOGGER.warn("IO exception loading input document", e);
            } catch (ClassNotFoundException e) {
                LOGGER.warn("Could not create object to return.", e);
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
     * The convention is that we add a suffix starting with an underscore, so if we find the last
     * underscore, then we can return the original field name.
     *
     * @param solrFieldName Solr index field name
     * @return the original field name
     */
    public String resolveFieldName(String solrFieldName) {
        int lastIndexOfUndercore = solrFieldName.lastIndexOf(FIRST_CHAR_OF_SUFFIX);

        if (lastIndexOfUndercore != -1) {
            return solrFieldName.substring(0, lastIndexOfUndercore);
        }
        return solrFieldName;
    }

    public boolean isPrivateField(String solrFieldName) {
        return PRIVATE_SOLR_FIELDS.contains(solrFieldName)
                || solrFieldName.endsWith(SchemaFields.SORT_KEY_SUFFIX);
    }

    /**
     * Attempts to resolve the name of a field without being given an {@link AttributeFormat}
     *
     * @param field
     *            user given field name
     * @return a list of possible Solr field names that match the given field. If none are found,
     *         then an empty list is returned
     */
    public List<String> getAnonymousField(String field) {
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
     * @param propertyName
     *            property name provided by user
     * @param format
     *            {@link AttributeFormat} that describes the type of {@link ddf.catalog.data.Attribute} the field is
     * @param isSearchedAsExactValue
     *            specifies if any special index suffixes need to be added to the field
     * @return the proper schema field name. If a schema name cannot be found in cache, returns a
     *         schema field name that matches the dynamic field type formatting.
     */
    public String getField(String propertyName, AttributeFormat format,
            boolean isSearchedAsExactValue) {

        if (Metacard.ANY_GEO.equals(propertyName)) {
            return Metacard.GEOGRAPHY + "_geo_index";
        }

        String fieldName = propertyName + schemaFields.getFieldSuffix(format) + (
                isSearchedAsExactValue ?
                        "" :
                        getSpecialIndexSuffix(format));

        if (fieldsCache.contains(fieldName)) {
            return fieldName;
        }

        switch (format) {
        case DOUBLE:
        case LONG:
        case INTEGER:
        case SHORT:
        case FLOAT:
            return findAnyMatchingNumericalField(propertyName);
        default:
            break;
        }

        LOGGER.debug(
                "Could not find exact schema field name for [{}], attempting to search with [{}]",
                propertyName,
                fieldName);

        return fieldName;
    }

    public String getFieldSuffix(AttributeFormat format) {
        return schemaFields.getFieldSuffix(format);
    }

    public MetacardType getMetacardType(SolrDocument doc) throws MetacardCreationException {
        String mTypeFieldName = doc.getFirstValue(SchemaFields.METACARD_TYPE_FIELD_NAME)
                .toString();

        MetacardType cachedMetacardType = metacardTypesCache.get(mTypeFieldName);

        if (cachedMetacardType != null) {
            return cachedMetacardType;
        }

        byte[] bytes = (byte[]) doc.getFirstValue(SchemaFields.METACARD_TYPE_OBJECT_FIELD_NAME);

        ByteArrayInputStream bais = null;
        ObjectInputStream in = null;
        try {

            bais = new ByteArrayInputStream(bytes);

            in = new ObjectInputStream(bais);

            cachedMetacardType = (MetacardType) in.readObject();

        } catch (IOException e) {

            LOGGER.warn("IO exception loading cached metacard type", e);

            throw new MetacardCreationException(COULD_NOT_READ_METACARD_TYPE_MESSAGE);
        } catch (ClassNotFoundException e) {
            LOGGER.warn("Class exception loading cached metacard type", e);

            throw new MetacardCreationException(COULD_NOT_READ_METACARD_TYPE_MESSAGE);
        } finally {
            IOUtils.closeQuietly(bais);
            IOUtils.closeQuietly(in);
        }

        metacardTypeNameToSerialCache.put(mTypeFieldName, bytes);
        metacardTypesCache.put(mTypeFieldName, cachedMetacardType);
        addToFieldsCache(cachedMetacardType.getAttributeDescriptors());
        return cachedMetacardType;
    }

    public String getCaseSensitiveField(String mappedPropertyName) {
        // TODO We can check if this field really does exist
        return mappedPropertyName + SchemaFields.HAS_CASE;
    }

    public String getWhitespaceTokenizedField(String mappedPropertyName) {
        return mappedPropertyName + SchemaFields.WHITESPACE_TEXT_SUFFIX;
    }

    protected String getSpecialIndexSuffix(AttributeFormat format) {

        switch (format) {
        case STRING:
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

    private void addToFieldsCache(Set<AttributeDescriptor> descriptors) {
        for (AttributeDescriptor ad : descriptors) {

            AttributeFormat format = ad.getType()
                    .getAttributeFormat();

            fieldsCache.add(ad.getName() + schemaFields.getFieldSuffix(format));

            if (!getSpecialIndexSuffix(format).equals("")) {
                fieldsCache.add(
                        ad.getName() + schemaFields.getFieldSuffix(format) + getSpecialIndexSuffix(
                                format));
            }

            if (format.equals(AttributeFormat.STRING)) {
                fieldsCache.add(
                        ad.getName() + schemaFields.getFieldSuffix(format) + getSpecialIndexSuffix(
                                format) + SchemaFields.HAS_CASE);
                anyTextFieldsCache.add(ad.getName() + schemaFields.getFieldSuffix(format));
            }

            if (format.equals(AttributeFormat.XML)) {
                fieldsCache.add(ad.getName() + SchemaFields.TEXT_SUFFIX + SchemaFields.TOKENIZED);
                fieldsCache.add(ad.getName() + SchemaFields.TEXT_SUFFIX + SchemaFields.TOKENIZED
                        + SchemaFields.HAS_CASE);
                fieldsCache.add(
                        ad.getName() + schemaFields.getFieldSuffix(format) + getSpecialIndexSuffix(
                                format));
            }
        }
    }

    private byte[] serialize(MetacardType anywhereMType) throws MetacardCreationException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            ObjectOutputStream out = new ObjectOutputStream(baos);
            out.writeObject(anywhereMType);

            byte[] bytes = baos.toByteArray();

            baos.close();
            out.close();

            return bytes;
        } catch (IOException e) {
            LOGGER.warn("IO exception reading metacard type message", e);
            throw new MetacardCreationException(COULD_NOT_READ_METACARD_TYPE_MESSAGE);
        }

    }

    private String findAnyMatchingNumericalField(String propertyName) {

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
                "Did not find any numerical schema fields for property [{}]. Replacing with property [{}]",
                propertyName,
                propertyName + SchemaFields.INTEGER_SUFFIX);
        return propertyName + SchemaFields.INTEGER_SUFFIX;
    }

    /**
     * Given xml as a string, this method will parse out element text and CDATA text. It separates
     * each by one space character.
     *
     * @param xmlDatas List of XML as {@code String}
     * @return parsed CDATA and element text
     */
    protected List<String> parseTextFrom(List<Serializable> xmlDatas) {

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

                    if (event == XMLStreamConstants.CHARACTERS
                            || event == XMLStreamConstants.CDATA) {

                        String text = xmlStreamReader.getText();

                        if (StringUtils.isNotBlank(text)) {
                            builder.append(" ")
                                    .append(text.trim());
                        }

                    }
                    if (event == XMLStreamConstants.START_ELEMENT) {
                        for (int i = 0; i < xmlStreamReader.getAttributeCount(); i++) {

                            String text = xmlStreamReader.getAttributeValue(i);

                            if (StringUtils.isNotBlank(text)) {
                                builder.append(" ")
                                        .append(text.trim());
                            }
                        }
                    }
                }
                parsedTexts.add(builder.toString());
                builder.setLength(0);
            }
        } catch (XMLStreamException e1) {
            LOGGER.warn(
                    "Failure occurred in parsing the xml data. No data has been stored or indexed.",
                    e1);
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
        Set<AttributeDescriptor> newAttributeDescriptors =
                new HashSet<>(attributeDescriptors.size());

        for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
            String name = attributeDescriptor.getName();
            boolean isIndexed = attributeDescriptor.isIndexed();
            boolean isStored = attributeDescriptor.isStored();
            boolean isTokenized = attributeDescriptor.isTokenized();
            boolean isMultiValued = attributeDescriptor.isMultiValued();
            AttributeType<?> attributeType = attributeDescriptor.getType();
            newAttributeDescriptors.add(new AttributeDescriptorImpl(name,
                    isIndexed,
                    isStored,
                    isTokenized,
                    isMultiValued,
                    attributeType));
        }

        return newAttributeDescriptors;
    }

    public String getSortKey(String field) {
        if (!(field.endsWith(SchemaFields.SORT_KEY_SUFFIX) || Result.DISTANCE.equals(field)
                || Result.RELEVANCE.equals(field) || Result.TEMPORAL.equals(field))) {
            field = field + SchemaFields.SORT_KEY_SUFFIX;
        }
        return field;
    }

    public Stream<String> anyTextFields() {
        return anyTextFieldsCache.stream();
    }
}
