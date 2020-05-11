/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.CswSubscriptionEndpoint;
import org.junit.Before;
import org.junit.Test;

public class CswSubscriptionConfigFactoryTest {

  private CswSubscriptionEndpoint subscriptionService;

  private CswSubscriptionConfigFactory cswSubscriptionConfigFactory;

  private static final String DELIVERY_URL = "http://localhost:8993/test";

  private static final String SUBSCRIPTION_ID = "bc9c7957-17d6-465e-ba0b-40e1c46725ff";

  private String filterXml;

  @Before
  public void setup() throws Exception {

    subscriptionService = mock(CswSubscriptionEndpoint.class);
    cswSubscriptionConfigFactory = new CswSubscriptionConfigFactory(subscriptionService);
    filterXml =
        IOUtils.toString(
            CswSubscriptionConfigFactoryTest.class.getResourceAsStream("/GetRecords.xml"), "UTF-8");
  }

  @Test
  public void testRestoreSubScription() throws Exception {
    cswSubscriptionConfigFactory.setDeliveryMethodUrl(DELIVERY_URL);
    cswSubscriptionConfigFactory.setSubscriptionId(SUBSCRIPTION_ID);
    cswSubscriptionConfigFactory.setFilterXml(filterXml);
    cswSubscriptionConfigFactory.restore();
    verify(subscriptionService).addOrUpdateSubscription(any(GetRecordsType.class), eq(false));
  }

  @Test
  public void testRestoreSubScriptionAlreadyExists() throws Exception {
    when(subscriptionService.hasSubscription(anyString())).thenReturn(true);
    cswSubscriptionConfigFactory.setDeliveryMethodUrl(DELIVERY_URL);
    cswSubscriptionConfigFactory.setSubscriptionId(SUBSCRIPTION_ID);
    cswSubscriptionConfigFactory.setFilterXml(filterXml);
    cswSubscriptionConfigFactory.restore();
    verify(subscriptionService, never())
        .addOrUpdateSubscription(any(GetRecordsType.class), anyBoolean());
  }
}
