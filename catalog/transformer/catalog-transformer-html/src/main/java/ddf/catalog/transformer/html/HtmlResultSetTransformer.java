package ddf.catalog.transformer.html;

import ddf.catalog.data.BinaryContent;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transform.QueryResponseTransformer;
import java.io.Serializable;
import java.util.Map;

public class HtmlResultSetTransformer implements QueryResponseTransformer {

  @Override
  public BinaryContent transform(SourceResponse sourceResponse, Map<String, Serializable> map)
      throws CatalogTransformerException {
    return null;
  }

}
