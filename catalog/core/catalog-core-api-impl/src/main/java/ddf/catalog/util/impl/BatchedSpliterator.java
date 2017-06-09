/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.util.impl;

import java.util.Comparator;
import java.util.Spliterator;
import java.util.function.Consumer;

import ddf.catalog.data.Result;

public class BatchedSpliterator implements Spliterator<Result> {
    private int suggestedPageSize = 64;

    /**
     * Default constructor
     */
    public BatchedSpliterator() {

    }

    /**
     * Overloaded constructor for taking in suggested page size
     * @param suggestedPageSize
     */
    public BatchedSpliterator(int suggestedPageSize) {

        // TODO: 6/9/17 Determine if this matches ideals for implementation
        this.suggestedPageSize = suggestedPageSize;
    }

    @Override
    public void forEachRemaining(Consumer<? super Result> action) {

        // TODO: 6/9/17 Potentially use this method to determine how many results remain after batches made?

    }

    @Override
    public long getExactSizeIfKnown() {
        return 0;
    }

    @Override
    public boolean hasCharacteristics(int characteristics) {
        return false;
    }

    @Override
    public Comparator<? super Result> getComparator() {
        return null;
    }

    @Override
    public boolean tryAdvance(Consumer<? super Result> action) {
        return false;
    }

    /**
     * By default the spliterator would split the batch into halves, but
     * we want to split by pageSize
     * @return
     */
    @Override
    public Spliterator<Result> trySplit() {
        // TODO: 6/9/17 Want to change this to fit pageSize constraints



        return null;
    }

    @Override
    public long estimateSize() {
        return 0;
    }

    @Override
    public int characteristics() {
        return 0;
    }
}
