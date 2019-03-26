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
package ddf.catalog.plugin.jpeg2000.thumbnail.converter;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.ResultImpl;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.impl.QueryResponseImpl;
import ddf.catalog.plugin.PluginExecutionException;
import ddf.catalog.plugin.StopProcessingException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.spi.IIORegistry;
import org.junit.Test;

public class Jpeg2000ThumbnailConverterTest {
  private final Jpeg2000ThumbnailConverter jpeg2000ThumbnailConverter =
      new Jpeg2000ThumbnailConverter();

  @Test
  public void testConversion()
      throws IOException, StopProcessingException, PluginExecutionException {
    IIORegistry.getDefaultInstance().registerServiceProvider(jpeg2000ThumbnailConverter);
    List<Result> resultList = new ArrayList<>();
    Metacard metacard = new MetacardImpl();
    byte[] j2kbytes = new byte[0];
    resultList.add(new ResultImpl(metacard));
    QueryResponseImpl queryResponse = new QueryResponseImpl(null, resultList, 1);
    // there are two possible byte signatures, so test an example of each one
    for (String image : new String[] {"/Bretagne2.j2k", "/Cevennes2.jp2"}) {
      URL imageResource = Jpeg2000ThumbnailConverterTest.class.getResource(image);
      if (imageResource == null) {
        fail("The Image Resource came back null. Was the resources folder removed?");
      }
      String imageResourcePath = new File(imageResource.getFile()).getAbsolutePath();
      j2kbytes = Files.readAllBytes(Paths.get(imageResourcePath));
      metacard.setAttribute(new AttributeImpl(Core.THUMBNAIL, j2kbytes));
      jpeg2000ThumbnailConverter.process(queryResponse);
      // verify the plugin converted the j2k/jp2 image
      assertTrue(!Arrays.equals(j2kbytes, metacard.getThumbnail()));
    }
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    ImageIO.write(ImageIO.read(new ByteArrayInputStream(j2kbytes)), "gif", output);
    metacard.setAttribute(new AttributeImpl(Core.THUMBNAIL, output.toByteArray()));
    jpeg2000ThumbnailConverter.process(queryResponse);
    // verify the plugin ignored  the non-j2k
    assertTrue(Arrays.equals(output.toByteArray(), metacard.getThumbnail()));
  }

  @Test
  public void testEmptyThumbnail() throws Exception {
    Metacard metacard = new MetacardImpl();
    metacard.setAttribute(new AttributeImpl(Core.THUMBNAIL, new byte[0]));

    List<Result> resultList = new ArrayList<>();
    resultList.add(new ResultImpl(metacard));
    QueryResponseImpl queryResponse = new QueryResponseImpl(null, resultList, 1);

    jpeg2000ThumbnailConverter.process(queryResponse);
  }
}
