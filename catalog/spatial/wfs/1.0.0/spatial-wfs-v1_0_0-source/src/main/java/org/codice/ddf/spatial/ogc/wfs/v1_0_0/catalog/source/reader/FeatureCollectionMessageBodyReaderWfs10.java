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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.source.reader;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.io.xml.WstxDriver;
import com.thoughtworks.xstream.security.NoTypePermission;
import ddf.catalog.data.Metacard;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.wfs.catalog.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.FeatureConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GmlEnvelopeConverter;
import org.codice.ddf.spatial.ogc.wfs.catalog.converter.impl.GmlGeometryConverter;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.converter.impl.FeatureCollectionConverterWfs10;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
@Provider
public class FeatureCollectionMessageBodyReaderWfs10
    implements MessageBodyReader<WfsFeatureCollection> {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(FeatureCollectionMessageBodyReaderWfs10.class);

  protected XStream xstream;

  protected FeatureCollectionConverterWfs10 featureCollectionConverter;

  protected Map<String, FeatureConverter> featureConverterMap = new HashMap<>();

  public FeatureCollectionMessageBodyReaderWfs10() {
    xstream = new XStream(new WstxDriver());
    xstream.addPermission(NoTypePermission.NONE);
    xstream.setClassLoader(this.getClass().getClassLoader());
    xstream.registerConverter(new GmlGeometryConverter());
    xstream.registerConverter(new GmlEnvelopeConverter());
    xstream.alias("FeatureCollection", WfsFeatureCollection.class);

    featureCollectionConverter = new FeatureCollectionConverterWfs10();
    featureCollectionConverter.setFeatureConverterMap(featureConverterMap);
    xstream.registerConverter(featureCollectionConverter);
  }

  @Override
  public boolean isReadable(
      Class<?> clazz, Type type, Annotation[] annotations, MediaType mediaType) {
    if (!WfsFeatureCollection.class.isAssignableFrom(clazz)) {
      LOGGER.debug("{} class is not readable", clazz);
    }
    return WfsFeatureCollection.class.isAssignableFrom(clazz);
  }

  @Override
  public WfsFeatureCollection readFrom(
      Class<WfsFeatureCollection> clazz,
      Type type,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> headers,
      InputStream inStream)
      throws IOException, WebApplicationException {

    // Save original input stream for any exception message that might need to be
    // created
    String originalInputStream = IOUtils.toString(inStream, StandardCharsets.UTF_8.name());

    // Re-create the input stream (since it has already been read for potential
    // exception message creation)
    inStream =
        new ByteArrayInputStream(originalInputStream.getBytes(StandardCharsets.UTF_8.name()));

    WfsFeatureCollection featureCollection = null;

    try {
      xstream.allowTypeHierarchy(WfsFeatureCollection.class);
      featureCollection = (WfsFeatureCollection) xstream.fromXML(inStream);
    } catch (XStreamException e) {
      // If a ServiceExceptionReport is sent from the remote WFS site it will be sent with an
      // JAX-RS "OK" status, hence the ErrorResponse exception mapper will not fire.
      // Instead the ServiceExceptionReport will come here and be treated like a GetFeature
      // response, resulting in an XStreamException since ExceptionReport cannot be
      // unmarshalled. So this catch clause is responsible for catching that XStream
      // exception and creating a JAX-RS response containing the original stream
      // (with the ExceptionReport) and rethrowing it as a WebApplicationException,
      // which CXF will wrap as a ClientException that the WfsSource catches, converts
      // to a WfsException, and logs.
      LOGGER.debug("Exception unmarshalling", e);
      ByteArrayInputStream bis =
          new ByteArrayInputStream(originalInputStream.getBytes(StandardCharsets.UTF_8));
      ResponseBuilder responseBuilder = Response.ok(bis);
      responseBuilder.type("text/xml");
      Response response = responseBuilder.build();
      throw new WebApplicationException(e, response);
    } finally {
      IOUtils.closeQuietly(inStream);
    }

    return featureCollection;
  }

  public void registerConverter(FeatureConverter converter) {
    featureConverterMap.put(converter.getMetacardType().getName(), converter);
    xstream.registerConverter(converter);
    xstream.alias(converter.getMetacardType().getName(), Metacard.class);
  }
}
