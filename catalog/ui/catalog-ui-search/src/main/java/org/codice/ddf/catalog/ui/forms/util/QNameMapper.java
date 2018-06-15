package org.codice.ddf.catalog.ui.forms.util;

import javax.xml.namespace.QName;

public class QNameMapper {

  private static final QName BOOLEAN = new QName("http://www.w3.org/2001/XMLSchema", "boolean");

  private static final QName STRING = new QName("http://www.w3.org/2001/XMLSchema", "string");

  private static final QName INTEGER = new QName("http://www.w3.org/2001/XMLSchema", "integer");

  public static Object convert(Object object, QName qName) {
    if (INTEGER.equals(qName)) {
      return Integer.valueOf(object.toString());
    } else if (BOOLEAN.equals(qName)) {
      return Boolean.valueOf(object.toString());
    }

    return object.toString();
  }

  public static QName convert(Object object) {
    if (object instanceof Integer) {
      return INTEGER;
    } else if (object instanceof Boolean) {
      return BOOLEAN;
    }

    return STRING;
  }

  private QNameMapper() {}
}
