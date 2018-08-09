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
package ddf.catalog.transformer.html;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Associations;
import ddf.catalog.data.types.Core;
import ddf.catalog.transformer.html.models.HtmlCategoryModel;
import ddf.catalog.transformer.html.models.HtmlExportCategory;
import ddf.catalog.transformer.html.models.HtmlMetacardModel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;

public class HtmlMetacardUtilityTest {

  private static final String METACARD_CLASS = ".metacard";
  private static final String CATEGORY_CLASS = ".metacard-category";
  private static final String CATEGORY_TABLE_CLASS = ".category-table";
  private static final String EMPTY_ATTRIBUTE_CLASS = ".empty-attribute";
  private static final String MEDIA_ATTRIBUTE_CLASS = ".media-attribute";
  private static final String METACARD_ATTRIBUTE_CLASS = ".metacard-attribute";

  private static final List<String> EMPTY_ATTRIBUTE_LIST = Collections.emptyList();
  private static final List<HtmlExportCategory> EMPTY_CATEGORY_LIST = Collections.emptyList();

  private List<String> associationsList;
  private List<String> coreList;

  private static final String[] ALL_CATEGORY_TITLES =
      new String[] {
        "Associations",
        "Contact",
        "Core",
        "DateTime",
        "Location",
        "Media",
        "Security",
        "Topic",
        "Validation",
        "Version"
      };

  @Before
  public void setup() {
    associationsList = new ArrayList<>();
    associationsList.add(Associations.RELATED);
    associationsList.add(Associations.DERIVED);
    associationsList.add(Associations.EXTERNAL);

    coreList = new ArrayList<>();
    coreList.add(Core.THUMBNAIL);
  }

  @Test
  public void testMetacardCreation() {
    Metacard metacard = new MetacardImpl();

    HtmlMetacardUtility htmlMetacardUtility = new HtmlMetacardUtility(EMPTY_CATEGORY_LIST);

    List<HtmlMetacardModel> metacardModelList = new ArrayList<>();
    metacardModelList.add(new HtmlMetacardModel(metacard, EMPTY_CATEGORY_LIST));
    metacardModelList.add(new HtmlMetacardModel(metacard, EMPTY_CATEGORY_LIST));

    Document doc = Jsoup.parse(htmlMetacardUtility.buildHtml(metacardModelList));

    assertThat(doc.select(METACARD_CLASS).size(), is(metacardModelList.size()));
  }

  @Test
  public void testCategoryCreation() {
    Metacard metacard = new MetacardImpl();

    List<HtmlExportCategory> categories = getAllEmptyCategories(ALL_CATEGORY_TITLES);
    List<HtmlMetacardModel> metacardModelList = new ArrayList<>();
    metacardModelList.add(new HtmlMetacardModel(metacard, categories));

    HtmlMetacardUtility htmlMetacardUtility = new HtmlMetacardUtility();

    Document doc = Jsoup.parse(htmlMetacardUtility.buildHtml(metacardModelList));

    assertThat(doc.select(METACARD_CLASS).size(), is(metacardModelList.size()));
    assertThat(doc.select(CATEGORY_CLASS).size(), is(categories.size()));
  }

  @Test
  public void testAssociationsAttributes() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setAttribute(Associations.RELATED, "");
    metacard.setAttribute(Associations.DERIVED, "");
    metacard.setAttribute(Associations.EXTERNAL, "");

    List<HtmlExportCategory> categories = new ArrayList<>();
    categories.add(getHtmlCategoryModel(metacard, "Associations", associationsList));

    List<HtmlMetacardModel> metacardModelList = new ArrayList<>();
    metacardModelList.add(new HtmlMetacardModel(metacard, categories));

    HtmlMetacardUtility htmlMetacardUtility = new HtmlMetacardUtility();

    Document doc = Jsoup.parse(htmlMetacardUtility.buildHtml(metacardModelList));

    assertThat(doc.select(METACARD_CLASS).size(), is(metacardModelList.size()));
    assertThat(doc.select(CATEGORY_CLASS).size(), is(categories.size()));
    assertThat(doc.select(CATEGORY_TABLE_CLASS).size(), is(categories.size()));
    assertThat(doc.select(METACARD_ATTRIBUTE_CLASS).size(), is(associationsList.size()));
  }

  @Test
  public void testEmptyAttributeValue() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setAttribute(Associations.RELATED, null);
    metacard.setAttribute(Associations.DERIVED, "");
    metacard.setAttribute(Associations.EXTERNAL, "");

    List<HtmlExportCategory> categories = new ArrayList<>();
    categories.add(getHtmlCategoryModel(metacard, "Associations", associationsList));

    List<HtmlMetacardModel> metacardModelList = new ArrayList<>();
    metacardModelList.add(new HtmlMetacardModel(metacard, categories));

    HtmlMetacardUtility htmlMetacardUtility = new HtmlMetacardUtility();

    Document doc = Jsoup.parse(htmlMetacardUtility.buildHtml(metacardModelList));

    assertThat(doc.select(METACARD_ATTRIBUTE_CLASS).size(), is(associationsList.size()));
    assertThat(doc.select(EMPTY_ATTRIBUTE_CLASS).size(), is(1));
  }

  @Test
  public void testMediaAttributeValue() {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setThumbnail(new byte[] {});

    List<HtmlExportCategory> categories = new ArrayList<>();
    categories.add(getHtmlCategoryModel(metacard, "Core", coreList));

    List<HtmlMetacardModel> metacardModelList = new ArrayList<>();
    metacardModelList.add(new HtmlMetacardModel(metacard, categories));

    HtmlMetacardUtility htmlMetacardUtility = new HtmlMetacardUtility();

    Document doc = Jsoup.parse(htmlMetacardUtility.buildHtml(metacardModelList));

    assertThat(doc.select(MEDIA_ATTRIBUTE_CLASS).size(), is(1));
  }

  private List<HtmlExportCategory> getAllEmptyCategories(String[] categoryTitles) {
    List<HtmlExportCategory> categories = new ArrayList<>();

    for (String title : categoryTitles) {
      categories.add(new HtmlCategoryModel(title, EMPTY_ATTRIBUTE_LIST));
    }

    return categories;
  }

  private HtmlExportCategory getHtmlCategoryModel(
      Metacard metacard, String title, List<String> attributeList) {
    HtmlCategoryModel category = new HtmlCategoryModel(title, attributeList);
    category.applyAttributeMappings(metacard);

    return category;
  }
}
