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

import org.apache.lucene.store.Directory;

public interface ContextualEvaluationCriteria {

    /**
     * The document that is to be stored and searched over.
     * 
     * @return
     */
    public Directory getIndex();

    /**
     * The search phrase which forms the criteria to search over the document
     * 
     * @return
     */
    public String getCriteria();

    public boolean isFuzzy();

    public boolean isCaseSensitiveSearch();

    public String getMetadata();

    public String[] getTextPaths();
}
