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

public class HtmlMetacardModel {

  private String title;

  private Metacard metacard;

  private List<HtmlCategoryModel> categories;

  public HtmlMetacardModel(Metacard metacard) {
    this.metacard = metacard;
    this.title = metacard.getTitle();
    this.categories = new ArrayList<>();
  }

  public HtmlMetacardModel(Metacard metacard, List<HtmlCategoryModel> categories) {
    this.metacard = metacard;
    this.title = metacard.getTitle();
    this.categories = categories;

    this.applyMetacard();
  }

  public void setMetacard(Metacard metacard) {
    this.metacard = metacard;
  }

  public Metacard getMetacard() {
    return this.metacard;
  }

  public void setCategories(List<HtmlCategoryModel> categories) {
    this.categories = categories;
  }

  public List<HtmlCategoryModel> getCategories() {
    return this.categories;
  }

  public void addCategory(HtmlCategoryModel category) {
    this.categories.add(category);
  }

  public void applyMetacard() {
    for (HtmlCategoryModel category : categories) {
      category.applyAttributeMappings(metacard);
    }
  }
}
