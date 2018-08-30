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

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class HtmlMetacardModelTest {

  private static final String METACARD_TITLE = "The Title";

  private List<String> coreList;

  @Before
  public void setup() {
    coreList = new ArrayList<>();
    coreList.add(Core.TITLE);
  }

  @Test
  public void testCategoryAttributeList() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setAttribute(Core.TITLE, METACARD_TITLE);

    List<HtmlExportCategory> categoryList = new ArrayList<>();
    categoryList.add(new HtmlCategoryModel("Core", coreList));

    HtmlMetacardModel metacardModel = new HtmlMetacardModel(metacard, categoryList);
    HtmlExportCategory category = metacardModel.getCategories().get(0);

    assertThat(category.getAttributes(), hasItem(Core.TITLE));
  }

  @Test
  public void testCategoryAttributeMapping() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setAttribute(Core.TITLE, METACARD_TITLE);

    List<HtmlExportCategory> categoryList = new ArrayList<>();
    categoryList.add(new HtmlCategoryModel("Core", coreList));

    HtmlMetacardModel metacardModel = new HtmlMetacardModel(metacard, categoryList);
    HtmlExportCategory category = metacardModel.getCategories().get(0);

    String titleValue = category.getAttributeMappings().get("Title").getValue();
    assertThat(titleValue, is(METACARD_TITLE));
  }
}
