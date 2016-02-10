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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ddf.catalog.event.DeliveryMethod;
import ddf.catalog.event.Subscription;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;

public class SubscriptionManagerTest {

    Logger logger = (Logger) LoggerFactory.getLogger(SubscriptionManager.class);

    private DeliveryMethod dm;

    private GetRecordsType request;

    private Filter filter;

    private Subscription subscription;

    private String deliveryMethodUrl = "https://localhost:12345/test";

    private BundleContext context;

    private String subscriptionId = UUID.randomUUID()
            .toString();

    private ServiceRegistration<Subscription> serviceRegstration;

    private ServiceReference<Subscription> subscriptionReference;

    private Bundle bundle;

    private Long bundleId = 42L;

    private static final String FILTER_STR = "filter serialized to a string";

    org.osgi.framework.Filter osgiFilter;

    private ServiceReference<ConfigurationAdmin> configAdminRef;

    private ConfigurationAdmin configAdmin;

    private Configuration config;

    private SubscriptionManager subscriptionManager;

    @Before
    public void setUp() throws Exception {
        subscriptionManager = new SubscriptionManager();
        logger.setLevel(Level.DEBUG);
        context = mock(BundleContext.class);
        request = mock(GetRecordsType.class);
        filter = mock(Filter.class);
        dm = mock(DeliveryMethod.class);
        serviceRegstration = mock(ServiceRegistration.class);
        subscriptionReference = mock(ServiceReference.class);
        bundle = mock(Bundle.class);
        osgiFilter = mock(org.osgi.framework.Filter.class);
        configAdminRef = mock(ServiceReference.class);
        configAdmin = mock(ConfigurationAdmin.class);
        config = mock(Configuration.class);
        Configuration[] configArry = {config};

        subscription = new CswSubscription(request, filter, dm, null, true);

        when(osgiFilter.toString()).thenReturn(FILTER_STR);
        doReturn(serviceRegstration).when(context)
                .registerService(eq(Subscription.class.getName()),
                        eq(subscription),
                        any(Dictionary.class));
        doReturn(configAdminRef).when(context)
                .getServiceReference(eq(ConfigurationAdmin.class.getName()));
        when(serviceRegstration.getReference()).thenReturn(subscriptionReference);
        doReturn(bundle).when(subscriptionReference)
                .getBundle();
        when(subscriptionReference.getBundle()).thenReturn(bundle);
        when(bundle.getBundleId()).thenReturn(bundleId);
        when(context.createFilter(anyString())).thenReturn(osgiFilter);
        when(context.getService(eq(configAdminRef))).thenReturn(configAdmin);
        when(context.getService(eq(subscriptionReference))).thenReturn(subscription);
        when(configAdmin.listConfigurations(eq(FILTER_STR))).thenReturn(configArry);

        subscriptionManager.addOrUpdateSubscription(context,
                subscription,
                subscriptionId,
                deliveryMethodUrl);

    }

    @Test
    public void testHasSubscription() throws Exception {
        assertTrue(subscriptionManager.hasSubscription(subscriptionId));

    }

    @Test
    public void testGetSubscriptionUuid() throws Exception {
        assertNotNull(subscriptionManager.getSubscriptionUuid(subscriptionId));
    }

    @Test
    public void testGetSubscription() throws Exception {
        assertNotNull(subscriptionManager.getSubscription(context, subscriptionId));
        verify(context).getService(eq(subscriptionReference));

    }

    @Test
    public void testUpdateSubscription() throws Exception {
        Set<String> sourceIds = new HashSet(Arrays.asList(new String[] {"source1"}));
        CswSubscription updatedSubscription = new CswSubscription(request,
                filter,
                dm,
                sourceIds,
                true);
        subscriptionManager.updateSubscription(context,
                updatedSubscription,
                subscriptionId,
                deliveryMethodUrl);
        verify(serviceRegstration).unregister();
        verify(config).delete();
        verify(context).registerService(eq(Subscription.class.getName()),
                eq(updatedSubscription),
                any(Dictionary.class));
    }

    @Test
    public void testAddSubscription() throws Exception {
        String subId = subscriptionManager.addSubscription(context,
                subscription,
                deliveryMethodUrl);
        assertTrue(subscriptionManager.hasSubscription(subId));
    }

    @Test
    public void testDeleteSubscription() throws Exception {
        subscriptionManager.deleteSubscription(context, subscriptionId);
        verify(serviceRegstration).unregister();
        verify(config).delete();

    }

    @Ignore //remove when persistence is added
    @Test
    public void testPersistSubscription() throws Exception {
        subscriptionManager.persistSubscription(context,
                subscription,
                deliveryMethodUrl,
                subscriptionId,
                subscriptionManager.getSubscriptionUuid(subscriptionId));
        verify(configAdmin).createFactoryConfiguration(anyString(), isNull(String.class));
        verify(config).update();

    }

    @Test
    public void testGetSubscriptionUuidFilter() throws Exception {
        assertNotNull(subscriptionManager.getSubscriptionUuidFilter(subscriptionId));
    }
}