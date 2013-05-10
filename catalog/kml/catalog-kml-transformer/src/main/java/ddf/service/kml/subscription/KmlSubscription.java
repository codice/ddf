/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.service.kml.subscription;

import java.util.Set;

import org.opengis.filter.FilterVisitor;

import ddf.catalog.event.Subscription;
import ddf.catalog.operation.Query;

public class KmlSubscription implements Subscription
{

    
    private String subscriptionId;
    private KmlUpdateDeliveryMethod deliveryMethod;
    private Query query;

    public KmlSubscription( String subscriptionId, KmlUpdateDeliveryMethod kmlUpdateDeliveryMethod, Query query )
    {
        this.subscriptionId = subscriptionId;
        this.deliveryMethod = kmlUpdateDeliveryMethod;
        this.query = query;
    }

    @Override
    public KmlUpdateDeliveryMethod getDeliveryMethod()
    {
        return this.deliveryMethod;
    }
    
    public String getSubscriptionId()
    {
        return this.subscriptionId;
    }

	@Override
	public Object accept(FilterVisitor arg0, Object arg1) {
		if(query != null)
		{
			return query.accept(arg0, arg1);
		}
		return null;
	}

	@Override
	public boolean evaluate(Object arg0) {
		if(query != null)
		{
			return query.evaluate(arg0);
		}
		return false;	
	}

	@Override
	public Set<String> getSourceIds() {
		return null;
	}

	@Override
	public boolean isEnterprise() {
		return false;
	}

}
