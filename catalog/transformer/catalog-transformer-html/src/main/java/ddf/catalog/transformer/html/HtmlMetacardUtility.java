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
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.context.JavaBeanValueResolver;
import com.github.jknack.handlebars.context.MapValueResolver;
import com.github.jknack.handlebars.context.MethodValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import ddf.catalog.transformer.html.models.HtmlExportCategory;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import javax.activation.MimeType;
import javax.activation.MimeTypeParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Utility class that loads templates and renders html based on object models. */
public class HtmlMetacardUtility {

  private static final Logger LOGGER = LoggerFactory.getLogger(HtmlMetacardUtility.class);

  private TemplateLoader templateLoader;

  private Handlebars handlebars;

  private ValueResolver[] resolvers;

  private Template template;

  private static MimeType mimeType;

  private String htmlTemplate;

  private static final String TEMPLATE_DIRECTORY = "/templates";

  private static final String TEMPLATE_SUFFIX = ".hbs";

  private static final String HTML_TEMPLATE = "template";

  private List<HtmlExportCategory> categoryList;

  static {
    try {
      mimeType = new MimeType("text/html");
    } catch (MimeTypeParseException e) {
      LOGGER.warn("Failed to apply mimetype text/html");
    }
  }

  public HtmlMetacardUtility() {
    this(HTML_TEMPLATE);
  }

  public HtmlMetacardUtility(List<HtmlExportCategory> categoryList) {
    this(HTML_TEMPLATE);
    this.categoryList = categoryList;
  }

  public HtmlMetacardUtility(String htmlTemplate) {
    this.htmlTemplate = htmlTemplate;

    this.templateLoader = new ClassPathTemplateLoader();
    this.templateLoader.setPrefix(TEMPLATE_DIRECTORY);
    this.templateLoader.setSuffix(TEMPLATE_SUFFIX);

    this.handlebars = new Handlebars(this.templateLoader);

    this.resolvers =
        new ValueResolver[] {
          FieldValueResolver.INSTANCE,
          MapValueResolver.INSTANCE,
          JavaBeanValueResolver.INSTANCE,
          MethodValueResolver.INSTANCE
        };

    try {
      this.template = this.handlebars.compile(htmlTemplate);
    } catch (IOException e) {
      LOGGER.warn("Failed to compile handlebars template {}", HTML_TEMPLATE, e);
    }
  }

  public static List<HtmlExportCategory> sortCategoryList(List<HtmlExportCategory> categories) {
    if (categories != null) {
      return categories.stream()
          .sorted(Comparator.comparing(HtmlExportCategory::getTitle))
          .collect(Collectors.toList());
    }

    return null;
  }

  public <T> String buildHtml(T model) {

    if (model == null || template == null) {
      return null;
    }

    try {
      Context context = Context.newBuilder(model).resolver(resolvers).build();
      return template.apply(context);
    } catch (IOException e) {
      LOGGER.warn("Failed to apply model to {}{}", htmlTemplate, TEMPLATE_SUFFIX, e);
    }

    return null;
  }

  public List<HtmlExportCategory> getCategoryList() {
    return this.categoryList;
  }

  public MimeType getMimeType() {
    return this.mimeType;
  }
}
