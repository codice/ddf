/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.ui.searchui.query.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ddf.catalog.operation.ProcessingDetails;

public class QueryStatus {

    private String sourceId;

    private Set<ProcessingDetails> details = new HashSet<ProcessingDetails>();

    private List<String> reasons = new ArrayList<>();

    private long resultCount;

    private long hits;

    private State state = State.ACTIVE;

    /* elapsed query time in milliseconds */
    private long elapsedMilliseconds = 0;

    public QueryStatus(String source) {
        sourceId = source;
    }

    public QueryStatus(String source, Set<ProcessingDetails> processingDetails, int totalHits,
            State state) {
        sourceId = source;
        details = processingDetails;
        hits = totalHits;
        this.state = state;
    }

    public long getElapsed() {
        return elapsedMilliseconds;
    }

    public void setElapsed(long elapsed) {
        elapsedMilliseconds = elapsed;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {

        this.state = state;
    }

    public long getResultCount() {
        return resultCount;
    }

    public void setResultCount(long count) {
        resultCount = count;
    }

    public long getHits() {
        return hits;
    }

    public void setHits(long totalHits) {
        hits = totalHits;
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

    public boolean isDone() {
        return !(state == State.ACTIVE);
    }

    public boolean hasReasons() {
        return !reasons.isEmpty();
    }

    public List<String> getReasons() {
        return Collections.unmodifiableList(reasons);
    }

    public void addReason(String reason) {
        this.reasons.add(reason);
    }

    enum State {ACTIVE, SUCCEEDED, FAILED}

}
