/*
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

package ddf.catalog.transformer.csv;

import static ddf.catalog.data.impl.BasicTypes.BINARY_TYPE;
import static ddf.catalog.data.impl.BasicTypes.DOUBLE_TYPE;
import static ddf.catalog.data.impl.BasicTypes.INTEGER_TYPE;
import static ddf.catalog.data.impl.BasicTypes.OBJECT_TYPE;
import static ddf.catalog.data.impl.BasicTypes.STRING_TYPE;
import static ddf.catalog.transformer.csv.CsvTransformerSupport.COLUMN_ORDER_KEY;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class CsvMetacardTransformerTest {

  private final CsvMetacardTransformer transformer = new CsvMetacardTransformer();

  private static final String METACARD_TYPE_NAME = "test-type";

  @Test(expected = CatalogTransformerException.class)
  public void testTransformerWithNullMetacard() throws CatalogTransformerException {
    transformer.transform(null, new HashMap<>());
  }

  @Test
  public void testCsvMetacardTransformerNoArguments()
      throws CatalogTransformerException, IOException {
    String stringAtt = "stringAtt";
    String intAtt = "intAtt";
    String doubleAtt = "doubleAtt";
    Metacard metacard =
        buildMetacard(
            Sets.newHashSet(
                buildAttributeDescriptor(stringAtt, STRING_TYPE),
                buildAttributeDescriptor(intAtt, INTEGER_TYPE),
                buildAttributeDescriptor(doubleAtt, DOUBLE_TYPE)),
            new AttributeImpl(stringAtt, "stringVal"),
            new AttributeImpl(intAtt, 101),
            new AttributeImpl(doubleAtt, 3.14159));
    BinaryContent binaryContent = transformer.transform(metacard, new HashMap<>());
    assertThat(binaryContent.getMimeType().toString(), is("text/csv"));

    List<String> lines = Arrays.asList(new String(binaryContent.getByteArray()).split("\r\n"));
    assertThat("The CSV didn't contain exactly two lines.", lines, hasSize(2));

    List<String> attNames = Arrays.asList(lines.get(0).split(","));
    List<String> attValues = Arrays.asList(lines.get(1).split(","));
    assertThat(
        "The headers and values had different lengths.", attNames, hasSize(attValues.size()));

    final Map<String, String> headersToValues = new HashMap<>();
    for (int i = 0; i < attNames.size(); ++i) {
      headersToValues.put(attNames.get(i), attValues.get(i));
    }
    assertThat(
        headersToValues,
        allOf(
            hasEntry(stringAtt, "stringVal"),
            hasEntry(intAtt, "101"),
            hasEntry(doubleAtt, "3.14159")));
  }

  @Test
  public void testCsvMetacardTransformerSupportsColumnOrder()
      throws CatalogTransformerException, IOException {
    String stringAtt = "stringAtt";
    String intAtt = "intAtt";
    String doubleAtt = "doubleAtt";
    Metacard metacard =
        buildMetacard(
            Sets.newHashSet(
                buildAttributeDescriptor(stringAtt, STRING_TYPE),
                buildAttributeDescriptor(intAtt, INTEGER_TYPE),
                buildAttributeDescriptor(doubleAtt, DOUBLE_TYPE)),
            new AttributeImpl(stringAtt, "stringVal"),
            new AttributeImpl(intAtt, 101),
            new AttributeImpl(doubleAtt, 3.14159));
    BinaryContent binaryContent =
        transformer.transform(
            metacard, singletonMap(COLUMN_ORDER_KEY, "doubleAtt,stringAtt,intAtt"));
    assertThat(binaryContent.getMimeType().toString(), is("text/csv"));

    List<String> lines = Arrays.asList(new String(binaryContent.getByteArray()).split("\r\n"));
    assertThat("The CSV didn't contain exactly two lines.", lines, hasSize(2));

    List<String> attNames = Arrays.asList(lines.get(0).split(","));
    List<String> attValues = Arrays.asList(lines.get(1).split(","));

    assertThat(attNames, contains(doubleAtt, stringAtt, intAtt));
    assertThat(attValues, contains("3.14159", "stringVal", "101"));
  }

  @Test
  public void testFieldsNotRequestedAreExcluded() throws CatalogTransformerException, IOException {
    String stringAtt = "stringAtt";
    String intAtt = "intAtt";
    String doubleAtt = "doubleAtt";
    Metacard metacard =
        buildMetacard(
            Sets.newHashSet(
                buildAttributeDescriptor(stringAtt, STRING_TYPE),
                buildAttributeDescriptor(intAtt, INTEGER_TYPE),
                buildAttributeDescriptor(doubleAtt, DOUBLE_TYPE)),
            new AttributeImpl(stringAtt, "stringVal"),
            new AttributeImpl(intAtt, 101),
            new AttributeImpl(doubleAtt, 3.14159));
    BinaryContent binaryContent =
        transformer.transform(
            metacard, singletonMap(COLUMN_ORDER_KEY, Lists.newArrayList(stringAtt)));
    assertThat(binaryContent.getMimeType().toString(), is("text/csv"));

    List<String> lines = Arrays.asList(new String(binaryContent.getByteArray()).split("\r\n"));
    assertThat("The CSV didn't contain exactly two lines.", lines, hasSize(2));

    List<String> attNames = Arrays.asList(lines.get(0).split(","));
    List<String> attValues = Arrays.asList(lines.get(1).split(","));

    assertThat(attNames, contains(stringAtt));
    assertThat(attValues, contains("stringVal"));
  }

  @Test
  public void testBinaryAndObjectFieldsAreExcluded()
      throws CatalogTransformerException, IOException {
    String stringAtt = "stringAtt";
    String binaryAtt = "binaryAtt";
    String objectAtt = "objectAtt";
    Metacard metacard =
        buildMetacard(
            Sets.newHashSet(
                buildAttributeDescriptor(stringAtt, STRING_TYPE),
                buildAttributeDescriptor(binaryAtt, BINARY_TYPE),
                buildAttributeDescriptor(objectAtt, OBJECT_TYPE)),
            new AttributeImpl(stringAtt, "stringVal"),
            new AttributeImpl(binaryAtt, new byte[] {1, 0, 1}),
            new AttributeImpl(objectAtt, new HashMap<>()));
    BinaryContent binaryContent = transformer.transform(metacard, new HashMap<>());
    assertThat(binaryContent.getMimeType().toString(), is("text/csv"));

    List<String> lines = Arrays.asList(new String(binaryContent.getByteArray()).split("\r\n"));
    assertThat("The CSV didn't contain exactly two lines.", lines, hasSize(2));

    List<String> attNames = Arrays.asList(lines.get(0).split(","));
    List<String> attValues = Arrays.asList(lines.get(1).split(","));

    assertThat(attNames, containsInAnyOrder(stringAtt, MetacardType.METACARD_TYPE));
    assertThat(attValues, hasSize(2));

    int stringAttIndex = attNames.indexOf(stringAtt);
    int typeIndex = attNames.indexOf(MetacardType.METACARD_TYPE);
    assertThat(attValues.get(stringAttIndex), is("stringVal"));
    assertThat(attValues.get(typeIndex), is(metacard.getMetacardType().getName()));
  }

  private static AttributeDescriptor buildAttributeDescriptor(String name, AttributeType<?> type) {
    return new AttributeDescriptorImpl(name, true, true, true, true, type);
  }

  private Metacard buildMetacard(
      final Set<AttributeDescriptor> attributeDescriptors, final Attribute... attributes) {
    MetacardType metacardType = new MetacardTypeImpl(METACARD_TYPE_NAME, attributeDescriptors);
    Metacard metacard = new MetacardImpl(metacardType);
    for (Attribute attribute : attributes) {
      metacard.setAttribute(attribute);
    }
    return metacard;
  }
}
