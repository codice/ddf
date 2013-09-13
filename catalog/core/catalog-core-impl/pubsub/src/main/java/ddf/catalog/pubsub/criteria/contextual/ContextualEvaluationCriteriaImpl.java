/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/

package ddf.catalog.pubsub.criteria.contextual;

import java.io.IOException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.apache.lucene.store.Directory;
import org.w3c.dom.Document;

public class ContextualEvaluationCriteriaImpl implements ContextualEvaluationCriteria {
    private String criteria;

    private boolean fuzzy;

    private boolean caseSensitiveSearch;

    private String[] textPaths;

    private String metadata;

    private Directory index;

    private static final Logger logger = Logger.getLogger(ContextualEvaluationCriteriaImpl.class);

    public ContextualEvaluationCriteriaImpl(String criteria, boolean fuzzy,
            boolean caseSensitiveSearch, Directory index) {
        super();
        this.criteria = criteria;
        this.fuzzy = fuzzy;
        this.caseSensitiveSearch = caseSensitiveSearch;
        this.textPaths = null;
        this.metadata = null;
        this.index = index;
    }

    public ContextualEvaluationCriteriaImpl(String criteria, boolean fuzzy,
            boolean caseSensitiveSearch, String[] textPaths, String metadata) throws IOException {
        super();

        if (logger.isDebugEnabled()) {
            logger.debug("criteria = " + criteria);
            logger.debug("textPaths:\n");
            for (String textPath : textPaths) {
                logger.debug(textPath);
            }
            // logger.debug( "metadata:\n" + XPathHelper.xmlToString( metadata ) );
        }

        this.criteria = criteria;
        this.fuzzy = fuzzy;
        this.caseSensitiveSearch = caseSensitiveSearch;
        this.metadata = metadata;
        if (textPaths != null) {
            this.textPaths = new String[textPaths.length];
            System.arraycopy(textPaths, 0, this.textPaths, 0, textPaths.length);
        }
        this.index = ContextualEvaluator.buildIndex(metadata, textPaths);
    }

    public String getCriteria() {
        return criteria;
    }

    public Directory getIndex() {
        return index;
    }

    public boolean isFuzzy() {
        return fuzzy;
    }

    public boolean isCaseSensitiveSearch() {
        return caseSensitiveSearch;
    }

    public String[] getTextPaths() {
        return textPaths;
    }

    public String getMetadata() {
        return metadata;
    }

}
