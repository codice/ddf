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
package org.codice.ddf.spatial.kml.transformer;

import static java.util.Collections.emptyList;
import static org.codice.ddf.spatial.kml.converter.MetacardToKml.addJtsGeoPointsToKmlGeo;
import static org.codice.ddf.spatial.kml.converter.MetacardToKml.getJtsGeoFromWkt;
import static org.codice.ddf.spatial.kml.converter.MetacardToKml.getKmlGeoFromJtsGeo;
import static org.codice.ddf.spatial.kml.util.KmlTransformations.encloseKml;

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.google.common.annotations.VisibleForTesting;
import ddf.action.ActionProvider;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType;
import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import javax.security.auth.Subject;
import javax.xml.bind.DatatypeConverter;
import net.opengis.kml.v_2_2_0.AbstractFeatureType;
import net.opengis.kml.v_2_2_0.AbstractGeometryType;
import net.opengis.kml.v_2_2_0.AbstractStyleSelectorType;
import net.opengis.kml.v_2_2_0.DataType;
import net.opengis.kml.v_2_2_0.ExtendedDataType;
import net.opengis.kml.v_2_2_0.KmlType;
import net.opengis.kml.v_2_2_0.ObjectFactory;
import net.opengis.kml.v_2_2_0.PlacemarkType;
import net.opengis.kml.v_2_2_0.TimeSpanType;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.codice.ddf.log.sanitizer.LogSanitizer;
import org.codice.ddf.spatial.kml.util.KmlMarshaller;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The base Transformer for handling KML requests to take a {@link Metacard} or {@link
 * SourceResponse} and produce a KML representation. This service attempts to first locate a {@link
 * KMLEntryTransformer} for a given {@link Metacard} based on the metadata-content-type. If no
 * {@link KMLEntryTransformer} can be found, the default transformation is performed.
 *
 * @author Ashraf Barakat, Ian Barnett, Keith C Wire
 */
public class KMLTransformerImpl implements KMLTransformer {

  private static final String KML_RESPONSE_QUEUE_PREFIX = "Results (";

  private static final String CLOSE_PARENTHESIS = ")";

  private static final String TEMPLATE_DIRECTORY = "/templates";

  private static final String TEMPLATE_SUFFIX = ".hbt";

  private static final String DESCRIPTION_TEMPLATE = "description";

  private static final Logger LOGGER = LoggerFactory.getLogger(KMLTransformerImpl.class);

  private static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

  @VisibleForTesting static final MimeType KML_MIMETYPE = new MimeType();

  private static final ObjectFactory KML220_OBJECT_FACTORY = new ObjectFactory();

  private List<AbstractStyleSelectorType> defaultStyle;

  static {
    try {
      KML_MIMETYPE.setPrimaryType("application");
      KML_MIMETYPE.setSubType("vnd.google-earth.kml+xml");
    } catch (MimeTypeParseException e) {
      LOGGER.info("Unable to parse KML MimeType.", e);
    }
  }

  protected BundleContext context;

  private ClassPathTemplateLoader templateLoader;

  private KmlStyleMap styleMapper;

  private DescriptionTemplateHelper templateHelper;

  private KmlMarshaller kmlMarshaller;

  private DateTimeFormatter formatter;

  public KMLTransformerImpl(
      BundleContext bundleContext,
      String defaultStylingName,
      KmlStyleMap mapper,
      ActionProvider actionProvider,
      KmlMarshaller kmlMarshaller) {

    this.context = Validate.notNull(bundleContext, "BundleContext must not be null.");
    this.styleMapper = Validate.notNull(mapper, "KmlStyleMap must not be null.");
    this.templateHelper = new DescriptionTemplateHelper(actionProvider);
    this.kmlMarshaller = Validate.notNull(kmlMarshaller, "KmlMarshaller must not be null.");
    this.formatter = DateTimeFormatter.ofPattern(ISO_8601_DATE_FORMAT);

    final URL stylingUrl = context.getBundle().getResource(defaultStylingName);

    try {
      LOGGER.trace("Reading in KML Style");
      defaultStyle =
          kmlMarshaller
              .unmarshal(stylingUrl.openStream())
              .map(KmlType::getAbstractFeatureGroup)
              .map(AbstractFeatureType::getAbstractStyleSelectorGroup)
              .orElse(emptyList());
    } catch (IOException e) {
      LOGGER.debug("Exception while opening default style resource.", e);
    }

    templateLoader = new ClassPathTemplateLoader();
    templateLoader.setPrefix(TEMPLATE_DIRECTORY);
    templateLoader.setSuffix(TEMPLATE_SUFFIX);
  }

  /**
   * This will return a KML Placemark (i.e. there are no kml tags) {@code <KML> ---> not included
   * <Placemark> ---> What is returned from this method ... ---> What is returned from this method
   * </Placemark> ---> What is returned from this method </KML> ---> not included }
   *
   * @param user
   * @param entry - the {@link Metacard} to be transformed
   * @param arguments - additional arguments to assist in the transformation
   * @return Placemark - kml object containing transformed content
   * @throws CatalogTransformerException
   */
  @Override
  public PlacemarkType transformEntry(
      Subject user, Metacard entry, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    return performDefaultTransformation(entry);
  }

  /**
   * The default Transformation from a {@link Metacard} to a KML {@link Placemark}. Protected to
   * easily allow other default transformations.
   *
   * @param entry - the {@link Metacard} to transform.
   * @param urlToMetacard
   * @return
   * @throws javax.xml.transform.TransformerException
   */
  protected PlacemarkType performDefaultTransformation(Metacard entry)
      throws CatalogTransformerException {

    // wrap metacard to work around classLoader/reflection issues
    entry = new MetacardImpl(entry);
    PlacemarkType kmlPlacemark = KML220_OBJECT_FACTORY.createPlacemarkType();
    kmlPlacemark.setId("Placemark-" + entry.getId());
    kmlPlacemark.setName(entry.getTitle());

    DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    String effectiveTime;
    if (entry.getEffectiveDate() == null) {
      effectiveTime = dateFormat.format(new Date());
    } else {
      effectiveTime = dateFormat.format(entry.getEffectiveDate());
    }

    TimeSpanType timeSpan = KML220_OBJECT_FACTORY.createTimeSpanType();
    timeSpan.setBegin(effectiveTime);
    //    kmlPlacemark.setTimePrimitive(timeSpan);
    kmlPlacemark.setAbstractTimePrimitiveGroup(KML220_OBJECT_FACTORY.createTimeSpan(timeSpan));

    //    kmlPlacemark.setGeometry(getKmlGeoWithPointsFromWkt(entry.getLocation()));
    kmlPlacemark.setAbstractGeometryGroup(getKmlGeoWithPointsFromWkt(entry.getLocation()));

    String description = entry.getTitle();
    Handlebars handlebars = new Handlebars(templateLoader);
    handlebars.registerHelpers(templateHelper);
    try {
      Template template = handlebars.compile(DESCRIPTION_TEMPLATE);
      description = template.apply(new HandlebarsMetacard(entry));
      LOGGER.debug(description);

    } catch (IOException e) {
      LOGGER.debug("Failed to apply description Template", e);
    }

    kmlPlacemark.setDescription(description);

    setExtendedData(kmlPlacemark, entry);

    String styleUrl = styleMapper.getStyleForMetacard(entry);
    if (StringUtils.isNotBlank(styleUrl)) {
      kmlPlacemark.setStyleUrl(styleUrl);
    }

    return kmlPlacemark;
  }

  private void setExtendedData(PlacemarkType placemark, Metacard metacard) {
    final ExtendedDataType extendedData = KML220_OBJECT_FACTORY.createExtendedDataType();

    final Set<AttributeDescriptor> attributeDescriptors =
        metacard.getMetacardType().getAttributeDescriptors();

    for (AttributeDescriptor attributeDescriptor : attributeDescriptors) {
      final String attributeName = attributeDescriptor.getName();
      final Attribute attribute = metacard.getAttribute(attributeName);
      if (attribute != null) {
        Serializable attributeValue = convertAttribute(attribute, attributeDescriptor);
        if (attributeValue == null) {
          LOGGER.debug("Attribute {} converted to null value.", attributeName);
        } else {
          final List<DataType> data = getData(attributeName, attributeValue.toString());
          //          extendedData.addToData(data);
          extendedData.setData(data);
        }
      }
    }
    placemark.setExtendedData(extendedData);
  }

  private List<DataType> getData(String attributeAlias, String attributeValue) {
    final DataType data = KML220_OBJECT_FACTORY.createDataType();
    data.setValue(attributeValue);
    data.setName(attributeAlias);
    return Collections.singletonList(data);
  }

  private Serializable convertAttribute(Attribute attribute, AttributeDescriptor descriptor) {

    if (descriptor.isMultiValued()) {
      List<Serializable> values = new ArrayList<>();
      for (Serializable value : attribute.getValues()) {
        Serializable convertedValue =
            convertValue(attribute.getName(), value, descriptor.getType().getAttributeFormat());
        if (convertedValue != null) {
          values.add(convertedValue);
        }
      }
      return StringUtils.join(values, ",");
    } else {
      return convertValue(
          attribute.getName(), attribute.getValue(), descriptor.getType().getAttributeFormat());
    }
  }

  private Serializable convertValue(
      String name, Serializable value, AttributeType.AttributeFormat format) {
    if (value == null) {
      return null;
    }

    switch (format) {
      case DATE:
        if (!(value instanceof Date)) {
          LOGGER.debug(
              "Dropping attribute date value {} for {} because it isn't a Date object.",
              value,
              name);
          return null;
        }
        Instant instant = ((Date) value).toInstant();
        ZoneId zoneId = ZoneId.of("UTC");
        ZonedDateTime zonedDateTime = instant.atZone(zoneId);
        return zonedDateTime.format(formatter);
      case BINARY:
        byte[] bytes = (byte[]) value;
        return DatatypeConverter.printBase64Binary(bytes);
      case BOOLEAN:
      case DOUBLE:
      case LONG:
      case INTEGER:
      case SHORT:
      case STRING:
      case XML:
      case FLOAT:
      case GEOMETRY:
        return value;
      case OBJECT:
      default:
        return null;
    }
  }

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    try {
      PlacemarkType placemark = transformEntry(null, metacard, arguments);

      /*
      if (placemark.getStyleSelector().isEmpty() && StringUtils.isBlank(placemark.getStyleUrl())) {
        placemark.getStyleSelector().addAll(defaultStyle);
      }

      if (!placemark.isSetAbstractStyleSelectorGroup() &&
      StringUtils.isBlank(placemark.getStyleUrl())) {
        placemark.getAbstractStyleSelectorGroup().addAll(defaultStyle);
        placemark.withAbstractStyleSelectorGroup(
            KML220_OBJECT_FACTORY.createAbstractStyleSelectorGroup(
                defaultStyle
            )
        );

      }
      */

      //      Kml kml = KmlFactory.createKml().withFeature(placemark);
      KmlType kml =
          KML220_OBJECT_FACTORY
              .createKmlType()
              .withAbstractFeatureGroup(KML220_OBJECT_FACTORY.createPlacemark(placemark));

      String transformedKmlString = kmlMarshaller.marshal(KML220_OBJECT_FACTORY.createKml(kml));

      InputStream kmlInputStream =
          new ByteArrayInputStream(transformedKmlString.getBytes(StandardCharsets.UTF_8));

      return new BinaryContentImpl(kmlInputStream, KML_MIMETYPE);
    } catch (Exception e) {
      LOGGER.debug("Error transforming metacard ({}) to KML: {}", metacard.getId(), e.getMessage());
      throw new CatalogTransformerException("Error transforming metacard to KML.", e);
    }
  }

  @Override
  public BinaryContent transform(
      SourceResponse upstreamResponse, Map<String, Serializable> arguments)
      throws CatalogTransformerException {
    LOGGER.trace("ENTERING: ResponseQueue transform");
    if (arguments == null) {
      LOGGER.debug("Null arguments, unable to complete transform");
      throw new CatalogTransformerException("Unable to complete transform without arguments");
    }
    String docId = UUID.randomUUID().toString();

    String restUriAbsolutePath = (String) arguments.get("url");
    LOGGER.debug("rest string url arg: {}", LogSanitizer.sanitize(restUriAbsolutePath));

    // Transform Metacards to KML
    Document kmlDoc = KmlFactory.createDocument();
    boolean needDefaultStyle = false;
    for (Result result : upstreamResponse.getResults()) {
      try {
        Placemark placemark = transformEntry(null, result.getMetacard(), arguments);
        if (placemark.getStyleSelector().isEmpty()
            && StringUtils.isEmpty(placemark.getStyleUrl())) {
          placemark.setStyleUrl("#default");
          needDefaultStyle = true;
        }
        kmlDoc.getFeature().add(placemark);
      } catch (CatalogTransformerException e) {
        LOGGER.debug(
            "Error transforming current metacard ({}) to KML and will continue with remaining query responses.",
            LogSanitizer.sanitize(result.getMetacard().getId()),
            e);
      }
    }

    if (needDefaultStyle) {
      kmlDoc.getStyleSelector().addAll(defaultStyle);
    }

    Kml kmlResult =
        encloseKml(
            kmlDoc,
            docId,
            KML_RESPONSE_QUEUE_PREFIX + kmlDoc.getFeature().size() + CLOSE_PARENTHESIS);

    String transformedKml = kmlMarshaller.marshal(kmlResult);

    InputStream kmlInputStream =
        new ByteArrayInputStream(transformedKml.getBytes(StandardCharsets.UTF_8));
    LOGGER.trace("EXITING: ResponseQueue transform");
    return new BinaryContentImpl(kmlInputStream, KML_MIMETYPE);
  }

  private AbstractGeometryType getKmlGeoWithPointsFromWkt(String wkt)
      throws CatalogTransformerException {
    final org.locationtech.jts.geom.Geometry jtsGeo = getJtsGeoFromWkt(wkt);
    // Geometry kmlGeo = getKmlGeoFromJtsGeo(jtsGeo);
    AbstractGeometryType kmlGeo = getKmlGeoFromJtsGeo(jtsGeo);
    kmlGeo = addJtsGeoPointsToKmlGeo(jtsGeo, kmlGeo);
    return kmlGeo;
  }
}
