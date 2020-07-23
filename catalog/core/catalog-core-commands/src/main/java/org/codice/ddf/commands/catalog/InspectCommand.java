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
package org.codice.ddf.commands.catalog;

import com.google.common.collect.ImmutableList;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.fusesource.jansi.Ansi;
import org.opengis.filter.Filter;

@Service
@Command(
    scope = CatalogCommands.NAMESPACE,
    name = "inspect",
    description = "Provides the various fields of a metacard for inspection.")
public class InspectCommand extends CatalogCommands {

  @Argument(
      name = "ID",
      description = "The id of the document that is to be inspected.",
      index = 0,
      multiValued = false,
      required = true)
  String id = null;

  @Override
  protected Object executeWithSubject() throws Exception {
    if (id == null) {
      return null;
    }

    Filter filter = filterBuilder.attribute(Metacard.ID).is().equalTo().text(id);

    QueryImpl query = new QueryImpl(filter);

    SourceResponse response = getCatalog().query(new QueryRequestImpl(query));

    String formatString = "%1$s%2$-30s%3$s: %4$-25s %n";

    String newLineFormatString = "%1$s%2$-30s%3$s  %4$-25s %n";

    if (response.getResults() != null && response.getResults().size() > 0) {

      Metacard card = response.getResults().get(0).getMetacard();

      MetacardType mType = card.getMetacardType();

      String rClass = card.getClass().getName();
      String siteShortName = card.getSourceId();

      console.println("------------------------------");

      console.printf(formatString, color(rClass), "Class", color(rClass), rClass);

      console.printf(
          formatString, color(siteShortName), "Sitename", color(siteShortName), siteShortName);

      for (AttributeDescriptor ad : mType.getAttributeDescriptors()) {

        boolean isMultiValued = ad.isMultiValued();

        Attribute attribute = card.getAttribute(ad.getName());

        List<Serializable> value = null;

        if (attribute != null) {
          value =
              isMultiValued
                  ? ImmutableList.copyOf(attribute.getValues())
                  : ImmutableList.of(attribute.getValue());
        }

        // indent items with new lines in the value
        if (value != null
            && (value.toString().contains("\r\n") || value.toString().contains("\n"))) {
          String valueString = value.toString();

          String[] values = valueString.split("\r\n");

          if (values.length < 2) {
            values = valueString.split("\n");
          }

          console.printf(formatString, color(attribute), ad.getName(), color(attribute), values[0]);

          for (int i = 1; i < values.length; i++) {
            console.printf(newLineFormatString, color(""), "", color(""), values[i]);
          }

        } else {
          console.printf(formatString, color(attribute), ad.getName(), color(attribute), value);
        }
      }

      // make sure we put the console default color back.
      console.print(defaultColor());
      console.println("------------------------------");
    }

    return null;
  }

  /**
   * Colors the text if a value exists.
   *
   * @param obj
   * @return
   */
  private String color(Object obj) {
    if (obj == null) {
      return defaultColor();
    }

    if (obj instanceof Collection) {
      if (((Collection) obj).isEmpty()) {
        return defaultColor();
      }
    }

    return color();
  }

  protected String defaultColor() {
    return Ansi.ansi().reset().toString();
  }

  protected String color() {
    return Ansi.ansi().fg(Ansi.Color.CYAN).toString();
  }
}
