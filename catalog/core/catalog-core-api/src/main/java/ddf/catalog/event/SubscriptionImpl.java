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
package ddf.catalog.event;

import java.util.Set;

import org.opengis.filter.Filter;
import org.opengis.filter.FilterVisitor;

/**
 * 
 * @deprecated Use ddf.catalog.event.impl.SubscriptionImpl
 */
@Deprecated
public class SubscriptionImpl implements Subscription {
    private Filter filter;

    private DeliveryMethod dm;

    private Set<String> sourceIds;

    private boolean enterprise;

    public SubscriptionImpl(Filter filter, DeliveryMethod dm, Set<String> sourceIds,
            boolean enterprise) {
        this.filter = filter;
        this.dm = dm;
        this.sourceIds = sourceIds;
        this.enterprise = enterprise;
    }

    @Override
    public boolean evaluate(Object object) {
        return filter.evaluate(object);
    }

    @Override
    public Object accept(FilterVisitor visitor, Object extraData) {
        return filter.accept(visitor, extraData);
    }

    @Override
    public Set<String> getSourceIds() {
        return sourceIds;
    }

    @Override
    public boolean isEnterprise() {
        return enterprise;
    }

    @Override
    public DeliveryMethod getDeliveryMethod() {
        return dm;
    }

}
