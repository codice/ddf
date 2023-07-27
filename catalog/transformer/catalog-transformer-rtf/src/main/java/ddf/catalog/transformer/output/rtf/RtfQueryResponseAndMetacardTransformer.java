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
package ddf.catalog.transformer.output.rtf;

import static com.tutego.jrtf.RtfHeader.font;

import com.tutego.jrtf.Rtf;
import com.tutego.jrtf.RtfHeaderFont;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transform.QueryResponseTransformer;
import ddf.catalog.transformer.output.rtf.model.ExportCategory;
import ddf.catalog.transformer.output.rtf.model.RtfCategory;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.apache.commons.lang.StringUtils;

public class RtfQueryResponseAndMetacardTransformer
    implements MetacardTransformer, QueryResponseTransformer {

  private final MimeType mimeType;

  private final List<RtfCategory> defaultCategories;

  public static final String COLUMN_ORDER_KEY = "columnOrder";

  public static final String COLUMN_ALIAS_KEY = "aliases";

  public RtfQueryResponseAndMetacardTransformer(List<RtfCategory> categories)
      throws MimeTypeParseException {
    this.defaultCategories = categories;
    this.mimeType = new MimeType("application/rtf");
  }

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    if (metacard == null) {
      throw new CatalogTransformerException("Null metacard cannot be transformed into RTF");
    }

    String aliasesArg = (String) arguments.getOrDefault("aliases", new String());
    Map<String, String> aliases =
        (StringUtils.isNotBlank(aliasesArg))
            ? Arrays.stream(aliasesArg.split(","))
                .map(s -> s.split("="))
                .collect(Collectors.toMap(k -> k[0], k -> k[1]))
            : Collections.EMPTY_MAP;
    String attributeString =
        arguments.get(COLUMN_ORDER_KEY) != null ? (String) arguments.get(COLUMN_ORDER_KEY) : "";
    List<String> attributes =
        Arrays.asList((attributeString).split(",")).stream()
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toList());
    final List<RtfCategory> categories =
        attributes.isEmpty() ? defaultCategories : createCategory("Details", attributes, aliases);

    ByteArrayInputStream rtfStream =
        Optional.of(metacard)
            .map(m -> toRtf(metacard, categories))
            .map(this::rtfStringToInputStream)
            .orElseThrow(this::emptyRtfException);

    return new BinaryContentImpl(rtfStream, mimeType);
  }

  @Override
  public BinaryContent transform(
      SourceResponse upstreamResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    if (upstreamResponse == null) {
      throw new CatalogTransformerException("Null result set cannot be transformed to RTF");
    }

    List<String> attributeOrder =
        Optional.ofNullable((List<String>) arguments.get(COLUMN_ORDER_KEY))
            .orElse(Collections.emptyList());

    Map<String, String> columnAliasMap =
        Optional.ofNullable((Map<String, String>) arguments.get(COLUMN_ALIAS_KEY))
            .orElse(Collections.emptyMap());

    final List<RtfCategory> categories =
        attributeOrder.isEmpty()
            ? defaultCategories
            : createCategory("Details", attributeOrder, columnAliasMap);

    Rtf doc = createRtfDoc();

    upstreamResponse.getResults().stream()
        .map(Result::getMetacard)
        .forEach(metacard -> toRtf(doc, metacard, categories));

    ByteArrayInputStream templateStream =
        Optional.ofNullable(doc)
            .map(Rtf::out)
            .map(Objects::toString)
            .map(this::rtfStringToInputStream)
            .orElseThrow(this::emptyRtfException);

    return new BinaryContentImpl(templateStream, mimeType);
  }

  private CatalogTransformerException emptyRtfException() {
    return new CatalogTransformerException("RTF template is empty, nothing to export");
  }

  private ByteArrayInputStream rtfStringToInputStream(String rtf) {
    return new ByteArrayInputStream(rtf.getBytes(StandardCharsets.UTF_8));
  }

  private Rtf createRtfDoc() {
    Rtf doc = Rtf.rtf();
    doc.header(
        font(RtfHeaderFont.ARIAL).family(RtfHeaderFont.FontFamily.MODERN).at(0),
        font(RtfHeaderFont.TIMES_ROMAN).at(1));

    return doc;
  }

  private String toRtf(Metacard metacard, List<RtfCategory> categories) {
    return Optional.ofNullable(toRtf(createRtfDoc(), metacard, categories))
        .map(Rtf::out)
        .map(Object::toString)
        .orElse("");
  }

  private Rtf toRtf(Rtf doc, Metacard metacard, List<RtfCategory> categories) {
    return new RtfTemplate.Builder()
        .withCategories(categories)
        .withMetacard(metacard)
        .build()
        .rtf(doc);
  }

  private List<RtfCategory> createCategory(
      String title, List<String> attributes, Map<String, String> aliases) {
    ExportCategory dynamicCategory = new ExportCategory();
    dynamicCategory.setTitle(title);
    dynamicCategory.setAttributes(attributes);
    dynamicCategory.setAliases(aliases);
    return Collections.singletonList(dynamicCategory);
  }
}
