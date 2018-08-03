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
package ddf.catalog.transformer.html.models;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang.WordUtils;

public class HtmlCategoryModel {

  private String title;

  private List<String> attributeList;

  private Map<String, Object> attributeMappings;

  public HtmlCategoryModel(String title, List<String> attributeList) {
    this.title = title;
    this.attributeList = attributeList;
    this.attributeMappings = new TreeMap<>();
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return this.title;
  }

  public void setAttributeList(List<String> attributeList) {
    this.attributeList = attributeList;
  }

  public List<String> getAttributeList() {
    return this.attributeList;
  }

  public void setAttributes(Map<String, Object> attributes) {
    this.attributeMappings = attributes;
  }

  public Map<String, Object> getAttributes() {
    return this.attributeMappings;
  }

  public void applyAttributeMappings(Metacard metacard) {
    this.attributeMappings = new TreeMap<>();

    for (String attrKey : attributeList) {
      // TODO Figure out what the difference between getValue() is and getValues()
      String readableKey = getHumanReadableAttribute(attrKey);
      Attribute attr = metacard.getAttribute(attrKey);

      if (attr == null) {
        this.attributeMappings.put(readableKey, new HtmlEmptyValueModel());
      } else if (attrKey.equals("thumbnail")) {
        byte[] imageData = (byte[]) attr.getValue();
        this.attributeMappings.put(readableKey, new HtmlMediaModel(imageData));
      } else {
        this.attributeMappings.put(readableKey, new HtmlBasicValueModel(attr.getValue()));
      }
    }
  }

  private String getHumanReadableAttribute(String attr) {
    int periodIndex = attr.lastIndexOf('.');
    if (periodIndex != -1) {
      attr = attr.substring(periodIndex + 1, attr.length());
    }

    attr = attr.replaceAll("-", " ");

    return WordUtils.capitalizeFully(attr);
  }
}
