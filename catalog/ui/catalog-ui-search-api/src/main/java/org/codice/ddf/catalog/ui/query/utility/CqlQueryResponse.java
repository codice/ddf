package org.codice.ddf.catalog.ui.query.utility;

import ddf.catalog.operation.QueryResponse;
import java.util.List;
import java.util.Map;

public interface CqlQueryResponse {

  QueryResponse getQueryResponse();

  List<CqlResult> getResults();

  Map<String, Map<String, MetacardAttribute>> getTypes();

  String getId();

  Status getStatus();
}
