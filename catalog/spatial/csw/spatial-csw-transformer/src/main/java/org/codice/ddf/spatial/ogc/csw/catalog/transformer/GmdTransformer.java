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
package org.codice.ddf.spatial.ogc.csw.catalog.transformer;

import com.google.common.io.ByteSource;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.XStreamException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.xml.QNameMap;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import com.thoughtworks.xstream.io.xml.StaxReader;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Associations;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Core;
import ddf.catalog.data.types.DateTime;
import ddf.catalog.data.types.Location;
import ddf.catalog.data.types.Media;
import ddf.catalog.data.types.Topic;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.InputTransformer;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.platform.util.TemporaryFileBackedOutputStream;
import org.codice.ddf.platform.util.XMLUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GmdConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswUnmarshallHelper;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.GmdConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.XstreamPathConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.XstreamPathValueTracker;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.io.WKTWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GmdTransformer extends AbstractGmdTransformer implements InputTransformer {

  private static final Logger LOGGER = LoggerFactory.getLogger(GmdTransformer.class);

  private static final String TRANSFORM_EXCEPTION_MSG =
      "Unable to transform from GMD Metadata to Metacard";

  private static final Pattern WHITE_SPACE_PATTER = Pattern.compile("(\\s)+");

  private static final Pattern NEW_LINE_PATTERN = Pattern.compile("(\\n)+");

  private static final String ZERO_HOUR_SUFFIX = "T00:00:00Z";

  private static final ThreadLocal<WKTWriter> WKT_WRITER_THREAD_LOCAL =
      new ThreadLocal<WKTWriter>() {
        @Override
        protected WKTWriter initialValue() {
          return new WKTWriter();
        }
      };

  private static final XMLUtils XML_UTILS = XMLUtils.getInstance();

  private final MetacardType gmdMetacardType;

  private final XStream xstream;

  private final DataHolder argumentHolder;

  private final XMLInputFactory xmlFactory;

  private GeometryFactory factory;

  private ByteSource byteArray;

  public GmdTransformer(MetacardType metacardType, Supplier<Converter> converterSupplier) {
    super(converterSupplier);

    gmdMetacardType = metacardType;
    factory = new GeometryFactory();

    QNameMap qmap = new QNameMap();
    qmap.setDefaultNamespace(GmdConstants.GMD_NAMESPACE);
    qmap.setDefaultPrefix("");
    StaxDriver staxDriver = new StaxDriver(qmap);

    xstream = new XStream(staxDriver);
    xstream.setClassLoader(this.getClass().getClassLoader());
    XstreamPathConverter converter = new XstreamPathConverter();
    xstream.registerConverter(converter);
    xstream.alias("MD_Metadata", XstreamPathValueTracker.class);
    argumentHolder = xstream.newDataHolder();
    argumentHolder.put(XstreamPathConverter.PATH_KEY, buildPaths());
    xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, Boolean.FALSE);
    xmlFactory.setProperty(
        XMLInputFactory.SUPPORT_DTD, Boolean.FALSE); // This disables DTDs entirely for that factory
    xmlFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
  }

  public GmdTransformer(MetacardType metacardType) {
    this(metacardType, GmdConverter::new);
  }

  public void destroy() {
    LOGGER.debug("Destroying wkt writer");
    WKT_WRITER_THREAD_LOCAL.remove();
  }

  public LinkedHashSet<Path> buildPaths() {
    LinkedHashSet<Path> paths = new LinkedHashSet<>();

    Arrays.asList(
            GmdConstants.ABSTRACT_PATH,
            GmdConstants.ALTITUDE_PATH,
            GmdConstants.ASSOCIATION_PATH,
            GmdConstants.CITATION_DATE_TYPE_PATH,
            GmdConstants.CODE_LIST_VALUE_PATH,
            GmdConstants.CRS_AUTHORITY_PATH,
            GmdConstants.CRS_CODE_PATH,
            GmdConstants.DATE_TIME_STAMP_PATH,
            GmdConstants.DATE_STAMP_PATH,
            GmdConstants.DISTRIBUTOR_FORMAT_PATH,
            GmdConstants.DISTRIBUTOR_FORMAT_VERSION_PATH,
            GmdConstants.FILE_IDENTIFIER_PATH,
            GmdConstants.FORMAT_PATH,
            GmdConstants.FORMAT_VERSION_PATH,
            GmdConstants.LANGUAGE_PATH,
            GmdConstants.LINKAGE_URI_PATH,
            GmdConstants.KEYWORD_PATH,
            GmdConstants.RESOURCE_STATUS_PATH,
            GmdConstants.TEMPORAL_START_PATH,
            GmdConstants.TEMPORAL_STOP_PATH,
            GmdConstants.TITLE_PATH,
            GmdConstants.TOPIC_CATEGORY_PATH,

            /* Location Paths */
            GmdConstants.BOUNDING_POLYGON_LINE_STRING_PATH,
            GmdConstants.BOUNDING_POLYGON_PATH,
            GmdConstants.BBOX_WEST_LON_PATH,
            GmdConstants.BBOX_EAST_LON_PATH,
            GmdConstants.BBOX_SOUTH_LAT_PATH,
            GmdConstants.BBOX_NORTH_LAT_PATH,

            /* Contact Paths */
            GmdConstants.POINT_OF_CONTACT_ADDRESS_ADMINISTRATIVE_AREA_PATH,
            GmdConstants.POINT_OF_CONTACT_ADDRESS_CITY_PATH,
            GmdConstants.POINT_OF_CONTACT_ADDRESS_COUNTRY_PATH,
            GmdConstants.POINT_OF_CONTACT_ADDRESS_DELIVERY_POINT_PATH,
            GmdConstants.POINT_OF_CONTACT_ADDRESS_POSTAL_CODE_PATH,
            GmdConstants.POINT_OF_CONTACT_EMAIL_PATH,
            GmdConstants.POINT_OF_CONTACT_ORGANISATION_PATH,
            GmdConstants.POINT_OF_CONTACT_PHONE_PATH,
            GmdConstants.CONTACT_ADDRESS_ADMINISTRATIVE_AREA_PATH,
            GmdConstants.CONTACT_ADDRESS_CITY_PATH,
            GmdConstants.CONTACT_ADDRESS_COUNTRY_PATH,
            GmdConstants.CONTACT_ADDRESS_DELIVERY_POINT_PATH,
            GmdConstants.CONTACT_ADDRESS_POSTAL_CODE_PATH,
            GmdConstants.CONTACT_EMAIL_PATH,
            GmdConstants.CONTACT_ORGANISATION_PATH,
            GmdConstants.COUNTRY_CODE_PATH,
            GmdConstants.CONTACT_PHONE_PATH)
        .stream()
        .map(this::toPath)
        .forEach(paths::add);

    return paths;
  }

  @Override
  public Metacard transform(InputStream inputStream)
      throws IOException, CatalogTransformerException {
    return transform(inputStream, null);
  }

  @Override
  public Metacard transform(InputStream inputStream, String id)
      throws IOException, CatalogTransformerException {
    return handleTransform(inputStream, id);
  }

  private Metacard handleTransform(InputStream inputStream, String id)
      throws CatalogTransformerException {
    String xml;
    XstreamPathValueTracker pathValueTracker;
    try (TemporaryFileBackedOutputStream temporaryFileBackedOutputStream =
        new TemporaryFileBackedOutputStream()) {
      IOUtils.copy(inputStream, temporaryFileBackedOutputStream);
      byteArray = temporaryFileBackedOutputStream.asByteSource();

      try (InputStream xmlSourceInputStream = getSourceInputStream()) {
        xml = IOUtils.toString(xmlSourceInputStream);
      }

      argumentHolder.put(XstreamPathConverter.PATH_KEY, buildPaths());
      XMLStreamReader streamReader = xmlFactory.createXMLStreamReader(new StringReader(xml));
      HierarchicalStreamReader reader = new StaxReader(new QNameMap(), streamReader);
      pathValueTracker = (XstreamPathValueTracker) xstream.unmarshal(reader, null, argumentHolder);
      Metacard metacard = toMetacard(pathValueTracker, id);
      metacard.setAttribute(new AttributeImpl(Core.METADATA, xml));
      return metacard;
    } catch (XStreamException | XMLStreamException | IOException e) {
      throw new CatalogTransformerException(TRANSFORM_EXCEPTION_MSG, e);
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  public InputStream getSourceInputStream() throws IOException {
    return byteArray.openStream();
  }

  public void setMetacardAttributes(
      MetacardImpl metacard, String id, final XstreamPathValueTracker pathValueTracker) {
    if (StringUtils.isNotEmpty(id)) {
      metacard.setAttribute(Core.ID, id);
    } else {
      metacard.setId(pathValueTracker.getFirstValue(toPath(GmdConstants.FILE_IDENTIFIER_PATH)));
    }
    addMetacardTitle(pathValueTracker, metacard);
    addMetacardLanguage(pathValueTracker, metacard);
    addMetacardType(pathValueTracker, metacard);
    addMetacardAbstract(pathValueTracker, metacard);
    addMetacardFormat(pathValueTracker, metacard);
    addMetacardCreatedAndModifiedDates(pathValueTracker, metacard);
    addMetacardKeywords(pathValueTracker, metacard);
    addMetacardTopics(pathValueTracker, metacard);
    addMetacardAssociations(pathValueTracker, metacard);
    addMetacardResourceUri(pathValueTracker, metacard);
    addMetacardPointOfContact(pathValueTracker, metacard);
    addMetacardAltitude(pathValueTracker, metacard);
    addMetacardTemporalStartStop(pathValueTracker, metacard);
    addMetacardResourceStatusCode(pathValueTracker, metacard);
    addMetacardCountryCodes(pathValueTracker, metacard);
    addMetacardCrs(pathValueTracker, metacard);
    addMetacardContactPOCInformation(pathValueTracker, metacard);
    addMetacardLocation(pathValueTracker, metacard);
    setMetacardDates(metacard, pathValueTracker);
    if (metacard.getLocation() == null) {
      setMetacardLocationFromBoundingPolygonPoints(metacard);
    }
  }

  public Metacard toMetacard(final XstreamPathValueTracker pathValueTracker, final String id) {
    MetacardImpl metacard = new MetacardImpl(gmdMetacardType);
    setMetacardAttributes(metacard, id, pathValueTracker);
    return metacard;
  }

  public void addMetacardLanguage(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    addFieldToMetacardIfPresent(
        pathValueTracker, metacard, GmdConstants.LANGUAGE_PATH, Core.LANGUAGE);
  }

  private void addMetacardTitle(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    addFieldToMetacardIfPresent(pathValueTracker, metacard, GmdConstants.TITLE_PATH, Core.TITLE);
  }

  private void addMetacardType(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    addFieldToMetacardIfPresent(
        pathValueTracker, metacard, GmdConstants.CODE_LIST_VALUE_PATH, Core.DATATYPE);
    // Set metadata-content-type for backward compatibility
    addFieldToMetacardIfPresent(
        pathValueTracker, metacard, GmdConstants.CODE_LIST_VALUE_PATH, Metacard.CONTENT_TYPE);
  }

  private void addMetacardAbstract(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    addFieldsToMetacardIfPresent(
        pathValueTracker, metacard, GmdConstants.ABSTRACT_PATH, Core.DESCRIPTION);
  }

  public void addMetacardFormat(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    String format = pathValueTracker.getFirstValue(toPath(GmdConstants.FORMAT_PATH));
    String formatVersion = pathValueTracker.getFirstValue(toPath(GmdConstants.FORMAT_VERSION_PATH));
    String distributorFormat =
        pathValueTracker.getFirstValue(toPath(GmdConstants.DISTRIBUTOR_FORMAT_PATH));
    String distributorFormatVersion =
        pathValueTracker.getFirstValue(toPath(GmdConstants.DISTRIBUTOR_FORMAT_VERSION_PATH));

    if (StringUtils.isNotEmpty(format)) {
      metacard.setAttribute(Media.FORMAT, format);
    } else if (StringUtils.isNotEmpty(distributorFormat)) {
      metacard.setAttribute(Media.FORMAT, distributorFormat);
    }

    if (StringUtils.isNotEmpty(formatVersion)) {
      metacard.setAttribute(Media.FORMAT_VERSION, formatVersion);
    } else if (StringUtils.isNotEmpty(distributorFormatVersion)) {
      metacard.setAttribute(Media.FORMAT_VERSION, distributorFormatVersion);
    }
  }

  private void addMetacardCreatedAndModifiedDates(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    String createdDateTime =
        pathValueTracker.getFirstValue(toPath(GmdConstants.DATE_TIME_STAMP_PATH));
    String createdDate = pathValueTracker.getFirstValue(toPath(GmdConstants.DATE_STAMP_PATH));

    if (StringUtils.isNotBlank(createdDateTime)) {
      Date date = CswUnmarshallHelper.convertToDate(createdDateTime);
      metacard.setAttribute(Core.METACARD_CREATED, date);
      metacard.setAttribute(Core.METACARD_MODIFIED, date);
    } else if (StringUtils.isNotBlank(createdDate)) {
      Date date = CswUnmarshallHelper.convertToDate(createdDate + ZERO_HOUR_SUFFIX);
      metacard.setAttribute(Core.METACARD_CREATED, date);
      metacard.setAttribute(Core.METACARD_MODIFIED, date);
    }
  }

  private void addMetacardKeywords(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    addFieldsToMetacardIfPresent(
        pathValueTracker, metacard, GmdConstants.KEYWORD_PATH, Topic.KEYWORD);
  }

  private void addMetacardTopics(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    addFieldsToMetacardIfPresent(
        pathValueTracker, metacard, GmdConstants.TOPIC_CATEGORY_PATH, Topic.CATEGORY);
  }

  private void addMetacardAssociations(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    addFieldsToMetacardIfPresent(
        pathValueTracker, metacard, GmdConstants.ASSOCIATION_PATH, Associations.RELATED);
  }

  private void addMetacardResourceUri(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    String linkage = pathValueTracker.getFirstValue(toPath(GmdConstants.LINKAGE_URI_PATH));
    if (StringUtils.isNotEmpty(linkage)) {
      try {
        metacard.setResourceURI(new URI(linkage.trim()));
      } catch (URISyntaxException e) {
        LOGGER.info("Unable to read resource URI {}", linkage, e);
      }
    }
  }

  private void addMetacardPointOfContact(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    List<String> organisationNames =
        pathValueTracker.getAllValues(toPath(GmdConstants.CONTACT_ORGANISATION_PATH));
    if (CollectionUtils.isNotEmpty(organisationNames)) {
      metacard.setAttribute(Contact.PUBLISHER_NAME, (Serializable) organisationNames);
    }

    addFieldsToMetacardIfPresent(
        pathValueTracker, metacard, GmdConstants.CONTACT_PHONE_PATH, Contact.PUBLISHER_PHONE);

    addFieldsToMetacardIfPresent(
        pathValueTracker, metacard, GmdConstants.CONTACT_EMAIL_PATH, Contact.PUBLISHER_EMAIL);

    List<String> deliveryPoints =
        pathValueTracker.getAllValues(toPath(GmdConstants.CONTACT_ADDRESS_DELIVERY_POINT_PATH));
    if (CollectionUtils.isNotEmpty(deliveryPoints)) {
      List<String> cities =
          pathValueTracker.getAllValues(toPath(GmdConstants.CONTACT_ADDRESS_CITY_PATH));
      List<String> administrativeAreas =
          pathValueTracker.getAllValues(
              toPath(GmdConstants.CONTACT_ADDRESS_ADMINISTRATIVE_AREA_PATH));
      List<String> postalCodes =
          pathValueTracker.getAllValues(toPath(GmdConstants.CONTACT_ADDRESS_POSTAL_CODE_PATH));
      List<String> countries =
          pathValueTracker.getAllValues(toPath(GmdConstants.CONTACT_ADDRESS_COUNTRY_PATH));
      List<String> addresses = new ArrayList<>();
      for (int i = 0; i < deliveryPoints.size(); i++) {
        StringBuilder address = new StringBuilder(deliveryPoints.get(i));
        address.append(getStringFromListAtIndexIfNotEmpty(cities, i));
        address.append(getStringFromListAtIndexIfNotEmpty(administrativeAreas, i));
        address.append(getStringFromListAtIndexIfNotEmpty(postalCodes, i));
        address.append(getStringFromListAtIndexIfNotEmpty(countries, i));
        addresses.add(address.toString().trim());
      }
      metacard.setAttribute(Contact.PUBLISHER_ADDRESS, (Serializable) addresses);
    }
  }

  private void addMetacardAltitude(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    List<String> altitudeList = pathValueTracker.getAllValues(toPath(GmdConstants.ALTITUDE_PATH));
    if (CollectionUtils.isNotEmpty(altitudeList)) {
      addMetacardAltitudeList(altitudeList, metacard);
    }
  }

  private void addMetacardAltitudeList(List<String> serviceExAltitude, MetacardImpl metacard) {
    List<Double> altitudes = covertStringListToDoubleList(serviceExAltitude);
    if (CollectionUtils.isNotEmpty(altitudes)) {
      metacard.setAttribute(Location.ALTITUDE, (Serializable) altitudes);
    }
  }

  private void addMetacardTemporalStartStop(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    List<String> temporalStartList =
        pathValueTracker.getAllValues(toPath(GmdConstants.TEMPORAL_START_PATH));
    List<String> temporalStopList =
        pathValueTracker.getAllValues(toPath(GmdConstants.TEMPORAL_STOP_PATH));

    if (CollectionUtils.isNotEmpty(temporalStartList)) {
      List<Date> dates = convertDateList(temporalStartList);
      metacard.setAttribute(DateTime.START, (Serializable) dates);
    }

    if (CollectionUtils.isNotEmpty(temporalStopList)) {
      List<Date> dates = convertDateList(temporalStopList);
      metacard.setAttribute(DateTime.END, (Serializable) dates);
    }
  }

  private void addMetacardResourceStatusCode(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    addFieldsToMetacardIfPresent(
        pathValueTracker,
        metacard,
        GmdConstants.RESOURCE_STATUS_PATH,
        GmdConstants.RESOURCE_STATUS);
  }

  private void addMetacardCountryCodes(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    List<String> countryCodes =
        pathValueTracker.getAllValues(toPath(GmdConstants.COUNTRY_CODE_PATH));
    if (CollectionUtils.isNotEmpty(countryCodes)) {
      setCountryCodes(countryCodes, metacard);
    }
  }

  public void addMetacardCrs(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    List<String> crsCodes = new ArrayList<>();
    List<String> types = pathValueTracker.getAllValues(toPath(GmdConstants.CRS_CODE_PATH));
    List<String> codes = pathValueTracker.getAllValues(toPath(GmdConstants.CRS_AUTHORITY_PATH));
    if (CollectionUtils.isNotEmpty(codes) && CollectionUtils.isNotEmpty(types)) {
      for (int i = 0; i < codes.size() && i < types.size(); i++) {
        String crs = codes.get(i) + ":" + types.get(i);
        crsCodes.add(crs);
      }
    }
    metacard.setAttribute(Location.COORDINATE_REFERENCE_SYSTEM_CODE, (Serializable) crsCodes);
  }

  private void addMetacardContactPOCInformation(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    List<String> organisationNames =
        pathValueTracker.getAllValues(toPath(GmdConstants.POINT_OF_CONTACT_ORGANISATION_PATH));
    if (CollectionUtils.isNotEmpty(organisationNames)) {
      metacard.setAttribute(Contact.POINT_OF_CONTACT_NAME, (Serializable) organisationNames);
    }

    addFieldsToMetacardIfPresent(
        pathValueTracker,
        metacard,
        GmdConstants.POINT_OF_CONTACT_PHONE_PATH,
        Contact.POINT_OF_CONTACT_PHONE);

    addFieldsToMetacardIfPresent(
        pathValueTracker,
        metacard,
        GmdConstants.POINT_OF_CONTACT_EMAIL_PATH,
        Contact.POINT_OF_CONTACT_EMAIL);

    List<String> deliveryPoints =
        pathValueTracker.getAllValues(
            toPath(GmdConstants.POINT_OF_CONTACT_ADDRESS_DELIVERY_POINT_PATH));
    if (CollectionUtils.isNotEmpty(deliveryPoints)) {
      List<String> cities =
          pathValueTracker.getAllValues(toPath(GmdConstants.POINT_OF_CONTACT_ADDRESS_CITY_PATH));
      List<String> administrativeAreas =
          pathValueTracker.getAllValues(
              toPath(GmdConstants.POINT_OF_CONTACT_ADDRESS_ADMINISTRATIVE_AREA_PATH));
      List<String> postalCodes =
          pathValueTracker.getAllValues(
              toPath(GmdConstants.POINT_OF_CONTACT_ADDRESS_POSTAL_CODE_PATH));
      List<String> countries =
          pathValueTracker.getAllValues(toPath(GmdConstants.POINT_OF_CONTACT_ADDRESS_COUNTRY_PATH));
      List<String> addresses = new ArrayList<>();
      for (int i = 0; i < deliveryPoints.size(); i++) {
        StringBuilder address = new StringBuilder(deliveryPoints.get(i));
        address.append(getStringFromListAtIndexIfNotEmpty(cities, i));
        address.append(getStringFromListAtIndexIfNotEmpty(administrativeAreas, i));
        address.append(getStringFromListAtIndexIfNotEmpty(postalCodes, i));
        address.append(getStringFromListAtIndexIfNotEmpty(countries, i));
        addresses.add(address.toString().trim());
      }
      metacard.setAttribute(Contact.POINT_OF_CONTACT_ADDRESS, (Serializable) addresses);
    }
  }

  private void setCountryCodes(List<String> countryCodes, MetacardImpl metacard) {
    List<String> filteredCountryCodes = filterCountryCodes(countryCodes);
    if (filteredCountryCodes.size() > 0) {
      metacard.setAttribute(Location.COUNTRY_CODE, (Serializable) filteredCountryCodes);
    }
  }

  private List<String> filterCountryCodes(List<String> countryCodes) {
    List<String> filteredCountryCodes = new ArrayList<>();
    countryCodes.forEach(
        countryCode -> {
          if (countryCode.length() == 3) {
            filteredCountryCodes.add(countryCode);
          } else {
            LOGGER.debug("{} does not match alpha-3 format.  Skipping country code.", countryCode);
          }
        });
    return filteredCountryCodes;
  }

  private void addMetacardLocation(
      final XstreamPathValueTracker pathValueTracker, MetacardImpl metacard) {
    String westLon = pathValueTracker.getFirstValue(toPath(GmdConstants.BBOX_WEST_LON_PATH));
    String eastLon = pathValueTracker.getFirstValue(toPath(GmdConstants.BBOX_EAST_LON_PATH));
    String southLat = pathValueTracker.getFirstValue(toPath(GmdConstants.BBOX_SOUTH_LAT_PATH));
    String northLat = pathValueTracker.getFirstValue(toPath(GmdConstants.BBOX_NORTH_LAT_PATH));

    String boundingPolygon =
        pathValueTracker.getFirstValue(toPath(GmdConstants.BOUNDING_POLYGON_PATH));
    String boundingPolygonLineString =
        pathValueTracker.getFirstValue(toPath(GmdConstants.BOUNDING_POLYGON_LINE_STRING_PATH));

    if (StringUtils.isNotEmpty(boundingPolygon)) {
      setLocationFromPolygon(boundingPolygon, metacard);
    } else if (StringUtils.isNotEmpty(boundingPolygonLineString)) {
      setLocationFromLineString(boundingPolygonLineString, metacard);
    } else if (StringUtils.isNotEmpty(northLat)) {
      setLocationFromBoundingBox(metacard, northLat, southLat, eastLon, westLon);
    }
  }

  private void setLocationFromBoundingBox(
      MetacardImpl metacard, String north, String south, String east, String west) {
    try {
      Envelope envelope =
          new Envelope(
              Double.parseDouble(east.trim()),
              Double.parseDouble(west.trim()),
              Double.parseDouble(south.trim()),
              Double.parseDouble(north.trim()));
      String wkt = WKT_WRITER_THREAD_LOCAL.get().write(factory.toGeometry(envelope));
      if (wkt != null) {
        metacard.setAttribute(Core.LOCATION, wkt);
      }
    } catch (NumberFormatException nfe) {
      LOGGER.debug(
          "Unable to parse double from GMD metadata {}, {}, {}, {}", west, east, south, north);
    }
  }

  private void setLocationFromPolygon(String boundingPolygon, MetacardImpl metacard) {
    try {
      String[] stringArray = boundingPolygon.split(" ");
      List<Coordinate> coordinates = new ArrayList<>();
      for (int i = 0; i < stringArray.length - 1; i += 2) {
        coordinates.add(
            new Coordinate(
                Double.parseDouble(stringArray[i]), Double.parseDouble(stringArray[i + 1])));
      }

      LinearRing linearRing =
          factory.createLinearRing(coordinates.toArray(new Coordinate[coordinates.size()]));
      String wkt = WKT_WRITER_THREAD_LOCAL.get().write(factory.createPolygon(linearRing, null));
      if (wkt != null) {
        metacard.setAttribute(Core.LOCATION, wkt);
      }
    } catch (NumberFormatException nfe) {
      LOGGER.debug(
          "Unable to parse double in {}. Metacard location will not be set.", boundingPolygon);
    }
  }

  private void setLocationFromLineString(String lineString, MetacardImpl metacard) {
    try {
      String[] stringArray = lineString.split(" ");
      List<Coordinate> coordinates = new ArrayList<>();
      for (int i = 0; i < stringArray.length - 1; i += 2) {
        coordinates.add(
            new Coordinate(
                Double.parseDouble(stringArray[i]), Double.parseDouble(stringArray[i + 1])));
      }
      String wkt =
          WKT_WRITER_THREAD_LOCAL
              .get()
              .write(
                  factory.createLineString(
                      coordinates.toArray(new Coordinate[coordinates.size()])));
      if (wkt != null) {
        metacard.setAttribute(Core.LOCATION, wkt);
      }
    } catch (NumberFormatException nfe) {
      LOGGER.debug("Unable to parse double in {}. Metacard location will not be set.", lineString);
    }
  }

  private void setMetacardLocationFromBoundingPolygonPoints(Metacard metacard) {
    try (InputStream inputStream = getSourceInputStream()) {
      DocumentBuilder builder = XML_UTILS.getSecureDocumentBuilder(false);
      Document document = builder.parse(inputStream);

      XPath xPath = XPathFactory.newInstance().newXPath();
      XPathExpression dataExpression = xPath.compile(GmdConstants.BOUNDING_POLYGON_POINT_PATH);
      NodeList dataList = (NodeList) dataExpression.evaluate(document, XPathConstants.NODESET);

      if (dataList.getLength() > 0) {
        List<Coordinate> coordinates = new ArrayList<>();
        for (int i = 0; i < dataList.getLength(); i++) {
          Node node = dataList.item(i);
          String[] coordinateStrings = node.getTextContent().split(" ");
          if (coordinateStrings.length == 2) {
            Double lat = Double.parseDouble(coordinateStrings[0]);
            Double lon = Double.parseDouble(coordinateStrings[1]);
            Coordinate coordinate = new Coordinate(lat, lon);
            coordinates.add(coordinate);
          }
        }

        if (CollectionUtils.isNotEmpty(coordinates)) {
          // Close the polygon
          coordinates.add(coordinates.get(0));
          LinearRing linearRing =
              factory.createLinearRing(coordinates.toArray(new Coordinate[coordinates.size()]));
          String wkt = WKT_WRITER_THREAD_LOCAL.get().write(factory.createPolygon(linearRing, null));
          if (wkt != null) {
            metacard.setAttribute(new AttributeImpl(Core.LOCATION, wkt));
          }
        }
      }
    } catch (NumberFormatException
        | ParserConfigurationException
        | IOException
        | SAXException
        | XPathExpressionException e) {
      LOGGER.debug(
          "Unable to parse location in XML document.  Metacard location will not be set.", e);
    }
  }

  private void setMetacardDates(Metacard metacard, final XstreamPathValueTracker pathValueTracker) {
    try (InputStream inputStream = getSourceInputStream()) {
      DocumentBuilder builder = XML_UTILS.getSecureDocumentBuilder(false);
      Document document = builder.parse(inputStream);

      XPath xPath = XPathFactory.newInstance().newXPath();
      XPathExpression dataExpression = xPath.compile(GmdConstants.RESOURCE_DATE_PATH);
      NodeList dataList = (NodeList) dataExpression.evaluate(document, XPathConstants.NODESET);

      if (dataList != null && dataList.getLength() > 0) {
        List<String> dateList = new ArrayList<>();
        List<String> dateTypes =
            pathValueTracker.getAllValues(toPath(GmdConstants.CITATION_DATE_TYPE_PATH));

        for (int i = 0; i < dataList.getLength(); i++) {
          Node node = dataList.item(i);
          String datestring =
              node.getTextContent()
                  .trim()
                  .replaceAll(WHITE_SPACE_PATTER.pattern(), " ")
                  .replaceAll(NEW_LINE_PATTERN.pattern(), " ");
          String[] stringArray = datestring.split(" ");
          dateList.add(stringArray[0]);
        }
        if (CollectionUtils.isNotEmpty(dateList)
            && CollectionUtils.isNotEmpty(dateTypes)
            && dateList.size() == dateTypes.size()) {
          setDates(dateList, dateTypes, metacard);
        }
      }
    } catch (ParserConfigurationException
        | IOException
        | SAXException
        | XPathExpressionException e) {
      LOGGER.debug(
          "Unable to parse dates in XML document.  Metacard Created / Effective / Modified dates will not be set.",
          e);
    }
  }

  private void setDates(List<String> dates, List<String> dateTypes, Metacard metacard) {
    for (int i = 0; i < dates.size() && i < dateTypes.size(); i++) {
      String type = dateTypes.get(i);
      String dateString = dates.get(i);
      if (StringUtils.isNotEmpty(dateString)) {
        if (dateString.length() == 10) {
          dateString = dateString + ZERO_HOUR_SUFFIX;
        }
        Date date = CswUnmarshallHelper.convertToDate(dateString);

        switch (type) {
          case GmdConstants.CREATION:
            metacard.setAttribute(new AttributeImpl(Core.CREATED, date));
            break;

          case GmdConstants.EXPIRY:
            metacard.setAttribute(new AttributeImpl(Core.EXPIRATION, date));
            break;

          case GmdConstants.LAST_UPDATE:
            metacard.setAttribute(new AttributeImpl(Core.MODIFIED, date));
            break;

          case GmdConstants.REVISION:
            metacard.setAttribute(new AttributeImpl(Core.MODIFIED, date));
            break;

          case GmdConstants.PUBLICATION:
            metacard.setAttribute(new AttributeImpl(Metacard.EFFECTIVE, date));
            break;
        }
      }
    }
  }

  public void addFieldToMetacardIfPresent(
      final XstreamPathValueTracker pathValueTracker,
      MetacardImpl metacard,
      String value,
      String metacardField) {
    String firstValue = pathValueTracker.getFirstValue(toPath(value));
    if (StringUtils.isNotEmpty(firstValue)) {
      metacard.setAttribute(metacardField, firstValue);
    }
  }

  public void addFieldsToMetacardIfPresent(
      final XstreamPathValueTracker pathValueTracker,
      MetacardImpl metacard,
      String value,
      String metacardField) {
    List<String> values = pathValueTracker.getAllValues(toPath(value));
    if (CollectionUtils.isNotEmpty(values)) {
      metacard.setAttribute(metacardField, (Serializable) values);
    }
  }

  public Path toPath(String stringPath) {
    return new Path(stringPath.replace(GCO_PREFIX, "").replace(GML_PREFIX, ""));
  }

  private String getStringFromListAtIndexIfNotEmpty(List<String> list, int index) {
    String result = "";
    if (CollectionUtils.isNotEmpty(list) && index < list.size()) {
      result = " " + list.get(index);
    }
    return result;
  }

  public List<Double> covertStringListToDoubleList(List<String> stringList) {
    List<Double> doubles = new ArrayList<>();
    stringList.forEach(
        string -> {
          try {
            Double altitudeDouble = Double.parseDouble(string);
            doubles.add(altitudeDouble);
          } catch (NumberFormatException e) {
            LOGGER.debug("Unable to parse {} into double.  Skipping entry.", string);
          }
        });
    return doubles;
  }

  private List<Date> convertDateList(List<String> starts) {
    List<Date> dates = new ArrayList<>();
    starts.forEach(
        dateString -> {
          if (dateString.length() == 10) {
            dateString = dateString + ZERO_HOUR_SUFFIX;
          }
          Date date = CswUnmarshallHelper.convertToDate(dateString);
          dates.add(date);
        });
    return dates;
  }
}
