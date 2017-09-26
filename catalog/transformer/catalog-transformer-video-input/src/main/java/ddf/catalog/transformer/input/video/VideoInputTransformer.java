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
package ddf.catalog.transformer.input.video;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.constants.core.DataType;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transformer.common.tika.MetacardCreator;
import ddf.catalog.transformer.common.tika.TikaMetadataExtractor;
import java.io.IOException;
import java.io.InputStream;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;

public class VideoInputTransformer implements InputTransformer {

  private MetacardType metacardType = null;

  public VideoInputTransformer(MetacardType metacardType) {

    this.metacardType = metacardType;
  }

  @Override
  public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
    return transform(input, null);
  }

  @Override
  public Metacard transform(InputStream input, String id)
      throws IOException, CatalogTransformerException {

    Metacard metacard;
    try {
      TikaMetadataExtractor tikaMetadataExtractor = new TikaMetadataExtractor(input);

      Metadata metadata = tikaMetadataExtractor.getMetadata();

      String metadataText = tikaMetadataExtractor.getMetadataXml();

      metacard = MetacardCreator.createMetacard(metadata, id, metadataText, metacardType);

      metacard.setAttribute(new AttributeImpl(Core.DATATYPE, DataType.MOVING_IMAGE.toString()));
    } catch (TikaException e) {
      throw new CatalogTransformerException(e);
    }
    return metacard;
  }
}
