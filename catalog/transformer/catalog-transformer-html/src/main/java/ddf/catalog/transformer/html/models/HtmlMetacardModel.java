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

import ddf.catalog.data.Metacard;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HtmlMetacardModel {

  private String title;

  private Metacard metacard;

  private List<HtmlExportCategory> categories;

  public HtmlMetacardModel(Metacard metacard) {
    this.metacard = metacard;
    this.title = metacard.getTitle();
    this.categories = new ArrayList<>();
  }

  public HtmlMetacardModel(Metacard metacard, List<HtmlExportCategory> categories) {
    this.metacard = metacard;
    this.title = metacard.getTitle();

    this.categories =
        categories.stream()
            .map(category -> new HtmlCategoryModel(category.getTitle(), category.getAttributes()))
            .collect(Collectors.toList());

    this.applyAttributeMappingsToMetacard();
  }

  public void setMetacard(Metacard metacard) {
    this.metacard = metacard;
  }

  public Metacard getMetacard() {
    return this.metacard;
  }

  public void setCategories(List<HtmlExportCategory> categories) {
    this.categories = categories;
  }

  public List<HtmlExportCategory> getCategories() {
    return this.categories;
  }

  private void applyAttributeMappingsToMetacard() {
    for (HtmlExportCategory category : categories) {
      category.applyAttributeMappings(metacard);
    }
  }
}
