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

import java.io.StringReader;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import org.codice.ddf.platform.util.XMLUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.CswQueryFactory;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.CswSubscriptionEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class CswSubscriptionConfigFactory {
  private static final Logger LOGGER = LoggerFactory.getLogger(CswSubscriptionConfigFactory.class);

  /**
   * The ManagedServiceFactory PID for the subscription's callback web service adapter, used to
   * dynamically instantiate the web service's adapter
   */
  public static final String FACTORY_PID = "CSW_Subscription";

  public static final String SUBSCRIPTION_ID = "subscriptionId";

  public static final String FILTER_XML = "filterXml";

  public static final String DELIVERY_METHOD_URL = "deliveryMethodUrl";

  public static final String SUBSCRIPTION_UUID = "subscriptionUuid";

  private final CswSubscriptionEndpoint subscriptionService;

  private String filterXml;

  private String subscriptionId;

  private String deliveryMethodUrl;

  public CswSubscriptionConfigFactory(CswSubscriptionEndpoint subscriptionService) {
    this.subscriptionService = subscriptionService;
  }

  public void restore() {
    try (StringReader sr = new StringReader(filterXml)) {
      SAXParserFactory spf = XMLUtils.getInstance().getSecureSAXParserFactory();
      spf.setNamespaceAware(true);
      Source xmlSource = new SAXSource(spf.newSAXParser().getXMLReader(), new InputSource(sr));
      Unmarshaller unmarshaller = CswQueryFactory.getJaxBContext().createUnmarshaller();
      JAXBElement<GetRecordsType> jaxbElement =
          (JAXBElement<GetRecordsType>) unmarshaller.unmarshal(xmlSource);
      GetRecordsType request = jaxbElement.getValue();
      if (!subscriptionService.hasSubscription(subscriptionId)) {
        subscriptionService.addOrUpdateSubscription(request, false);
      }
    } catch (JAXBException | CswException | ParserConfigurationException | SAXException e) {
      LOGGER.info(
          String.format(
              "Error restoring subscription: %s with delivery URL: %s XML: %s",
              subscriptionId, deliveryMethodUrl, filterXml),
          e);
    }
  }

  public void setFilterXml(String filterXml) {
    this.filterXml = filterXml;
  }

  public void setSubscriptionId(String subscriptionId) {
    this.subscriptionId = subscriptionId;
  }

  public void setDeliveryMethodUrl(String deliveryMethodUrl) {
    this.deliveryMethodUrl = deliveryMethodUrl;
  }
}
