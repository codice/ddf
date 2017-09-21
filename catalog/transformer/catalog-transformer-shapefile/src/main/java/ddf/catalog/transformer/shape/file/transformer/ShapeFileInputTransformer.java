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
package ddf.catalog.transformer.shape.file.transformer;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Topic;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import ddf.catalog.transformer.shape.file.reader.api.ShapeFileHeader;
import ddf.catalog.transformer.shape.file.reader.api.ShapeFileReader;
import ddf.catalog.transformer.shape.file.reader.api.ShapeFileReaderFactory;
import ddf.catalog.transformer.shape.file.transformer.api.ShapeFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/** This class is used convert the a .shp file into a metacard for DDF to display. */
public class ShapeFileInputTransformer implements InputTransformer {

  /** SHAPE_CATAGORY - catagory of the metacard. */
  private static final String SHAPE_CATAGORY = "Shape File";

  /** metacardType - the metacard type for the source. */
  private MetacardType metacardType;

  /** reader - Reads the contents of a shape file. */
  private ShapeFileReaderFactory factory;

  /**
   * Constructor for ShapeFileInputTransformer class.
   *
   * @param type @see {@link
   *     ddf.catalog.transformer.shape.file.transformer.ShapeFileInputTransformer#metacardType}
   */
  public ShapeFileInputTransformer(ShapeFileReaderFactory factory, MetacardType type) {
    this.factory = factory;
    this.metacardType = type;
  }

  /**
   * Transforms an input stream into a metacard.
   *
   * @param input input stream containing the shape file
   * @return Metacard the metacard created from the shape file
   */
  @Override
  public Metacard transform(InputStream input) throws IOException, CatalogTransformerException {
    return transform(input, null);
  }

  /**
   * Transforms an input stream into a metacard and also allows for setting a custom ID for the
   * metacard.
   *
   * @param input input stream containing the shape file
   * @param id an id which can be used to set the metacard id
   * @return Metacard the metacard created from the shape file
   */
  @Override
  public Metacard transform(InputStream input, String id)
      throws IOException, CatalogTransformerException {
    return transformShapeFile(input, id);
  }

  /**
   * @param input input stream containing the shape file
   * @param id an id which can be used to set the metacard id
   * @return the metacard created from the shape file
   */
  private Metacard transformShapeFile(InputStream input, String id)
      throws CatalogTransformerException {
    MetacardImpl metacard = new MetacardImpl(metacardType);

    if (id != null) {
      metacard.setId(id);
    }

    metacard.setContentTypeName(SHAPE_CATAGORY);
    metacard.setAttribute(Topic.CATEGORY, SHAPE_CATAGORY);

    try {
      ShapeFileReader reader = factory.createShapeFileReader(input);
      reader.readHeader();

      ShapeFileHeader header = reader.getShapeFileHeader();

      metacard.setAttribute(ShapeFile.SHAPE_TYPE, header.getShapeType().toString());
      metacard.setLocation(header.getBbox());

      metacard.setAttribute(ShapeFile.SHAPE_MIN_LAT, header.getMinLat());
      metacard.setAttribute(ShapeFile.SHAPE_MAX_LAT, header.getMaxLat());
      metacard.setAttribute(ShapeFile.SHAPE_MIN_LON, header.getMinLon());
      metacard.setAttribute(ShapeFile.SHAPE_MAX_LON, header.getMaxLon());
    } catch (Exception e) {
      throw new CatalogTransformerException("Failed to read shape file", e);
    }

    // All tags to the metacard
    Set<String> tags = new HashSet<>();
    tags.add(Metacard.DEFAULT_TAG);
    tags.add(metacardType.getName());
    metacard.setTags(tags);

    return metacard;
  }
}
