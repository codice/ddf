package org.codice.ddf.catalog.ui.query.utility;

import ddf.action.Action;
import java.util.List;
import java.util.Map;

public interface CqlResult {
  Map<String, Object> getMetacard();

  Double getDistance();

  Double getRelevance();

  List<Action> getActions();

  boolean getHasThumbnail();

  boolean getIsResourceLocal();

  Map<String, Integer> getMatches();
}
