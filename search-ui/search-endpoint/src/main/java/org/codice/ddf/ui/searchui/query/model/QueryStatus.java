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
package org.codice.ddf.ui.searchui.query.model;

import ddf.catalog.operation.ProcessingDetails;

import java.util.HashSet;
import java.util.Set;

public class QueryStatus {

    private String sourceId;

    private Set<ProcessingDetails> details = new HashSet<ProcessingDetails>();

    private long totalHits;

    private boolean isDone = false;

    public long getElapsed() {
        return elapsedMilliseconds;
    }

    public void setElapsed(long elapsed) {
        elapsedMilliseconds = elapsed;
    }

    /* elapsed query time in milliseconds */
    private long elapsedMilliseconds = 0;

    public QueryStatus(String source) {
        sourceId = source;
    }

    public QueryStatus(String source, Set<ProcessingDetails> processingDetails, int hits,
            boolean done) {
        sourceId = source;
        details = processingDetails;
        totalHits = hits;
        isDone = done;
    }

    public boolean isDone() {
        return isDone;
    }

    public void setDone(boolean done) {
        isDone = done;
    }

    public long getHits() {
        return totalHits;
    }

    public void setHits(long hits) {
        totalHits = hits;
    }

    public Set<ProcessingDetails> getDetails() {
        return details;
    }

    public void setDetails(Set<ProcessingDetails> processingDetails) {
        details = processingDetails;
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public boolean isSuccessful() {
        for (ProcessingDetails detail : details) {
            if (detail.hasException()) {
                return false;
            }
        }

        return true;
    }
}
