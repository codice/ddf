package org.codice.ddf.catalog.ui.events;

import org.apache.solr.common.params.EventParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.AbstractSolrEventListener;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SolrUpdateEventListener extends AbstractSolrEventListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(SolrUpdateEventListener.class);

  public SolrUpdateEventListener(SolrCore core) {
    super(core);
  }

  @Override
  public void init(NamedList args) {
    LOGGER.trace("INIT CALLED");
    LOGGER.trace(args.toString());
  }

  @Override
  public void postCommit() {
    LOGGER.trace("POST COMMIT CALLED");
  }

  @Override
  protected NamedList addEventParms(SolrIndexSearcher currentSearcher, NamedList nlst) {
    NamedList params = new NamedList(nlst.asMap(100));
    if (currentSearcher != null) {
      params.add(EventParams.EVENT, EventParams.NEW_SEARCHER);
    } else {
      params.add(EventParams.EVENT, EventParams.FIRST_SEARCHER);
    }
    params.add("hello", "world");
    return params;
  }
}
