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
package ddf.catalog.data;

import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Default implementation of the {@link Result} interface, which is a
 * {@link Metacard} catalog entry wrapped with the extra attributes
 * {@code relevanceScore} and {@code distanceInMeters}.
 * 
 * @deprecated As of release 2.3.0, replaced by ddf.catalog.data.impl.ResultImpl
 * 
 */
@Deprecated
public class ResultImpl implements Result {

    private Metacard metacard;

    private Double distance;

    private Double relevance;

    /**
     * Default constructor
     */
    public ResultImpl() {
        super();
    }

    /**
     * Instantiates a new metacard result.
     * 
     * @param metacard
     *            the {@link Metacard}
     */
    public ResultImpl(Metacard metacard) {
        this.metacard = metacard;
    }

    /*
     * (non-Javadoc)
     * 
     * @see ddf.catalog.Result#getDistanceInMeters()
     */
    @Override
    public Double getDistanceInMeters() {
        return this.distance;
    }

    /**
     * Sets the distance in meters.
     * 
     * @param distance
     *            the new distance in meters
     */
    public void setDistanceInMeters(Double inDistance) {
        this.distance = inDistance;
    }

    /**
     * @return relevance
     */
    @Override
    public Double getRelevanceScore() {
        return relevance;
    }

    /**
     * Sets the relevance score.
     * 
     * @param relevance
     *            the new relevance score
     */
    public void setRelevanceScore(Double inRelevance) {
        this.relevance = inRelevance;
    }

    /**
     * String representation of this {@code ResultImpl}.
     * 
     * @return the String representation of this {@code ResultImpl}
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public Metacard getMetacard() {
        return metacard;
    }

    /**
     * Sets the {@link Metacard}.
     * 
     * @param metacard
     *            the {@link Metacard}
     */
    public void setMetacard(Metacard metacard) {
        this.metacard = metacard;
    }

}
