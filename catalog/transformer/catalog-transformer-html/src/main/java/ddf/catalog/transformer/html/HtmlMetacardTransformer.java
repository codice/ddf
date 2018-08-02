package ddf.catalog.transformer.html;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BinaryContentImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.MetacardTransformer;
import ddf.catalog.transformer.html.models.MetacardModel;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HtmlMetacardTransformer extends HtmlMetacard implements MetacardTransformer {

  @Override
  public BinaryContent transform(Metacard metacard, Map<String, Serializable> map)
      throws CatalogTransformerException {

    if (metacard == null) {
      throw new CatalogTransformerException("Null metacard cannot be transformed to HTML");
    }

    List<MetacardModel> metacardModelList = new ArrayList<>();
    // TODO Add in metacard categories
    metacardModelList.add(new MetacardModel(metacard, new ArrayList<>()));

    String html = buildHtml(metacardModelList);

    if (html == null) {
      throw new CatalogTransformerException("Metacard cannot be transformed to HTML");
    } else {
      return new BinaryContentImpl(new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8)));
    }
  }

}
