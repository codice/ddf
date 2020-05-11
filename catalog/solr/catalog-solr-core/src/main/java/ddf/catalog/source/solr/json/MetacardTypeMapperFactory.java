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
package ddf.catalog.source.solr.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class MetacardTypeMapperFactory {

  private static final String NAME = "name";
  private static final String ATTRIBUTE_DESCRIPTORS = "attributeDescriptors";
  private static final String INDEXED = "indexed";
  private static final String STORED = "stored";
  private static final String TOKENIZED = "tokenized";
  private static final String MULTI_VALUED = "multiValued";
  private static final String ATTRIBUTE_FORMAT = "attributeFormat";
  private static final String BINDING = "binding";

  private MetacardTypeMapperFactory() {}

  public static ObjectMapper newObjectMapper() {
    SimpleModule module = new SimpleModule();
    module.addSerializer(MetacardType.class, new MetacardTypeSerializer());
    module.addDeserializer(MetacardType.class, new MetacardTypeDeserializer());

    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModule(module);
    return mapper;
  }

  private static class MetacardTypeSerializer extends StdSerializer<MetacardType> {

    MetacardTypeSerializer() {
      super(MetacardType.class);
    }

    @Override
    public void serialize(MetacardType value, JsonGenerator gen, SerializerProvider provider)
        throws IOException {
      gen.writeStartObject();
      gen.writeStringField(NAME, value.getName());
      gen.writeArrayFieldStart(ATTRIBUTE_DESCRIPTORS);
      for (AttributeDescriptor attributeDescriptor : value.getAttributeDescriptors()) {
        gen.writeStartObject();
        gen.writeStringField(NAME, attributeDescriptor.getName());
        gen.writeBooleanField(INDEXED, attributeDescriptor.isIndexed());
        gen.writeBooleanField(STORED, attributeDescriptor.isStored());
        gen.writeBooleanField(TOKENIZED, attributeDescriptor.isTokenized());
        gen.writeBooleanField(MULTI_VALUED, attributeDescriptor.isMultiValued());
        gen.writeStringField(
            ATTRIBUTE_FORMAT, attributeDescriptor.getType().getAttributeFormat().name());
        gen.writeStringField(BINDING, attributeDescriptor.getType().getBinding().getName());
        gen.writeEndObject();
      }
      gen.writeEndArray();
      gen.writeEndObject();
    }
  }

  private static class MetacardTypeDeserializer extends StdDeserializer<MetacardType> {

    MetacardTypeDeserializer() {
      super(MetacardType.class);
    }

    @Override
    public MetacardType deserialize(
        JsonParser jsonParser, DeserializationContext deserializationContext)
        throws IOException, JsonProcessingException {
      JsonNode node = jsonParser.getCodec().readTree(jsonParser);

      String name = node.get(NAME).textValue();

      Set<AttributeDescriptor> attributeDescriptors = new HashSet<>();

      Iterator<JsonNode> i = node.get(ATTRIBUTE_DESCRIPTORS).elements();
      while (i.hasNext()) {
        JsonNode adNode = i.next();
        String attributeName = adNode.get(NAME).textValue();
        boolean indexed = adNode.get(INDEXED).booleanValue();
        boolean stored = adNode.get(STORED).booleanValue();
        boolean tokenized = adNode.get(TOKENIZED).booleanValue();
        boolean multiValued = adNode.get(MULTI_VALUED).booleanValue();
        String attributeFormat = adNode.get(ATTRIBUTE_FORMAT).textValue();

        AttributeType attributeType = BasicTypes.getAttributeType(attributeFormat);

        attributeDescriptors.add(
            new AttributeDescriptorImpl(
                attributeName, indexed, stored, tokenized, multiValued, attributeType));
      }

      return new MetacardTypeImpl(name, attributeDescriptors);
    }
  }
}
