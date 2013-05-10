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
package ddf.catalog.pubsub;

import java.util.Set;

import javax.security.auth.Subject;

import ddf.catalog.event.DeliveryMethod;
import ddf.catalog.event.Subscription;


public class MockSubscription extends MockQuery implements Subscription 
{
    private DeliveryMethod dm;
    private boolean isEnterprise = false;
    private Set<String> siteNames = null;


    public MockSubscription(Subject user, DeliveryMethod dm) {
        super();
        setDeliveryMethod(dm);
    }

    /**
     * Create a Federated Subscription
     * 
     * @param user
     * @param compoundCriteria
     * @param dm
     * @param siteNames
     *            , null if you want to query the whole Enteprise
     */
    public MockSubscription(Subject user, DeliveryMethod dm, Set<String> siteNames) {
        super();
        setDeliveryMethod(dm);
        if (siteNames == null) {
            super.setIsEnterprise(true);
        } else {
            super.setIsEnterprise(false);
            super.setSiteIds(siteNames);
        }
    }

    private void setDeliveryMethod(DeliveryMethod dm) {
        if (dm == null) {
            throw new IllegalArgumentException("DeliveryMethod must not be null for a Subscription");
        }
        this.dm = dm;
    }

    @Override
    public DeliveryMethod getDeliveryMethod() {
        return dm;
    }

    @Override
    public Set<String> getSourceIds() {
        return siteNames;
    }

    public void setSourceIds(Set<String> siteNames) {
        this.siteNames = siteNames;
    }

    @Override
    public boolean isEnterprise() {
        return isEnterprise;
    }

    public void setIsEnterprise(boolean isEnterprise) {
        this.isEnterprise = isEnterprise;
    }
}
