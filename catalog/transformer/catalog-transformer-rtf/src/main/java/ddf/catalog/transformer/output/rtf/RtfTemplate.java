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

import com.tutego.jrtf.Rtf;
import com.tutego.jrtf.RtfPara;
import com.tutego.jrtf.RtfPicture;
import com.tutego.jrtf.RtfRow;
import ddf.catalog.data.Metacard;
import ddf.catalog.transformer.output.rtf.model.ExportCategory;
import ddf.catalog.transformer.output.rtf.model.RtfCategory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RtfTemplate {

  static class Builder {
    private List<RtfCategory> categories;
    private Metacard metacard;

    public Builder usingCategories(List<RtfCategory> categories) {
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

    this.categories
        .stream()
        .forEach(exportCategory -> appendSection(doc, exportCategory, this.metacard));

    return doc;
  }

  private Rtf appendSection(Rtf rtf, RtfCategory category, Metacard metacard) {
    rtf.p();
    rtf.p(bold(category.getTitle()));

    Collection<RtfPara> rows =
        category
            .toExportMap(metacard)
            .entrySet()
            .stream()
            .map(this::appendProperty)
            .collect(Collectors.toList());

    return rtf.section(rows);
  }

  private RtfRow appendProperty(Map.Entry<String, ExportCategory.ExportValue> entry) {
    if (ExportCategory.ValueType.MEDIA.equals(entry.getValue().getType())) {
      ExportCategory.ExportValue<byte[], ExportCategory.ValueType> value = entry.getValue();
      InputStream mediaStream = fromBytes(value.getValue());

      return row(entry.getKey(), picture(mediaStream).type(RtfPicture.PictureType.AUTOMATIC));
    }

    return row(entry.getKey(), entry.getValue().getValue());
  }

  private InputStream fromBytes(byte[] data) {
    return new ByteArrayInputStream(data);
  }
}
