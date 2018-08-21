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
import ddf.catalog.transformer.output.rtf.model.RtfCategory;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;

public class RtfQueryResponseAndMetacardTransformer
    implements MetacardTransformer, QueryResponseTransformer {

  private static final String MIME_TYPE = "application/rtf";

  private final List<RtfCategory> categories;

  public RtfQueryResponseAndMetacardTransformer(List<RtfCategory> categories) {
    this.categories = categories;
  }

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    if (metacard == null) {
      throw new CatalogTransformerException("Null metacard cannot be transformed into RTF");
    }

    ByteArrayInputStream rtfStream =
        Optional.of(metacard).map(this::toRtf).map(this::rtfStringToInputStream).orElse(null);

    return createAndReturn(rtfStream);
  }

  @Override
  public BinaryContent transform(
      SourceResponse upstreamResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    if (upstreamResponse == null) {
      throw new CatalogTransformerException("Null result set cannot be transformed to RTF");
    }

    Rtf doc = createRtfDoc();

    upstreamResponse
        .getResults()
        .stream()
        .map(Result::getMetacard)
        .forEach(metacard -> toRtf(doc, metacard));

    ByteArrayInputStream templateStream =
        Optional.ofNullable(doc)
            .map(Rtf::out)
            .map(Objects::toString)
            .map(this::rtfStringToInputStream)
            .orElse(null);

    return createAndReturn(templateStream);
  }

  private BinaryContent createAndReturn(ByteArrayInputStream templateStream)
      throws CatalogTransformerException {
    if (templateStream == null) {
      throw new CatalogTransformerException("RTF template is empty, nothing to export");
    }

    try {
      return new BinaryContentImpl(templateStream, new MimeType(MIME_TYPE));
    } catch (MimeTypeParseException e) {
      throw new CatalogTransformerException(
          String.format("Unable to transform to RTF: %s", e.getCause()));
    }
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

  private String toRtf(Metacard metacard) {
    return Optional.ofNullable(toRtf(createRtfDoc(), metacard))
        .map(Rtf::out)
        .map(Object::toString)
        .orElse("");
  }

  private Rtf toRtf(Rtf doc, Metacard metacard) {
    return new RtfTemplate.Builder()
        .usingCategories(this.categories)
        .withMetacard(metacard)
        .build()
        .rtf(doc);
  }
}
