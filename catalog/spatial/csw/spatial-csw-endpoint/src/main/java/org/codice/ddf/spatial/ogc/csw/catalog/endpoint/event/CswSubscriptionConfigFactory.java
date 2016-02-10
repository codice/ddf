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
 */
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event;

import java.io.StringReader;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;

import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.CswQueryFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.CswSubscriptionEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.opengis.cat.csw.v_2_0_2.GetRecordsType;

public class CswSubscriptionConfigFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(CswSubscriptionConfigFactory.class);

    /**
     * The ManagedServiceFactory PID for the subscription's callback web service adapter,
     * used to dynamically instantiate the web service's adapter
     */
    public static final String FACTORY_PID = "CSW_Subscription";

    public static final String SUBSCRIPTION_ID = "subscriptionId";

    public static final String FILTER_XML = "filterXml";

    public static final String DELIVERY_METHOD_URL = "deliveryMethodUrl";

    public static final String SUBSCRIPTION_UUID = "subscriptionUuid";

    private String filterXml;

    private String subscriptionId;

    private String deliveryMethodUrl;

    private CswSubscriptionEndpoint subscriptionService;

    public CswSubscriptionConfigFactory(CswSubscriptionEndpoint subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    private void restore() {
        if (isComplete()) {
            try {
                Unmarshaller unmarshaller = CswQueryFactory.getJaxBContext()
                        .createUnmarshaller();
                StringReader sr = new StringReader(filterXml);
                JAXBElement<GetRecordsType> jaxbElement =
                        (JAXBElement<GetRecordsType>) unmarshaller.unmarshal(sr);
                GetRecordsType request = jaxbElement.getValue();
                if (!subscriptionService.hasSubscription(subscriptionId)) {
                    subscriptionService.addOrUpdateSubscription(request, false);
                }
            } catch (Exception e) {
                LOGGER.error("Error restoring subscription: {} with delivery URL: {} XML: {}",
                        new Object[] {subscriptionId, deliveryMethodUrl, filterXml},
                        e);
            }
        }
    }

    /**
     * Helper method to determine if all of the fields of a CswSubscriptionConfigFactory have been set.
     *
     * @return true if all fields of this CswSubscriptionConfigFactory have been set; false otherwise
     */
    private boolean isComplete() {
        return (filterXml != null && !filterXml.isEmpty() &&
                deliveryMethodUrl != null && !deliveryMethodUrl.isEmpty() &&
                subscriptionId != null && !subscriptionId.isEmpty());
    }

    public void setFilterXml(String filterXml) {
        this.filterXml = filterXml;
        restore();
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
        restore();
    }

    public void setDeliveryMethodUrl(String deliveryMethodUrl) {
        this.deliveryMethodUrl = deliveryMethodUrl;
        restore();
    }
}
