package ddf.catalog.transformer.html;

import com.github.jknack.handlebars.Context;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import com.github.jknack.handlebars.ValueResolver;
import com.github.jknack.handlebars.context.FieldValueResolver;
import com.github.jknack.handlebars.io.ClassPathTemplateLoader;
import com.github.jknack.handlebars.io.TemplateLoader;
import ddf.catalog.transformer.html.models.MetacardModel;
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

  public HtmlMetacard() {
    this.templateLoader = new ClassPathTemplateLoader();
    this.templateLoader.setPrefix(TEMPLATE_DIRECTORY);
    this.templateLoader.setSuffix(TEMPLATE_SUFFIX);

    this.handlebars = new Handlebars(this.templateLoader);

    this.resolvers = new ValueResolver[] {
        FieldValueResolver.INSTANCE
    };

    try {
      this.template = this.handlebars.compile(HTML_TEMPLATE);
    } catch (IOException e) {
      LOGGER.error("Failed to compile handlebars template {}", HTML_TEMPLATE, e);
    }
  }

  public String buildHtml(List<MetacardModel> metacardModels) {

    if (metacardModels == null) {
      return null;
    }

    try {
      Context context = Context.newBuilder(metacardModels).resolver(resolvers).build();
      return template.apply(context);
    } catch (IOException e) {
      LOGGER.error("Failed to apply context to {}.{}", HTML_TEMPLATE, TEMPLATE_SUFFIX, e);
    }

    return null;
  }
}
