/*
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.kml.converter;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.DateTime;
import java.text.ParseException;
import java.util.Date;
import net.opengis.kml.v_2_2_0.AbstractFeatureType;
import net.opengis.kml.v_2_2_0.AbstractTimePrimitiveType;
import net.opengis.kml.v_2_2_0.KmlType;
import net.opengis.kml.v_2_2_0.TimeSpanType;
import net.opengis.kml.v_2_2_0.TimeStampType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KmlToMetacard {
  public static final Logger LOGGER = LoggerFactory.getLogger(KmlToMetacard.class);

  public static final String Y = "yyyy";

  public static final String YM = "yyyy-MM";

  public static final String YMD = "yyyy-MM-dd";

  public static final String YMDHM = "yyyy-MM-dd'T'hh:mm";

  public static final String YMDHMS = "yyyy-MM-dd'T'hh:mm:ss";

  public static final String YMDHMSFS = "yyyy-MM-dd'T'hh:mm:ss.s";

  public static final String YMDHMZ = "yyyy-MM-dd'T'hh:mmZ";

  public static final String YMDHMSZ = "yyyy-MM-dd'T'hh:mm:ssZ";

  public static final String YMDHMSFSZ = "yyyy-MM-dd'T'hh:mm:ss.sZ";

  private static final String[] DATE_FORMATS = {
    Y, YM, YMD, YMDHM, YMDHMS, YMDHMSFS, YMDHMZ, YMDHMSZ, YMDHMSFSZ
  };

  private static final ThreadLocal<WKTWriter> WKT_WRITER_THREAD_LOCAL =
      ThreadLocal.withInitial(WKTWriter::new);

  private KmlToMetacard() {}

  public static Metacard from(Metacard metacard, KmlType kml) {
    if (kml == null) {
      LOGGER.debug("Null kml received. Nothing to convert.");
      return null;
    }

    setLocation(metacard, kml);
    setDates(metacard, kml);
    setDescription(metacard, kml);
    setContactInfo(metacard, kml);

    return metacard;
  }

  private static void setLocation(Metacard metacard, KmlType kml) {
    String location = getBboxFromKml(kml);

    if (StringUtils.isNotBlank(location)) {
      ((MetacardImpl) metacard).setLocation(location);
    }
  }

  private static void setDates(Metacard metacard, KmlType kml) {
    setDatesFromFeature(metacard, kml.getAbstractFeatureGroup().getValue());
  }

  private static void setDescription(Metacard metacard, KmlType kml) {
    setDescriptionFromFeature(metacard, kml.getAbstractFeatureGroup().getValue());
  }

  private static void setContactInfo(Metacard metacard, KmlType kml) {
    setContactInfoFromFeature(metacard, kml.getAbstractFeatureGroup().getValue());
  }

  private static void setContactInfoFromFeature(Metacard metacard, AbstractFeatureType feature) {
    if (feature == null) {
      return;
    }

    String phoneNumber = feature.getPhoneNumber();
    if (StringUtils.isNotBlank(phoneNumber)) {
      metacard.setAttribute(new AttributeImpl(Contact.POINT_OF_CONTACT_PHONE, phoneNumber));
    }

    String address = feature.getAddress();
    if (StringUtils.isNotBlank(address)) {
      metacard.setAttribute(new AttributeImpl(Contact.POINT_OF_CONTACT_ADDRESS, address));
    }
  }

  private static void setDescriptionFromFeature(Metacard metacard, AbstractFeatureType feature) {
    if (feature == null) {
      return;
    }

    String description = feature.getDescription();
    if (StringUtils.isNotBlank(description)) {
      ((MetacardImpl) metacard).setDescription(description);
    }
  }

  private static void setDatesFromFeature(Metacard metacard, AbstractFeatureType feature) {
    if (feature == null) {
      return;
    }

    setDatesFromTimePrimitive(metacard, feature.getAbstractTimePrimitiveGroup().getValue());
  }

  private static void setDatesFromTimePrimitive(
      Metacard metacard, AbstractTimePrimitiveType timePrimitive) {
    if (timePrimitive == null) {
      return;
    }

    if (timePrimitive instanceof TimeSpanType) {
      setDatesFromTimeSpan(metacard, (TimeSpanType) timePrimitive);
    }

    if (timePrimitive instanceof TimeStampType) {
      setDatesFromTimeStamp(metacard, (TimeStampType) timePrimitive);
    }
  }

  private static void setDatesFromTimeSpan(Metacard metacard, TimeSpanType timeSpan) {
    if (timeSpan == null) {
      return;
    }

    String begin = timeSpan.getBegin();
    if (StringUtils.isNotBlank(begin)) {
      ((MetacardImpl) metacard).setCreatedDate(getDateFromString(begin));
    }

    String end = timeSpan.getEnd();
    if (StringUtils.isNotBlank(end) && StringUtils.isNotBlank(begin)) {
      metacard.setAttribute(new AttributeImpl(DateTime.END, getDateFromString(end)));
      metacard.setAttribute(new AttributeImpl(DateTime.START, getDateFromString(begin)));
    }
  }

  private static void setDatesFromTimeStamp(Metacard metacard, TimeStampType timeStamp) {
    if (timeStamp == null) {
      return;
    }

    String created = timeStamp.getWhen();
    if (StringUtils.isNotBlank(created)) {
      ((MetacardImpl) metacard).setCreatedDate(getDateFromString(created));
    }
  }

  private static String getBboxFromKml(KmlType kml) {
    Geometry geometry = KmlToJtsConverter.from(kml);
    String bBox = "";
    if (geometry != null) {
      bBox = WKT_WRITER_THREAD_LOCAL.get().write(geometry.getEnvelope());
    }

    return bBox;
  }

  private static Date getDateFromString(String dateTime) {
    if (StringUtils.isBlank(dateTime)) {
      return null;
    }

    Date date = null;
    try {
      date = DateUtils.parseDate(dateTime, DATE_FORMATS);
    } catch (ParseException e) {
      LOGGER.debug("Error parsing date from the string '{}'.", dateTime, e);
    }

    return date;
  }
}
