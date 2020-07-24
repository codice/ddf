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

import static com.tutego.jrtf.RtfPara.p;
import static com.tutego.jrtf.RtfPara.row;
import static com.tutego.jrtf.RtfText.bold;
import static com.tutego.jrtf.RtfText.font;
import static com.tutego.jrtf.RtfText.picture;
import static com.tutego.jrtf.RtfText.text;
import static ddf.catalog.transformer.output.rtf.model.ExportCategory.EMPTY_VALUE;

import com.tutego.jrtf.Rtf;
import com.tutego.jrtf.RtfPara;
import com.tutego.jrtf.RtfPicture;
import com.tutego.jrtf.RtfRow;
import com.tutego.jrtf.RtfText;
import ddf.catalog.data.Metacard;
import ddf.catalog.transformer.output.rtf.model.ExportCategory;
import ddf.catalog.transformer.output.rtf.model.RtfCategory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class builds an RTF document using {@link Metacard} and a list of {@link RtfCategory}s. The
 * resulting document consists of a main title, which comes from the {@link Metacard} title, section
 * headings ({@link RtfCategory} title) and a table of {@link RtfCategory} property names and
 * values.
 */
public class RtfTemplate {

  private static final Logger LOGGER = LoggerFactory.getLogger(RtfTemplate.class);

  private static final int MINIMUM_VALID_IMAGE_DATA_SIZE = 10;

  static class Builder {
    private List<RtfCategory> categories;
    private Metacard metacard;

    public Builder withCategories(List<RtfCategory> categories) {
      this.categories = categories;

      return this;
    }

    public Builder withMetacard(Metacard metacard) {
      this.metacard = metacard;

      return this;
    }

    public RtfTemplate build() {
      return new RtfTemplate(this.categories, this.metacard);
    }
  }

  private final List<RtfCategory> categories;
  private final Metacard metacard;

  private RtfTemplate(List<RtfCategory> categories, Metacard metacard) {
    this.categories = categories;
    this.metacard = metacard;
  }

  public Rtf rtf(Rtf doc) {
    doc.section(p(), p(), p(font(1, bold(this.metacard.getTitle()))).alignCentered());

    this.categories.forEach(exportCategory -> appendSection(doc, exportCategory, this.metacard));

    return doc;
  }

  private Function<String, Function<Map.Entry, RtfRow>> memoizeForRowData =
      metacardId -> entry -> appendProperty(metacardId, entry);

  private Function<String, Function<byte[], InputStream>> memoizeForImageData =
      metacardId -> data -> fromBytes(metacardId, data);

  private void appendSection(Rtf rtf, RtfCategory category, Metacard metacard) {
    rtf.p();
    rtf.p(bold(category.getTitle()));

    Function<Map.Entry, RtfRow> appendPropertyFunction = memoizeForRowData.apply(metacard.getId());

    Collection<RtfPara> rows =
        category.toExportMap(metacard).entrySet().stream()
            .map(appendPropertyFunction)
            .collect(Collectors.toList());

    rtf.section(rows);
  }

  private RtfRow appendProperty(
      String metacardId, Map.Entry<String, ExportCategory.ExportValue> entry) {
    if (ExportCategory.ValueType.MEDIA.equals(entry.getValue().getType())) {
      ExportCategory.ExportValue<byte[], ExportCategory.ValueType> value = entry.getValue();

      Function<byte[], InputStream> fromByteArrayFunction = memoizeForImageData.apply(metacardId);

      RtfText picture =
          Optional.of(value.getValue())
              .map(fromByteArrayFunction)
              .map(this::imageFromStream)
              .orElse(text(EMPTY_VALUE));

      return row(entry.getKey(), picture);
    }

    return row(entry.getKey(), entry.getValue().getValue());
  }

  private RtfText imageFromStream(InputStream stream) {
    return picture(stream).type(RtfPicture.PictureType.AUTOMATIC);
  }

  private InputStream fromBytes(String metacardId, byte[] data) {
    if (data == null || data.length < MINIMUM_VALID_IMAGE_DATA_SIZE) {
      LOGGER.debug(
          "Image cannot be exported in RTF format, malformed thumbnail data for metacard id: {}",
          metacardId);
      return null;
    }

    return new ByteArrayInputStream(data);
  }
}
