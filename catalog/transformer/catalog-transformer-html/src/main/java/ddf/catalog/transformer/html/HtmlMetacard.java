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

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Options;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.helper.IfHelper;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import ddf.catalog.transformer.html.models.HtmlBasicValueModel;
import ddf.catalog.transformer.html.models.HtmlCategoryModel;
import ddf.catalog.transformer.html.models.HtmlEmptyValueModel;
import ddf.catalog.transformer.html.models.HtmlMediaModel;
import ddf.catalog.transformer.html.models.HtmlMetacardModel;
import java.io.IOException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HtmlMetacard {

  private static final Logger LOGGER = LoggerFactory.getLogger(HtmlMetacardTransformer.class);

  private TemplateLoader templateLoader;

  private Handlebars handlebars;

  private ValueResolver[] resolvers;

  private Template template;

  private static final String TEMPLATE_DIRECTORY = "/templates";

  private static final String TEMPLATE_SUFFIX = ".hbs";

  private static final String HTML_TEMPLATE = "template";

  private List<HtmlCategoryModel> categoryList;

  public HtmlMetacard() {
    this.templateLoader = new ClassPathTemplateLoader();
    this.templateLoader.setPrefix(TEMPLATE_DIRECTORY);
    this.templateLoader.setSuffix(TEMPLATE_SUFFIX);

    this.handlebars = new Handlebars(this.templateLoader);

    this.resolvers = new ValueResolver[] {FieldValueResolver.INSTANCE, MapValueResolver.INSTANCE};

    this.registerHelpers();

    try {
      this.template = this.handlebars.compile(HTML_TEMPLATE);
    } catch (IOException e) {
      LOGGER.error("Failed to compile handlebars template {}", HTML_TEMPLATE, e);
    }
  }

  public HtmlMetacard(List<HtmlCategoryModel> categoryList) {
    this();

    this.categoryList = categoryList;
  }

  private void registerHelpers() {
    handlebars.registerHelper(
        "isBasicValue",
        new IfHelper() {
          public CharSequence apply(Object context, Options options) throws IOException {
            return (context instanceof HtmlBasicValueModel) ? options.fn() : options.inverse();
          }
        });

    handlebars.registerHelper(
        "isEmptyValue",
        new IfHelper() {
          @Override
          public CharSequence apply(Object context, Options options) throws IOException {
            return (context instanceof HtmlEmptyValueModel) ? options.fn() : options.inverse();
          }
        });

    handlebars.registerHelper(
        "isMediaValue",
        new IfHelper() {
          @Override
          public CharSequence apply(Object context, Options options) throws IOException {
            return (context instanceof HtmlMediaModel) ? options.fn() : options.inverse();
          }
        });
  }

  public String buildHtml(List<HtmlMetacardModel> metacardModels) {

    if (metacardModels == null) {
      return null;
    }

    try {
      Context context = Context.newBuilder(metacardModels).resolver(resolvers).build();
      return template.apply(context);
    } catch (IOException e) {
      LOGGER.error("Failed to apply context to {}{}", HTML_TEMPLATE, TEMPLATE_SUFFIX, e);
    }

    return null;
  }

  public void setCategoryList(List<HtmlCategoryModel> categoryList) {
    this.categoryList = categoryList;
  }

  public List<HtmlCategoryModel> getCategoryList() {
    return this.categoryList;
  }
}
