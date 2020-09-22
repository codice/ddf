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
import ddf.catalog.data.types.Core;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.lang.WordUtils;

public class HtmlCategoryModel implements HtmlExportCategory {

  private String title;

  private List<String> attributes;

  private Map<String, HtmlValueModel> attributeMappings;

  public HtmlCategoryModel() {
    this("", new ArrayList<>());
    this.attributeMappings = new TreeMap<>();
  }

  public HtmlCategoryModel(String title, List<String> attributes) {
    this.title = title;
    this.attributes = attributes;
    this.attributeMappings = new TreeMap<>();
  }

  public void init() {
    // Called from blueprint
  }

  public void destroy(int code) {
    // Called from blueprint
  }

  @Override
  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public String getTitle() {
    return this.title;
  }

  @Override
  public void setAttributes(List<String> attributes) {
    this.attributes = attributes;
  }

  @Override
  public List<String> getAttributes() {
    return this.attributes;
  }

  public void setAttributes(Map<String, HtmlValueModel> attributes) {
    this.attributeMappings = attributes;
  }

  @Override
  public Map<String, HtmlValueModel> getAttributeMappings() {
    return this.attributeMappings;
  }

  @Override
  public void applyAttributeMappings(Metacard metacard) {
    this.attributeMappings = new TreeMap<>();

    for (String attrKey : attributes) {
      String readableKey = getHumanReadableAttribute(attrKey);
      Attribute attr = metacard.getAttribute(attrKey);

      HtmlValueModel model;

      if (attr == null) {
        model = new HtmlEmptyValueModel();
        this.attributeMappings.put(readableKey, model);
      } else if (attrKey.equals(Core.THUMBNAIL)) {
        byte[] imageData = (byte[]) attr.getValue();
        model = new HtmlMediaModel(imageData);
      } else {
        model = new HtmlBasicValueModel(attr.getValue());
      }

      this.attributeMappings.put(readableKey, model);
    }
  }

  private String getHumanReadableAttribute(String attr) {
    String readableAttr = attr;

    int periodIndex = readableAttr.lastIndexOf('.');
    if (periodIndex != -1) {
      readableAttr = attr.substring(periodIndex + 1);
    }

    readableAttr = readableAttr.replaceAll("-", " ");

    return WordUtils.capitalizeFully(readableAttr);
  }
}
