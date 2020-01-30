package org.codice.ddf.catalog.ui.query.utility;

public interface Status {
  long getHits();

  long getElapsed();

  String getId();

  long getCount();

  boolean getSuccessful();
}
