package org.codice.ddf.catalog.ui.query.utility;

import ddf.catalog.data.Result;
import java.util.Map;

public interface EndpointUtility {
  Map<String, Result> getMetacardsByTag(String tagStr);
}
