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
package ddf.catalog.impl.operations;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Core;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.collections.MapUtils;

public class OverrideAttributesSupport {
  private OverrideAttributesSupport() {}

  public static void overrideAttributes(
      List<ContentItem> contentItems, Map<String, Metacard> metacardMap) {
    for (ContentItem contentItem : contentItems) {
      if (contentItem.getMetacard() == null) {
        continue;
      }

      Metacard currentMetacard = metacardMap.get(contentItem.getId());
      Metacard overrideMetacard = contentItem.getMetacard();

      Metacard updatedMetacard = overrideMetacard(currentMetacard, overrideMetacard, false);

      metacardMap.put(contentItem.getId(), updatedMetacard);
    }
  }

  public static Metacard overrideMetacard(
      Metacard currentMetacard, Metacard overrideMetacard, boolean onlyFillNull) {

    MetacardImpl updatedMetacard = new MetacardImpl(currentMetacard);
    updatedMetacard.setType(
        new MetacardTypeImpl(
            currentMetacard.getMetacardType().getName(),
            currentMetacard.getMetacardType(),
            overrideMetacard.getMetacardType().getAttributeDescriptors()));

    addAttributes(updatedMetacard, overrideMetacard, onlyFillNull);
    return updatedMetacard;
  }

  private static void addAttributes(
      Metacard metacard, Metacard otherMetacard, boolean onlyFillNull) {
    metacard
        .getMetacardType()
        .getAttributeDescriptors()
        .stream()
        .map(attributeDescriptor -> otherMetacard.getAttribute(attributeDescriptor.getName()))
        .filter(Objects::nonNull)
        .filter(attribute -> !attribute.getName().equals(Core.ID))
        .filter(attribute -> !onlyFillNull || metacard.getAttribute(attribute.getName()) == null)
        .forEach(metacard::setAttribute);
  }

  public static void applyAttributeOverridesToMetacardMap(
      Map<String, Serializable> attributeOverrideMap, Map<String, Metacard> metacardMap) {

    if (MapUtils.isEmpty(attributeOverrideMap) || MapUtils.isEmpty(metacardMap)) {
      return;
    }

    metacardMap
        .values()
        .forEach(
            metacard ->
                attributeOverrideMap
                    .keySet()
                    .stream()
                    .map(
                        attributeName ->
                            metacard.getMetacardType().getAttributeDescriptor(attributeName))
                    .filter(Objects::nonNull)
                    .map(ad -> overrideAttributeValue(ad, attributeOverrideMap.get(ad.getName())))
                    .filter(Objects::nonNull)
                    .forEach(metacard::setAttribute));
  }

  private static AttributeImpl overrideAttributeValue(
      AttributeDescriptor attributeDescriptor, Serializable overrideValue) {
    List<Serializable> newValue = new ArrayList<>();
    for (Object o :
        overrideValue instanceof List
            ? (List) overrideValue
            : Collections.singletonList(overrideValue)) {
      try {
        String override = String.valueOf(o);
        switch (attributeDescriptor.getType().getAttributeFormat()) {
          case INTEGER:
            newValue.add(Integer.parseInt(override));
            break;
          case FLOAT:
            newValue.add(Float.parseFloat(override));
            break;
          case DOUBLE:
            newValue.add(Double.parseDouble(override));
            break;
          case SHORT:
            newValue.add(Short.parseShort(override));
            break;
          case LONG:
            newValue.add(Long.parseLong(override));
            break;
          case DATE:
            Calendar calendar = DatatypeConverter.parseDateTime(override);
            newValue.add(calendar.getTime());
            break;
          case BOOLEAN:
            newValue.add(Boolean.parseBoolean(override));
            break;
          case BINARY:
            newValue.add(override.getBytes(Charset.forName("UTF-8")));
            break;
          case OBJECT:
          case STRING:
          case GEOMETRY:
          case XML:
            newValue.add(override);
            break;
        }
      } catch (IllegalArgumentException e) {
        return null;
      }
    }
    return new AttributeImpl(attributeDescriptor.getName(), newValue);
  }
}
